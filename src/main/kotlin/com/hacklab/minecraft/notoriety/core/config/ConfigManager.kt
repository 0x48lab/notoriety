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

    /**
     * Cooldown between sigil teleports in seconds.
     * Default: 30 seconds
     */
    val sigilTeleportCooldown: Int
        get() = config.getInt("territory.sigil-teleport-cooldown", 30)

    /**
     * Whether to show territory entry/exit notifications.
     * Default: true
     */
    val territoryNotificationsEnabled: Boolean
        get() = config.getBoolean("territory.notifications-enabled", true)

    // === Indirect PK Settings ===

    /**
     * Whether indirect PK detection is enabled.
     * Default: true
     */
    fun isIndirectPkEnabled(): Boolean =
        config.getBoolean("indirect-pk.enabled", true)

    /**
     * Tracking duration in seconds for hazard placements.
     * Default: 10
     */
    fun getIndirectPkTrackingDurationSeconds(): Long =
        config.getLong("indirect-pk.tracking-duration-seconds", 10)

    /**
     * Maximum distance in blocks between hazard and death location.
     * Default: 32
     */
    fun getIndirectPkMaxDistanceBlocks(): Int =
        config.getInt("indirect-pk.max-distance-blocks", 32)

    /**
     * Whether to track lava placements.
     * Default: true
     */
    fun isLavaTrackingEnabled(): Boolean =
        config.getBoolean("indirect-pk.tracking.lava", true)

    /**
     * Whether to track TNT ignitions.
     * Default: true
     */
    fun isTntTrackingEnabled(): Boolean =
        config.getBoolean("indirect-pk.tracking.tnt", true)

    /**
     * Whether to track fall damage from combat.
     * Default: true
     */
    fun isFallTrackingEnabled(): Boolean =
        config.getBoolean("indirect-pk.tracking.fall", true)

    /**
     * Whether to track piston push deaths.
     * Default: false (P3: Future implementation)
     */
    fun isPistonTrackingEnabled(): Boolean =
        config.getBoolean("indirect-pk.tracking.piston", false)

    /**
     * Whether to track water flow deaths.
     * Default: false (P3: Future implementation)
     */
    fun isWaterTrackingEnabled(): Boolean =
        config.getBoolean("indirect-pk.tracking.water", false)

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
