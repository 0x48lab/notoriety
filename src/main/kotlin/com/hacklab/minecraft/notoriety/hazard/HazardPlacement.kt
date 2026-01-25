package com.hacklab.minecraft.notoriety.hazard

import java.time.Instant
import java.util.UUID

/**
 * 危険物設置記録
 * マグマ、水源等の危険物を設置したプレイヤー、位置、時刻を記録
 */
data class HazardPlacement(
    /** 設置記録のユニークID */
    val id: UUID = UUID.randomUUID(),

    /** 危険物種別 */
    val type: HazardType,

    /** 設置したプレイヤーのUUID */
    val placerUuid: UUID,

    /** 設置位置（ワールド名） */
    val worldName: String,

    /** 設置位置（X座標） */
    val x: Int,

    /** 設置位置（Y座標） */
    val y: Int,

    /** 設置位置（Z座標） */
    val z: Int,

    /** 設置時刻 */
    val placedAt: Instant = Instant.now()
) {
    /**
     * 指定位置との距離の二乗を計算
     * @param otherWorld ワールド名
     * @param otherX X座標
     * @param otherY Y座標
     * @param otherZ Z座標
     * @return 距離の二乗（異なるワールドの場合はInt.MAX_VALUE）
     */
    fun distanceSquared(otherWorld: String, otherX: Int, otherY: Int, otherZ: Int): Int {
        if (worldName != otherWorld) return Int.MAX_VALUE
        val dx = x - otherX
        val dy = y - otherY
        val dz = z - otherZ
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * 指定位置が距離内にあるかチェック
     * @param otherWorld ワールド名
     * @param otherX X座標
     * @param otherY Y座標
     * @param otherZ Z座標
     * @param maxDistance 最大距離（ブロック）
     * @return 距離内ならtrue
     */
    fun isWithinDistance(otherWorld: String, otherX: Int, otherY: Int, otherZ: Int, maxDistance: Int): Boolean {
        return distanceSquared(otherWorld, otherX, otherY, otherZ) <= maxDistance * maxDistance
    }
}
