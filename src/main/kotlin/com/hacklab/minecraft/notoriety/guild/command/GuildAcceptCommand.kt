package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildAcceptCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "accept"
    override val description = "Accept a guild invitation"
    override val usage = "/guild accept [guild-name]"
    override val aliases = listOf("join")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        try {
            if (args.isNotEmpty()) {
                // ギルド名を指定した場合
                val guildName = args.joinToString(" ")
                guildService.acceptInvitationByGuildName(guildName, player.uniqueId)

                val guild = guildService.getPlayerGuild(player.uniqueId)
                if (guild != null) {
                    player.sendMessage(Component.text()
                        .append(Component.text("You have joined ").color(NamedTextColor.GREEN))
                        .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                        .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                        .build())
                }
            } else {
                // 招待を一覧表示して最初のものを承認
                val invitations = guildService.getPendingInvitations(player.uniqueId)
                if (invitations.isEmpty()) {
                    player.sendError("You have no pending invitations")
                    return true
                }

                if (invitations.size > 1) {
                    player.sendInfo("You have ${invitations.size} pending invitations")
                    player.sendInfo("Specify a guild name: /guild accept <guild-name>")
                    player.sendInfo("Or view all with: /guild invites")
                    return true
                }

                val invitation = invitations.first()
                guildService.acceptInvitation(invitation.id, player.uniqueId)

                val guild = guildService.getPlayerGuild(player.uniqueId)
                if (guild != null) {
                    player.sendMessage(Component.text()
                        .append(Component.text("You have joined ").color(NamedTextColor.GREEN))
                        .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                        .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                        .build())
                }
            }
        } catch (e: GuildException.InvitationNotFoundByGuild) {
            player.sendError("You have no invitation from that guild")
        } catch (e: GuildException.InvitationNotFound) {
            player.sendError("Invitation not found or expired")
        } catch (e: GuildException.InvitationExpired) {
            player.sendError("This invitation has expired")
        } catch (e: GuildException.AlreadyInGuild) {
            player.sendError("You are already in a guild. Leave first with /guild leave")
        } catch (e: GuildException.GuildFull) {
            player.sendError("The guild is full")
        } catch (e: GuildException) {
            player.sendError("Failed to accept invitation: ${e.message}")
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
