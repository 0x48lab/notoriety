package com.hacklab.minecraft.notoriety.achievement.command

import com.hacklab.minecraft.notoriety.achievement.model.Achievement
import com.hacklab.minecraft.notoriety.achievement.model.AchievementCategory
import com.hacklab.minecraft.notoriety.achievement.model.Achievements
import com.hacklab.minecraft.notoriety.achievement.service.AchievementService
import com.hacklab.minecraft.notoriety.command.SubCommand
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

/**
 * アチーブメント一覧表示コマンド
 * /noty achievements [player] [category] [page]
 * /noty ach [player] [category] [page]
 */
class AchievementCommand(
    private val achievementService: AchievementService,
    private val i18nManager: I18nManager
) : SubCommand {

    companion object {
        private const val PAGE_SIZE = 10
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player

        // Parse arguments
        var targetUuid: UUID? = null
        var category: AchievementCategory? = null
        var page = 1

        // Determine locale
        val locale = if (player != null) {
            i18nManager.getPlayerLocale?.invoke(player.uniqueId) ?: i18nManager.defaultLocale
        } else {
            i18nManager.defaultLocale
        }

        // Process arguments
        val argsList = args.toMutableList()

        // Check for page number at the end
        if (argsList.isNotEmpty() && argsList.last().toIntOrNull() != null) {
            page = argsList.removeLast().toInt()
        }

        // Check for category
        for (arg in argsList.toList()) {
            val foundCategory = findCategory(arg)
            if (foundCategory != null) {
                category = foundCategory
                argsList.remove(arg)
                break
            }
        }

        // Remaining arg is player name
        if (argsList.isNotEmpty()) {
            val targetPlayer = Bukkit.getOfflinePlayer(argsList[0])
            targetUuid = targetPlayer.uniqueId
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                sender.sendMessage(
                    Component.text(i18nManager.getForLocale(locale, "message.player_not_found", "Player not found"))
                        .color(NamedTextColor.RED)
                )
                return true
            }
        } else if (player != null) {
            targetUuid = player.uniqueId
        } else {
            sender.sendMessage(
                Component.text("Usage: /noty achievements <player> [category] [page]")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        // Display achievements
        if (category != null) {
            displayCategoryAchievements(sender, targetUuid, category, page, locale)
        } else {
            displayAllAchievements(sender, targetUuid, page, locale)
        }

        return true
    }

    /**
     * 全カテゴリのアチーブメント概要を表示
     */
    private fun displayAllAchievements(
        sender: CommandSender,
        targetUuid: UUID,
        page: Int,
        locale: String
    ) {
        val targetName = Bukkit.getOfflinePlayer(targetUuid).name ?: "Unknown"
        val (totalUnlocked, totalAll) = achievementService.getTotalProgress(targetUuid)

        // Header
        sender.sendMessage(
            AchievementCommandHelper.formatOverallSummary(totalUnlocked, totalAll, targetName)
        )
        sender.sendMessage(Component.empty())

        // Category summaries
        AchievementCategory.entries.forEach { category ->
            val (unlocked, total) = achievementService.getProgress(targetUuid, category)
            val categoryName = i18nManager.getForLocale(locale, category.displayKey, category.name)

            sender.sendMessage(
                Component.text("  ")
                    .append(AchievementCommandHelper.formatCategoryHeader(categoryName, unlocked, total))
            )
        }

        sender.sendMessage(Component.empty())
        sender.sendMessage(
            Component.text(i18nManager.getForLocale(locale, "achievement.hint.category", "Use /noty achievements <category> for details"))
                .color(NamedTextColor.GRAY)
        )
    }

    /**
     * 特定カテゴリのアチーブメント一覧を表示
     */
    private fun displayCategoryAchievements(
        sender: CommandSender,
        targetUuid: UUID,
        category: AchievementCategory,
        page: Int,
        locale: String
    ) {
        val targetName = Bukkit.getOfflinePlayer(targetUuid).name ?: "Unknown"
        val categoryAchievements = Achievements.byCategory(category)
        val unlockedIds = achievementService.getUnlockedIds(targetUuid)

        // Paginate
        val totalPages = (categoryAchievements.size + PAGE_SIZE - 1) / PAGE_SIZE
        val currentPage = page.coerceIn(1, maxOf(1, totalPages))
        val startIndex = (currentPage - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, categoryAchievements.size)

        val (unlocked, total) = achievementService.getProgress(targetUuid, category)
        val categoryName = i18nManager.getForLocale(locale, category.displayKey, category.name)

        // Header
        sender.sendMessage(
            Component.text("=== $targetName - ")
                .color(NamedTextColor.GOLD)
                .append(Component.text(categoryName).color(NamedTextColor.WHITE))
                .append(Component.text(" ===").color(NamedTextColor.GOLD))
        )
        sender.sendMessage(
            AchievementCommandHelper.renderProgressBarWithPercentage(unlocked, total)
        )
        sender.sendMessage(Component.empty())

        // Achievement list
        categoryAchievements.subList(startIndex, endIndex).forEach { achievement ->
            val isUnlocked = achievement.id in unlockedIds
            sender.sendMessage(formatAchievementLine(achievement, isUnlocked, locale))
        }

        // Pagination
        if (totalPages > 1) {
            sender.sendMessage(Component.empty())
            sender.sendMessage(
                AchievementCommandHelper.formatPagination(
                    currentPage,
                    totalPages,
                    "/noty achievements $targetName ${category.name.lowercase()}"
                )
            )
        }
    }

    /**
     * アチーブメント行のフォーマット
     */
    private fun formatAchievementLine(
        achievement: Achievement,
        unlocked: Boolean,
        locale: String
    ): Component {
        val name = i18nManager.getForLocale(locale, achievement.nameKey, achievement.id)
        val description = i18nManager.getForLocale(locale, achievement.descriptionKey, "")

        return AchievementCommandHelper.formatUnlockStatus(unlocked)
            .append(AchievementCommandHelper.formatRarityBadge(achievement.rarity))
            .append(Component.text(" "))
            .append(AchievementCommandHelper.formatAchievementName(name, achievement.rarity, unlocked))
            .append(Component.newline())
            .append(Component.text("    "))
            .append(
                Component.text(description)
                    .color(if (unlocked) NamedTextColor.GRAY else NamedTextColor.DARK_GRAY)
            )
    }

    /**
     * カテゴリを名前から検索
     */
    private fun findCategory(name: String): AchievementCategory? {
        val lower = name.lowercase()
        return AchievementCategory.entries.find { category ->
            category.name.lowercase() == lower ||
                    category.aliases.any { it.lowercase() == lower }
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                // Player names and category names
                val players = Bukkit.getOnlinePlayers().map { it.name }
                val categories = AchievementCategory.entries.flatMap { listOf(it.name.lowercase()) + it.aliases }
                (players + categories)
                    .filter { it.lowercase().startsWith(args[0].lowercase()) }
            }
            2 -> {
                // Category names or page numbers
                val categories = AchievementCategory.entries.flatMap { listOf(it.name.lowercase()) + it.aliases }
                val pages = (1..10).map { it.toString() }
                (categories + pages)
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            }
            3 -> {
                // Page numbers
                (1..10).map { it.toString() }
                    .filter { it.startsWith(args[2]) }
            }
            else -> emptyList()
        }
    }
}
