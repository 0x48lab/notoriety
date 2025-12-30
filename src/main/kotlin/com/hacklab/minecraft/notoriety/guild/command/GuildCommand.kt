package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.gui.GuildGUIManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class GuildCommand(
    private val plugin: JavaPlugin,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager
) : CommandExecutor, TabCompleter {

    private val subCommands = mutableMapOf<String, GuildSubCommand>()
    private val aliasMap = mutableMapOf<String, String>()

    init {
        // サブコマンドを登録
        registerSubCommand(GuildMenuCommand(guiManager))
        registerSubCommand(GuildCreateCommand(guildService))
        registerSubCommand(GuildInfoCommand(guildService))
        registerSubCommand(GuildListCommand(guildService))
        registerSubCommand(GuildMembersCommand(guildService))
        registerSubCommand(GuildInviteCommand(plugin, guildService))
        registerSubCommand(GuildKickCommand(guildService))
        registerSubCommand(GuildLeaveCommand(guildService))
        registerSubCommand(GuildAcceptCommand(guildService))
        registerSubCommand(GuildDenyCommand(guildService))
        registerSubCommand(GuildInvitesCommand(guildService))
        registerSubCommand(GuildPromoteCommand(guildService))
        registerSubCommand(GuildDemoteCommand(guildService))
        registerSubCommand(GuildTransferCommand(guildService))
        registerSubCommand(GuildDissolveCommand(guildService))
        registerSubCommand(GuildColorCommand(guildService))
        registerSubCommand(GuildHelpCommand(subCommands))
    }

    private fun registerSubCommand(subCommand: GuildSubCommand) {
        subCommands[subCommand.name.lowercase()] = subCommand
        subCommand.aliases.forEach { alias ->
            aliasMap[alias.lowercase()] = subCommand.name.lowercase()
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            // 引数なしの場合はGUIを開く（プレイヤーの場合）
            if (sender is Player) {
                guiManager.openMainMenu(sender)
                return true
            }
            return subCommands["help"]?.execute(sender, emptyArray()) ?: true
        }

        val subCommandName = args[0].lowercase()
        val actualName = aliasMap[subCommandName] ?: subCommandName
        val subCommand = subCommands[actualName]

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown subcommand: ${args[0]}")
                .color(NamedTextColor.RED))
            sender.sendMessage(Component.text("Use /guild help for a list of commands")
                .color(NamedTextColor.GRAY))
            return true
        }

        // プレイヤー限定チェック
        if (subCommand.requiresPlayer && sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players")
                .color(NamedTextColor.RED))
            return true
        }

        // 権限チェック
        subCommand.permission?.let { perm ->
            if (!sender.hasPermission(perm)) {
                sender.sendMessage(Component.text("You don't have permission to use this command")
                    .color(NamedTextColor.RED))
                return true
            }
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
            // サブコマンド名の補完
            val allNames = subCommands.keys + aliasMap.keys
            return allNames.filter { it.startsWith(args[0].lowercase()) }.sorted()
        }

        // サブコマンドに委譲
        val subCommandName = args[0].lowercase()
        val actualName = aliasMap[subCommandName] ?: subCommandName
        val subCommand = subCommands[actualName] ?: return emptyList()

        return subCommand.tabComplete(sender, args.drop(1).toTypedArray())
    }
}
