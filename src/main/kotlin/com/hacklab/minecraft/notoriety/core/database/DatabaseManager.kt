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
                        crime_point INT DEFAULT 0,
                        pk_count INT DEFAULT 0,
                        karma INT DEFAULT 0,
                        fame INT DEFAULT 0,
                        play_time_minutes BIGINT DEFAULT 0,
                        last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())

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
            }
        }
        plugin.logger.info("Database tables initialized successfully")
    }
}
