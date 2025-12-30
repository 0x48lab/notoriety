package com.hacklab.minecraft.notoriety.chat

import com.hacklab.minecraft.notoriety.chat.model.ChatMode
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChatListener(
    private val chatService: ChatService,
    private val guildService: GuildService
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        chatService.loadSettings(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        chatService.unloadSettings(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val originalMessage = PlainTextComponentSerializer.plainText().serialize(event.message())

        // 有効なチャットモードを取得
        val mode = chatService.getEffectiveChatMode(player, originalMessage)

        // ギルドチャットでギルドに所属していない場合
        if (mode == ChatMode.GUILD && guildService.getPlayerGuild(player.uniqueId) == null) {
            event.isCancelled = true
            player.sendMessage(Component.text("You must be in a guild to use guild chat")
                .color(NamedTextColor.RED))
            return
        }

        // プレフィックスを削除
        var message = chatService.stripChatPrefix(originalMessage, mode)

        if (message.isBlank()) {
            event.isCancelled = true
            return
        }

        // ローマ字変換（有効な場合）
        val settings = chatService.getSettings(player.uniqueId)
        if (settings.romajiEnabled) {
            message = RomajiConverter.convert(message)
        }

        // メッセージをフォーマット
        val formattedMessage = chatService.formatMessage(player, message, mode)

        // 受信者を決定
        val recipients = when (mode) {
            ChatMode.LOCAL -> chatService.getLocalRecipients(player)
            ChatMode.GLOBAL -> player.server.onlinePlayers.toSet()
            ChatMode.GUILD -> chatService.getGuildRecipients(player)
        }

        // イベントをキャンセルしてカスタム送信
        event.isCancelled = true

        // 受信者にメッセージを送信
        recipients.forEach { recipient ->
            recipient.sendMessage(formattedMessage)
        }

        // コンソールにもログ
        val modePrefix = when (mode) {
            ChatMode.LOCAL -> "[Local]"
            ChatMode.GLOBAL -> "[Global]"
            ChatMode.GUILD -> "[Guild]"
        }
        player.server.consoleSender.sendMessage(
            Component.text("$modePrefix ${player.name}: $message")
        )

        // ローカルチャットで誰にも聞こえなかった場合の通知
        if (mode == ChatMode.LOCAL && recipients.size == 1) {
            player.sendMessage(Component.text("No one is nearby to hear you...")
                .color(NamedTextColor.GRAY))
        }
    }
}
