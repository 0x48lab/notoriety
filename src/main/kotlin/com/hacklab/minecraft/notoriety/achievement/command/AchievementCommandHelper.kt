package com.hacklab.minecraft.notoriety.achievement.command

import com.hacklab.minecraft.notoriety.achievement.model.AchievementRarity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * アチーブメントコマンド用ヘルパーユーティリティ
 */
object AchievementCommandHelper {

    private const val PROGRESS_BAR_LENGTH = 20
    private const val PROGRESS_FILLED = '█'
    private const val PROGRESS_EMPTY = '░'

    /**
     * プログレスバーをテキストで生成
     * @param current 現在の達成数
     * @param total 全体数
     * @return プログレスバーを表すComponent
     */
    fun renderProgressBar(current: Int, total: Int): Component {
        val percentage = if (total > 0) current.toDouble() / total else 0.0
        val filled = (percentage * PROGRESS_BAR_LENGTH).toInt()
        val empty = PROGRESS_BAR_LENGTH - filled

        val bar = PROGRESS_FILLED.toString().repeat(filled) +
                PROGRESS_EMPTY.toString().repeat(empty)

        val color = when {
            percentage >= 1.0 -> NamedTextColor.GREEN
            percentage >= 0.5 -> NamedTextColor.YELLOW
            percentage > 0 -> NamedTextColor.GOLD
            else -> NamedTextColor.GRAY
        }

        return Component.text("[")
            .color(NamedTextColor.DARK_GRAY)
            .append(Component.text(bar).color(color))
            .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
            .append(Component.text(" $current/$total").color(NamedTextColor.WHITE))
    }

    /**
     * パーセンテージ表示付きプログレスバー
     */
    fun renderProgressBarWithPercentage(current: Int, total: Int): Component {
        val percentage = if (total > 0) (current.toDouble() / total * 100).toInt() else 0
        return renderProgressBar(current, total)
            .append(Component.text(" (${percentage}%)").color(NamedTextColor.GRAY))
    }

    /**
     * レアリティに応じた装飾付きアチーブメント名を生成
     */
    fun formatAchievementName(name: String, rarity: AchievementRarity, unlocked: Boolean): Component {
        val color = if (unlocked) rarity.color else NamedTextColor.DARK_GRAY
        val component = Component.text(name).color(color)

        return if (unlocked && rarity == AchievementRarity.LEGENDARY) {
            component.decorate(TextDecoration.BOLD)
        } else {
            component
        }
    }

    /**
     * アチーブメント達成状態のアイコンを生成
     */
    fun formatUnlockStatus(unlocked: Boolean): Component {
        return if (unlocked) {
            Component.text("✓ ").color(NamedTextColor.GREEN)
        } else {
            Component.text("✗ ").color(NamedTextColor.DARK_GRAY)
        }
    }

    /**
     * レアリティバッジを生成
     */
    fun formatRarityBadge(rarity: AchievementRarity): Component {
        val badge = when (rarity) {
            AchievementRarity.COMMON -> "[C]"
            AchievementRarity.UNCOMMON -> "[U]"
            AchievementRarity.RARE -> "[R]"
            AchievementRarity.EPIC -> "[E]"
            AchievementRarity.LEGENDARY -> "[L]"
        }
        return Component.text(badge).color(rarity.color)
    }

    /**
     * カテゴリヘッダーを生成
     */
    fun formatCategoryHeader(categoryName: String, current: Int, total: Int): Component {
        return Component.text("=== ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(categoryName).color(NamedTextColor.WHITE))
            .append(Component.text(" ===").color(NamedTextColor.GOLD))
            .append(Component.text(" "))
            .append(renderProgressBar(current, total))
    }

    /**
     * ページネーションフッターを生成
     */
    fun formatPagination(currentPage: Int, totalPages: Int, baseCommand: String): Component {
        val hasPrev = currentPage > 1
        val hasNext = currentPage < totalPages

        var component = Component.text("--- ")
            .color(NamedTextColor.GRAY)

        if (hasPrev) {
            component = component.append(
                Component.text("<< 前 ")
                    .color(NamedTextColor.AQUA)
            )
        }

        component = component.append(
            Component.text("[$currentPage/$totalPages]")
                .color(NamedTextColor.WHITE)
        )

        if (hasNext) {
            component = component.append(
                Component.text(" 次 >>")
                    .color(NamedTextColor.AQUA)
            )
        }

        return component.append(Component.text(" ---").color(NamedTextColor.GRAY))
    }

    /**
     * 達成率サマリーを生成
     */
    fun formatOverallSummary(unlocked: Int, total: Int, playerName: String): Component {
        val percentage = if (total > 0) (unlocked.toDouble() / total * 100).toInt() else 0

        return Component.text("=== ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(playerName).color(NamedTextColor.WHITE))
            .append(Component.text(" のアチーブメント ===").color(NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("達成率: ").color(NamedTextColor.GRAY))
            .append(renderProgressBarWithPercentage(unlocked, total))
    }
}
