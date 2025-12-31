package com.hacklab.minecraft.notoriety.chat.repository

import com.hacklab.minecraft.notoriety.chat.model.ChatMode
import com.hacklab.minecraft.notoriety.chat.model.PlayerChatSettings
import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import java.util.UUID

class ChatSettingsRepository(private val databaseManager: DatabaseManager) {

    fun findByPlayerUuid(playerUuid: UUID): PlayerChatSettings? {
        return databaseManager.provider.useConnection { conn ->
            val sql = "SELECT * FROM player_chat_settings WHERE player_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        // warnings_enabled カラムが存在しない場合のデフォルト値
                        val warningsEnabled = try {
                            rs.getBoolean("warnings_enabled")
                        } catch (e: Exception) {
                            true // デフォルト: ON
                        }
                        PlayerChatSettings(
                            playerUuid = UUID.fromString(rs.getString("player_uuid")),
                            chatMode = ChatMode.fromString(rs.getString("chat_mode")) ?: ChatMode.LOCAL,
                            romajiEnabled = rs.getBoolean("romaji_enabled"),
                            warningsEnabled = warningsEnabled
                        )
                    } else null
                }
            }
        }
    }

    fun save(settings: PlayerChatSettings) {
        databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT OR REPLACE INTO player_chat_settings (player_uuid, chat_mode, romaji_enabled, warnings_enabled)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settings.playerUuid.toString())
                stmt.setString(2, settings.chatMode.name)
                stmt.setBoolean(3, settings.romajiEnabled)
                stmt.setBoolean(4, settings.warningsEnabled)
                stmt.executeUpdate()
            }
        }
    }

    fun updateChatMode(playerUuid: UUID, mode: ChatMode) {
        databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT OR REPLACE INTO player_chat_settings (player_uuid, chat_mode, romaji_enabled, warnings_enabled)
                VALUES (?, ?,
                    COALESCE((SELECT romaji_enabled FROM player_chat_settings WHERE player_uuid = ?), FALSE),
                    COALESCE((SELECT warnings_enabled FROM player_chat_settings WHERE player_uuid = ?), TRUE))
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, mode.name)
                stmt.setString(3, playerUuid.toString())
                stmt.setString(4, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    fun updateRomajiEnabled(playerUuid: UUID, enabled: Boolean) {
        databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT OR REPLACE INTO player_chat_settings (player_uuid, chat_mode, romaji_enabled, warnings_enabled)
                VALUES (?,
                    COALESCE((SELECT chat_mode FROM player_chat_settings WHERE player_uuid = ?), 'LOCAL'),
                    ?,
                    COALESCE((SELECT warnings_enabled FROM player_chat_settings WHERE player_uuid = ?), TRUE))
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, playerUuid.toString())
                stmt.setBoolean(3, enabled)
                stmt.setString(4, playerUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    fun updateWarningsEnabled(playerUuid: UUID, enabled: Boolean) {
        databaseManager.provider.useConnection { conn ->
            val sql = """
                INSERT OR REPLACE INTO player_chat_settings (player_uuid, chat_mode, romaji_enabled, warnings_enabled)
                VALUES (?,
                    COALESCE((SELECT chat_mode FROM player_chat_settings WHERE player_uuid = ?), 'LOCAL'),
                    COALESCE((SELECT romaji_enabled FROM player_chat_settings WHERE player_uuid = ?), FALSE),
                    ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, playerUuid.toString())
                stmt.setString(3, playerUuid.toString())
                stmt.setBoolean(4, enabled)
                stmt.executeUpdate()
            }
        }
    }

    fun resetGuildChatMode(playerUuids: List<UUID>) {
        if (playerUuids.isEmpty()) return
        databaseManager.provider.useConnection { conn ->
            val placeholders = playerUuids.joinToString(",") { "?" }
            val sql = """
                UPDATE player_chat_settings
                SET chat_mode = 'LOCAL'
                WHERE player_uuid IN ($placeholders) AND chat_mode = 'GUILD'
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                playerUuids.forEachIndexed { index, uuid ->
                    stmt.setString(index + 1, uuid.toString())
                }
                stmt.executeUpdate()
            }
        }
    }

    fun getOrCreate(playerUuid: UUID): PlayerChatSettings {
        return findByPlayerUuid(playerUuid) ?: PlayerChatSettings(playerUuid).also { save(it) }
    }
}
