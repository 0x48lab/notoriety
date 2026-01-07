package com.hacklab.minecraft.notoriety.territory.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

/**
 * 領地データのDB操作リポジトリ
 */
class TerritoryRepository(private val databaseManager: DatabaseManager) {

    private val provider get() = databaseManager.provider

    /**
     * 領地を作成
     * @param guildId ギルドID
     * @return 作成された領地ID
     */
    fun createTerritory(guildId: Long): Long {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO guild_territories (guild_id) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
            )
            stmt.setLong(1, guildId)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getLong(1)
                else throw IllegalStateException("Failed to create territory")
            }
        }
    }

    /**
     * チャンクを追加
     * @param chunk チャンクデータ
     * @return 作成されたチャンクID
     */
    fun addChunk(chunk: TerritoryChunk): Long {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO territory_chunks
                (territory_id, world_name, center_x, center_z, beacon_y, add_order)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(), Statement.RETURN_GENERATED_KEYS)
            stmt.setLong(1, chunk.territoryId)
            stmt.setString(2, chunk.worldName)
            stmt.setInt(3, chunk.centerX)
            stmt.setInt(4, chunk.centerZ)
            stmt.setInt(5, chunk.beaconY)
            stmt.setInt(6, chunk.addOrder)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getLong(1)
                else throw IllegalStateException("Failed to create chunk")
            }
        }
    }

    /**
     * ギルドIDから領地を取得
     */
    fun getTerritoryByGuild(guildId: Long): GuildTerritory? {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM guild_territories WHERE guild_id = ?"
            )
            stmt.setLong(1, guildId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val territory = mapTerritory(rs)
                    territory.chunks.addAll(getChunksWithConnection(conn, territory.id))
                    territory
                } else null
            }
        }
    }

    /**
     * 領地IDからチャンクリストを取得
     */
    fun getChunks(territoryId: Long): List<TerritoryChunk> {
        return provider.useConnection { conn ->
            getChunksWithConnection(conn, territoryId)
        }
    }

    /**
     * 既存の接続を使ってチャンクリストを取得（内部用）
     */
    private fun getChunksWithConnection(conn: java.sql.Connection, territoryId: Long): List<TerritoryChunk> {
        val stmt = conn.prepareStatement(
            "SELECT * FROM territory_chunks WHERE territory_id = ? ORDER BY add_order"
        )
        stmt.setLong(1, territoryId)
        return stmt.executeQuery().use { rs ->
            val chunks = mutableListOf<TerritoryChunk>()
            while (rs.next()) {
                chunks.add(mapChunk(rs))
            }
            chunks
        }
    }

    /**
     * 領地を削除
     */
    fun deleteTerritory(guildId: Long) {
        provider.useConnection { conn ->
            conn.prepareStatement("DELETE FROM guild_territories WHERE guild_id = ?").use {
                it.setLong(1, guildId)
                it.executeUpdate()
            }
        }
    }

    /**
     * チャンクを削除
     */
    fun deleteChunk(chunkId: Long) {
        provider.useConnection { conn ->
            conn.prepareStatement("DELETE FROM territory_chunks WHERE id = ?").use {
                it.setLong(1, chunkId)
                it.executeUpdate()
            }
        }
    }

    /**
     * 全領地を取得
     */
    fun getAllTerritories(): List<GuildTerritory> {
        return provider.useConnection { conn ->
            val territories = mutableListOf<GuildTerritory>()
            conn.createStatement().executeQuery("SELECT * FROM guild_territories").use { rs ->
                while (rs.next()) {
                    val territory = mapTerritory(rs)
                    territory.chunks.addAll(getChunksWithConnection(conn, territory.id))
                    territories.add(territory)
                }
            }
            territories
        }
    }

    /**
     * 領地IDから領地を取得
     */
    fun getTerritoryById(territoryId: Long): GuildTerritory? {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM guild_territories WHERE id = ?"
            )
            stmt.setLong(1, territoryId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val territory = mapTerritory(rs)
                    territory.chunks.addAll(getChunksWithConnection(conn, territory.id))
                    territory
                } else null
            }
        }
    }

    private fun mapTerritory(rs: ResultSet): GuildTerritory {
        val createdAtStr = rs.getString("created_at")
        val createdAt = parseTimestamp(createdAtStr)
        return GuildTerritory(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            createdAt = createdAt
        )
    }

    private fun mapChunk(rs: ResultSet): TerritoryChunk {
        val addedAtStr = rs.getString("added_at")
        val addedAt = parseTimestamp(addedAtStr)
        return TerritoryChunk(
            id = rs.getLong("id"),
            territoryId = rs.getLong("territory_id"),
            worldName = rs.getString("world_name"),
            centerX = rs.getInt("center_x"),
            centerZ = rs.getInt("center_z"),
            beaconY = rs.getInt("beacon_y"),
            addOrder = rs.getInt("add_order"),
            addedAt = addedAt
        )
    }

    private fun parseTimestamp(str: String?): Instant {
        if (str == null) return Instant.now()
        return try {
            // SQLiteの形式に対応
            if (str.contains("T")) {
                Instant.parse(str)
            } else {
                // "yyyy-MM-dd HH:mm:ss" 形式の場合
                Instant.parse(str.replace(" ", "T") + "Z")
            }
        } catch (e: Exception) {
            Instant.now()
        }
    }
}
