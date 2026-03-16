package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildDemoteCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "demote"
    override val description = "Demote a vice master to regular member"
    override val usage = "/guild demote <member>"

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val (guild, cleanedArgs) = resolveTargetGuild(player, args, guildService)

        if (cleanedArgs.isEmpty()) {
            player.sendError("Usage: $usage")
            return true
        }

        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        val targetName = cleanedArgs[0]
        val target = Bukkit.getOfflinePlayer(targetName)

        try {
            guildService.demoteToMember(guild.id, target.uniqueId, player.uniqueId)

            player.sendSuccess("Demoted ${target.name} to Member")

            target.player?.sendMessage(Component.text()
                .append(Component.text("You have been demoted to ").color(NamedTextColor.YELLOW))
                .append(Component.text("Member").color(NamedTextColor.WHITE))
                .append(Component.text(" in [${guild.tag}] ${guild.name}").color(NamedTextColor.WHITE))
                .build())
        } catch (e: GuildException.NotMaster) {
            player.sendError("Only the guild master can demote members")
        } catch (e: GuildException.NotMember) {
            player.sendError("${target.name} is not a member of your guild")
        } catch (e: GuildException.NotViceMaster) {
            player.sendError("${target.name} is not a Vice Master")
        } catch (e: GuildException) {
            player.sendError("Failed to demote member: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val cleanedArgs = stripGovFlag(args)
        if (cleanedArgs.size == 1 && sender is Player) {
            val (guild, _) = resolveTargetGuild(sender, args, guildService)
            if (guild == null) return emptyList()
            val members = guildService.getMembers(guild.id, 0, 50)
            val input = cleanedArgs[0].lowercase()

            val names = members
                .filter { it.role == GuildRole.VICE_MASTER }
                .mapNotNull { Bukkit.getOfflinePlayer(it.playerUuid).name }
                .filter { it.lowercase().startsWith(input) }
            if (args.size == 1 && !hasGovFlag(args)) {
                return names + listOf("--gov").filter { it.startsWith(input) }
            }
            return names
        }
        if (!hasGovFlag(args)) {
            return listOf("--gov").filter { it.startsWith(args.last().lowercase()) }
        }
        return emptyList()
    }
}
