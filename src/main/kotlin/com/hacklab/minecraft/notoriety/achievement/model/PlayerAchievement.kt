package com.hacklab.minecraft.notoriety.achievement.model

import java.time.Instant
import java.util.*

/**
 * プレイヤーのアチーブメント達成記録
 */
data class PlayerAchievement(
    val id: Long,
    val playerUuid: UUID,
    val achievementId: String,
    val unlockedAt: Instant
) {
    /**
     * 対応するアチーブメント定義を取得
     */
    fun getAchievement(): Achievement? = Achievements.findById(achievementId)
}
