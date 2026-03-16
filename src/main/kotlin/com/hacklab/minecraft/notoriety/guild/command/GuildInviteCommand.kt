package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class GuildInviteCommand(
    private val plugin: JavaPlugin,
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "invite"
    override val description = "Invite a player to your guild"
    override val usage = "/guild invite <player>"
    override val aliases = listOf("inv")

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

        if (!target.hasPlayedBefore() && !target.isOnline) {
            player.sendError("Player '$targetName' has never played on this server")
            return true
        }

        try {
            guildService.invitePlayer(guild.id, target.uniqueId, player.uniqueId)

            player.sendSuccess("Invited ${target.name} to your guild")

            // オンラインの場合、招待通知を送信
            target.player?.let { onlineTarget ->
                onlineTarget.sendMessage(Component.text()
                    .append(Component.text("You have been invited to join ").color(NamedTextColor.GREEN))
                    .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                    .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                    .append(Component.text(" by ${player.name}").color(NamedTextColor.GREEN))
                    .build())
                onlineTarget.sendMessage(Component.text()
                    .append(Component.text("[Accept]")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/guild accept ${guild.name}")))
                    .append(Component.text(" "))
                    .append(Component.text("[Deny]")
                        .color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/guild deny ${guild.name}")))
                    .build())
            }
        } catch (e: GuildException.NotMasterOrVice) {
            player.sendError("Only the master or vice masters can invite players")
        } catch (e: GuildException.AlreadyInGuild) {
            player.sendError("${target.name} is already in a guild")
        } catch (e: GuildException.AlreadyInvited) {
            player.sendError("${target.name} has already been invited to your guild")
        } catch (e: GuildException.GuildFull) {
            player.sendError("Your guild is full")
        } catch (e: GuildException) {
            player.sendError("Failed to invite player: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val cleanedArgs = stripGovFlag(args)
        if (cleanedArgs.size == 1) {
            val input = cleanedArgs[0].lowercase()
            val players = Bukkit.getOnlinePlayers()
                .filter { it.uniqueId != (sender as? Player)?.uniqueId }
                .map { it.name }
                .filter { it.lowercase().startsWith(input) }
            if (args.size == 1 && !hasGovFlag(args)) {
                return players + listOf("--gov").filter { it.startsWith(input) }
            }
            return players
        }
        if (args.size == 2 && !hasGovFlag(args)) {
            return listOf("--gov").filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }
}
