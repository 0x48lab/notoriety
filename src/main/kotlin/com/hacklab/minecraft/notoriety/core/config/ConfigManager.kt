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
