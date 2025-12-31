package com.hacklab.minecraft.notoriety.chat.command

import com.hacklab.minecraft.notoriety.chat.model.ChatMode
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ChatCommand(
    private val chatService: ChatService
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        return handleCommand(sender, args)
    }

    /**
     * コマンド処理のコア部分（ラッパーからも呼び出し可能）
     */
    fun handleCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players")
                .color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "local", "l" -> chatService.setChatMode(sender, ChatMode.LOCAL)
            "global", "g" -> chatService.setChatMode(sender, ChatMode.GLOBAL)
            "guild" -> chatService.setChatMode(sender, ChatMode.GUILD)
            "romaji", "r" -> {
                if (args.size > 1) {
                    when (args[1].lowercase()) {
                        "on", "enable" -> chatService.setRomajiEnabled(sender, true)
                        "off", "disable" -> chatService.setRomajiEnabled(sender, false)
                        else -> chatService.toggleRomaji(sender)
                    }
                } else {
                    chatService.toggleRomaji(sender)
                }
            }
            "warning", "warnings", "w" -> {
                if (args.size > 1) {
                    when (args[1].lowercase()) {
                        "on", "enable" -> chatService.setWarningsEnabled(sender, true)
                        "off", "disable" -> chatService.setWarningsEnabled(sender, false)
                        else -> chatService.toggleWarnings(sender)
                    }
                } else {
                    chatService.toggleWarnings(sender)
                }
            }
            "status" -> showStatus(sender)
            else -> showUsage(sender)
        }

        return true
    }

    private fun showUsage(player: Player) {
        player.sendMessage(Component.text("=== Chat Commands ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("/chat local").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Local chat (50 blocks)").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/chat global").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Global chat (! prefix)").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/chat guild").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Guild chat (@ prefix)").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/chat romaji").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Toggle romaji conversion").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/chat warning").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Toggle crime warnings").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/chat status").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Show current settings").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("").color(NamedTextColor.GRAY))
        player.sendMessage(Component.text("Tip: Use ! or @ prefix to temporarily switch modes")
            .color(NamedTextColor.GRAY))
    }

    private fun showStatus(player: Player) {
        val settings = chatService.getSettings(player.uniqueId)
        val modeDisplay = when (settings.chatMode) {
            ChatMode.LOCAL -> "Local (50 blocks)"
            ChatMode.GLOBAL -> "Global"
            ChatMode.GUILD -> "Guild"
        }

        player.sendMessage(Component.text("=== Chat Settings ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("Mode: ").color(NamedTextColor.GRAY)
            .append(Component.text(modeDisplay).color(NamedTextColor.WHITE)))
        player.sendMessage(Component.text("Romaji: ").color(NamedTextColor.GRAY)
            .append(Component.text(if (settings.romajiEnabled) "Enabled" else "Disabled")
                .color(if (settings.romajiEnabled) NamedTextColor.GREEN else NamedTextColor.RED)))
        player.sendMessage(Component.text("Warnings: ").color(NamedTextColor.GRAY)
            .append(Component.text(if (settings.warningsEnabled) "Enabled" else "Disabled")
                .color(if (settings.warningsEnabled) NamedTextColor.GREEN else NamedTextColor.RED)))
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return handleTabComplete(sender, args)
    }

    /**
     * タブ補完処理のコア部分（ラッパーからも呼び出し可能）
     */
    fun handleTabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val options = listOf("local", "global", "guild", "romaji", "warning", "status")
            return options.filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() in listOf("romaji", "warning", "warnings")) {
            return listOf("on", "off").filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }
}
