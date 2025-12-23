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
                    crimePoint = rs.getInt("crime_point"),
                    pkCount = rs.getInt("pk_count"),
                    karma = rs.getInt("karma"),
                    fame = rs.getInt("fame"),
                    playTimeMinutes = rs.getLong("play_time_minutes"),
                    lastSeen = rs.getTimestamp("last_seen")?.toInstant() ?: Instant.now()
                )
            } else {
                null
            }
        }
    }

    fun save(data: PlayerData) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO player_data (uuid, crime_point, pk_count, karma, fame, play_time_minutes, last_seen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    crime_point = excluded.crime_point,
                    pk_count = excluded.pk_count,
                    karma = excluded.karma,
                    fame = excluded.fame,
                    play_time_minutes = excluded.play_time_minutes,
                    last_seen = excluded.last_seen
            """.trimIndent())

            stmt.setString(1, data.uuid.toString())
            stmt.setInt(2, data.crimePoint)
            stmt.setInt(3, data.pkCount)
            stmt.setInt(4, data.karma)
            stmt.setInt(5, data.fame)
            stmt.setLong(6, data.playTimeMinutes)
            stmt.setTimestamp(7, Timestamp.from(data.lastSeen))
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
}
