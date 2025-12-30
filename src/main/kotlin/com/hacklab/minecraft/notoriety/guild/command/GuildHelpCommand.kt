package com.hacklab.minecraft.notoriety.guild.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildHelpCommand(
    private val subCommands: Map<String, GuildSubCommand>
) : GuildSubCommand {

    override val name = "help"
    override val description = "Show guild command help"
    override val usage = "/guild help [command]"
    override val aliases = listOf("?", "h")
    override val requiresPlayer = false

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            // 特定のコマンドのヘルプ
            val cmdName = args[0].lowercase()
            val subCommand = subCommands[cmdName]

            if (subCommand == null) {
                sender.sendMessage(Component.text("Unknown command: $cmdName")
                    .color(NamedTextColor.RED))
                return true
            }

            sender.sendMessage(Component.text("=== /guild ${subCommand.name} ===")
                .color(NamedTextColor.GOLD))
            sender.sendMessage(Component.text("Description: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(subCommand.description).color(NamedTextColor.WHITE)))
            sender.sendMessage(Component.text("Usage: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(subCommand.usage).color(NamedTextColor.WHITE)))
            if (subCommand.aliases.isNotEmpty()) {
                sender.sendMessage(Component.text("Aliases: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(subCommand.aliases.joinToString(", ")).color(NamedTextColor.WHITE)))
            }
            return true
        }

        // 全コマンド一覧
        sender.sendMessage(Component.text("=== Guild Commands ===").color(NamedTextColor.GOLD))

        // カテゴリ別に表示
        val categories = mapOf(
            "General" to listOf("help", "info", "list", "members"),
            "Membership" to listOf("create", "leave", "invite", "accept", "deny", "invites"),
            "Management" to listOf("kick", "promote", "demote", "transfer", "color", "dissolve")
        )

        categories.forEach { (category, commands) ->
            sender.sendMessage(Component.text("$category:").color(NamedTextColor.AQUA))
            commands.forEach { cmdName ->
                val cmd = subCommands[cmdName] ?: return@forEach
                sender.sendMessage(Component.text()
                    .append(Component.text("  /$cmdName")
                        .color(NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.suggestCommand("/guild $cmdName ")))
                    .append(Component.text(" - ${cmd.description}").color(NamedTextColor.GRAY))
                    .build())
            }
        }

        if (sender is Player) {
            sender.sendMessage(Component.text()
                .color(NamedTextColor.GRAY)
                .append(Component.text("Tip: Click a command to use it"))
                .build())
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            return subCommands.keys.filter { it.startsWith(input) }.sorted()
        }
        return emptyList()
    }
}
