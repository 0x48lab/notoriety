package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryCommand(private val plugin: Notoriety) : SubCommand {
    private val dateFormatter = DateTimeFormatter
        .ofPattern("MM/dd HH:mm")
        .withZone(ZoneId.systemDefault())

    private val i18n: I18nManager get() = plugin.i18nManager

    companion object {
        const val PAGE_SIZE = 10
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val target = if (args.isEmpty()) {
            (sender as? Player)?.uniqueId ?: run {
                i18n.sendError(sender, "history.player_required", "Player name required")
                return true
            }
        } else {
            Bukkit.getOfflinePlayer(args[0]).uniqueId
        }

        val page = args.getOrNull(1)?.toIntOrNull() ?: 1
        val targetName = Bukkit.getOfflinePlayer(target).name ?: "Unknown"

        val totalCount = plugin.notorietyService.getHistoryCount(target)
        val totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

        if (totalCount == 0) {
            i18n.sendInfo(sender, "history.no_records", "No crime history")
            return true
        }

        val history = plugin.notorietyService.getHistory(target, page, PAGE_SIZE)

        // ヘッダー
        sender.sendMessage(
            Component.text("=== ").color(NamedTextColor.GRAY)
                .append(Component.text(targetName).color(NamedTextColor.RED))
                .append(Component.text(" ").color(NamedTextColor.GRAY))
                .append(Component.text(i18n.get("history.header_suffix", "Crime History")).color(NamedTextColor.GRAY))
                .append(Component.text(" [$page/$totalPages] ===").color(NamedTextColor.GRAY))
        )

        history.forEach { record ->
            val time = dateFormatter.format(record.committedAt)
            val crimeType = i18n.get("crime.${record.crimeType.name.lowercase()}", record.crimeType.name)
            val detailComponent = record.detail?.let {
                Component.text(" [$it]").color(NamedTextColor.GRAY)
            } ?: Component.empty()

            sender.sendMessage(
                Component.text(time).color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(" ").color(NamedTextColor.WHITE))
                    .append(Component.text(crimeType).color(NamedTextColor.WHITE))
                    .append(detailComponent)
            )
        }

        // ページナビゲーション
        if (sender is Player) {
            val prevPage = if (page > 1) page - 1 else null
            val nextPage = if (page < totalPages) page + 1 else null

            val prevText = i18n.get("history.nav_prev", "<< Prev")
            val nextText = i18n.get("history.nav_next", "Next >>")

            val nav = Component.text("  ")
                .append(
                    if (prevPage != null) {
                        Component.text(prevText).color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/noty history $targetName $prevPage"))
                    } else {
                        Component.text(prevText).color(NamedTextColor.DARK_GRAY)
                    }
                )
                .append(Component.text("  |  ").color(NamedTextColor.GRAY))
                .append(
                    if (nextPage != null) {
                        Component.text(nextText).color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/noty history $targetName $nextPage"))
                    } else {
                        Component.text(nextText).color(NamedTextColor.DARK_GRAY)
                    }
                )
            sender.sendMessage(nav)
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
