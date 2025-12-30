package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildKickCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "kick"
    override val description = "Kick a member from your guild"
    override val usage = "/guild kick <member>"

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
            guildService.kickMember(guild.id, target.uniqueId, player.uniqueId)

            player.sendSuccess("Kicked ${target.name} from the guild")

            // キックされたプレイヤーに通知
            target.player?.sendMessage(Component.text()
                .append(Component.text("You have been kicked from ").color(NamedTextColor.RED))
                .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                .build())
        } catch (e: GuildException.NotMaster) {
            player.sendError("Only the guild master can kick members")
        } catch (e: GuildException.NotMember) {
            player.sendError("${target.name} is not a member of your guild")
        } catch (e: GuildException.CannotKickSelf) {
            player.sendError("You cannot kick yourself. Use /guild leave instead")
        } catch (e: GuildException) {
            player.sendError("Failed to kick member: ${e.message}")
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
                .mapNotNull { Bukkit.getOfflinePlayer(it.playerUuid).name }
                .filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }
}
