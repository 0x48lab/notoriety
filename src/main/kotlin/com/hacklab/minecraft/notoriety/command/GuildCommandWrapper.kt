package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.guild.command.GuildCommand
import com.hacklab.minecraft.notoriety.territory.command.GuildHomeCommand
import com.hacklab.minecraft.notoriety.territory.command.GuildSigilCommand
import com.hacklab.minecraft.notoriety.territory.command.GuildTerritoryCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * GuildCommandを/notyのサブコマンドとしてラップするクラス
 */
class GuildCommandWrapper(private val plugin: Notoriety) : SubCommand {

    private val guildCommand: GuildCommand by lazy {
        val command = GuildCommand(plugin, plugin.guildService, plugin.guildGUIManager, plugin.economyService, plugin.i18nManager)
        // 領地システムのサブコマンドを追加
        command.addSubCommand(GuildTerritoryCommand(
            territoryService = plugin.territoryService,
            guildService = plugin.guildService,
            i18n = plugin.i18nManager
        ))
        // シギル関連コマンドを追加
        command.addSubCommand(GuildHomeCommand(
            sigilService = plugin.sigilService,
            territoryService = plugin.territoryService,
            guildService = plugin.guildService,
            i18n = plugin.i18nManager
        ))
        command.addSubCommand(GuildSigilCommand(
            sigilService = plugin.sigilService,
            territoryService = plugin.territoryService,
            guildService = plugin.guildService,
            i18n = plugin.i18nManager
        ))
        command
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            // 引数なしの場合はGUIを開く（プレイヤーの場合）
            if (sender is Player) {
                plugin.guildGUIManager.openMainMenu(sender)
                return true
            }
            sender.sendMessage(Component.text("Use /noty guild help for a list of commands")
                .color(NamedTextColor.GRAY))
            return true
        }

        // GuildCommandの処理を直接委譲
        return guildCommand.handleCommand(sender, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return guildCommand.handleTabComplete(sender, args)
    }
}
