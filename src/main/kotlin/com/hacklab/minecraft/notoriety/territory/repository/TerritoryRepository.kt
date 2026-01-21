package com.hacklab.minecraft.notoriety.territory.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import java.sql.Connection
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
     * チャンクを追加（新形式: ネイティブチャンク座標）
     * @param chunk チャンクデータ
     * @return 作成されたチャンクID
     */
    fun addChunk(chunk: TerritoryChunk): Long {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO territory_chunks
                (territory_id, world_name, chunk_x, chunk_z, sigil_id, add_order, center_x, center_z, beacon_y)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(), Statement.RETURN_GENERATED_KEYS)
            stmt.setLong(1, chunk.territoryId)
            stmt.setString(2, chunk.worldName)
            stmt.setInt(3, chunk.chunkX)
            stmt.setInt(4, chunk.chunkZ)
            if (chunk.sigilId != null) {
                stmt.setLong(5, chunk.sigilId)
            } else {
                stmt.setNull(5, java.sql.Types.BIGINT)
            }
            stmt.setInt(6, chunk.addOrder)
            // Legacy fields - set to center of chunk for backward compatibility
            stmt.setInt(7, chunk.centerBlockX)
            stmt.setInt(8, chunk.centerBlockZ)
            stmt.setInt(9, 64) // Default beacon Y
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getLong(1)
                else throw IllegalStateException("Failed to create chunk")
            }
        }
    }

    /**
     * チャンクのシギルIDを更新
     * @param chunkId チャンクID
     * @param sigilId シギルID（nullで解除）
     */
    fun updateChunkSigilId(chunkId: Long, sigilId: Long?) {
        provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "UPDATE territory_chunks SET sigil_id = ? WHERE id = ?"
            )
            if (sigilId != null) {
                stmt.setLong(1, sigilId)
            } else {
                stmt.setNull(1, java.sql.Types.BIGINT)
            }
            stmt.setLong(2, chunkId)
            stmt.executeUpdate()
        }
    }

    /**
     * 指定シギルの全チャンクのシギルIDを別のシギルに変更
     * グループ統合時に使用
     */
    fun updateChunksSigilId(oldSigilId: Long, newSigilId: Long) {
        provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "UPDATE territory_chunks SET sigil_id = ? WHERE sigil_id = ?"
            )
            stmt.setLong(1, newSigilId)
            stmt.setLong(2, oldSigilId)
            stmt.executeUpdate()
        }
    }

    /**
     * ギルドIDから領地を取得（シギル含む）
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
                    territory.sigils.addAll(getSigilsWithConnection(conn, territory.id))
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
    private fun getChunksWithConnection(conn: Connection, territoryId: Long): List<TerritoryChunk> {
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
     * 既存の接続を使ってシギルリストを取得（内部用）
     */
    private fun getSigilsWithConnection(conn: Connection, territoryId: Long): List<TerritorySigil> {
        val stmt = conn.prepareStatement(
            "SELECT * FROM territory_sigils WHERE territory_id = ? ORDER BY created_at"
        )
        stmt.setLong(1, territoryId)
        return stmt.executeQuery().use { rs ->
            val sigils = mutableListOf<TerritorySigil>()
            while (rs.next()) {
                sigils.add(mapSigil(rs))
            }
            sigils
        }
    }

    /**
     * 指定位置のチャンクが他のギルドに存在するか確認
     * @return 存在する場合、そのギルドID。存在しない場合null
     */
    fun findOverlappingGuildId(worldName: String, chunkX: Int, chunkZ: Int, excludeGuildId: Long? = null): Long? {
        return provider.useConnection { conn ->
            val sql = if (excludeGuildId != null) {
                """
                SELECT gt.guild_id FROM territory_chunks tc
                JOIN guild_territories gt ON tc.territory_id = gt.id
                WHERE tc.world_name = ? AND tc.chunk_x = ? AND tc.chunk_z = ?
                AND gt.guild_id != ?
                LIMIT 1
                """.trimIndent()
            } else {
                """
                SELECT gt.guild_id FROM territory_chunks tc
                JOIN guild_territories gt ON tc.territory_id = gt.id
                WHERE tc.world_name = ? AND tc.chunk_x = ? AND tc.chunk_z = ?
                LIMIT 1
                """.trimIndent()
            }
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, worldName)
            stmt.setInt(2, chunkX)
            stmt.setInt(3, chunkZ)
            if (excludeGuildId != null) {
                stmt.setLong(4, excludeGuildId)
            }
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getLong("guild_id") else null
            }
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
                    territory.sigils.addAll(getSigilsWithConnection(conn, territory.id))
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
                    territory.sigils.addAll(getSigilsWithConnection(conn, territory.id))
                    territory
                } else null
            }
        }
    }

    /**
     * 指定位置を含む領地を検索
     */
    fun getTerritoryAtLocation(worldName: String, blockX: Int, blockZ: Int): GuildTerritory? {
        val (chunkX, chunkZ) = TerritoryChunk.fromBlockCoords(blockX, blockZ)
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                SELECT gt.* FROM guild_territories gt
                JOIN territory_chunks tc ON gt.id = tc.territory_id
                WHERE tc.world_name = ? AND tc.chunk_x = ? AND tc.chunk_z = ?
                LIMIT 1
            """.trimIndent())
            stmt.setString(1, worldName)
            stmt.setInt(2, chunkX)
            stmt.setInt(3, chunkZ)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val territory = mapTerritory(rs)
                    territory.chunks.addAll(getChunksWithConnection(conn, territory.id))
                    territory.sigils.addAll(getSigilsWithConnection(conn, territory.id))
                    territory
                } else null
            }
        }
    }

    /**
     * モンスタースポーン設定を更新
     */
    fun updateMobSpawnEnabled(territoryId: Long, enabled: Boolean) {
        provider.useConnection { conn ->
            conn.prepareStatement(
                "UPDATE guild_territories SET mob_spawn_enabled = ? WHERE id = ?"
            ).use { stmt ->
                stmt.setBoolean(1, enabled)
                stmt.setLong(2, territoryId)
                stmt.executeUpdate()
            }
        }
    }

    private fun mapTerritory(rs: ResultSet): GuildTerritory {
        val createdAtStr = rs.getString("created_at")
        val createdAt = parseTimestamp(createdAtStr)
        val mobSpawnEnabled = try {
            rs.getBoolean("mob_spawn_enabled")
        } catch (e: Exception) {
            false
        }
        return GuildTerritory(
            id = rs.getLong("id"),
            guildId = rs.getLong("guild_id"),
            createdAt = createdAt,
            mobSpawnEnabled = mobSpawnEnabled
        )
    }

    private fun mapChunk(rs: ResultSet): TerritoryChunk {
        val addedAtStr = rs.getString("added_at")
        val addedAt = parseTimestamp(addedAtStr)

        // Try to read new columns first, fallback to legacy
        val chunkX = try {
            val value = rs.getInt("chunk_x")
            if (rs.wasNull()) {
                // Fallback: calculate from legacy center_x
                rs.getInt("center_x") shr 4
            } else value
        } catch (e: Exception) {
            rs.getInt("center_x") shr 4
        }

        val chunkZ = try {
            val value = rs.getInt("chunk_z")
            if (rs.wasNull()) {
                rs.getInt("center_z") shr 4
            } else value
        } catch (e: Exception) {
            rs.getInt("center_z") shr 4
        }

        val sigilId = try {
            val value = rs.getLong("sigil_id")
            if (rs.wasNull()) null else value
        } catch (e: Exception) {
            null
        }

        return TerritoryChunk(
            id = rs.getLong("id"),
            territoryId = rs.getLong("territory_id"),
            worldName = rs.getString("world_name"),
            chunkX = chunkX,
            chunkZ = chunkZ,
            sigilId = sigilId,
            addOrder = rs.getInt("add_order"),
            addedAt = addedAt,
            // Legacy fields
            centerX = try { rs.getInt("center_x") } catch (e: Exception) { null },
            centerZ = try { rs.getInt("center_z") } catch (e: Exception) { null },
            beaconY = try { rs.getInt("beacon_y") } catch (e: Exception) { null }
        )
    }

    private fun mapSigil(rs: ResultSet): TerritorySigil {
        return TerritorySigil(
            id = rs.getLong("id"),
            territoryId = rs.getLong("territory_id"),
            name = rs.getString("name"),
            worldName = rs.getString("world_name"),
            x = rs.getDouble("x"),
            y = rs.getDouble("y"),
            z = rs.getDouble("z"),
            createdAt = parseTimestamp(rs.getString("created_at"))
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
