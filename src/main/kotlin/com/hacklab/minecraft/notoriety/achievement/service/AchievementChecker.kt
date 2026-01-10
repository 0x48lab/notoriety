package com.hacklab.minecraft.notoriety.achievement.service

import com.hacklab.minecraft.notoriety.achievement.model.Achievement
import com.hacklab.minecraft.notoriety.achievement.model.Achievements
import com.hacklab.minecraft.notoriety.core.player.PlayerData

/**
 * アチーブメント条件判定サービス
 */
class AchievementChecker {

    /**
     * プレイヤーが達成可能なアチーブメントをチェック
     * @param playerData プレイヤーデータ
     * @param context 判定コンテキスト
     * @param unlockedIds 既に達成済みのアチーブメントID
     * @return 新しく達成したアチーブメントのリスト
     */
    fun checkNewUnlocks(
        playerData: PlayerData,
        context: AchievementContext,
        unlockedIds: Set<String>
    ): List<Achievement> {
        return Achievements.all()
            .filter { it.id !in unlockedIds }
            .filter { it.condition.check(playerData, context) }
    }

    /**
     * 特定のアチーブメントが達成条件を満たすかチェック
     */
    fun checkCondition(
        achievement: Achievement,
        playerData: PlayerData,
        context: AchievementContext
    ): Boolean {
        return achievement.condition.check(playerData, context)
    }

    /**
     * プレイヤーの全アチーブメント進捗を取得
     * @return アチーブメントIDと進捗情報のマップ
     */
    fun getAllProgress(playerData: PlayerData): Map<String, Pair<Int, Int>?> {
        return Achievements.all().associate { achievement ->
            achievement.id to achievement.condition.getProgress(playerData)
        }
    }
}
