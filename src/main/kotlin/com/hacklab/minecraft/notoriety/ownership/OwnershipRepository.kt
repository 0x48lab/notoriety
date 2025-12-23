package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.core.BlockLocation
import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.util.*

class OwnershipRepository(private val databaseManager: DatabaseManager) {

    fun setOwner(location: BlockLocation, ownerUuid: UUID) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO block_ownership (world, x, y, z, owner_uuid)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(world, x, y, z) DO UPDATE SET
                    owner_uuid = excluded.owner_uuid,
                    placed_at = CURRENT_TIMESTAMP
            """.trimIndent())

            stmt.setString(1, location.world)
            stmt.setInt(2, location.x)
            stmt.setInt(3, location.y)
            stmt.setInt(4, location.z)
            stmt.setString(5, ownerUuid.toString())
            stmt.executeUpdate()
        }
    }

    fun getOwner(location: BlockLocation): UUID? {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT owner_uuid FROM block_ownership WHERE world = ? AND x = ? AND y = ? AND z = ?"
            )
            stmt.setString(1, location.world)
            stmt.setInt(2, location.x)
            stmt.setInt(3, location.y)
            stmt.setInt(4, location.z)

            val rs = stmt.executeQuery()
            if (rs.next()) {
                UUID.fromString(rs.getString("owner_uuid"))
            } else {
                null
            }
        }
    }

    fun removeOwner(location: BlockLocation) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "DELETE FROM block_ownership WHERE world = ? AND x = ? AND y = ? AND z = ?"
            )
            stmt.setString(1, location.world)
            stmt.setInt(2, location.x)
            stmt.setInt(3, location.y)
            stmt.setInt(4, location.z)
            stmt.executeUpdate()
        }
    }

    fun getBlocksByOwner(ownerUuid: UUID): List<BlockLocation> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT world, x, y, z FROM block_ownership WHERE owner_uuid = ?"
            )
            stmt.setString(1, ownerUuid.toString())

            val rs = stmt.executeQuery()
            val blocks = mutableListOf<BlockLocation>()
            while (rs.next()) {
                blocks.add(BlockLocation(
                    world = rs.getString("world"),
                    x = rs.getInt("x"),
                    y = rs.getInt("y"),
                    z = rs.getInt("z")
                ))
            }
            blocks
        }
    }
}
