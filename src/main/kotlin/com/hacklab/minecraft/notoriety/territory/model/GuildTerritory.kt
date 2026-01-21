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
    var mobSpawnEnabled: Boolean = false,
    val chunks: MutableList<TerritoryChunk> = mutableListOf(),
    val sigils: MutableList<TerritorySigil> = mutableListOf()
) {
    /** 所有チャンク数（総数、全ワールド合計） */
    val chunkCount: Int get() = chunks.size

    /** 総チャンク数のエイリアス */
    val totalChunkCount: Int get() = chunkCount

    /** シギル数 */
    val sigilCount: Int get() = sigils.size

    /**
     * 指定位置のチャンクを取得
     * @param worldName ワールド名
     * @param x ブロックX座標
     * @param z ブロックZ座標
     * @return 該当するチャンク、なければnull
     */
    fun getChunkAt(worldName: String, x: Int, z: Int): TerritoryChunk? {
        return chunks.find { it.worldName == worldName && it.containsBlock(x, z) }
    }

    /**
     * チャンク座標でチャンクを取得
     * @param worldName ワールド名
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     * @return 該当するチャンク、なければnull
     */
    fun getChunkByCoords(worldName: String, chunkX: Int, chunkZ: Int): TerritoryChunk? {
        return chunks.find {
            it.worldName == worldName && it.chunkX == chunkX && it.chunkZ == chunkZ
        }
    }

    /**
     * 指定位置が領地内かどうか
     * @param worldName ワールド名
     * @param x ブロックX座標
     * @param z ブロックZ座標
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

    /**
     * 指定シギルに属するチャンクを取得
     * @param sigilId シギルID
     * @return 該当するチャンクのリスト
     */
    fun getChunksForSigil(sigilId: Long): List<TerritoryChunk> {
        return chunks.filter { it.sigilId == sigilId }
    }

    /**
     * シギルをID検索
     * @param sigilId シギルID
     * @return 該当するシギル、なければnull
     */
    fun getSigilById(sigilId: Long): TerritorySigil? {
        return sigils.find { it.id == sigilId }
    }

    /**
     * シギルを名前検索（大文字小文字を区別しない）
     * @param name シギル名
     * @return 該当するシギル、なければnull
     */
    fun getSigilByName(name: String): TerritorySigil? {
        return sigils.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * 指定位置に隣接する既存チャンクを検索
     * @param worldName ワールド名
     * @param chunkX 新規チャンクのX座標
     * @param chunkZ 新規チャンクのZ座標
     * @return 隣接するチャンクのリスト
     */
    fun findAdjacentChunks(worldName: String, chunkX: Int, chunkZ: Int): List<TerritoryChunk> {
        return chunks.filter { chunk ->
            chunk.worldName == worldName &&
            ((kotlin.math.abs(chunk.chunkX - chunkX) == 1 && chunk.chunkZ == chunkZ) ||
             (chunk.chunkX == chunkX && kotlin.math.abs(chunk.chunkZ - chunkZ) == 1))
        }
    }

    /**
     * 連続領地グループを計算（BFS）
     * 同じワールド内で隣接するチャンクを1つのグループとする
     * @return グループのリスト（各グループはチャンクのセット）
     */
    fun calculateContiguousGroups(): List<Set<TerritoryChunk>> {
        val visited = mutableSetOf<TerritoryChunk>()
        val groups = mutableListOf<Set<TerritoryChunk>>()

        for (chunk in chunks) {
            if (chunk in visited) continue
            val group = mutableSetOf<TerritoryChunk>()
            val queue = ArrayDeque<TerritoryChunk>()
            queue.add(chunk)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current in visited) continue
                visited.add(current)
                group.add(current)

                // 隣接するチャンクをキューに追加
                chunks.filter { it !in visited && current.isAdjacentTo(it) }
                    .forEach { queue.add(it) }
            }
            groups.add(group)
        }
        return groups
    }

    /**
     * 隣接するシギルIDのセットを取得
     * グループ統合時に使用
     * @param worldName ワールド名
     * @param chunkX 新規チャンクのX座標
     * @param chunkZ 新規チャンクのZ座標
     * @return 隣接するチャンクが持つシギルIDのセット
     */
    fun getAdjacentSigilIds(worldName: String, chunkX: Int, chunkZ: Int): Set<Long> {
        return findAdjacentChunks(worldName, chunkX, chunkZ)
            .mapNotNull { it.sigilId }
            .toSet()
    }

    /**
     * 次のチャンクの追加順序を取得
     */
    fun getNextAddOrder(): Int {
        return (chunks.maxOfOrNull { it.addOrder } ?: 0) + 1
    }
}
