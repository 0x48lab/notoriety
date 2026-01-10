package com.hacklab.minecraft.notoriety.achievement.service

import com.hacklab.minecraft.notoriety.reputation.NameColor

/**
 * アチーブメント判定コンテキスト
 * イベントトリガー時の付加情報を保持
 */
data class AchievementContext(
    val triggeredEvent: String? = null,
    val isFirstTime: Boolean = false,
    val previousColor: NameColor? = null,
    val additionalData: Map<String, Any> = emptyMap()
) {
    companion object {
        val EMPTY = AchievementContext()

        fun forGoodDeed() = AchievementContext(triggeredEvent = "GOOD_DEED")
        fun forColorChange(previousColor: NameColor) = AchievementContext(
            triggeredEvent = "COLOR_CHANGE",
            previousColor = previousColor
        )
        fun forRedKill(isFirst: Boolean) = AchievementContext(
            triggeredEvent = "RED_KILL",
            isFirstTime = isFirst
        )
        fun forTrade() = AchievementContext(triggeredEvent = "TRADE")
        fun forGuildJoin() = AchievementContext(triggeredEvent = "GUILD_JOIN")
        fun forGuildCreate() = AchievementContext(triggeredEvent = "GUILD_CREATE")
        fun forBountyClaim() = AchievementContext(triggeredEvent = "BOUNTY_CLAIM")
    }
}
