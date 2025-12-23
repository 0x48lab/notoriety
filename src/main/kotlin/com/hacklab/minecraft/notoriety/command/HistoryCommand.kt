package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
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

    companion object {
        const val PAGE_SIZE = 10
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val target = if (args.isEmpty()) {
            (sender as? Player)?.uniqueId ?: run {
                sender.sendMessage("Player name required")
                return true
            }
        } else {
            Bukkit.getOfflinePlayer(args[0]).uniqueId
        }

        val page = args.getOrNull(1)?.toIntOrNull() ?: 1
        val targetName = Bukkit.getOfflinePlayer(target).name ?: "Unknown"

        val totalCount = plugin.crimeService.getHistoryCount(target)
        val totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

        if (totalCount == 0) {
            sender.sendMessage("犯罪履歴はありません")
            return true
        }

        val history = plugin.crimeService.getHistory(target, page, PAGE_SIZE)

        sender.sendMessage("§7=== §c$targetName §7の犯罪履歴 [$page/$totalPages] ===")

        history.forEach { record ->
            val time = dateFormatter.format(record.committedAt)
            val crimeType = plugin.i18nManager.get("crime.${record.crimeType.name.lowercase()}", record.crimeType.name)
            val detail = record.detail?.let { " §7[$it]" } ?: ""
            sender.sendMessage("§8$time §f$crimeType$detail")
        }

        // ページナビゲーション
        if (sender is Player) {
            val prevPage = if (page > 1) page - 1 else null
            val nextPage = if (page < totalPages) page + 1 else null

            val nav = Component.text("  ")
                .append(
                    if (prevPage != null) {
                        Component.text("§a<< 前")
                            .clickEvent(ClickEvent.runCommand("/noty history $targetName $prevPage"))
                    } else {
                        Component.text("§8<< 前")
                    }
                )
                .append(Component.text("  §7|  "))
                .append(
                    if (nextPage != null) {
                        Component.text("§a次 >>")
                            .clickEvent(ClickEvent.runCommand("/noty history $targetName $nextPage"))
                    } else {
                        Component.text("§8次 >>")
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
