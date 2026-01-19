package com.hacklab.minecraft.notoriety.guild.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.TagColor
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

class GuildRepository(private val databaseManager: DatabaseManager) {

    fun insert(guild: Guild): Long {
        return databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT INTO guilds (name, tag, tag_color, description, master_uuid, max_members, is_government)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, guild.name)
                stmt.setString(2, guild.tag)
                stmt.setString(3, guild.tagColor.name)
                stmt.setString(4, guild.description)
                stmt.setString(5, guild.masterUuid.toString())
                stmt.setInt(6, guild.maxMembers)
                stmt.setBoolean(7, guild.isGovernment)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    if (rs.next()) rs.getLong(1) else -1L
                }
            }
        }
    }

    fun findById(id: Long): Guild? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guilds WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToGuild(rs) else null
                }
            }
        }
    }

    fun findByName(name: String): Guild? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guilds WHERE name = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, name)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToGuild(rs) else null
                }
            }
        }
    }

    fun findByTag(tag: String): Guild? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guilds WHERE tag = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tag)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToGuild(rs) else null
                }
            }
        }
    }

    fun findByMasterUuid(masterUuid: UUID): Guild? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guilds WHERE master_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, masterUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapToGuild(rs) else null
                }
            }
        }
    }

    fun findAll(page: Int = 0, pageSize: Int = 10): List<Guild> {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM guilds ORDER BY name LIMIT ? OFFSET ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pageSize)
                stmt.setInt(2, page * pageSize)
                stmt.executeQuery().use { rs ->
                    val guilds = mutableListOf<Guild>()
                    while (rs.next()) {
                        guilds.add(mapToGuild(rs))
                    }
                    guilds
                }
            }
        }
    }

    fun count(): Int {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT COUNT(*) FROM guilds"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun updateTagColor(id: Long, color: TagColor) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guilds SET tag_color = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, color.name)
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun updateMaster(id: Long, masterUuid: UUID) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guilds SET master_uuid = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, masterUuid.toString())
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun updateName(id: Long, name: String) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guilds SET name = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, name)
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun updateTag(id: Long, tag: String) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guilds SET tag = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tag)
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun updateDescription(id: Long, description: String?) {
        databaseManager.provider.useConnection { conn ->
            val sql = "UPDATE guilds SET description = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, description)
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        databaseManager.provider.useConnection { conn ->
            val sql = "DELETE FROM guilds WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
        }
    }

    fun existsByName(name: String): Boolean = findByName(name) != null

    fun existsByTag(tag: String): Boolean = findByTag(tag) != null

    private fun mapToGuild(rs: ResultSet): Guild {
        // Read is_government with fallback for legacy data
        val isGovernment = try {
            rs.getBoolean("is_government")
        } catch (e: Exception) {
            false
        }

        return Guild(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            tag = rs.getString("tag"),
            tagColor = TagColor.fromString(rs.getString("tag_color")) ?: TagColor.WHITE,
            description = rs.getString("description"),
            masterUuid = UUID.fromString(rs.getString("master_uuid")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            maxMembers = rs.getInt("max_members"),
            isGovernment = isGovernment
        )
    }
}
