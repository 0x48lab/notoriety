package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildLeaveCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "leave"
    override val description = "Leave your current guild"
    override val usage = "/guild leave"
    override val aliases = listOf("quit")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        val guild = guildService.getPlayerGuild(player.uniqueId)
        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        try {
            guildService.leaveGuild(player.uniqueId)

            player.sendMessage(Component.text()
                .append(Component.text("You have left ").color(NamedTextColor.YELLOW))
                .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                .build())
        } catch (e: GuildException.MasterCannotLeave) {
            player.sendError("As the guild master, you cannot leave while other members exist")
            player.sendInfo("Transfer leadership with /guild transfer <member> or dissolve with /guild dissolve")
        } catch (e: GuildException) {
            player.sendError("Failed to leave guild: ${e.message}")
        }

        return true
    }
}
