package com.hacklab.minecraft.notoriety.guild.listener

import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.guild.display.GuildTagManager
import com.hacklab.minecraft.notoriety.guild.event.*
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.reputation.ReputationService
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
    private val reputationService: ReputationService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildDissolve(event: GuildDissolveEvent) {
        // ギルド解散時、メンバーのチャットモードをリセット
        val members = event.formerMembers
        chatService.resetGuildChatMode(members)

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
                reputationService.updateDisplay(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        // ギルドチャットモードをリセット
        chatService.resetGuildChatMode(listOf(event.memberUuid))

        // 離脱/追放されたプレイヤーの表示を更新
        Bukkit.getPlayer(event.memberUuid)?.let { player ->
            reputationService.updateDisplay(player)
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
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        // ギルドタグを表示
        guildTagManager.setGuildTag(event.member, event.guild)

        // 表示を更新
        reputationService.updateDisplay(event.member)

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
        reputationService.updateDisplay(player)

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
}
