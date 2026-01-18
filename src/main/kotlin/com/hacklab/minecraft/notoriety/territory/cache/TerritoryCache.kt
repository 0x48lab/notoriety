package com.hacklab.minecraft.notoriety.territory.cache

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import java.util.concurrent.ConcurrentHashMap

/**
 * 領地データのインメモリキャッシュ
 * 高速な座標検索のためにチャンク座標インデックスを保持
 */
class TerritoryCache {
    /** ギルドID -> 領地データ */
    private val byGuildId = ConcurrentHashMap<Long, GuildTerritory>()

    /** (ワールド名, チャンクX, チャンクZ) -> (チャンク, ギルドID) */
    private val byChunkCoords = ConcurrentHashMap<ChunkKey, Pair<TerritoryChunk, Long>>()

    /** シギルID -> シギル */
    private val sigilById = ConcurrentHashMap<Long, TerritorySigil>()

    /**
     * チャンク座標キー
     */
    data class ChunkKey(val worldName: String, val chunkX: Int, val chunkZ: Int)

    /**
     * ギルドIDから領地を取得
     */
    fun getTerritoryByGuild(guildId: Long): GuildTerritory? = byGuildId[guildId]

    /**
     * 指定ワールドの全チャンクを取得
     */
    fun getChunksInWorld(worldName: String): List<Pair<TerritoryChunk, Long>> {
        return byChunkCoords.entries
            .filter { it.key.worldName == worldName }
            .map { it.value }
    }

    /**
     * 指定ブロック座標のチャンクを検索
     * @param worldName ワールド名
     * @param blockX ブロックX座標
     * @param blockZ ブロックZ座標
     * @return チャンクとギルドIDのペア、見つからなければnull
     */
    fun findChunkAt(worldName: String, blockX: Int, blockZ: Int): Pair<TerritoryChunk, Long>? {
        val (chunkX, chunkZ) = TerritoryChunk.fromBlockCoords(blockX, blockZ)
        return findChunkByCoords(worldName, chunkX, chunkZ)
    }

    /**
     * 指定チャンク座標のチャンクを検索
     * @param worldName ワールド名
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     * @return チャンクとギルドIDのペア、見つからなければnull
     */
    fun findChunkByCoords(worldName: String, chunkX: Int, chunkZ: Int): Pair<TerritoryChunk, Long>? {
        val key = ChunkKey(worldName, chunkX, chunkZ)
        return byChunkCoords[key]
    }

    /**
     * 指定座標の領地を検索
     */
    fun findTerritoryAt(worldName: String, blockX: Int, blockZ: Int): GuildTerritory? {
        val (_, guildId) = findChunkAt(worldName, blockX, blockZ) ?: return null
        return byGuildId[guildId]
    }

    /**
     * 指定座標が領地内か確認
     */
    fun isInTerritory(worldName: String, blockX: Int, blockZ: Int): Boolean {
        return findChunkAt(worldName, blockX, blockZ) != null
    }

    /**
     * 指定座標が指定ギルドの領地内か確認
     */
    fun isInGuildTerritory(worldName: String, blockX: Int, blockZ: Int, guildId: Long): Boolean {
        val result = findChunkAt(worldName, blockX, blockZ) ?: return false
        return result.second == guildId
    }

    /**
     * シギルIDからシギルを取得
     */
    fun getSigilById(sigilId: Long): TerritorySigil? = sigilById[sigilId]

    /**
     * 領地をキャッシュに追加
     */
    fun addTerritory(territory: GuildTerritory) {
        byGuildId[territory.guildId] = territory
        territory.chunks.forEach { chunk ->
            addChunkToIndex(chunk, territory.guildId)
        }
        territory.sigils.forEach { sigil ->
            sigilById[sigil.id] = sigil
        }
    }

    /**
     * チャンクをインデックスに追加
     */
    private fun addChunkToIndex(chunk: TerritoryChunk, guildId: Long) {
        val key = ChunkKey(chunk.worldName, chunk.chunkX, chunk.chunkZ)
        byChunkCoords[key] = chunk to guildId
    }

    /**
     * 領地をキャッシュから削除
     */
    fun removeTerritory(guildId: Long) {
        val territory = byGuildId.remove(guildId) ?: return
        territory.chunks.forEach { chunk ->
            val key = ChunkKey(chunk.worldName, chunk.chunkX, chunk.chunkZ)
            byChunkCoords.remove(key)
        }
        territory.sigils.forEach { sigil ->
            sigilById.remove(sigil.id)
        }
    }

    /**
     * チャンクをキャッシュから削除
     */
    fun removeChunk(chunk: TerritoryChunk, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.chunks.removeIf { it.id == chunk.id }
        val key = ChunkKey(chunk.worldName, chunk.chunkX, chunk.chunkZ)
        byChunkCoords.remove(key)
    }

    /**
     * チャンクをキャッシュに追加
     */
    fun addChunk(chunk: TerritoryChunk, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.chunks.add(chunk)
        addChunkToIndex(chunk, guildId)
    }

    /**
     * シギルをキャッシュに追加
     */
    fun addSigil(sigil: TerritorySigil, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.sigils.add(sigil)
        sigilById[sigil.id] = sigil
    }

    /**
     * シギルをキャッシュから削除
     */
    fun removeSigil(sigilId: Long, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.sigils.removeIf { it.id == sigilId }
        sigilById.remove(sigilId)
    }

    /**
     * チャンクのシギルIDを更新
     */
    fun updateChunkSigilId(chunkId: Long, newSigilId: Long?, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        val chunkIndex = territory.chunks.indexOfFirst { it.id == chunkId }
        if (chunkIndex >= 0) {
            val oldChunk = territory.chunks[chunkIndex]
            val newChunk = oldChunk.copy(sigilId = newSigilId)
            territory.chunks[chunkIndex] = newChunk
            // Update index
            val key = ChunkKey(newChunk.worldName, newChunk.chunkX, newChunk.chunkZ)
            byChunkCoords[key] = newChunk to guildId
        }
    }

    /**
     * 全領地を取得
     */
    fun getAllTerritories(): List<GuildTerritory> = byGuildId.values.toList()

    /**
     * キャッシュをクリア
     */
    fun clear() {
        byGuildId.clear()
        byChunkCoords.clear()
        sigilById.clear()
    }

    /**
     * キャッシュを再読み込み（リポジトリから全データを読み込み）
     */
    fun reload(territories: List<GuildTerritory>) {
        clear()
        territories.forEach { addTerritory(it) }
    }
}
