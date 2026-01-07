package com.hacklab.minecraft.notoriety.territory.model

import java.time.Instant

/**
 * ギルド領地エンティティ
 * 1ギルドにつき最大1つの領地を持つことができる
 */
data class GuildTerritory(
    val id: Long = 0,
    val guildId: Long,
    val createdAt: Instant = Instant.now(),
    val chunks: MutableList<TerritoryChunk> = mutableListOf()
) {
    /** 所有チャンク数 */
    val chunkCount: Int get() = chunks.size

    /**
     * 指定位置のチャンクを取得
     * @param worldName ワールド名
     * @param x X座標
     * @param z Z座標
     * @return 該当するチャンク、なければnull
     */
    fun getChunkAt(worldName: String, x: Int, z: Int): TerritoryChunk? {
        return chunks.find { it.worldName == worldName && it.contains(x, z) }
    }

    /**
     * 指定位置が領地内かどうか
     * @param worldName ワールド名
     * @param x X座標
     * @param z Z座標
     * @return 領地内ならtrue
     */
    fun containsLocation(worldName: String, x: Int, z: Int): Boolean {
        return getChunkAt(worldName, x, z) != null
    }

    /**
     * add_orderが最大のチャンク（最後に追加されたチャンク）を取得
     */
    fun getNewestChunk(): TerritoryChunk? {
        return chunks.maxByOrNull { it.addOrder }
    }

    /**
     * add_order順でソートされたチャンクリストを取得（降順：新しい順）
     */
    fun getChunksByAddOrderDesc(): List<TerritoryChunk> {
        return chunks.sortedByDescending { it.addOrder }
    }
}
