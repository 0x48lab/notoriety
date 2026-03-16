package com.hacklab.minecraft.notoriety.zone.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.zone.model.ProtectedZone
import java.time.Instant
import java.util.UUID

class ProtectedZoneRepository(private val databaseManager: DatabaseManager) {

    fun findAll(): List<ProtectedZone> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM protected_zones")
            val rs = stmt.executeQuery()
            val zones = mutableListOf<ProtectedZone>()
            while (rs.next()) {
                zones.add(mapRow(rs))
            }
            zones
        }
    }

    fun findByName(name: String): ProtectedZone? {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM protected_zones WHERE name = ?")
            stmt.setString(1, name)
            val rs = stmt.executeQuery()
            if (rs.next()) mapRow(rs) else null
        }
    }

    fun insert(zone: ProtectedZone): Long {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                """INSERT INTO protected_zones (name, world_name, x1, y1, z1, x2, y2, z2, creator_uuid)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, zone.name)
            stmt.setString(2, zone.worldName)
            stmt.setInt(3, zone.x1)
            stmt.setInt(4, zone.y1)
            stmt.setInt(5, zone.z1)
            stmt.setInt(6, zone.x2)
            stmt.setInt(7, zone.y2)
            stmt.setInt(8, zone.z2)
            stmt.setString(9, zone.creatorUuid.toString())
            stmt.executeUpdate()

            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getLong(1) else throw IllegalStateException("Failed to get generated key")
            }
        }
    }

    fun deleteByName(name: String): Boolean {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("DELETE FROM protected_zones WHERE name = ?")
            stmt.setString(1, name)
            stmt.executeUpdate() > 0
        }
    }

    private fun mapRow(rs: java.sql.ResultSet): ProtectedZone {
        return ProtectedZone(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            worldName = rs.getString("world_name"),
            x1 = rs.getInt("x1"),
            y1 = rs.getInt("y1"),
            z1 = rs.getInt("z1"),
            x2 = rs.getInt("x2"),
            y2 = rs.getInt("y2"),
            z2 = rs.getInt("z2"),
            creatorUuid = UUID.fromString(rs.getString("creator_uuid")),
            createdAt = rs.getTimestamp("created_at")?.toInstant() ?: Instant.now()
        )
    }
}
