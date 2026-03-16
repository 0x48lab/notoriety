package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

interface GuildSubCommand {
    val name: String
    val description: String
    val usage: String
    val aliases: List<String>
        get() = emptyList()
    val requiresPlayer: Boolean
        get() = true
    val permission: String?
        get() = null

    fun execute(sender: CommandSender, args: Array<out String>): Boolean

    fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = emptyList()

    /**
     * --gov フラグに基づいて対象ギルドを解決する。
     * --gov あり → 政府ギルド
     * --gov なし → 民間優先（なければ政府）
     * @return 対象ギルドと --gov を除去したargs
     */
    fun resolveTargetGuild(
        player: Player,
        args: Array<out String>,
        guildService: GuildService
    ): Pair<Guild?, Array<String>> {
        val hasGov = args.any { it == "--gov" }
        val cleanedArgs = args.filter { it != "--gov" }.toTypedArray()
        val guild = if (hasGov) {
            guildService.getPlayerGovernmentGuild(player.uniqueId)
        } else {
            guildService.getPlayerGuild(player.uniqueId)
        }
        return guild to cleanedArgs
    }

    /**
     * argsから --gov フラグを除去する
     */
    fun stripGovFlag(args: Array<out String>): Array<String> =
        args.filter { it != "--gov" }.toTypedArray()

    /**
     * argsに --gov フラグが含まれているかチェックする
     */
    fun hasGovFlag(args: Array<out String>): Boolean =
        args.any { it == "--gov" }

    fun Player.sendError(message: String) {
        sendMessage(net.kyori.adventure.text.Component.text(message)
            .color(net.kyori.adventure.text.format.NamedTextColor.RED))
    }

    fun Player.sendSuccess(message: String) {
        sendMessage(net.kyori.adventure.text.Component.text(message)
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN))
    }

    fun Player.sendInfo(message: String) {
        sendMessage(net.kyori.adventure.text.Component.text(message)
            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW))
    }
}
