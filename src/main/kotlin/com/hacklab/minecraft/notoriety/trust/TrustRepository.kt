package com.hacklab.minecraft.notoriety.trust

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.util.*

class TrustRepository(private val databaseManager: DatabaseManager) {

    fun addTrust(truster: UUID, trusted: UUID) {
        setTrustState(truster, trusted, TrustState.TRUST)
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
                "SELECT trusted_uuid FROM player_trust WHERE truster_uuid = ? AND state = 'TRUST'"
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
        return getTrustState(truster, trusted) == TrustState.TRUST
    }

    fun getTrusterCount(trusted: UUID): Int {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM player_trust WHERE trusted_uuid = ? AND state = 'TRUST'"
            )
            stmt.setString(1, trusted.toString())
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        }
    }

    fun getPlayersWhoTrust(trusted: UUID): List<UUID> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT truster_uuid FROM player_trust WHERE trusted_uuid = ? AND state = 'TRUST'"
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

    // === 三段階信頼システム ===

    fun getTrustState(truster: UUID, trusted: UUID): TrustState? {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT state FROM player_trust WHERE truster_uuid = ? AND trusted_uuid = ?"
            )
            stmt.setString(1, truster.toString())
            stmt.setString(2, trusted.toString())
            val rs = stmt.executeQuery()
            if (rs.next()) {
                TrustState.fromString(rs.getString("state"))
            } else null
        }
    }

    fun setTrustState(truster: UUID, trusted: UUID, state: TrustState) {
        databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT OR REPLACE INTO player_trust (truster_uuid, trusted_uuid, state)
                VALUES (?, ?, ?)
            """.trimIndent())
            stmt.setString(1, truster.toString())
            stmt.setString(2, trusted.toString())
            stmt.setString(3, state.name)
            stmt.executeUpdate()
        }
    }

    fun getDistrustedPlayers(truster: UUID): List<UUID> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT trusted_uuid FROM player_trust WHERE truster_uuid = ? AND state = 'DISTRUST'"
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

    fun getAllTrustRelations(truster: UUID): Map<UUID, TrustState> {
        return databaseManager.provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT trusted_uuid, state FROM player_trust WHERE truster_uuid = ?"
            )
            stmt.setString(1, truster.toString())

            val rs = stmt.executeQuery()
            val relations = mutableMapOf<UUID, TrustState>()
            while (rs.next()) {
                val trustedUuid = UUID.fromString(rs.getString("trusted_uuid"))
                val state = TrustState.fromString(rs.getString("state"))
                if (state != null) {
                    relations[trustedUuid] = state
                }
            }
            relations
        }
    }
}
