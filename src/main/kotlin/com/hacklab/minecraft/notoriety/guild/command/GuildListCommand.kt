package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildListCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "list"
    override val description = "List all guilds"
    override val usage = "/guild list [page]"
    override val aliases = listOf("l", "guilds")
    override val requiresPlayer = false

    companion object {
        private const val PAGE_SIZE = 10
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val page = if (args.isNotEmpty()) {
            args[0].toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: 0
        } else {
            0
        }

        val totalGuilds = guildService.getGuildCount()
        val totalPages = (totalGuilds + PAGE_SIZE - 1) / PAGE_SIZE
        val guilds = guildService.getAllGuilds(page, PAGE_SIZE)

        if (guilds.isEmpty()) {
            sender.sendMessage(Component.text("No guilds found").color(NamedTextColor.GRAY))
            return true
        }

        sender.sendMessage(Component.text("=== Guild List (${page + 1}/$totalPages) ===")
            .color(NamedTextColor.GOLD))

        guilds.forEach { guild ->
            val memberCount = guildService.getMemberCount(guild.id)
            val guildLine = Component.text()
                .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))

            // 政府ギルドマーカー
            if (guild.isGovernment) {
                guildLine.append(Component.text(" [政府]").color(NamedTextColor.GOLD))
            }

            guildLine.append(Component.text(" - $memberCount members").color(NamedTextColor.GRAY))
                .clickEvent(ClickEvent.runCommand("/guild info ${guild.name}"))

            sender.sendMessage(guildLine.build())
        }

        // ページナビゲーション
        if (totalPages > 1) {
            val nav = Component.text()
            if (page > 0) {
                nav.append(Component.text("[< Prev] ")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/guild list $page")))
            }
            if (page < totalPages - 1) {
                nav.append(Component.text("[Next >]")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/guild list ${page + 2}")))
            }
            sender.sendMessage(nav.build())
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val totalGuilds = guildService.getGuildCount()
            val totalPages = (totalGuilds + PAGE_SIZE - 1) / PAGE_SIZE
            return (1..totalPages).map { it.toString() }
                .filter { it.startsWith(args[0]) }
        }
        return emptyList()
    }
}
