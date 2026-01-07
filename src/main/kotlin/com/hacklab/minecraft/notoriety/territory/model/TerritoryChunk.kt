package com.hacklab.minecraft.notoriety.territory.model

import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant

/**
 * 領地チャンクエンティティ
 * 16×16ブロック（プレイヤー中心から前後左右8ブロック）のエリア
 */
data class TerritoryChunk(
    val id: Long = 0,
    val territoryId: Long = 0,
    val worldName: String,
    val centerX: Int,
    val centerZ: Int,
    val beaconY: Int,
    val addOrder: Int = 0,
    val addedAt: Instant = Instant.now()
) {
    companion object {
        /** 中心から各方向への半径（8ブロック） */
        const val HALF_SIZE = 8
    }

    /** チャンクの最小X座標 */
    val minX: Int get() = centerX - HALF_SIZE

    /** チャンクの最大X座標（0-indexed） */
    val maxX: Int get() = centerX + HALF_SIZE - 1

    /** チャンクの最小Z座標 */
    val minZ: Int get() = centerZ - HALF_SIZE

    /** チャンクの最大Z座標（0-indexed） */
    val maxZ: Int get() = centerZ + HALF_SIZE - 1

    /**
     * 指定座標がこのチャンク内かどうか
     * @param x X座標
     * @param z Z座標
     * @return チャンク内ならtrue
     */
    fun contains(x: Int, z: Int): Boolean {
        return x in minX..maxX && z in minZ..maxZ
    }

    /**
     * 指定Locationがこのチャンク内かどうか
     * @param location 位置
     * @return チャンク内ならtrue
     */
    fun containsLocation(location: Location): Boolean {
        return location.world?.name == worldName &&
                contains(location.blockX, location.blockZ)
    }

    /**
     * ビーコンのLocation
     */
    val beaconLocation: Location?
        get() = Bukkit.getWorld(worldName)?.let { world ->
            Location(world, centerX.toDouble(), beaconY.toDouble(), centerZ.toDouble())
        }

    /**
     * チャンクの中心Location（地面レベル）
     */
    val centerLocation: Location?
        get() = Bukkit.getWorld(worldName)?.let { world ->
            Location(world, centerX.toDouble(), beaconY.toDouble(), centerZ.toDouble())
        }

    /**
     * 他のチャンクと重複しているかチェック
     * @param other 比較対象チャンク
     * @return 重複している場合true
     */
    fun overlapsWith(other: TerritoryChunk): Boolean {
        if (worldName != other.worldName) return false
        return !(maxX < other.minX || minX > other.maxX ||
                maxZ < other.minZ || minZ > other.maxZ)
    }
}
