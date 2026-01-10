package com.hacklab.minecraft.notoriety.achievement.repository

import com.hacklab.minecraft.notoriety.achievement.model.PlayerAchievement
import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.sql.Timestamp
import java.time.Instant
import java.util.*

/**
 * アチーブメント達成記録のリポジトリ
 */
class AchievementRepository(private val databaseManager: DatabaseManager) {

    /**
     * 達成記録を保存
     */
    fun save(playerUuid: UUID, achievementId: String): PlayerAchievement {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                """
                INSERT INTO player_achievements (player_uuid, achievement_id, unlocked_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, achievement_id) DO NOTHING
                """.trimIndent(),
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            val now = Instant.now()
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, achievementId)
            stmt.setTimestamp(3, Timestamp.from(now))
            stmt.executeUpdate()

            val generatedKeys = stmt.generatedKeys
            val id = if (generatedKeys.next()) generatedKeys.getLong(1) else 0L

            PlayerAchievement(
                id = id,
                playerUuid = playerUuid,
                achievementId = achievementId,
                unlockedAt = now
            )
        }
    }

    /**
     * プレイヤーの達成済みアチーブメントIDセットを取得
     */
    fun getUnlockedIds(playerUuid: UUID): Set<String> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT achievement_id FROM player_achievements WHERE player_uuid = ?"
            )
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()

            val ids = mutableSetOf<String>()
            while (rs.next()) {
                ids.add(rs.getString("achievement_id"))
            }
            ids
        }
    }

    /**
     * プレイヤーの達成記録一覧を取得
     */
    fun getAll(playerUuid: UUID): List<PlayerAchievement> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM player_achievements WHERE player_uuid = ? ORDER BY unlocked_at DESC"
            )
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()

            val achievements = mutableListOf<PlayerAchievement>()
            while (rs.next()) {
                achievements.add(
                    PlayerAchievement(
                        id = rs.getLong("id"),
                        playerUuid = UUID.fromString(rs.getString("player_uuid")),
                        achievementId = rs.getString("achievement_id"),
                        unlockedAt = rs.getTimestamp("unlocked_at")?.toInstant() ?: Instant.now()
                    )
                )
            }
            achievements
        }
    }

    /**
     * 特定のアチーブメントが達成済みか確認
     */
    fun isUnlocked(playerUuid: UUID, achievementId: String): Boolean {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT 1 FROM player_achievements WHERE player_uuid = ? AND achievement_id = ?"
            )
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, achievementId)
            val rs = stmt.executeQuery()
            rs.next()
        }
    }

    /**
     * プレイヤーの達成数を取得
     */
    fun getUnlockedCount(playerUuid: UUID): Int {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM player_achievements WHERE player_uuid = ?"
            )
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        }
    }
}
