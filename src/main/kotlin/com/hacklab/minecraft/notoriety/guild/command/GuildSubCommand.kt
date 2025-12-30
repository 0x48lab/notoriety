package com.hacklab.minecraft.notoriety.guild.command

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
