package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildTransferCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "transfer"
    override val description = "Transfer guild leadership to another member"
    override val usage = "/guild transfer <member>"

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
            guildService.transferMaster(guild.id, target.uniqueId, player.uniqueId)

            player.sendMessage(Component.text()
                .append(Component.text("Guild leadership transferred to ").color(NamedTextColor.YELLOW))
                .append(Component.text(target.name ?: "Unknown").color(NamedTextColor.GOLD))
                .build())
            player.sendInfo("You are now a Vice Master")

            target.player?.sendMessage(Component.text()
                .append(Component.text("You are now the ").color(NamedTextColor.GREEN))
                .append(Component.text("Guild Master").color(NamedTextColor.GOLD))
                .append(Component.text(" of [${guild.tag}] ${guild.name}").color(NamedTextColor.WHITE))
                .build())
        } catch (e: GuildException.NotMaster) {
            player.sendError("Only the guild master can transfer leadership")
        } catch (e: GuildException.NotMember) {
            player.sendError("${target.name} is not a member of your guild")
        } catch (e: GuildException) {
            player.sendError("Failed to transfer leadership: ${e.message}")
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
                .filter { it.role != GuildRole.MASTER }
                .mapNotNull { Bukkit.getOfflinePlayer(it.playerUuid).name }
                .filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }
}
