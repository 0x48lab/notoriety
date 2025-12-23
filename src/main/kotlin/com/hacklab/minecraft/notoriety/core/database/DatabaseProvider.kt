package com.hacklab.minecraft.notoriety.core.database

import java.sql.Connection

interface DatabaseProvider {
    fun initialize()
    fun shutdown()
    fun getConnection(): Connection

    fun <T> useConnection(block: (Connection) -> T): T {
        return getConnection().use { block(it) }
    }
}
