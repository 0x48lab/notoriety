package com.hacklab.minecraft.notoriety.achievement.model

import net.kyori.adventure.text.format.NamedTextColor

/**
 * アチーブメントのレア度
 */
enum class AchievementRarity(val color: NamedTextColor) {
    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.BLUE),
    EPIC(NamedTextColor.DARK_PURPLE),
    LEGENDARY(NamedTextColor.GOLD);

    fun shouldAnnounce(): Boolean = this == EPIC || this == LEGENDARY
}
