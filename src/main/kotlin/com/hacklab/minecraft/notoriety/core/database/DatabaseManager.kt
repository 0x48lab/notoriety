package com.hacklab.minecraft.notoriety.core.database

import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin

class DatabaseManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager
) {
    lateinit var provider: DatabaseProvider
        private set

    fun initialize() {
        provider = when (configManager.databaseType) {
            "mysql" -> MySQLProvider(configManager.getMySQLConfig())
            else -> SQLiteProvider(plugin, configManager.sqliteFileName)
        }
        provider.initialize()
        createTables()
    }

    fun shutdown() {
        if (::provider.isInitialized) {
            provider.shutdown()
        }
    }

    private fun createTables() {
        provider.useConnection { conn ->
            conn.createStatement().use { stmt ->
                // Player data table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid VARCHAR(36) PRIMARY KEY,
                        alignment INT DEFAULT 0,
                        pk_count INT DEFAULT 0,
                        fame INT DEFAULT 0,
                        play_time_minutes BIGINT DEFAULT 0,
                        last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

                // Migration: If old schema exists, migrate to new schema
                try {
                    stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN alignment INT DEFAULT 0")
                } catch (e: Exception) {
                    // Column already exists, ignore
                }

                // Block ownership table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS block_ownership (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        world VARCHAR(64) NOT NULL,
                        x INT NOT NULL,
                        y INT NOT NULL,
                        z INT NOT NULL,
                        owner_uuid VARCHAR(36) NOT NULL,
                        placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

                // Create unique index for block ownership
                stmt.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_block_location
                    ON block_ownership (world, x, y, z)
                """.trimIndent())

                // Create index for owner lookup
                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_block_owner
                    ON block_ownership (owner_uuid)
                """.trimIndent())

                // Player trust table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_trust (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        truster_uuid VARCHAR(36) NOT NULL,
                        trusted_uuid VARCHAR(36) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

                // Create unique index for trust relationships
                stmt.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_trust_pair
                    ON player_trust (truster_uuid, trusted_uuid)
                """.trimIndent())

                // Crime history table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS crime_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        criminal_uuid VARCHAR(36) NOT NULL,
                        crime_type VARCHAR(32) NOT NULL,
                        victim_uuid VARCHAR(36),
                        victim_name VARCHAR(64),
                        world VARCHAR(64),
                        x INT,
                        y INT,
                        z INT,
                        detail VARCHAR(255),
                        crime_point INT NOT NULL,
                        committed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

                // Create indexes for crime history
                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_crime_criminal
                    ON crime_history (criminal_uuid, committed_at)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_crime_victim
                    ON crime_history (victim_uuid)
                """.trimIndent())

                // ===== Guild System Tables =====

                // Guilds table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS guilds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(32) NOT NULL UNIQUE,
                        tag VARCHAR(4) NOT NULL UNIQUE,
                        tag_color VARCHAR(16) NOT NULL DEFAULT 'WHITE',
                        description VARCHAR(255),
                        master_uuid VARCHAR(36) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        max_members INT DEFAULT 50
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_guild_master
                    ON guilds (master_uuid)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_guild_tag
                    ON guilds (tag)
                """.trimIndent())

                // Guild memberships table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS guild_memberships (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        guild_id INTEGER NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL UNIQUE,
                        role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
                        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_membership_guild
                    ON guild_memberships (guild_id)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_membership_player
                    ON guild_memberships (player_uuid)
                """.trimIndent())

                // Guild invitations table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS guild_invitations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        guild_id INTEGER NOT NULL,
                        inviter_uuid VARCHAR(36) NOT NULL,
                        invitee_uuid VARCHAR(36) NOT NULL,
                        invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        expires_at TIMESTAMP NOT NULL,
                        FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_invitation_invitee
                    ON guild_invitations (invitee_uuid)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_invitation_guild
                    ON guild_invitations (guild_id)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_invitation_expires
                    ON guild_invitations (expires_at)
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_invitation_unique
                    ON guild_invitations (guild_id, invitee_uuid)
                """.trimIndent())

                // Migration: Add state column to player_trust for three-state trust
                try {
                    stmt.executeUpdate("ALTER TABLE player_trust ADD COLUMN state VARCHAR(16) NOT NULL DEFAULT 'TRUST'")
                } catch (e: Exception) {
                    // Column already exists, ignore
                }

                // Player chat settings table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_chat_settings (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        chat_mode VARCHAR(16) NOT NULL DEFAULT 'LOCAL',
                        romaji_enabled BOOLEAN NOT NULL DEFAULT FALSE
                    )
                """.trimIndent())
            }
        }
        plugin.logger.info("Database tables initialized successfully")
    }
}
