package com.hacklab.minecraft.notoriety.territory.cache

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import java.util.concurrent.ConcurrentHashMap

/**
 * 領地データのインメモリキャッシュ
 * 高速な座標検索のためにワールド別チャンクリストも保持
 */
class TerritoryCache {
    /** ギルドID -> 領地データ */
    private val byGuildId = ConcurrentHashMap<Long, GuildTerritory>()

    /** ワールド名 -> チャンクリスト（高速検索用） */
    private val byWorld = ConcurrentHashMap<String, MutableList<Pair<TerritoryChunk, Long>>>()

    /**
     * ギルドIDから領地を取得
     */
    fun getTerritoryByGuild(guildId: Long): GuildTerritory? = byGuildId[guildId]

    /**
     * 指定ワールドのチャンクリストを取得
     */
    fun getChunksInWorld(worldName: String): List<Pair<TerritoryChunk, Long>> =
        byWorld[worldName]?.toList() ?: emptyList()

    /**
     * 指定座標のチャンクを検索
     * @return チャンクとギルドIDのペア、見つからなければnull
     */
    fun findChunkAt(worldName: String, x: Int, z: Int): Pair<TerritoryChunk, Long>? {
        return getChunksInWorld(worldName).find { (chunk, _) -> chunk.contains(x, z) }
    }

    /**
     * 指定座標の領地を検索
     */
    fun findTerritoryAt(worldName: String, x: Int, z: Int): GuildTerritory? {
        val (_, guildId) = findChunkAt(worldName, x, z) ?: return null
        return byGuildId[guildId]
    }

    /**
     * 領地をキャッシュに追加
     */
    fun addTerritory(territory: GuildTerritory) {
        byGuildId[territory.guildId] = territory
        territory.chunks.forEach { chunk ->
            addChunkToWorldIndex(chunk, territory.guildId)
        }
    }

    /**
     * チャンクをワールドインデックスに追加
     */
    private fun addChunkToWorldIndex(chunk: TerritoryChunk, guildId: Long) {
        byWorld.getOrPut(chunk.worldName) { mutableListOf() }.add(chunk to guildId)
    }

    /**
     * 領地をキャッシュから削除
     */
    fun removeTerritory(guildId: Long) {
        val territory = byGuildId.remove(guildId) ?: return
        territory.chunks.forEach { chunk ->
            byWorld[chunk.worldName]?.removeIf { (c, _) -> c.id == chunk.id }
        }
    }

    /**
     * チャンクをキャッシュから削除
     */
    fun removeChunk(chunk: TerritoryChunk, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.chunks.removeIf { it.id == chunk.id }
        byWorld[chunk.worldName]?.removeIf { (c, _) -> c.id == chunk.id }
    }

    /**
     * チャンクをキャッシュに追加
     */
    fun addChunk(chunk: TerritoryChunk, guildId: Long) {
        val territory = byGuildId[guildId] ?: return
        territory.chunks.add(chunk)
        addChunkToWorldIndex(chunk, guildId)
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
        byWorld.clear()
    }

    /**
     * キャッシュを再読み込み（リポジトリから全データを読み込み）
     */
    fun reload(territories: List<GuildTerritory>) {
        clear()
        territories.forEach { addTerritory(it) }
    }
}
