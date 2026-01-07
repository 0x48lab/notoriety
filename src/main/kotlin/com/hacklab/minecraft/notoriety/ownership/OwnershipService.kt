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
    }

    fun registerOwnership(location: Location, owner: UUID) {
        repository.setOwner(location.toBlockLoc(), owner)
    }

    fun removeOwnership(location: Location) {
        repository.removeOwner(location.toBlockLoc())
    }

    /**
     * Returns the owner UUID of the block at the given location.
     * Returns null if:
     * - The block has no owner
     * - The ownership has expired (owner hasn't logged in for expiration-days AND block was placed expiration-days ago)
     */
    fun getOwner(location: Location): UUID? {
        val expirationDays = configManager?.ownershipExpirationDays ?: 0

        // If expiration is disabled (0), just return the owner without checking
        if (expirationDays <= 0 || playerRepository == null) {
            return repository.getOwner(location.toBlockLoc())
        }

        // Get ownership info including placed_at timestamp
        val ownershipInfo = repository.getOwnershipInfo(location) ?: return null
        val ownerUuid = ownershipInfo.ownerUuid
        val placedAt = ownershipInfo.placedAt ?: return ownerUuid // If no placed_at, don't expire

        // Check if the block was placed more than expiration days ago
        val now = Instant.now()
        val daysSincePlaced = Duration.between(placedAt, now).toDays()
        if (daysSincePlaced < expirationDays) {
            // Block is still within grace period based on placement date
            return ownerUuid
        }

        // Block is old enough to potentially expire, now check owner's last login
        val lastSeen = playerRepository.getLastSeen(ownerUuid)
        if (lastSeen == null) {
            // Player has no login record - ownership expires
            return null
        }

        val daysSinceLogin = Duration.between(lastSeen, now).toDays()
        if (daysSinceLogin >= expirationDays) {
            // Owner hasn't logged in for expiration days - ownership expires
            return null
        }

        return ownerUuid
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
