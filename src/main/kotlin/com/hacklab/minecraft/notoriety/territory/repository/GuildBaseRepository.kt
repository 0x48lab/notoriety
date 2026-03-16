package com.hacklab.minecraft.notoriety.territory.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.territory.model.GuildBase
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant
import java.util.UUID

class GuildBaseRepository(private val databaseManager: DatabaseManager) {

    private val provider get() = databaseManager.provider

    fun upsert(base: GuildBase): Long {
        return provider.useConnection { conn ->
            // Try update first
            val updateStmt = conn.prepareStatement("""
                UPDATE guild_bases
                SET world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, set_by = ?, created_at = CURRENT_TIMESTAMP
                WHERE guild_id = ?
            """.trimIndent())
            updateStmt.setString(1, base.worldName)
            updateStmt.setDouble(2, base.x)
            updateStmt.setDouble(3, base.y)
            updateStmt.setDouble(4, base.z)
            updateStmt.setFloat(5, base.yaw)
            updateStmt.setFloat(6, base.pitch)
            updateStmt.setString(7, base.setBy.toString())
            updateStmt.setLong(8, base.guildId)
            val updated = updateStmt.executeUpdate()

            if (updated > 0) {
                // Return existing ID
                val selectStmt = conn.prepareStatement("SELECT id FROM guild_bases WHERE guild_id = ?")
                selectStmt.setLong(1, base.guildId)
                selectStmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            } else {
                // Insert new
                val insertStmt = conn.prepareStatement("""
                    INSERT INTO guild_bases (guild_id, world_name, x, y, z, yaw, pitch, set_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(), Statement.RETURN_GENERATED_KEYS)
                insertStmt.setLong(1, base.guildId)
                insertStmt.setString(2, base.worldName)
                insertStmt.setDouble(3, base.x)
                insertStmt.setDouble(4, base.y)
                insertStmt.setDouble(5, base.z)
                insertStmt.setFloat(6, base.yaw)
                insertStmt.setFloat(7, base.pitch)
                insertStmt.setString(8, base.setBy.toString())
                insertStmt.executeUpdate()

                insertStmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getLong(1)
                    else throw IllegalStateException("Failed to create guild base")
                }
            }
        }
    }

    fun findByGuildId(guildId: Long): GuildBase? {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM guild_bases WHERE guild_id = ?")
            stmt.setLong(1, guildId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapGuildBase(rs) else null
            }
        }
    }

    fun deleteByGuildId(guildId: Long): Boolean {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("DELETE FROM guild_bases WHERE guild_id = ?")
            stmt.setLong(1, guildId)
            stmt.executeUpdate() > 0
        }
    }

    private fun mapGuildBase(rs: ResultSet): GuildBase {
        return GuildBase(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            worldName = rs.getString("world_name"),
            x = rs.getDouble("x"),
            y = rs.getDouble("y"),
            z = rs.getDouble("z"),
            yaw = rs.getFloat("yaw"),
            pitch = rs.getFloat("pitch"),
            setBy = UUID.fromString(rs.getString("set_by")),
            createdAt = parseTimestamp(rs.getString("created_at"))
        )
    }

    private fun parseTimestamp(str: String?): Instant {
        if (str == null) return Instant.now()
        return try {
            if (str.contains("T")) {
                Instant.parse(str)
            } else {
                Instant.parse(str.replace(" ", "T") + "Z")
            }
        } catch (e: Exception) {
            Instant.now()
        }
    }
}
