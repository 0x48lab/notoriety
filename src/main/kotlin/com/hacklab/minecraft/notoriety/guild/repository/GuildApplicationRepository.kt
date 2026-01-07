package com.hacklab.minecraft.notoriety.guild.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.guild.model.GuildApplication
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * ギルド入会申請リポジトリ
 */
class GuildApplicationRepository(private val databaseManager: DatabaseManager) {

    fun insert(application: GuildApplication): Long {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT INTO guild_applications (guild_id, applicant_uuid, message, expires_at)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setLong(1, application.guildId)
                stmt.setString(2, application.applicantUuid.toString())
                stmt.setString(3, application.message)
                stmt.setTimestamp(4, Timestamp.from(application.expiresAt))
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getLong(1) else -1L
                }
            }
        }
    }

    fun findById(id: Long): GuildApplication? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guild_applications WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToApplication(rs) else null
                }
            }
        }
    }

    fun findByApplicantUuid(applicantUuid: UUID): List<GuildApplication> {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_applications
                WHERE applicant_uuid = ? AND expires_at > ?
                ORDER BY applied_at DESC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, applicantUuid.toString())
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    val applications = mutableListOf<GuildApplication>()
                    while (rs.next()) {
                        applications.add(mapToApplication(rs))
                    }
                    applications
                }
            }
        }
    }

    fun findByGuildId(guildId: Long): List<GuildApplication> {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_applications
                WHERE guild_id = ? AND expires_at > ?
                ORDER BY applied_at DESC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    val applications = mutableListOf<GuildApplication>()
                    while (rs.next()) {
                        applications.add(mapToApplication(rs))
                    }
                    applications
                }
            }
        }
    }

    fun findByGuildAndApplicant(guildId: Long, applicantUuid: UUID): GuildApplication? {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT * FROM guild_applications
                WHERE guild_id = ? AND applicant_uuid = ? AND expires_at > ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setString(2, applicantUuid.toString())
                stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToApplication(rs) else null
                }
            }
        }
    }

    fun findByGuildName(guildName: String, applicantUuid: UUID): GuildApplication? {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT ga.* FROM guild_applications ga
                JOIN guilds g ON ga.guild_id = g.id
                WHERE g.name = ? AND ga.applicant_uuid = ? AND ga.expires_at > ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, guildName)
                stmt.setString(2, applicantUuid.toString())
                stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToApplication(rs) else null
                }
            }
        }
    }

    fun delete(id: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_applications WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteByGuildId(guildId: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_applications WHERE guild_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteByApplicant(applicantUuid: UUID) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_applications WHERE applicant_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, applicantUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    fun deleteExpired() {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guild_applications WHERE expires_at <= ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.executeUpdate()
            }
        }
    }

    fun existsByGuildAndApplicant(guildId: Long, applicantUuid: UUID): Boolean {
        return findByGuildAndApplicant(guildId, applicantUuid) != null
    }

    fun countByGuildId(guildId: Long): Int {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                SELECT COUNT(*) FROM guild_applications
                WHERE guild_id = ? AND expires_at > ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, guildId)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    private fun mapToApplication(rs: ResultSet): GuildApplication {
        return GuildApplication(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            applicantUuid = UUID.fromString(rs.getString("applicant_uuid")),
            message = rs.getString("message"),
            appliedAt = rs.getTimestamp("applied_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant()
        )
    }
}
