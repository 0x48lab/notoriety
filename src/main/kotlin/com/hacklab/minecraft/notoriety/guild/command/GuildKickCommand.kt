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
        val cleanedArgs = stripGovFlag(args)
        if (cleanedArgs.size == 1 && sender is Player) {
            val (guild, _) = resolveTargetGuild(sender, args, guildService)
            if (guild == null) return emptyList()
            val members = guildService.getMembers(guild.id, 0, 50)
            val input = cleanedArgs[0].lowercase()

            val names = members
                .filter { it.playerUuid != sender.uniqueId }
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
