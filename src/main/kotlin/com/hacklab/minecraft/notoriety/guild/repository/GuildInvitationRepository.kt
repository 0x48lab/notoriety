package com.hacklab.minecraft.notoriety.guild.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.guild.model.GuildInvitation
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class GuildInvitationRepository(private val databaseManager: DatabaseManager) {

    fun insert(invitation: GuildInvitation): Long {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT INTO guild_invitations (guild_id, inviter_uuid, invitee_uuid, expires_at)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setLong(1, invitation.guildId)
                stmt.setString(2, invitation.inviterUuid.toString())
                stmt.setString(3, invitation.inviteeUuid.toString())
                stmt.setTimestamp(4, Timestamp.from(invitation.expiresAt))
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getLong(1) else -1L
                }
            }
        }
    }

    fun findById(id: Long): GuildInvitation? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guild_invitations WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToInvitation(rs) else null
                }
            }
        }
    }

    fun findByInviteeUuid(inviteeUuid: UUID): List<GuildInvitation> {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_invitations
                WHERE invitee_uuid = ? AND expires_at > ?
                ORDER BY invited_at DESC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, inviteeUuid.toString())
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    val invitations = mutableListOf<GuildInvitation>()
                    while (rs.next()) {
                        invitations.add(mapToInvitation(rs))
                    }
                    invitations
                }
            }
        }
    }

    fun findByGuildAndInvitee(guildId: Long, inviteeUuid: UUID): GuildInvitation? {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_invitations
                WHERE guild_id = ? AND invitee_uuid = ? AND expires_at > ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setString(2, inviteeUuid.toString())
                stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToInvitation(rs) else null
                }
            }
        }
    }

    fun findByGuildName(guildName: String, inviteeUuid: UUID): GuildInvitation? {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT gi.* FROM guild_invitations gi
                JOIN guilds g ON gi.guild_id = g.id
                WHERE g.name = ? AND gi.invitee_uuid = ? AND gi.expires_at > ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, guildName)
                stmt.setString(2, inviteeUuid.toString())
                stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToInvitation(rs) else null
                }
            }
        }
    }

    fun delete(id: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_invitations WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteByGuildId(guildId: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_invitations WHERE guild_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteExpired() {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_invitations WHERE expires_at <= ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.executeUpdate()
            }
        }
    }

    fun existsByGuildAndInvitee(guildId: Long, inviteeUuid: UUID): Boolean {
        return findByGuildAndInvitee(guildId, inviteeUuid) != null
    }

    fun findByGuildId(guildId: Long): List<GuildInvitation> {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_invitations
                WHERE guild_id = ? AND expires_at > ?
                ORDER BY invited_at DESC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    val invitations = mutableListOf<GuildInvitation>()
                    while (rs.next()) {
                        invitations.add(mapToInvitation(rs))
                    }
                    invitations
                }
            }
        }
    }

    private fun mapToInvitation(rs: ResultSet): GuildInvitation {
        return GuildInvitation(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            inviterUuid = UUID.fromString(rs.getString("inviter_uuid")),
            inviteeUuid = UUID.fromString(rs.getString("invitee_uuid")),
            invitedAt = rs.getTimestamp("invited_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant()
        )
    }
}
