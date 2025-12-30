package com.hacklab.minecraft.notoriety.chat.command

import com.hacklab.minecraft.notoriety.chat.RomajiConverter
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ウィスパー（プライベートメッセージ）コマンド
 * /w <player> <message> - 指定プレイヤーにメッセージを送信
 * /r <message> - 最後にウィスパーを送ってきたプレイヤーに返信
 */
class WhisperCommand(
    private val chatService: ChatService
) : CommandExecutor, TabCompleter {

    // 最後にウィスパーを送ってきたプレイヤーを記録（返信用）
    private val lastWhisperFrom = ConcurrentHashMap<UUID, UUID>()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players")
                .color(NamedTextColor.RED))
            return true
        }

        return when (label.lowercase()) {
            "w", "whisper", "msg", "tell" -> handleWhisper(sender, args)
            "r", "reply" -> handleReply(sender, args)
            else -> false
        }
    }

    private fun handleWhisper(sender: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /w <player> <message>")
                .color(NamedTextColor.RED))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /w <player> <message>")
                .color(NamedTextColor.RED))
            return true
        }

        val targetName = args[0]
        val target = Bukkit.getPlayer(targetName)

        if (target == null) {
            sender.sendMessage(Component.text("Player '$targetName' is not online")
                .color(NamedTextColor.RED))
            return true
        }

        if (target == sender) {
            sender.sendMessage(Component.text("You cannot whisper to yourself")
                .color(NamedTextColor.RED))
            return true
        }

        val message = args.drop(1).joinToString(" ")
        sendWhisper(sender, target, message)
        return true
    }

    private fun handleReply(sender: Player, args: Array<out String>): Boolean {
        val lastSender = lastWhisperFrom[sender.uniqueId]

        if (lastSender == null) {
            sender.sendMessage(Component.text("You have no one to reply to")
                .color(NamedTextColor.RED))
            return true
        }

        val target = Bukkit.getPlayer(lastSender)
        if (target == null) {
            sender.sendMessage(Component.text("That player is no longer online")
                .color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /r <message>")
                .color(NamedTextColor.RED))
            return true
        }

        val message = args.joinToString(" ")
        sendWhisper(sender, target, message)
        return true
    }

    private fun sendWhisper(sender: Player, target: Player, message: String) {
        // ローマ字変換（有効な場合）
        val settings = chatService.getSettings(sender.uniqueId)
        val finalMessage = if (settings.romajiEnabled) {
            RomajiConverter.convert(message)
        } else {
            message
        }

        // 送信者への表示
        val toMessage = Component.text("[To ")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, true)
            .append(Component.text(target.name).color(NamedTextColor.WHITE))
            .append(Component.text("] ").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.text(finalMessage).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false))

        // 受信者への表示
        val fromMessage = Component.text("[From ")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, true)
            .append(Component.text(sender.name).color(NamedTextColor.WHITE))
            .append(Component.text("] ").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.text(finalMessage).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false))

        sender.sendMessage(toMessage)
        target.sendMessage(fromMessage)

        // 返信用に記録
        lastWhisperFrom[target.uniqueId] = sender.uniqueId

        // コンソールにもログ
        sender.server.consoleSender.sendMessage(
            Component.text("[Whisper] ${sender.name} -> ${target.name}: $finalMessage")
        )
    }

    /**
     * プレイヤーがログアウトした時にキャッシュをクリア
     */
    fun onPlayerQuit(playerUuid: UUID) {
        lastWhisperFrom.remove(playerUuid)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (label.lowercase() in listOf("w", "whisper", "msg", "tell")) {
            if (args.size == 1) {
                return Bukkit.getOnlinePlayers()
                    .filter { it != sender }
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[0].lowercase()) }
            }
        }
        return emptyList()
    }
}
