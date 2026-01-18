package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.cache.TerritoryCache
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import com.hacklab.minecraft.notoriety.territory.repository.SigilRepository
import com.hacklab.minecraft.notoriety.territory.repository.TerritoryRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.logging.Logger

/**
 * 既存領地データをv0.4.0シギルシステムにマイグレーションするサービス
 *
 * 処理内容:
 * 1. シギルを持たないチャンク（sigil_id = NULL）を持つ領地を検出
 * 2. BFSで連続チャンクをグループ化
 * 3. 各グループにデフォルトシギルを作成
 * 4. チャンクをシギルに紐付け
 * 5. ビーコンを配置
 */
class TerritoryMigrationService(
    private val repository: TerritoryRepository,
    private val sigilRepository: SigilRepository,
    private val beaconManager: BeaconManager,
    private val cache: TerritoryCache,
    private val logger: Logger
) {

    /**
     * マイグレーションを実行
     * @return マイグレーションされた領地数
     */
    fun migrateExistingTerritories(): MigrationResult {
        val territories = cache.getAllTerritories()
        var migratedTerritories = 0
        var createdSigils = 0
        var updatedChunks = 0

        for (territory in territories) {
            val result = migrateTerritory(territory)
            if (result.sigils > 0) {
                migratedTerritories++
                createdSigils += result.sigils
                updatedChunks += result.chunks
            }
        }

        if (migratedTerritories > 0) {
            logger.info("Territory migration completed: $migratedTerritories territories, $createdSigils sigils created, $updatedChunks chunks updated")
        }

        return MigrationResult(migratedTerritories, createdSigils, updatedChunks)
    }

    /**
     * 単一領地をマイグレーション
     */
    private fun migrateTerritory(territory: GuildTerritory): SingleMigrationResult {
        // シギルがないチャンクを検出
        val orphanChunks = territory.chunks.filter { it.sigilId == null }
        if (orphanChunks.isEmpty()) {
            return SingleMigrationResult(0, 0)
        }

        logger.info("Migrating territory ID ${territory.id} (guild ${territory.guildId}): ${orphanChunks.size} orphan chunks found")

        // BFSで連続グループを計算
        val groups = calculateContiguousGroups(orphanChunks)
        var totalSigils = 0
        var totalChunks = 0

        for ((index, group) in groups.withIndex()) {
            val result = createSigilForGroup(territory, group, index)
            if (result != null) {
                totalSigils++
                totalChunks += group.size
            }
        }

        return SingleMigrationResult(totalSigils, totalChunks)
    }

    /**
     * チャンクグループをBFSで計算（孤立チャンク専用）
     */
    private fun calculateContiguousGroups(chunks: List<TerritoryChunk>): List<Set<TerritoryChunk>> {
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
     * グループ用のシギルを作成してチャンクを紐付け
     */
    private fun createSigilForGroup(
        territory: GuildTerritory,
        group: Set<TerritoryChunk>,
        groupIndex: Int
    ): TerritorySigil? {
        if (group.isEmpty()) return null

        // 代表チャンク（追加順が最も古いもの）を選択
        val representativeChunk = group.minByOrNull { it.addOrder } ?: return null

        // シギル位置を計算（チャンク中央、地表）
        val world = Bukkit.getWorld(representativeChunk.worldName)
        if (world == null) {
            logger.warning("World '${representativeChunk.worldName}' not found, skipping sigil creation")
            return null
        }

        val centerX = representativeChunk.centerBlockX + 8  // チャンク中央
        val centerZ = representativeChunk.centerBlockZ + 8
        val surfaceY = world.getHighestBlockYAt(centerX, centerZ) + 1

        // デフォルト名を生成（既存シギル数 + グループインデックス）
        val existingSigilCount = territory.sigilCount
        val sigilName = TerritorySigil.generateDefaultName(existingSigilCount + groupIndex)

        // シギルを作成
        val newSigil = TerritorySigil(
            territoryId = territory.id,
            name = sigilName,
            worldName = representativeChunk.worldName,
            x = centerX.toDouble(),
            y = surfaceY.toDouble(),
            z = centerZ.toDouble()
        )
        val sigilId = sigilRepository.create(newSigil)
        val savedSigil = newSigil.copy(id = sigilId)

        // チャンクにシギルIDを設定
        for (chunk in group) {
            repository.updateChunkSigilId(chunk.id, sigilId)
            // キャッシュも更新
            cache.updateChunkSigilId(chunk.id, sigilId, territory.guildId)
        }

        // シギルをキャッシュに追加
        cache.addSigil(savedSigil, territory.guildId)

        // ビーコンを配置
        val beaconLocation = Location(world, centerX.toDouble(), surfaceY.toDouble(), centerZ.toDouble())
        beaconManager.placeBeacon(beaconLocation)

        logger.info("  Created sigil '$sigilName' at ($centerX, $surfaceY, $centerZ) for ${group.size} chunks")

        return savedSigil
    }

    data class MigrationResult(
        val territories: Int,
        val sigils: Int,
        val chunks: Int
    )

    private data class SingleMigrationResult(
        val sigils: Int,
        val chunks: Int
    )
}
