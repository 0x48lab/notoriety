package com.hacklab.minecraft.notoriety.core.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection

class SQLiteProvider(private val plugin: JavaPlugin, private val fileName: String) : DatabaseProvider {
    private lateinit var dataSource: HikariDataSource

    override fun initialize() {
        val dbFile = plugin.dataFolder.resolve(fileName)
        plugin.dataFolder.mkdirs()

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            maximumPoolSize = 1
            connectionTestQuery = "SELECT 1"
            poolName = "notoriety-sqlite-pool"
        }
        dataSource = HikariDataSource(config)
    }

    override fun shutdown() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }

    override fun getConnection(): Connection = dataSource.connection
}
