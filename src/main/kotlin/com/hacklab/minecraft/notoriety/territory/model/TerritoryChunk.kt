package com.hacklab.minecraft.notoriety.territory.model

import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import kotlin.math.abs

/**
 * 領地チャンク - Minecraftネイティブチャンク単位（16×16ブロック）
 *
 * チャンク座標はMinecraftのネイティブチャンク座標を使用:
 * - chunkX = blockX >> 4 (= blockX / 16)
 * - chunkZ = blockZ >> 4 (= blockZ / 16)
 */
data class TerritoryChunk(
    val id: Long = 0,
    val territoryId: Long = 0,
    val worldName: String,
    val chunkX: Int,              // Minecraft ネイティブチャンクX座標
    val chunkZ: Int,              // Minecraft ネイティブチャンクZ座標
    val sigilId: Long? = null,    // 所属シギルID
    val addOrder: Int = 0,
    val addedAt: Instant = Instant.now(),
    // Legacy fields for backward compatibility during migration
    @Deprecated("Use chunkX instead")
    val centerX: Int? = null,
    @Deprecated("Use chunkZ instead")
    val centerZ: Int? = null,
    @Deprecated("Beacon position is now stored in TerritorySigil")
    val beaconY: Int? = null
) {
    companion object {
        /** チャンクサイズ（16ブロック） */
        const val CHUNK_SIZE = 16

        /**
         * ブロック座標からチャンク座標を計算
         * @param blockX ブロックX座標
         * @param blockZ ブロックZ座標
         * @return チャンク座標のペア (chunkX, chunkZ)
         */
        fun fromBlockCoords(blockX: Int, blockZ: Int): Pair<Int, Int> {
            return (blockX shr 4) to (blockZ shr 4)
        }

        /**
         * Locationからチャンク座標を計算
         */
        fun fromLocation(location: Location): Pair<Int, Int> {
            return fromBlockCoords(location.blockX, location.blockZ)
        }
    }

    /** チャンクの最小ブロックX座標 */
    val minBlockX: Int get() = chunkX shl 4

    /** チャンクの最大ブロックX座標 */
    val maxBlockX: Int get() = minBlockX + 15

    /** チャンクの最小ブロックZ座標 */
    val minBlockZ: Int get() = chunkZ shl 4

    /** チャンクの最大ブロックZ座標 */
    val maxBlockZ: Int get() = minBlockZ + 15

    /** チャンクの中心ブロックX座標 */
    val centerBlockX: Int get() = minBlockX + 8

    /** チャンクの中心ブロックZ座標 */
    val centerBlockZ: Int get() = minBlockZ + 8

    /**
     * ブロック座標がこのチャンク内かどうか
     * @param blockX ブロックX座標
     * @param blockZ ブロックZ座標
     * @return チャンク内ならtrue
     */
    fun containsBlock(blockX: Int, blockZ: Int): Boolean {
        return blockX in minBlockX..maxBlockX &&
               blockZ in minBlockZ..maxBlockZ
    }

    /**
     * 指定座標がこのチャンク内かどうか（旧APIとの互換性）
     */
    fun contains(x: Int, z: Int): Boolean = containsBlock(x, z)

    /**
     * 指定Locationがこのチャンク内かどうか
     * @param location 位置
     * @return チャンク内ならtrue
     */
    fun containsLocation(location: Location): Boolean {
        return location.world?.name == worldName &&
               containsBlock(location.blockX, location.blockZ)
    }

    /**
     * 他のチャンクと隣接しているか（4方向のみ、斜めは含まない）
     * @param other 比較対象チャンク
     * @return 隣接している場合true
     */
    fun isAdjacentTo(other: TerritoryChunk): Boolean {
        if (worldName != other.worldName) return false
        val dx = abs(chunkX - other.chunkX)
        val dz = abs(chunkZ - other.chunkZ)
        return (dx == 1 && dz == 0) || (dx == 0 && dz == 1)
    }

    /**
     * 他のチャンクと同じ位置か
     */
    fun isSamePosition(other: TerritoryChunk): Boolean {
        return worldName == other.worldName &&
               chunkX == other.chunkX &&
               chunkZ == other.chunkZ
    }

    /**
     * チャンクの中心Location（地面レベル推定）
     */
    fun getCenterLocation(y: Double = 64.0): Location? {
        return Bukkit.getWorld(worldName)?.let { world ->
            Location(world, centerBlockX.toDouble(), y, centerBlockZ.toDouble())
        }
    }

    // ===== Legacy compatibility methods =====

    /**
     * ビーコンのLocation（後方互換用）
     * @deprecated シギルシステムに移行。TerritorySigil.locationを使用してください
     */
    @Deprecated("Use TerritorySigil.location instead")
    val beaconLocation: Location?
        get() = beaconY?.let { y ->
            Bukkit.getWorld(worldName)?.let { world ->
                Location(world, centerBlockX.toDouble(), y.toDouble(), centerBlockZ.toDouble())
            }
        }

    /** 旧APIとの互換性: minX */
    @Deprecated("Use minBlockX instead")
    val minX: Int get() = minBlockX

    /** 旧APIとの互換性: maxX */
    @Deprecated("Use maxBlockX instead")
    val maxX: Int get() = maxBlockX

    /** 旧APIとの互換性: minZ */
    @Deprecated("Use minBlockZ instead")
    val minZ: Int get() = minBlockZ

    /** 旧APIとの互換性: maxZ */
    @Deprecated("Use maxBlockZ instead")
    val maxZ: Int get() = maxBlockZ

    /**
     * 他のチャンクと重複しているかチェック（旧API互換）
     * ネイティブチャンクでは同じ座標なら重複
     */
    @Deprecated("Native chunks don't overlap - use isSamePosition instead")
    fun overlapsWith(other: TerritoryChunk): Boolean {
        return isSamePosition(other)
    }
}
