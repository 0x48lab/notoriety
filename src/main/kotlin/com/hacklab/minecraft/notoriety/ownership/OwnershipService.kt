package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.core.BlockLocation
import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.core.player.PlayerRepository
import com.hacklab.minecraft.notoriety.core.toBlockLoc
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.trust.TrustService
import org.bukkit.Location
import org.bukkit.Material
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OwnershipService(
    private val repository: OwnershipRepository,
    private val playerRepository: PlayerRepository? = null,
    private val configManager: ConfigManager? = null
) {
    private val pendingCrimes = ConcurrentHashMap<BlockLocation, PendingCrime>()

    companion object {
        const val GRACE_PERIOD_SECONDS = 5L
        private const val OWNER_CACHE_TTL_MS = 5000L  // 5秒間キャッシュ
        private const val OWNER_CACHE_MAX_SIZE = 10000
        private const val OWNER_CACHE_CLEANUP_THRESHOLD = 12000 // maxSize の 1.2 倍でクリーンアップ
    }

    // 所有権キャッシュ: BlockLocation -> (owner UUID or null, timestamp)
    private data class CachedOwner(val owner: UUID?, val cachedAt: Long)
    private val ownerCache = ConcurrentHashMap<BlockLocation, CachedOwner>()

    fun registerOwnership(location: Location, owner: UUID) {
        val blockLoc = location.toBlockLoc()
        repository.setOwner(blockLoc, owner)
        // キャッシュを即座に更新
        ownerCache[blockLoc] = CachedOwner(owner, System.currentTimeMillis())
    }

    fun removeOwnership(location: Location) {
        val blockLoc = location.toBlockLoc()
        repository.removeOwner(blockLoc)
        // キャッシュから削除
        ownerCache.remove(blockLoc)
    }

    /**
     * Returns the owner UUID of the block at the given location.
     * Uses a TTL cache to avoid repeated DB queries for the same block.
     * Returns null if:
     * - The block has no owner
     * - The ownership has expired (owner hasn't logged in for expiration-days AND block was placed expiration-days ago)
     */
    fun getOwner(location: Location): UUID? {
        val blockLoc = location.toBlockLoc()
        val now = System.currentTimeMillis()

        // キャッシュヒットチェック
        val cached = ownerCache[blockLoc]
        if (cached != null && (now - cached.cachedAt) < OWNER_CACHE_TTL_MS) {
            return cached.owner
        }

        // キャッシュミス: DBから取得
        val owner = queryOwner(blockLoc)
        ownerCache[blockLoc] = CachedOwner(owner, now)

        // キャッシュサイズが閾値を超えたら古いエントリを削除
        if (ownerCache.size > OWNER_CACHE_CLEANUP_THRESHOLD) {
            cleanupOwnerCache(now)
        }

        return owner
    }

    /**
     * DBから所有者を取得（期限切れチェック含む）
     */
    private fun queryOwner(blockLoc: BlockLocation): UUID? {
        val expirationDays = configManager?.ownershipExpirationDays ?: 0

        // If expiration is disabled (0), just return the owner without checking
        if (expirationDays <= 0 || playerRepository == null) {
            return repository.getOwner(blockLoc)
        }

        // Get ownership info including placed_at timestamp
        val ownershipInfo = repository.getOwnershipInfoByBlockLoc(blockLoc) ?: return null
        val ownerUuid = ownershipInfo.ownerUuid
        val placedAt = ownershipInfo.placedAt ?: return ownerUuid // If no placed_at, don't expire

        // Check if the block was placed more than expiration days ago
        val now = Instant.now()
        val daysSincePlaced = Duration.between(placedAt, now).toDays()
        if (daysSincePlaced < expirationDays) {
            return ownerUuid
        }

        // Block is old enough to potentially expire, now check owner's last login
        val lastSeen = playerRepository.getLastSeen(ownerUuid)
        if (lastSeen == null) {
            return null
        }

        val daysSinceLogin = Duration.between(lastSeen, now).toDays()
        if (daysSinceLogin >= expirationDays) {
            return null
        }

        return ownerUuid
    }

    /**
     * 期限切れキャッシュエントリを削除
     */
    private fun cleanupOwnerCache(now: Long) {
        val iterator = ownerCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((now - entry.value.cachedAt) >= OWNER_CACHE_TTL_MS) {
                iterator.remove()
            }
        }
        // まだ多すぎる場合は古い順に削除
        if (ownerCache.size > OWNER_CACHE_MAX_SIZE) {
            val sorted = ownerCache.entries.sortedBy { it.value.cachedAt }
            val toRemove = sorted.take(ownerCache.size - OWNER_CACHE_MAX_SIZE)
            toRemove.forEach { ownerCache.remove(it.key) }
        }
    }

    fun isProtected(location: Location): Boolean =
        getOwner(location) != null

    @Deprecated("Use canAccess with GuildService instead for proper guild trust integration")
    fun canAccess(location: Location, player: UUID, trustService: TrustService): Boolean {
        val owner = getOwner(location) ?: return true  // 所有者なし = 自由
        if (owner == player) return true               // 本人
        return trustService.isTrusted(owner, player)   // 信頼されている
    }

    /**
     * ギルド信頼を含むアクセスチェック
     * - 所有者なし: 許可
     * - 本人: 許可
     * - 明示的に信頼: 許可
     * - 明示的に不信頼: 拒否（ギルドメンバーでも）
     * - 未設定 + 同じギルド: 許可
     * - それ以外: 拒否
     */
    fun canAccess(location: Location, player: UUID, guildService: GuildService): Boolean {
        val owner = getOwner(location) ?: return true  // 所有者なし = 自由
        if (owner == player) return true               // 本人
        return guildService.isAccessAllowed(owner, player)
    }

    /**
     * コンテナからアイテムを取り出せるかチェック
     */
    fun canTakeFromContainer(location: Location, player: UUID, guildService: GuildService): Boolean {
        val owner = getOwner(location) ?: return true
        if (owner == player) return true
        return guildService.canTakeFromContainer(owner, player)
    }

    // 保留犯罪システム
    fun addPendingCrime(crime: PendingCrime) {
        pendingCrimes[crime.location] = crime
    }

    fun cancelPendingCrime(location: BlockLocation, player: UUID, blockType: Material): Boolean {
        val pending = pendingCrimes[location] ?: return false
        if (pending.playerUuid == player && pending.blockType == blockType) {
            pendingCrimes.remove(location)
            return true
        }
        return false
    }

    fun getExpiredPendingCrimes(): List<PendingCrime> {
        val now = Instant.now()
        val expired = pendingCrimes.values.filter {
            Duration.between(it.brokenAt, now).seconds >= GRACE_PERIOD_SECONDS
        }
        expired.forEach { pendingCrimes.remove(it.location) }
        return expired
    }

    fun getPendingCrime(location: BlockLocation): PendingCrime? = pendingCrimes[location]

    fun clearPendingCrimes() {
        pendingCrimes.clear()
    }
}
