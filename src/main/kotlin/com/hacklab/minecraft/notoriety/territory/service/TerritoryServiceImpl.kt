package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.cache.TerritoryCache
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
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
    private val guildService: GuildService,
    private val beaconManager: BeaconManager,
    private val configManager: ConfigManager
) : TerritoryService {

    private val cache = TerritoryCache()

    companion object {
        /** 領地確保に必要な最小メンバー数 */
        const val MIN_MEMBERS_FOR_TERRITORY = 5

        /** 1チャンクあたりの必要メンバー数 */
        const val MEMBERS_PER_CHUNK = 5
    }

    init {
        // 起動時にキャッシュを読み込み
        reloadCache()
    }

    // === 領地管理 ===

    override fun claimTerritory(guildId: Long, location: Location, requester: UUID): ClaimResult {
        // ギルド存在チェック
        val guild = guildService.getGuild(guildId) ?: return ClaimResult.GuildNotFound

        // 権限チェック（ギルドマスターのみ）
        if (guild.masterUuid != requester) return ClaimResult.NotGuildMaster

        // メンバー数チェック
        val memberCount = guildService.getMemberCount(guildId)
        if (memberCount < MIN_MEMBERS_FOR_TERRITORY) return ClaimResult.NotEnoughMembers

        // チャンク上限チェック
        val maxByConfig = configManager.territoryMaxChunks
        val maxByMembers = calculateAllowedChunks(memberCount)
        val currentChunks = getTerritory(guildId)?.chunkCount ?: 0

        if (currentChunks >= maxByConfig) return ClaimResult.MaxChunksReached
        if (currentChunks >= maxByMembers) return ClaimResult.MemberChunkLimitReached

        // 重複チェック
        val centerX = location.blockX
        val centerZ = location.blockZ
        val worldName = location.world?.name ?: return ClaimResult.GuildNotFound

        // 新しいチャンクを作成（重複チェック用）
        val newChunkForCheck = TerritoryChunk(
            worldName = worldName,
            centerX = centerX,
            centerZ = centerZ,
            beaconY = location.blockY
        )

        for (territory in getAllTerritories()) {
            if (territory.guildId == guildId) continue  // 自ギルドとの重複はOK
            for (chunk in territory.chunks) {
                if (newChunkForCheck.overlapsWith(chunk)) {
                    val otherGuild = guildService.getGuild(territory.guildId)
                    return ClaimResult.OverlapOtherGuild(otherGuild?.name ?: "Unknown")
                }
            }
        }

        // 領地作成/取得
        var territory = getTerritory(guildId)
        val territoryId: Long
        if (territory == null) {
            territoryId = repository.createTerritory(guildId)
            territory = GuildTerritory(id = territoryId, guildId = guildId)
            cache.addTerritory(territory)
        } else {
            territoryId = territory.id
        }

        // チャンク追加
        val newChunk = TerritoryChunk(
            territoryId = territoryId,
            worldName = worldName,
            centerX = centerX,
            centerZ = centerZ,
            beaconY = location.blockY,
            addOrder = currentChunks + 1
        )
        val chunkId = repository.addChunk(newChunk)
        val savedChunk = newChunk.copy(id = chunkId)

        // ビーコン設置
        beaconManager.placeBeacon(location)

        // キャッシュ更新
        cache.addChunk(savedChunk, guildId)

        plugin.logger.info("Territory claimed: Guild ${guild.name} (ID: $guildId) claimed chunk at ($centerX, $centerZ) in $worldName. Total: ${currentChunks + 1} chunks")

        return ClaimResult.Success(savedChunk)
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

        chunksToRemove.forEach { chunk ->
            // ビーコン削除
            chunk.beaconLocation?.let { beaconManager.removeBeacon(it) }
            // DB削除
            repository.deleteChunk(chunk.id)
            // キャッシュ更新
            cache.removeChunk(chunk, guildId)
        }

        // 全チャンク削除なら領地自体を削除
        if (targetChunkCount == 0) {
            repository.deleteTerritory(guildId)
            cache.removeTerritory(guildId)
        }

        plugin.logger.info("Territory shrunk: Guild ID $guildId shrunk from $fromCount to $targetChunkCount chunks (removed ${fromCount - targetChunkCount} chunks)")
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

    override fun calculateAllowedChunks(memberCount: Int): Int {
        if (memberCount < MIN_MEMBERS_FOR_TERRITORY) return 0
        return minOf(memberCount / MEMBERS_PER_CHUNK, configManager.territoryMaxChunks)
    }

    override fun canClaimMoreChunks(guildId: Long): Boolean {
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
