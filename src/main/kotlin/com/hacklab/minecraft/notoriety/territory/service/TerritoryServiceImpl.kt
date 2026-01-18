package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.cache.TerritoryCache
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import com.hacklab.minecraft.notoriety.territory.repository.SigilRepository
import com.hacklab.minecraft.notoriety.territory.repository.TerritoryRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * 領地サービスの実装
 */
class TerritoryServiceImpl(
    private val plugin: JavaPlugin,
    private val repository: TerritoryRepository,
    private val sigilRepository: SigilRepository,
    private val guildService: GuildService,
    private val beaconManager: BeaconManager,
    private val configManager: ConfigManager
) : TerritoryService {

    private val cache = TerritoryCache()
    private val migrationService by lazy {
        TerritoryMigrationService(
            repository = repository,
            sigilRepository = sigilRepository,
            beaconManager = beaconManager,
            cache = cache,
            logger = plugin.logger
        )
    }

    companion object {
        /** 領地確保に必要な最小メンバー数（新方式: 不要、1人でも可能） */
        const val MIN_MEMBERS_FOR_TERRITORY = 1

        /** 新方式: 1 + floor(memberCount / 3) の除数 */
        const val MEMBERS_PER_CHUNK_DIVISOR = 3

        /** 旧方式: 1チャンクあたりの必要メンバー数（後方互換用） */
        @Deprecated("Use MEMBERS_PER_CHUNK_DIVISOR instead")
        const val MEMBERS_PER_CHUNK = 5
    }

    init {
        // 起動時にキャッシュを読み込み
        reloadCache()
        // 既存領地のシギルマイグレーション実行
        runMigration()
    }

    /**
     * 既存領地のシギルマイグレーションを実行
     */
    private fun runMigration() {
        val result = migrationService.migrateExistingTerritories()
        if (result.territories > 0) {
            plugin.logger.info("Migrated ${result.territories} territories with ${result.sigils} sigils and ${result.chunks} chunks")
        }
    }

    /**
     * キャッシュを取得（SigilServiceと共有するため）
     */
    fun getCache(): TerritoryCache = cache

    // === 領地管理 ===

    override fun claimTerritory(guildId: Long, location: Location, requester: UUID, sigilName: String?): ClaimResult {
        // ギルド存在チェック
        val guild = guildService.getGuild(guildId) ?: return ClaimResult.GuildNotFound

        // 権限チェック（ギルドマスターのみ）
        if (guild.masterUuid != requester) return ClaimResult.NotGuildMaster

        // メンバー数チェック
        val memberCount = guildService.getMemberCount(guildId)
        if (memberCount < MIN_MEMBERS_FOR_TERRITORY) return ClaimResult.NotEnoughMembers

        // 政府ギルドでなければチャンク上限チェック
        if (!guild.isGovernment) {
            val maxByConfig = configManager.territoryMaxChunks
            val maxByMembers = calculateAllowedChunks(memberCount)
            val currentChunks = getTerritory(guildId)?.chunkCount ?: 0

            if (currentChunks >= maxByConfig) return ClaimResult.MaxChunksReached
            if (currentChunks >= maxByMembers) return ClaimResult.MemberChunkLimitReached
        }

        // チャンク座標計算（Minecraft native chunk coords）
        val worldName = location.world?.name ?: return ClaimResult.GuildNotFound
        val (chunkX, chunkZ) = TerritoryChunk.fromBlockCoords(location.blockX, location.blockZ)

        // 他ギルドとの重複チェック
        val overlappingGuildId = repository.findOverlappingGuildId(worldName, chunkX, chunkZ, guildId)
        if (overlappingGuildId != null) {
            val otherGuild = guildService.getGuild(overlappingGuildId)
            return ClaimResult.OverlapOtherGuild(otherGuild?.name ?: "Unknown")
        }

        // 領地作成/取得
        var territory = getTerritory(guildId)
        val territoryId: Long
        val isFirstChunk: Boolean
        if (territory == null) {
            territoryId = repository.createTerritory(guildId)
            territory = GuildTerritory(id = territoryId, guildId = guildId)
            cache.addTerritory(territory)
            isFirstChunk = true
        } else {
            territoryId = territory.id
            isFirstChunk = territory.chunkCount == 0

            // 同じチャンクを既に持っているかチェック
            if (territory.getChunkByCoords(worldName, chunkX, chunkZ) != null) {
                return ClaimResult.AlreadyClaimed
            }
        }

        // 隣接チャンクとシギルの判定
        val adjacentChunks = findAdjacentChunks(guildId, worldName, chunkX, chunkZ)
        val adjacentSigilIds = territory.getAdjacentSigilIds(worldName, chunkX, chunkZ)
        val isEnclave = adjacentChunks.isEmpty() && !isFirstChunk

        // シギル処理
        var sigil: TerritorySigil? = null
        var isNewSigil = false
        var mergedSigilIds = emptyList<Long>()
        var assignedSigilId: Long? = null

        when {
            // 初回チャンク or 飛び地 → 新規シギル作成
            isFirstChunk || isEnclave -> {
                // シギル名バリデーション
                if (sigilName != null && !TerritorySigil.isValidName(sigilName)) {
                    return ClaimResult.InvalidSigilName("名前は32文字以内で、英数字・ひらがな・カタカナ・漢字のみ使用可能です")
                }
                if (sigilName != null && sigilRepository.existsByName(territoryId, sigilName)) {
                    return ClaimResult.SigilNameAlreadyExists(sigilName)
                }

                val actualName = sigilName ?: TerritorySigil.generateDefaultName(territory.sigilCount)
                val newSigil = TerritorySigil(
                    territoryId = territoryId,
                    name = actualName,
                    worldName = worldName,
                    x = location.x,
                    y = location.y,
                    z = location.z
                )
                val sigilId = sigilRepository.create(newSigil)
                sigil = newSigil.copy(id = sigilId)
                assignedSigilId = sigilId
                isNewSigil = true

                // ビーコン設置
                beaconManager.placeBeacon(location)

                cache.addSigil(sigil, guildId)
                plugin.logger.info("New sigil created: '${sigil.name}' for guild ${guild.name}")
            }

            // 複数グループを接続 → マージ
            adjacentSigilIds.size > 1 -> {
                val targetSigilId = mergeGroups(guildId, adjacentSigilIds)
                assignedSigilId = targetSigilId
                mergedSigilIds = adjacentSigilIds.filter { it != targetSigilId }
                sigil = targetSigilId?.let { cache.getSigilById(it) }
                plugin.logger.info("Groups merged: ${adjacentSigilIds.size} groups -> 1 for guild ${guild.name}")
            }

            // 隣接チャンクあり → 既存シギルに追加
            adjacentSigilIds.size == 1 -> {
                assignedSigilId = adjacentSigilIds.first()
                sigil = cache.getSigilById(assignedSigilId)
            }
        }

        // チャンク追加
        val newChunk = TerritoryChunk(
            territoryId = territoryId,
            worldName = worldName,
            chunkX = chunkX,
            chunkZ = chunkZ,
            sigilId = assignedSigilId,
            addOrder = territory.getNextAddOrder()
        )
        val chunkId = repository.addChunk(newChunk)
        val savedChunk = newChunk.copy(id = chunkId)

        // キャッシュ更新
        cache.addChunk(savedChunk, guildId)

        val totalChunks = territory.chunkCount + 1
        plugin.logger.info("Territory claimed: Guild ${guild.name} (ID: $guildId) claimed chunk ($chunkX, $chunkZ) in $worldName. Total: $totalChunks chunks")

        return ClaimResult.Success(savedChunk, sigil, isNewSigil, mergedSigilIds)
    }

    // === 隣接・飛び地判定 ===

    override fun findAdjacentChunks(guildId: Long, worldName: String, chunkX: Int, chunkZ: Int): List<TerritoryChunk> {
        val territory = getTerritory(guildId) ?: return emptyList()
        return territory.findAdjacentChunks(worldName, chunkX, chunkZ)
    }

    override fun isEnclave(guildId: Long, worldName: String, chunkX: Int, chunkZ: Int): Boolean {
        val territory = getTerritory(guildId) ?: return true  // 領地なし = 最初のチャンク
        if (territory.chunkCount == 0) return false  // 最初のチャンクは飛び地ではない
        return findAdjacentChunks(guildId, worldName, chunkX, chunkZ).isEmpty()
    }

    override fun mergeGroups(guildId: Long, adjacentSigilIds: Set<Long>): Long? {
        if (adjacentSigilIds.isEmpty()) return null
        if (adjacentSigilIds.size == 1) return adjacentSigilIds.first()

        val territory = getTerritory(guildId) ?: return null

        // 最も古いシギル（最初に作成されたもの）を残す
        val targetSigilId = adjacentSigilIds.minByOrNull { sigilId ->
            territory.getSigilById(sigilId)?.createdAt?.toEpochMilli() ?: Long.MAX_VALUE
        } ?: return null

        // 他のシギルのチャンクを対象シギルに移行
        for (oldSigilId in adjacentSigilIds) {
            if (oldSigilId == targetSigilId) continue

            // DBでチャンクのシギルIDを更新
            repository.updateChunksSigilId(oldSigilId, targetSigilId)

            // 古いシギルのビーコンを削除
            val oldSigil = cache.getSigilById(oldSigilId)
            oldSigil?.location?.let { beaconManager.removeBeacon(it) }

            // 古いシギルを削除
            sigilRepository.delete(oldSigilId)
            cache.removeSigil(oldSigilId, guildId)

            // キャッシュのチャンクも更新
            territory.chunks.filter { it.sigilId == oldSigilId }.forEach { chunk ->
                cache.updateChunkSigilId(chunk.id, targetSigilId, guildId)
            }
        }

        return targetSigilId
    }

    override fun releaseAllTerritory(guildId: Long, requester: UUID): ReleaseResult {
        val guild = guildService.getGuild(guildId) ?: return ReleaseResult.GuildNotFound

        // 権限チェック
        if (guild.masterUuid != requester) return ReleaseResult.NotGuildMaster

        val territory = getTerritory(guildId) ?: return ReleaseResult.NoTerritory

        val chunkCount = territory.chunkCount

        // ビーコン削除
        territory.chunks.forEach { chunk ->
            chunk.beaconLocation?.let { beaconManager.removeBeacon(it) }
        }

        // DB削除
        repository.deleteTerritory(guildId)

        // キャッシュ更新
        cache.removeTerritory(guildId)

        plugin.logger.info("Territory released: Guild ${guild.name} (ID: $guildId) released all territory ($chunkCount chunks)")

        return ReleaseResult.Success(chunkCount)
    }

    override fun releaseChunk(guildId: Long, chunkNumber: Int, requester: UUID): ReleaseResult {
        val guild = guildService.getGuild(guildId) ?: return ReleaseResult.GuildNotFound

        // 権限チェック
        if (guild.masterUuid != requester) return ReleaseResult.NotGuildMaster

        val territory = getTerritory(guildId) ?: return ReleaseResult.NoTerritory

        // 指定番号のチャンクを検索
        val chunk = territory.chunks.find { it.addOrder == chunkNumber }
            ?: return ReleaseResult.ChunkNotFound(chunkNumber)

        // ビーコン削除
        chunk.beaconLocation?.let { beaconManager.removeBeacon(it) }

        // DB削除
        repository.deleteChunk(chunk.id)

        // キャッシュ更新
        cache.removeChunk(chunk, guildId)

        // 全チャンク削除なら領地自体を削除
        if (territory.chunkCount <= 1) {
            repository.deleteTerritory(guildId)
            cache.removeTerritory(guildId)
        }

        plugin.logger.info("Territory chunk released: Guild ${guild.name} (ID: $guildId) released chunk #$chunkNumber at (${chunk.centerX}, ${chunk.centerZ})")

        return ReleaseResult.Success(1)
    }

    override fun shrinkTerritoryTo(guildId: Long, targetChunkCount: Int) {
        val territory = getTerritory(guildId) ?: return
        if (territory.chunkCount <= targetChunkCount) return

        val fromCount = territory.chunkCount

        // LIFO順で削除（add_orderが大きい順）
        val chunksToRemove = territory.getChunksByAddOrderDesc()
            .take(territory.chunkCount - targetChunkCount)

        // 削除されるチャンクに紐づくシギルIDを記録
        val affectedSigilIds = chunksToRemove.mapNotNull { it.sigilId }.toSet()

        chunksToRemove.forEach { chunk ->
            // DB削除
            repository.deleteChunk(chunk.id)
            // キャッシュ更新
            cache.removeChunk(chunk, guildId)
        }

        // 全チャンク削除なら領地自体を削除
        if (targetChunkCount == 0) {
            // 全シギルのビーコンを削除
            territory.sigils.forEach { sigil ->
                sigil.location?.let { beaconManager.removeBeacon(it) }
            }
            // シギル削除
            sigilRepository.deleteByTerritory(territory.id)
            // 領地削除
            repository.deleteTerritory(guildId)
            cache.removeTerritory(guildId)
        } else {
            // シギルのクリーンアップ - チャンクがなくなったシギルを削除
            cleanupEmptySigils(guildId, affectedSigilIds)
        }

        plugin.logger.info("Territory shrunk: Guild ID $guildId shrunk from $fromCount to $targetChunkCount chunks (removed ${fromCount - targetChunkCount} chunks)")
    }

    /**
     * チャンクがなくなったシギルを削除
     */
    private fun cleanupEmptySigils(guildId: Long, affectedSigilIds: Set<Long>) {
        val territory = getTerritory(guildId) ?: return

        for (sigilId in affectedSigilIds) {
            val chunksForSigil = territory.getChunksForSigil(sigilId)
            if (chunksForSigil.isEmpty()) {
                val sigil = cache.getSigilById(sigilId) ?: continue
                // ビーコン削除
                sigil.location?.let { beaconManager.removeBeacon(it) }
                // DB削除
                sigilRepository.delete(sigilId)
                // キャッシュ削除
                cache.removeSigil(sigilId, guildId)
                plugin.logger.info("Sigil '${sigil.name}' deleted (no remaining chunks)")
            }
        }
    }

    // === 領地取得 ===

    override fun getTerritory(guildId: Long): GuildTerritory? {
        return cache.getTerritoryByGuild(guildId)
    }

    override fun getChunkAt(location: Location): TerritoryChunk? {
        val worldName = location.world?.name ?: return null
        return cache.findChunkAt(worldName, location.blockX, location.blockZ)?.first
    }

    override fun getTerritoryAt(location: Location): GuildTerritory? {
        val worldName = location.world?.name ?: return null
        return cache.findTerritoryAt(worldName, location.blockX, location.blockZ)
    }

    override fun getAllTerritories(): List<GuildTerritory> {
        return cache.getAllTerritories()
    }

    // === 判定 ===

    override fun isInTerritory(location: Location): Boolean {
        return getTerritoryAt(location) != null
    }

    override fun isInGuildTerritory(location: Location, guildId: Long): Boolean {
        val territory = getTerritoryAt(location) ?: return false
        return territory.guildId == guildId
    }

    override fun canAccessAt(location: Location, playerUuid: UUID): Boolean {
        val territory = getTerritoryAt(location) ?: return true  // 領地外は自由

        // ギルドメンバーかチェック
        val playerGuild = guildService.getPlayerGuild(playerUuid)
        return playerGuild?.id == territory.guildId
    }

    // === 計算 ===

    /**
     * メンバー数から許可されるチャンク数を計算
     * 新方式: 1 + floor(memberCount / 3)
     */
    override fun calculateAllowedChunks(memberCount: Int): Int {
        if (memberCount < MIN_MEMBERS_FOR_TERRITORY) return 0
        // 新方式: 1 + floor(memberCount / 3)
        val calculatedMax = 1 + (memberCount / MEMBERS_PER_CHUNK_DIVISOR)
        return minOf(calculatedMax, configManager.territoryMaxChunks)
    }

    override fun canClaimMoreChunks(guildId: Long): Boolean {
        // 政府ギルドは無制限
        val guild = guildService.getGuild(guildId)
        if (guild?.isGovernment == true) return true

        val territory = getTerritory(guildId)
        val currentChunks = territory?.chunkCount ?: 0

        val memberCount = guildService.getMemberCount(guildId)
        val maxByMembers = calculateAllowedChunks(memberCount)
        val maxByConfig = configManager.territoryMaxChunks

        return currentChunks < maxByMembers && currentChunks < maxByConfig
    }

    // === キャッシュ管理 ===

    override fun reloadCache() {
        val territories = repository.getAllTerritories()
        cache.reload(territories)
        plugin.logger.info("Loaded ${territories.size} territories into cache")
    }
}
