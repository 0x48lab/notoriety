package com.hacklab.minecraft.notoriety.core.player

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class PlayerRepository(private val databaseManager: DatabaseManager) {

    fun load(uuid: UUID): PlayerData? {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM player_data WHERE uuid = ?"
            )
            stmt.setString(1, uuid.toString())
            val rs = stmt.executeQuery()

            if (rs.next()) {
                PlayerData(
                    uuid = uuid,
                    alignment = rs.getInt("alignment"),
                    pkCount = rs.getInt("pk_count"),
                    fame = rs.getInt("fame"),
                    playTimeMinutes = rs.getLong("play_time_minutes"),
                    lastSeen = rs.getTimestamp("last_seen")?.toInstant() ?: Instant.now(),
                    locale = rs.getString("locale")
                )
            } else {
                null
            }
        }
    }

    fun save(data: PlayerData) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO player_data (uuid, alignment, pk_count, fame, play_time_minutes, last_seen, locale)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    alignment = excluded.alignment,
                    pk_count = excluded.pk_count,
                    fame = excluded.fame,
                    play_time_minutes = excluded.play_time_minutes,
                    last_seen = excluded.last_seen,
                    locale = excluded.locale
            """.trimIndent())

            stmt.setString(1, data.uuid.toString())
            stmt.setInt(2, data.alignment)
            stmt.setInt(3, data.pkCount)
            stmt.setInt(4, data.fame)
            stmt.setLong(5, data.playTimeMinutes)
            stmt.setTimestamp(6, Timestamp.from(data.lastSeen))
            stmt.setString(7, data.locale)
            stmt.executeUpdate()
        }
    }

    fun delete(uuid: UUID) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("DELETE FROM player_data WHERE uuid = ?")
            stmt.setString(1, uuid.toString())
            stmt.executeUpdate()
        }
    }

    /**
     * 灰色プレイヤーを取得（alignment < 0 かつ pk_count == 0）
     */
    fun findGrayPlayers(): List<PlayerData> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM player_data WHERE alignment < 0 AND pk_count = 0 ORDER BY alignment ASC"
            )
            val rs = stmt.executeQuery()
            val players = mutableListOf<PlayerData>()
            while (rs.next()) {
                players.add(PlayerData(
                    uuid = UUID.fromString(rs.getString("uuid")),
                    alignment = rs.getInt("alignment"),
                    pkCount = rs.getInt("pk_count"),
                    fame = rs.getInt("fame"),
                    playTimeMinutes = rs.getLong("play_time_minutes"),
                    lastSeen = rs.getTimestamp("last_seen")?.toInstant() ?: Instant.now(),
                    locale = rs.getString("locale")
                ))
            }
            players
        }
    }

    /**
     * 赤プレイヤーを取得（pk_count >= 1）
     */
    fun findRedPlayers(): List<PlayerData> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM player_data WHERE pk_count >= 1 ORDER BY pk_count DESC"
            )
            val rs = stmt.executeQuery()
            val players = mutableListOf<PlayerData>()
            while (rs.next()) {
                players.add(PlayerData(
                    uuid = UUID.fromString(rs.getString("uuid")),
                    alignment = rs.getInt("alignment"),
                    pkCount = rs.getInt("pk_count"),
                    fame = rs.getInt("fame"),
                    playTimeMinutes = rs.getLong("play_time_minutes"),
                    lastSeen = rs.getTimestamp("last_seen")?.toInstant() ?: Instant.now(),
                    locale = rs.getString("locale")
                ))
            }
            players
        }
    }
}
