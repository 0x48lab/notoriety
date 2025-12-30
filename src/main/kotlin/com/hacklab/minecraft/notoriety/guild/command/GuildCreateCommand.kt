package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildCreateCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "create"
    override val description = "Create a new guild"
    override val usage = "/guild create <name> <tag> [description]"
    override val aliases = listOf("new")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        if (args.size < 2) {
            player.sendError("Usage: $usage")
            player.sendInfo("Name: 3-32 characters (letters, numbers, spaces, underscores, hyphens)")
            player.sendInfo("Tag: 2-4 characters (letters and numbers only)")
            return true
        }

        val name = args[0]
        val tag = args[1]
        val description = if (args.size > 2) args.drop(2).joinToString(" ") else null

        try {
            val guild = guildService.createGuild(player.uniqueId, name, tag, description)
            player.sendMessage(Component.text()
                .append(Component.text("Guild ").color(NamedTextColor.GREEN))
                .append(Component.text("[${guild.tag}] ").color(NamedTextColor.GOLD))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                .append(Component.text(" created!").color(NamedTextColor.GREEN))
                .build())
        } catch (e: GuildException.InvalidName) {
            player.sendError("Invalid guild name. Use 3-32 characters (letters, numbers, spaces, underscores, hyphens)")
        } catch (e: GuildException.InvalidTag) {
            player.sendError("Invalid guild tag. Use 2-4 characters (letters and numbers only)")
        } catch (e: GuildException.NameTaken) {
            player.sendError("A guild with this name already exists")
        } catch (e: GuildException.TagTaken) {
            player.sendError("A guild with this tag already exists")
        } catch (e: GuildException.AlreadyInGuild) {
            player.sendError("You are already in a guild. Leave your current guild first")
        } catch (e: GuildException) {
            player.sendError("Failed to create guild: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("<name>")
            2 -> listOf("<tag>")
            3 -> listOf("[description]")
            else -> emptyList()
        }
    }
}
