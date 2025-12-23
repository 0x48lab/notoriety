package com.hacklab.minecraft.notoriety.crime

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.sql.Timestamp
import java.util.*

class CrimeRepository(private val databaseManager: DatabaseManager) {

    fun recordCrime(record: CrimeRecord) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO crime_history
                (criminal_uuid, crime_type, victim_uuid, victim_name, world, x, y, z, detail, crime_point, committed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent())

            stmt.setString(1, record.criminalUuid.toString())
            stmt.setString(2, record.crimeType.name)
            stmt.setString(3, record.victimUuid?.toString())
            stmt.setString(4, record.victimName)
            stmt.setString(5, record.world)
            if (record.x != null) stmt.setInt(6, record.x) else stmt.setNull(6, java.sql.Types.INTEGER)
            if (record.y != null) stmt.setInt(7, record.y) else stmt.setNull(7, java.sql.Types.INTEGER)
            if (record.z != null) stmt.setInt(8, record.z) else stmt.setNull(8, java.sql.Types.INTEGER)
            stmt.setString(9, record.detail)
            stmt.setInt(10, record.crimePoint)
            stmt.setTimestamp(11, Timestamp.from(record.committedAt))
            stmt.executeUpdate()
        }
    }

    fun getHistory(playerUuid: UUID, page: Int, pageSize: Int): List<CrimeRecord> {
        return databaseManager.provider.useConnection { conn ->
            val offset = (page - 1) * pageSize
            val stmt = conn.prepareStatement("""
                SELECT * FROM crime_history
                WHERE criminal_uuid = ?
                ORDER BY committed_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent())
            stmt.setString(1, playerUuid.toString())
            stmt.setInt(2, pageSize)
            stmt.setInt(3, offset)

            val rs = stmt.executeQuery()
            val records = mutableListOf<CrimeRecord>()
            while (rs.next()) {
                records.add(CrimeRecord(
                    id = rs.getLong("id"),
                    criminalUuid = UUID.fromString(rs.getString("criminal_uuid")),
                    crimeType = CrimeType.valueOf(rs.getString("crime_type")),
                    victimUuid = rs.getString("victim_uuid")?.let { UUID.fromString(it) },
                    victimName = rs.getString("victim_name"),
                    world = rs.getString("world"),
                    x = rs.getObject("x") as? Int,
                    y = rs.getObject("y") as? Int,
                    z = rs.getObject("z") as? Int,
                    detail = rs.getString("detail"),
                    crimePoint = rs.getInt("crime_point"),
                    committedAt = rs.getTimestamp("committed_at").toInstant()
                ))
            }
            records
        }
    }

    fun getHistoryCount(playerUuid: UUID): Int {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM crime_history WHERE criminal_uuid = ?"
            )
            stmt.setString(1, playerUuid.toString())
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        }
    }

    fun getRecentCrimes(playerUuid: UUID, limit: Int): List<CrimeRecord> {
        return getHistory(playerUuid, 1, limit)
    }
}
