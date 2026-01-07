package com.hacklab.minecraft.notoriety.core.config

import com.hacklab.minecraft.notoriety.core.database.MySQLConfig
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class ConfigManager(private val plugin: JavaPlugin) {
    private val config: FileConfiguration
        get() = plugin.config

    val locale: String
        get() = config.getString("locale", "ja") ?: "ja"

    val databaseType: String
        get() = config.getString("database.type", "sqlite") ?: "sqlite"

    val sqliteFileName: String
        get() = config.getString("database.sqlite.file", "data.db") ?: "data.db"

    /**
     * Number of days before ownership expires when owner hasn't logged in.
     * Set to 0 to disable expiration.
     */
    val ownershipExpirationDays: Int
        get() = config.getInt("ownership.expiration-days", 30)

    // === Territory Settings ===

    /**
     * Maximum number of chunks a guild can own.
     * Default: 10
     */
    val territoryMaxChunks: Int
        get() = config.getInt("territory.max-chunks", 10)

    /**
     * Number of members required per chunk.
     * Default: 10 (10 members = 1 chunk, 20 members = 2 chunks, etc.)
     */
    val territoryMembersPerChunk: Int
        get() = config.getInt("territory.members-per-chunk", 10)

    init {
        plugin.saveDefaultConfig()
    }

    fun getMySQLConfig(): MySQLConfig {
        return MySQLConfig(
            host = config.getString("database.mysql.host", "localhost") ?: "localhost",
            port = config.getInt("database.mysql.port", 3306),
            database = config.getString("database.mysql.database", "notoriety") ?: "notoriety",
            username = config.getString("database.mysql.username", "minecraft") ?: "minecraft",
            password = config.getString("database.mysql.password", "") ?: "",
            poolSize = config.getInt("database.mysql.pool.maximum-pool-size", 10)
        )
    }

    fun reload() {
        plugin.reloadConfig()
    }
}
