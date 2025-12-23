package com.hacklab.minecraft.notoriety.trust

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.util.*

class TrustRepository(private val databaseManager: DatabaseManager) {

    fun addTrust(truster: UUID, trusted: UUID) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT OR IGNORE INTO player_trust (truster_uuid, trusted_uuid)
                VALUES (?, ?)
            """.trimIndent())
            stmt.setString(1, truster.toString())
            stmt.setString(2, trusted.toString())
            stmt.executeUpdate()
        }
    }

    fun removeTrust(truster: UUID, trusted: UUID) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "DELETE FROM player_trust WHERE truster_uuid = ? AND trusted_uuid = ?"
            )
            stmt.setString(1, truster.toString())
            stmt.setString(2, trusted.toString())
            stmt.executeUpdate()
        }
    }

    fun getTrustedPlayers(truster: UUID): List<UUID> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT trusted_uuid FROM player_trust WHERE truster_uuid = ?"
            )
            stmt.setString(1, truster.toString())

            val rs = stmt.executeQuery()
            val players = mutableListOf<UUID>()
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("trusted_uuid")))
            }
            players
        }
    }

    fun isTrusted(truster: UUID, trusted: UUID): Boolean {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT 1 FROM player_trust WHERE truster_uuid = ? AND trusted_uuid = ?"
            )
            stmt.setString(1, truster.toString())
            stmt.setString(2, trusted.toString())
            stmt.executeQuery().next()
        }
    }

    fun getTrusterCount(trusted: UUID): Int {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM player_trust WHERE trusted_uuid = ?"
            )
            stmt.setString(1, trusted.toString())
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        }
    }

    fun getPlayersWhoTrust(trusted: UUID): List<UUID> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT truster_uuid FROM player_trust WHERE trusted_uuid = ?"
            )
            stmt.setString(1, trusted.toString())

            val rs = stmt.executeQuery()
            val players = mutableListOf<UUID>()
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("truster_uuid")))
            }
            players
        }
    }
}
