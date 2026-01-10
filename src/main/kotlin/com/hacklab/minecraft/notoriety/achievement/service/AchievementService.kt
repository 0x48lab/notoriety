package com.hacklab.minecraft.notoriety.achievement.service

import com.hacklab.minecraft.notoriety.achievement.model.Achievement
import com.hacklab.minecraft.notoriety.achievement.model.AchievementCategory
import com.hacklab.minecraft.notoriety.achievement.model.PlayerAchievement
import java.util.*

/**
 * アチーブメントサービスインターフェース
 */
interface AchievementService {
    /**
     * プレイヤーのアチーブメントをチェックし、達成していれば解除する
     * @param playerUuid プレイヤーUUID
     * @param context 判定コンテキスト（トリガーイベント情報など）
     * @return 新しく解除されたアチーブメントのリスト
     */
    fun checkAndUnlock(playerUuid: UUID, context: AchievementContext): List<Achievement>

    /**
     * プレイヤーの達成済みアチーブメント一覧を取得
     * @param playerUuid プレイヤーUUID
     * @return 達成記録のリスト
     */
    fun getUnlockedAchievements(playerUuid: UUID): List<PlayerAchievement>

    /**
     * プレイヤーが特定のアチーブメントを達成済みか確認
     * @param playerUuid プレイヤーUUID
     * @param achievementId アチーブメントID
     * @return 達成済みならtrue
     */
    fun hasUnlocked(playerUuid: UUID, achievementId: String): Boolean

    /**
     * カテゴリ別のアチーブメント進捗を取得
     * @param playerUuid プレイヤーUUID
     * @param category カテゴリ
     * @return (達成数, 全体数) のペア
     */
    fun getProgress(playerUuid: UUID, category: AchievementCategory): Pair<Int, Int>

    /**
     * 全アチーブメント定義を取得
     * @return アチーブメントのリスト
     */
    fun getAllAchievements(): List<Achievement>

    /**
     * カテゴリ別のアチーブメント定義を取得
     * @param category カテゴリ
     * @return アチーブメントのリスト
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<Achievement>

    /**
     * プレイヤーの全体進捗を取得
     * @param playerUuid プレイヤーUUID
     * @return (達成数, 全体数) のペア
     */
    fun getTotalProgress(playerUuid: UUID): Pair<Int, Int>

    /**
     * 達成済みアチーブメントIDのセットを取得（キャッシュ用）
     */
    fun getUnlockedIds(playerUuid: UUID): Set<String>
}
