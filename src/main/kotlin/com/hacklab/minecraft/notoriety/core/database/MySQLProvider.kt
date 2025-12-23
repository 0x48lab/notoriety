package com.hacklab.minecraft.notoriety.core.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

data class MySQLConfig(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val poolSize: Int = 10
)

class MySQLProvider(private val config: MySQLConfig) : DatabaseProvider {
    private lateinit var dataSource: HikariDataSource

    override fun initialize() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}?useSSL=false&allowPublicKeyRetrieval=true"
            username = config.username
            password = config.password
            maximumPoolSize = config.poolSize
            minimumIdle = 2
            connectionTimeout = 30000
            poolName = "notoriety-mysql-pool"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        dataSource = HikariDataSource(hikariConfig)
    }

    override fun shutdown() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }

    override fun getConnection(): Connection = dataSource.connection
}
