package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.inspect.InspectCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class NotorietyCommand(private val plugin: Notoriety) : CommandExecutor, TabCompleter {

    private val subCommands: Map<String, SubCommand> by lazy {
        mapOf(
            "status" to StatusCommand(plugin),
            "history" to HistoryCommand(plugin),
            "bounty" to BountyCommand(plugin),
            "trust" to TrustCommand(plugin),
            "admin" to AdminCommand(plugin),
            "inspect" to InspectCommand(plugin.inspectService, plugin.inspectionStick, plugin.i18nManager),
            "locale" to LocaleCommand(plugin),
            "guild" to GuildCommandWrapper(plugin),
            "chat" to ChatCommandWrapper(plugin.chatService)
        )
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        val subCommand = subCommands[args[0].lowercase()]
        if (subCommand == null) {
            sender.sendMessage("Unknown subcommand: ${args[0]}")
            showHelp(sender)
            return true
        }

        return subCommand.execute(sender, args.drop(1).toTypedArray())
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return subCommands.keys.filter { it.startsWith(args[0].lowercase()) }
        }

        val subCommand = subCommands[args[0].lowercase()]
        return subCommand?.tabComplete(sender, args.drop(1).toTypedArray()) ?: emptyList()
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("=== Notoriety Commands ===")
        sender.sendMessage("/noty status [player] - Show reputation status")
        sender.sendMessage("/noty history [player] - Show crime history")
        sender.sendMessage("/noty bounty <set|list|check> - Bounty system")
        sender.sendMessage("/noty trust <add|remove|list|check> - Trust system")
        sender.sendMessage("/noty guild <subcommand> - Guild system")
        sender.sendMessage("/noty chat <mode> - Chat settings")
        sender.sendMessage("/noty inspect [tool] - Inspect mode / Get inspection stick")
        sender.sendMessage("/noty locale [ja|en|reset] - Change language")
        sender.sendMessage("/noty admin ... - Admin commands")
    }
}
