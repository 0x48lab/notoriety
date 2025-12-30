package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildDenyCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "deny"
    override val description = "Decline a guild invitation"
    override val usage = "/guild deny [guild-name]"
    override val aliases = listOf("decline", "reject")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        try {
            if (args.isNotEmpty()) {
                val guildName = args.joinToString(" ")
                guildService.declineInvitationByGuildName(guildName, player.uniqueId)
                player.sendSuccess("Declined invitation from $guildName")
            } else {
                val invitations = guildService.getPendingInvitations(player.uniqueId)
                if (invitations.isEmpty()) {
                    player.sendError("You have no pending invitations")
                    return true
                }

                if (invitations.size > 1) {
                    player.sendInfo("You have ${invitations.size} pending invitations")
                    player.sendInfo("Specify a guild name: /guild deny <guild-name>")
                    player.sendInfo("Or view all with: /guild invites")
                    return true
                }

                val invitation = invitations.first()
                val guild = guildService.getGuild(invitation.guildId)
                guildService.declineInvitation(invitation.id, player.uniqueId)
                player.sendSuccess("Declined invitation from ${guild?.name ?: "Unknown"}")
            }
        } catch (e: GuildException.InvitationNotFoundByGuild) {
            player.sendError("You have no invitation from that guild")
        } catch (e: GuildException.InvitationNotFound) {
            player.sendError("Invitation not found or expired")
        } catch (e: GuildException) {
            player.sendError("Failed to decline invitation: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1 && sender is Player) {
            val invitations = guildService.getPendingInvitations(sender.uniqueId)
            val input = args[0].lowercase()

            return invitations.mapNotNull { inv ->
                guildService.getGuild(inv.guildId)?.name
            }.filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }
}
