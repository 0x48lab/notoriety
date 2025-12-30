package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildPromoteCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "promote"
    override val description = "Promote a member to vice master"
    override val usage = "/guild promote <member>"

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        if (args.isEmpty()) {
            player.sendError("Usage: $usage")
            return true
        }

        val guild = guildService.getPlayerGuild(player.uniqueId)
        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        val targetName = args[0]
        val target = Bukkit.getOfflinePlayer(targetName)

        try {
            guildService.promoteToViceMaster(guild.id, target.uniqueId, player.uniqueId)

            player.sendSuccess("Promoted ${target.name} to Vice Master")

            target.player?.sendMessage(Component.text()
                .append(Component.text("You have been promoted to ").color(NamedTextColor.GREEN))
                .append(Component.text("Vice Master").color(NamedTextColor.AQUA))
                .append(Component.text(" in [${guild.tag}] ${guild.name}").color(NamedTextColor.WHITE))
                .build())
        } catch (e: GuildException.NotMaster) {
            player.sendError("Only the guild master can promote members")
        } catch (e: GuildException.NotMember) {
            player.sendError("${target.name} is not a member of your guild")
        } catch (e: GuildException.AlreadyViceMaster) {
            player.sendError("${target.name} is already a Vice Master")
        } catch (e: GuildException) {
            player.sendError("Failed to promote member: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1 && sender is Player) {
            val guild = guildService.getPlayerGuild(sender.uniqueId) ?: return emptyList()
            val members = guildService.getMembers(guild.id, 0, 50)
            val input = args[0].lowercase()

            return members
                .filter { it.playerUuid != sender.uniqueId }
                .filter { it.role == com.hacklab.minecraft.notoriety.guild.model.GuildRole.MEMBER }
                .mapNotNull { Bukkit.getOfflinePlayer(it.playerUuid).name }
                .filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }
}
