package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.TagColor
import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildColorCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "color"
    override val description = "Set your guild's tag color"
    override val usage = "/guild color <color>"
    override val aliases = listOf("tagcolor", "colour")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        val guild = guildService.getPlayerGuild(player.uniqueId)
        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        if (args.isEmpty()) {
            // 色一覧を表示
            player.sendMessage(Component.text("=== Available Colors ===").color(NamedTextColor.GOLD))
            player.sendMessage(Component.text("Current: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text("[${guild.tag}]").color(guild.tagColor.namedTextColor)))

            val colors = TagColor.entries.chunked(4)
            colors.forEach { row ->
                val line = Component.text()
                row.forEachIndexed { index, color ->
                    if (index > 0) line.append(Component.text(" "))
                    line.append(Component.text(color.name.lowercase()).color(color.namedTextColor))
                }
                player.sendMessage(line.build())
            }

            player.sendInfo("Usage: /guild color <color>")
            return true
        }

        val colorName = args[0].uppercase()
        val color = TagColor.fromString(colorName)

        if (color == null) {
            player.sendError("Invalid color: ${args[0]}")
            player.sendInfo("Use /guild color to see available colors")
            return true
        }

        try {
            guildService.setTagColor(guild.id, color, player.uniqueId)

            player.sendMessage(Component.text()
                .append(Component.text("Guild tag color changed to ").color(NamedTextColor.GREEN))
                .append(Component.text("[${guild.tag}]").color(color.namedTextColor))
                .build())
        } catch (e: GuildException.NotMaster) {
            player.sendError("Only the guild master can change the tag color")
        } catch (e: GuildException) {
            player.sendError("Failed to change tag color: ${e.message}")
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            return TagColor.entries
                .map { it.name.lowercase() }
                .filter { it.startsWith(input) }
        }
        return emptyList()
    }
}
