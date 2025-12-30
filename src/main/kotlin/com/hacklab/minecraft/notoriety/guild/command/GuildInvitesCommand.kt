package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant

class GuildInvitesCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "invites"
    override val description = "View pending guild invitations"
    override val usage = "/guild invites"
    override val aliases = listOf("invitations", "pending")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        val invitations = guildService.getPendingInvitations(player.uniqueId)

        if (invitations.isEmpty()) {
            player.sendInfo("You have no pending guild invitations")
            return true
        }

        player.sendMessage(Component.text("=== Pending Invitations (${invitations.size}) ===")
            .color(NamedTextColor.GOLD))

        invitations.forEach { invitation ->
            val guild = guildService.getGuild(invitation.guildId)
            val inviterName = Bukkit.getOfflinePlayer(invitation.inviterUuid).name ?: "Unknown"
            val timeLeft = Duration.between(Instant.now(), invitation.expiresAt)
            val daysLeft = timeLeft.toDays()
            val hoursLeft = timeLeft.toHours() % 24

            val expiryText = when {
                daysLeft > 0 -> "${daysLeft}d ${hoursLeft}h left"
                hoursLeft > 0 -> "${hoursLeft}h left"
                else -> "expires soon"
            }

            if (guild != null) {
                player.sendMessage(Component.text()
                    .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                    .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                    .append(Component.text(" from $inviterName").color(NamedTextColor.GRAY))
                    .append(Component.text(" ($expiryText)").color(NamedTextColor.DARK_GRAY))
                    .build())
                player.sendMessage(Component.text()
                    .append(Component.text("  "))
                    .append(Component.text("[Accept]")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/guild accept ${guild.name}")))
                    .append(Component.text(" "))
                    .append(Component.text("[Deny]")
                        .color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/guild deny ${guild.name}")))
                    .build())
            }
        }

        return true
    }
}
