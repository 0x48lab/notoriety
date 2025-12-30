package com.hacklab.minecraft.notoriety.guild.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.guild.model.GuildMembership
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

class GuildMembershipRepository(private val databaseManager: DatabaseManager) {

    fun insert(membership: GuildMembership): Long {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT INTO guild_memberships (guild_id, player_uuid, role)
                VALUES (?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setLong(1, membership.guildId)
                stmt.setString(2, membership.playerUuid.toString())
                stmt.setString(3, membership.role.name)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getLong(1) else -1L
                }
            }
        }
    }

    fun findByPlayerUuid(playerUuid: UUID): GuildMembership? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guild_memberships WHERE player_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToMembership(rs) else null
                }
            }
        }
    }

    fun findByGuildId(guildId: Long, page: Int = 0, pageSize: Int = 45): List<GuildMembership> {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_memberships
                WHERE guild_id = ?
                ORDER BY
                    CASE role
                        WHEN 'MASTER' THEN 1
                        WHEN 'VICE_MASTER' THEN 2
                        ELSE 3
                    END,
                    joined_at ASC
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setInt(2, pageSize)
                stmt.setInt(3, page * pageSize)
                stmt.executeQuery().use { rs ->
                    val memberships = mutableListOf<GuildMembership>()
                    while (rs.next()) {
                        memberships.add(mapToMembership(rs))
                    }
                    memberships
                }
            }
        }
    }

    fun findAllByGuildId(guildId: Long): List<GuildMembership> {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guild_memberships WHERE guild_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.executeQuery().use { rs ->
                    val memberships = mutableListOf<GuildMembership>()
                    while (rs.next()) {
                        memberships.add(mapToMembership(rs))
                    }
                    memberships
                }
            }
        }
    }

    fun countByGuildId(guildId: Long): Int {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT COUNT(*) FROM guild_memberships WHERE guild_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun updateRole(playerUuid: UUID, role: GuildRole) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guild_memberships SET role = ? WHERE player_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, role.name)
                stmt.setString(2, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    fun deleteByPlayerUuid(playerUuid: UUID) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_memberships WHERE player_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    fun deleteByGuildId(guildId: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_memberships WHERE guild_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.executeUpdate()
            }
        }
    }

    fun areInSameGuild(player1: UUID, player2: UUID): Boolean {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT COUNT(*) FROM guild_memberships m1
                JOIN guild_memberships m2 ON m1.guild_id = m2.guild_id
                WHERE m1.player_uuid = ? AND m2.player_uuid = ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, player1.toString())
                stmt.setString(2, player2.toString())
                stmt.executeQuery().use { rs ->
                    rs.next() && rs.getInt(1) > 0
                }
            }
        }
    }

    private fun mapToMembership(rs: ResultSet): GuildMembership {
        return GuildMembership(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            playerUuid = UUID.fromString(rs.getString("player_uuid")),
            role = GuildRole.valueOf(rs.getString("role")),
            joinedAt = rs.getTimestamp("joined_at").toInstant()
        )
    }
}
