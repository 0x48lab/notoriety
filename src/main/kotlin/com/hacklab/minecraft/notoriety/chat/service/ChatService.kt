package com.hacklab.minecraft.notoriety.chat.service

import com.hacklab.minecraft.notoriety.chat.model.ChatMode
import com.hacklab.minecraft.notoriety.chat.model.PlayerChatSettings
import com.hacklab.minecraft.notoriety.chat.repository.ChatSettingsRepository
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.reputation.NameColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatService(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val guildService: GuildService,
    private val playerManager: PlayerManager,
    private val titleProvider: (UUID) -> String?  // NotorietyService.getLocalizedTitle
) {
    companion object {
        const val LOCAL_CHAT_RANGE = 50.0
    }

    // プレイヤー設定のキャッシュ
    private val settingsCache = ConcurrentHashMap<UUID, PlayerChatSettings>()

    fun getSettings(playerUuid: UUID): PlayerChatSettings {
        return settingsCache.getOrPut(playerUuid) {
            chatSettingsRepository.getOrCreate(playerUuid)
        }
    }

    fun loadSettings(playerUuid: UUID) {
        settingsCache[playerUuid] = chatSettingsRepository.getOrCreate(playerUuid)
    }

    fun unloadSettings(playerUuid: UUID) {
        settingsCache.remove(playerUuid)
    }

    fun setChatMode(player: Player, mode: ChatMode): Boolean {
        // ギルドチャットはギルドメンバーのみ
        if (mode == ChatMode.GUILD) {
            val guild = guildService.getPlayerGuild(player.uniqueId)
            if (guild == null) {
                player.sendMessage(Component.text("You must be in a guild to use guild chat")
                    .color(NamedTextColor.RED))
                return false
            }
        }

        chatSettingsRepository.updateChatMode(player.uniqueId, mode)
        val settings = getSettings(player.uniqueId).copy(chatMode = mode)
        settingsCache[player.uniqueId] = settings

        val modeDisplay = when (mode) {
            ChatMode.LOCAL -> "Local (50 blocks)"
            ChatMode.GLOBAL -> "Global (!)"
            ChatMode.GUILD -> "Guild (@)"
        }
        player.sendMessage(Component.text("Chat mode set to: $modeDisplay")
            .color(NamedTextColor.GREEN))

        return true
    }

    fun setRomajiEnabled(player: Player, enabled: Boolean) {
        chatSettingsRepository.updateRomajiEnabled(player.uniqueId, enabled)
        val settings = getSettings(player.uniqueId).copy(romajiEnabled = enabled)
        settingsCache[player.uniqueId] = settings

        if (enabled) {
            player.sendMessage(Component.text("Romaji conversion enabled")
                .color(NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("Romaji conversion disabled")
                .color(NamedTextColor.YELLOW))
        }
    }

    fun toggleRomaji(player: Player) {
        val settings = getSettings(player.uniqueId)
        setRomajiEnabled(player, !settings.romajiEnabled)
    }

    fun setWarningsEnabled(player: Player, enabled: Boolean) {
        chatSettingsRepository.updateWarningsEnabled(player.uniqueId, enabled)
        val settings = getSettings(player.uniqueId).copy(warningsEnabled = enabled)
        settingsCache[player.uniqueId] = settings

        if (enabled) {
            player.sendMessage(Component.text("Crime warnings enabled")
                .color(NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("Crime warnings disabled")
                .color(NamedTextColor.YELLOW))
        }
    }

    fun toggleWarnings(player: Player) {
        val settings = getSettings(player.uniqueId)
        setWarningsEnabled(player, !settings.warningsEnabled)
    }

    fun isWarningsEnabled(playerUuid: UUID): Boolean {
        return getSettings(playerUuid).warningsEnabled
    }

    fun getEffectiveChatMode(player: Player, message: String): ChatMode {
        // プレフィックスで一時的にモード変更
        ChatMode.fromPrefix(message)?.let { return it }

        // デフォルトのチャットモードを使用
        return getSettings(player.uniqueId).chatMode
    }

    fun stripChatPrefix(message: String, mode: ChatMode): String {
        return if (mode != ChatMode.LOCAL && message.startsWith(mode.prefix)) {
            message.removePrefix(mode.prefix).trimStart()
        } else {
            message
        }
    }

    fun formatMessage(player: Player, message: String, mode: ChatMode): Component {
        val guild = guildService.getPlayerGuild(player.uniqueId)
        val membership = guildService.getMembership(player.uniqueId)

        return when (mode) {
            ChatMode.LOCAL -> formatLocalMessage(player, message, guild)
            ChatMode.GLOBAL -> formatGlobalMessage(player, message, guild)
            ChatMode.GUILD -> formatGuildMessage(player, message, guild)
        }
    }

    private fun formatLocalMessage(player: Player, message: String, guild: Guild?): Component {
        val nameColor = getPlayerNameColor(player)
        return Component.text()
            .append(formatPlayerPrefix(player, guild))
            .append(Component.text(player.name).color(nameColor))
            .append(Component.text(": ").color(NamedTextColor.GRAY))
            .append(Component.text(message).color(NamedTextColor.WHITE))
            .build()
    }

    private fun formatGlobalMessage(player: Player, message: String, guild: Guild?): Component {
        val nameColor = getPlayerNameColor(player)
        return Component.text()
            .append(Component.text("[G] ").color(NamedTextColor.YELLOW))
            .append(formatPlayerPrefix(player, guild))
            .append(Component.text(player.name).color(nameColor))
            .append(Component.text(": ").color(NamedTextColor.GRAY))
            .append(Component.text(message).color(NamedTextColor.WHITE))
            .build()
    }

    private fun formatGuildMessage(player: Player, message: String, guild: Guild?): Component {
        if (guild == null) {
            return Component.text("[Error] Not in a guild").color(NamedTextColor.RED)
        }

        val nameColor = getPlayerNameColor(player)
        return Component.text()
            .append(Component.text("[Guild] ").color(NamedTextColor.GREEN))
            .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
            .append(Component.text(player.name).color(nameColor))
            .append(Component.text(": ").color(NamedTextColor.GRAY))
            .append(Component.text(message).color(NamedTextColor.GREEN))
            .build()
    }

    /**
     * プレイヤーのプレフィックスを構築（称号 + ギルドタグ）
     */
    private fun formatPlayerPrefix(player: Player, guild: Guild?): Component {
        val builder = Component.text()

        // 称号を追加（ローカライズ済み）
        val title = titleProvider(player.uniqueId)
        if (title != null) {
            val data = playerManager.getPlayer(player)
            val titleColor = when (data?.getNameColor()) {
                NameColor.BLUE -> NamedTextColor.AQUA
                NameColor.GRAY -> NamedTextColor.GRAY
                NameColor.RED -> NamedTextColor.DARK_RED
                null -> NamedTextColor.AQUA
            }
            builder.append(Component.text("$title ").color(titleColor))
        }

        // ギルドタグを追加
        if (guild != null) {
            builder.append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
        }

        return builder.build()
    }

    /**
     * プレイヤーの名前色を取得
     */
    private fun getPlayerNameColor(player: Player): NamedTextColor {
        val data = playerManager.getPlayer(player) ?: return NamedTextColor.BLUE
        return data.getNameColor().chatColor
    }

    fun getLocalRecipients(player: Player): Set<Player> {
        val location = player.location
        return player.world.players
            .filter { it.location.distance(location) <= LOCAL_CHAT_RANGE }
            .toSet()
    }

    fun getGuildRecipients(player: Player): Set<Player> {
        val guild = guildService.getPlayerGuild(player.uniqueId) ?: return emptySet()
        val members = guildService.getMembers(guild.id, 0, 1000)
        return members.mapNotNull { member ->
            player.server.getPlayer(member.playerUuid)
        }.toSet()
    }

    fun resetGuildChatMode(memberUuids: List<UUID>) {
        chatSettingsRepository.resetGuildChatMode(memberUuids)
        memberUuids.forEach { uuid ->
            settingsCache[uuid]?.let { settings ->
                if (settings.chatMode == ChatMode.GUILD) {
                    settingsCache[uuid] = settings.copy(chatMode = ChatMode.LOCAL)
                }
            }
        }
    }
}
