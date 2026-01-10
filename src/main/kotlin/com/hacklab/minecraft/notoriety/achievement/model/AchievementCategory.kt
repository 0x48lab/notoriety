package com.hacklab.minecraft.notoriety.achievement.model

/**
 * アチーブメントのカテゴリ
 */
enum class AchievementCategory(val displayKey: String, val aliases: List<String>) {
    REPUTATION("achievement.category.reputation", listOf("reputation", "rep")),
    COMBAT("achievement.category.combat", listOf("combat", "cmb")),
    GUILD("achievement.category.guild", listOf("guild", "gld")),
    REDEMPTION("achievement.category.redemption", listOf("redemption", "rdm")),
    VILLAGE("achievement.category.village", listOf("village", "vlg"));

    companion object {
        fun fromAlias(alias: String): AchievementCategory? {
            val lower = alias.lowercase()
            return entries.find { category ->
                category.aliases.any { it == lower }
            }
        }

        fun allAliases(): List<String> = entries.flatMap { it.aliases }
    }
}
