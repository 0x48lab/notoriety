package com.hacklab.minecraft.notoriety.achievement.model

import com.hacklab.minecraft.notoriety.achievement.service.AchievementCondition

/**
 * アチーブメント定義
 */
data class Achievement(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    val condition: AchievementCondition,
    val fameReward: Int = 0,
    val alignmentReward: Int = 0,
    val announceOnUnlock: Boolean = false,
    /** Minecraft Advancement key (e.g., "reputation/path_of_the_squire") */
    val advancementKey: String = ""
) {
    /**
     * サーバー全体にアナウンスするべきか判定
     */
    fun shouldAnnounce(): Boolean = announceOnUnlock || rarity.shouldAnnounce()
}
