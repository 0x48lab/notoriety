package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildMembersCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "members"
    override val description = "List guild members"
    override val usage = "/guild members [page]"
    override val aliases = listOf("m", "roster")

    companion object {
        private const val PAGE_SIZE = 15
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val guild = guildService.getPlayerGuild(player.uniqueId)

        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        val page = if (args.isNotEmpty()) {
            args[0].toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: 0
        } else {
            0
        }

        val totalMembers = guildService.getMemberCount(guild.id)
        val totalPages = (totalMembers + PAGE_SIZE - 1) / PAGE_SIZE
        val members = guildService.getMembers(guild.id, page, PAGE_SIZE)

        player.sendMessage(Component.text("=== [${guild.tag}] ${guild.name} Members (${page + 1}/$totalPages) ===")
            .color(NamedTextColor.GOLD))

        members.forEach { membership ->
            val memberName = Bukkit.getOfflinePlayer(membership.playerUuid).name ?: "Unknown"
            val isOnline = Bukkit.getPlayer(membership.playerUuid) != null

            val roleColor = when (membership.role) {
                GuildRole.MASTER -> NamedTextColor.GOLD
                GuildRole.VICE_MASTER -> NamedTextColor.AQUA
                GuildRole.MEMBER -> NamedTextColor.WHITE
            }
            val rolePrefix = when (membership.role) {
                GuildRole.MASTER -> "[M] "
                GuildRole.VICE_MASTER -> "[V] "
                GuildRole.MEMBER -> ""
            }
            val statusColor = if (isOnline) NamedTextColor.GREEN else NamedTextColor.GRAY

            player.sendMessage(Component.text()
                .append(Component.text(rolePrefix).color(roleColor))
                .append(Component.text(memberName).color(statusColor))
                .append(if (isOnline) Component.text(" (online)").color(NamedTextColor.GREEN)
                        else Component.empty())
                .build())
        }

        // ページナビゲーション
        if (totalPages > 1) {
            val nav = Component.text()
            if (page > 0) {
                nav.append(Component.text("[< Prev] ")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/guild members $page")))
            }
            if (page < totalPages - 1) {
                nav.append(Component.text("[Next >]")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/guild members ${page + 2}")))
            }
            player.sendMessage(nav.build())
        }

        player.sendMessage(Component.text("Total: $totalMembers/${guild.maxMembers}")
            .color(NamedTextColor.GRAY))

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}
