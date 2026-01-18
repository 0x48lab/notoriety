package com.hacklab.minecraft.notoriety.guild.listener

import com.hacklab.minecraft.notoriety.NotorietyService
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.display.GuildTagManager
import com.hacklab.minecraft.notoriety.guild.event.*
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.TerritoryReleaseEvent
import com.hacklab.minecraft.notoriety.territory.service.ReleaseReason
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class GuildEventListener(
    private val guildService: GuildService,
    private val chatService: ChatService,
    private val guildTagManager: GuildTagManager,
    private val notorietyService: NotorietyService,
    private val territoryService: TerritoryService? = null,
    private val i18n: I18nManager? = null
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildDissolve(event: GuildDissolveEvent) {
        // ギルド解散時、メンバーのチャットモードをリセット
        val members = event.formerMembers
        chatService.resetGuildChatMode(members)

        // 領地を解放（ギルド解散時）
        territoryService?.let { service ->
            val territory = service.getTerritory(event.guild.id)
            if (territory != null && territory.chunkCount > 0) {
                // 領地解放イベントを発火
                Bukkit.getPluginManager().callEvent(
                    TerritoryReleaseEvent(
                        guildId = event.guild.id,
                        guildName = event.guild.name,
                        releasedChunks = territory.chunks.toList(),
                        reason = ReleaseReason.GUILD_DISSOLVED,
                        remainingChunks = 0
                    )
                )
                // 領地を完全に解放（ビーコン削除を含む）
                service.shrinkTerritoryTo(event.guild.id, 0)
            }
        }

        // 全メンバーに通知
        members.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.sendMessage(Component.text()
                    .append(Component.text("Your guild ").color(NamedTextColor.RED))
                    .append(Component.text("[${event.guild.tag}] ").color(event.guild.tagColor.namedTextColor))
                    .append(Component.text(event.guild.name).color(NamedTextColor.WHITE))
                    .append(Component.text(" has been dissolved").color(NamedTextColor.RED))
                    .build())

                // 表示を更新
                notorietyService.updateDisplay(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        // ギルドチャットモードをリセット
        chatService.resetGuildChatMode(listOf(event.memberUuid))

        // 離脱/追放されたプレイヤーの表示を更新
        Bukkit.getPlayer(event.memberUuid)?.let { player ->
            notorietyService.updateDisplay(player)
        }

        // ギルドメンバーに通知（解散以外）
        if (event.reason != LeaveReason.DISSOLVED) {
            val memberName = Bukkit.getOfflinePlayer(event.memberUuid).name ?: "Unknown"
            val message = when (event.reason) {
                LeaveReason.LEAVE -> Component.text("$memberName has left the guild")
                    .color(NamedTextColor.YELLOW)
                LeaveReason.KICKED -> Component.text("$memberName has been kicked from the guild")
                    .color(NamedTextColor.RED)
                LeaveReason.DISSOLVED -> Component.empty()
            }

            if (message != Component.empty()) {
                val members = guildService.getMembers(event.guild.id, 0, 1000)
                members.forEach { member ->
                    Bukkit.getPlayer(member.playerUuid)?.sendMessage(message)
                }
            }

            // 領地縮小チェック（メンバー減少時）
            checkTerritoryShrink(event.guild.id, event.guild.name)
        }
    }

    /**
     * メンバー減少時に領地縮小が必要かチェックして実行
     */
    private fun checkTerritoryShrink(guildId: Long, guildName: String) {
        val service = territoryService ?: return
        val territory = service.getTerritory(guildId) ?: return
        if (territory.chunkCount == 0) return

        val memberCount = guildService.getMemberCount(guildId)
        val allowedChunks = service.calculateAllowedChunks(memberCount)

        // メンバー数が10人未満になった場合、領地を完全解放
        if (memberCount < TerritoryService.MIN_MEMBERS_FOR_TERRITORY) {
            // 全領地を解放
            Bukkit.getPluginManager().callEvent(
                TerritoryReleaseEvent(
                    guildId = guildId,
                    guildName = guildName,
                    releasedChunks = territory.chunks.toList(),
                    reason = ReleaseReason.MEMBER_DECREASE,
                    remainingChunks = 0
                )
            )
            service.shrinkTerritoryTo(guildId, 0)

            // オンラインのギルドメンバーに通知
            notifyTerritoryChange(guildId, 0, territory.chunkCount)
        } else if (territory.chunkCount > allowedChunks) {
            // チャンク数を縮小
            val chunksToRemove = territory.chunks
                .sortedByDescending { it.addOrder }
                .take(territory.chunkCount - allowedChunks)

            Bukkit.getPluginManager().callEvent(
                TerritoryReleaseEvent(
                    guildId = guildId,
                    guildName = guildName,
                    releasedChunks = chunksToRemove,
                    reason = ReleaseReason.MEMBER_DECREASE,
                    remainingChunks = allowedChunks
                )
            )
            service.shrinkTerritoryTo(guildId, allowedChunks)

            // オンラインのギルドメンバーに通知
            notifyTerritoryChange(guildId, allowedChunks, territory.chunkCount)
        }
    }

    /**
     * 領地縮小をギルドメンバーに通知
     */
    private fun notifyTerritoryChange(guildId: Long, newChunkCount: Int, oldChunkCount: Int) {
        val members = guildService.getMembers(guildId, 0, 1000)

        members.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.let { player ->
                if (newChunkCount == 0) {
                    i18n?.sendError(player, "territory.shrink_all_released",
                        "Warning: All territory has been released due to insufficient members")
                        ?: player.sendMessage(Component.text("Warning: All territory has been released due to insufficient members").color(NamedTextColor.RED))
                } else {
                    i18n?.sendError(player, "territory.shrink_warning",
                        "Warning: Territory shrunk to %d chunks due to insufficient members",
                        newChunkCount)
                        ?: player.sendMessage(Component.text("Warning: Territory shrunk to $newChunkCount chunks due to insufficient members").color(NamedTextColor.RED))
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        // ギルドタグを表示
        guildTagManager.setGuildTag(event.member, event.guild)

        // 表示を更新
        notorietyService.updateDisplay(event.member)

        // ギルドメンバーに通知
        val members = guildService.getMembers(event.guild.id, 0, 1000)
        members.filter { it.playerUuid != event.member.uniqueId }.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.sendMessage(
                Component.text("${event.member.name} has joined the guild")
                    .color(NamedTextColor.GREEN)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildRoleChange(event: GuildRoleChangeEvent) {
        val player = Bukkit.getPlayer(event.memberUuid) ?: return
        val memberName = player.name

        // 役職変更をギルドメンバーに通知
        val message = Component.text()
            .append(Component.text(memberName).color(NamedTextColor.WHITE))
            .append(Component.text(" has been ").color(NamedTextColor.GRAY))
            .append(Component.text(
                if (event.newRole.level > event.oldRole.level) "promoted" else "demoted"
            ).color(if (event.newRole.level > event.oldRole.level) NamedTextColor.GREEN else NamedTextColor.YELLOW))
            .append(Component.text(" to ").color(NamedTextColor.GRAY))
            .append(Component.text(event.newRole.name).color(NamedTextColor.GOLD))
            .build()

        val members = guildService.getMembers(event.guild.id, 0, 1000)
        members.filter { it.playerUuid != event.memberUuid }.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.sendMessage(message)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildCreate(event: GuildCreateEvent) {
        val player = event.creator ?: return

        // 表示を更新
        notorietyService.updateDisplay(player)

        // サーバーにアナウンス
        Bukkit.broadcast(Component.text()
            .append(Component.text("Guild ").color(NamedTextColor.GOLD))
            .append(Component.text("[${event.guild.tag}] ").color(event.guild.tagColor.namedTextColor))
            .append(Component.text(event.guild.name).color(NamedTextColor.WHITE))
            .append(Component.text(" has been created by ${player.name}!").color(NamedTextColor.GOLD))
            .build())
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val guild = guildService.getPlayerGuild(player.uniqueId) ?: return

        // ギルドタグを設定
        guildTagManager.setGuildTag(player, guild)

        // ギルドメンバーに通知
        val members = guildService.getMembers(guild.id, 0, 1000)
        members.filter { it.playerUuid != player.uniqueId }.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.sendMessage(
                Component.text("[Guild] ${player.name} is now online")
                    .color(NamedTextColor.GREEN)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildApplication(event: GuildApplicationEvent) {
        val applicantName = Bukkit.getOfflinePlayer(event.applicantUuid).name ?: "Unknown"
        val guildName = event.guild.name

        // GMと副GMを取得
        val members = guildService.getMembers(event.guild.id, 0, 1000)
        val managers = members.filter {
            it.role == GuildRole.MASTER || it.role == GuildRole.VICE_MASTER
        }

        // 通知メッセージを作成
        managers.forEach { manager ->
            Bukkit.getPlayer(manager.playerUuid)?.let { player ->
                val message = i18n?.get(
                    player.uniqueId,
                    "guild.application_received",
                    "%s has applied to join %s",
                    applicantName,
                    guildName
                ) ?: "$applicantName has applied to join $guildName"

                player.sendMessage(Component.text()
                    .append(Component.text("[Guild] ").color(NamedTextColor.GOLD))
                    .append(Component.text(message).color(NamedTextColor.AQUA))
                    .build())

                // クリック可能な案内メッセージ
                player.sendMessage(Component.text()
                    .append(Component.text("  → ").color(NamedTextColor.GRAY))
                    .append(Component.text("/guild menu").color(NamedTextColor.YELLOW))
                    .append(Component.text(" で確認できます").color(NamedTextColor.GRAY))
                    .build())
            }
        }
    }
}
