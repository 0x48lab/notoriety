package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GuildInfoCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "info"
    override val description = "Show guild information"
    override val usage = "/guild info [guild-name]"
    override val aliases = listOf("i", "show")
    override val requiresPlayer = false

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val guild: Guild? = if (args.isNotEmpty()) {
            guildService.getGuildByName(args.joinToString(" "))
        } else if (sender is Player) {
            guildService.getPlayerGuild(sender.uniqueId)
        } else {
            sender.sendMessage(Component.text("Specify a guild name: /guild info <name>")
                .color(NamedTextColor.RED))
            return true
        }

        if (guild == null) {
            if (args.isEmpty() && sender is Player) {
                sender.sendMessage(Component.text("You are not in a guild")
                    .color(NamedTextColor.RED))
            } else {
                sender.sendMessage(Component.text("Guild not found")
                    .color(NamedTextColor.RED))
            }
            return true
        }

        val memberCount = guildService.getMemberCount(guild.id)
        val masterName = Bukkit.getOfflinePlayer(guild.masterUuid).name ?: "Unknown"
        val members = guildService.getMembers(guild.id, 0, 50)
        val viceMasters = members.filter { it.role == GuildRole.VICE_MASTER }
            .map { Bukkit.getOfflinePlayer(it.playerUuid).name ?: "Unknown" }

        sender.sendMessage(Component.text("=== Guild Information ===").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text()
            .append(Component.text("Name: ").color(NamedTextColor.GRAY))
            .append(Component.text(guild.name).color(NamedTextColor.WHITE))
            .build())
        sender.sendMessage(Component.text()
            .append(Component.text("Tag: ").color(NamedTextColor.GRAY))
            .append(Component.text("[${guild.tag}]").color(guild.tagColor.namedTextColor))
            .build())

        guild.description?.let {
            sender.sendMessage(Component.text()
                .append(Component.text("Description: ").color(NamedTextColor.GRAY))
                .append(Component.text(it).color(NamedTextColor.WHITE))
                .build())
        }

        sender.sendMessage(Component.text()
            .append(Component.text("Master: ").color(NamedTextColor.GRAY))
            .append(Component.text(masterName).color(NamedTextColor.GOLD))
            .build())

        if (viceMasters.isNotEmpty()) {
            sender.sendMessage(Component.text()
                .append(Component.text("Vice Masters: ").color(NamedTextColor.GRAY))
                .append(Component.text(viceMasters.joinToString(", ")).color(NamedTextColor.AQUA))
                .build())
        }

        sender.sendMessage(Component.text()
            .append(Component.text("Members: ").color(NamedTextColor.GRAY))
            .append(Component.text("$memberCount/${guild.maxMembers}").color(NamedTextColor.WHITE))
            .build())

        sender.sendMessage(Component.text()
            .append(Component.text("Created: ").color(NamedTextColor.GRAY))
            .append(Component.text(dateFormatter.format(guild.createdAt)).color(NamedTextColor.WHITE))
            .build())

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val input = args[0].lowercase()
            return guildService.getAllGuilds(0, 100)
                .map { it.name }
                .filter { it.lowercase().startsWith(input) }
        }
        return emptyList()
    }
}
