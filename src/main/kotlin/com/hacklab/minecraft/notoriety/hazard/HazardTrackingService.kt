package com.hacklab.minecraft.notoriety.hazard

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 危険物追跡サービス
 * マグマ、TNT等の設置・起動を追跡し、間接PK判定のための原因プレイヤー特定を行う
 */
class HazardTrackingService(
    /** 追跡期限（秒） */
    private val trackingDurationSeconds: Long = 10,

    /** 最大追跡距離（ブロック） */
    private val maxDistanceBlocks: Int = 32
) {
    // マグマ等の危険物設置記録（位置キー → 設置情報）
    private val hazardPlacements = ConcurrentHashMap<String, HazardPlacement>()

    // TNT点火者マップ（TNTエンティティUUID → 点火者情報）
    private val tntIgniters = ConcurrentHashMap<UUID, TntIgniterInfo>()

    // 爆発後のTNT追跡（位置キー → 点火者情報）
    private val recentExplosions = ConcurrentHashMap<String, TntIgniterInfo>()

    // レッドストーントリガー作動記録（位置キー → 作動情報）
    private val redstoneActivations = ConcurrentHashMap<String, RedstoneActivation>()

    // ========== マグマ追跡 ==========

    /**
     * マグマ設置を記録
     * @param placer 設置したプレイヤー
     * @param location 設置位置
     */
    fun trackLavaPlacement(placer: Player, location: Location) {
        val key = locationKey(location)
        hazardPlacements[key] = HazardPlacement(
            type = HazardType.LAVA,
            placerUuid = placer.uniqueId,
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }

    /**
     * マグマ死亡の原因プレイヤーを検索
     * @param deathLocation 死亡位置
     * @return 設置者のUUID、見つからない場合はnull
     */
    fun findLavaPlacer(deathLocation: Location): UUID? {
        val now = Instant.now()
        val worldName = deathLocation.world.name
        val x = deathLocation.blockX
        val y = deathLocation.blockY
        val z = deathLocation.blockZ

        return hazardPlacements.values
            .filter { it.type == HazardType.LAVA }
            .filter { Duration.between(it.placedAt, now).seconds < trackingDurationSeconds }
            .filter { it.isWithinDistance(worldName, x, y, z, maxDistanceBlocks) }
            .maxByOrNull { it.placedAt }
            ?.placerUuid
    }

    // ========== TNT追跡 ==========

    /**
     * TNT点火を記録
     * @param igniter 点火したプレイヤー
     * @param tntEntity TNTエンティティ
     */
    fun trackTntIgnition(igniter: Player, tntEntity: TNTPrimed) {
        val location = tntEntity.location
        tntIgniters[tntEntity.uniqueId] = TntIgniterInfo(
            tntEntityUuid = tntEntity.uniqueId,
            igniterUuid = igniter.uniqueId,
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }

    /**
     * TNT爆発を記録し、爆発後の追跡を開始
     * @param tntEntity 爆発したTNTエンティティ
     * @param explosionLocation 爆発位置
     */
    fun onTntExplode(tntEntity: TNTPrimed, explosionLocation: Location) {
        val info = tntIgniters.remove(tntEntity.uniqueId) ?: return
        val updatedInfo = info.copy(
            explodedAt = Instant.now(),
            worldName = explosionLocation.world.name,
            x = explosionLocation.blockX,
            y = explosionLocation.blockY,
            z = explosionLocation.blockZ
        )
        val key = locationKey(explosionLocation)
        recentExplosions[key] = updatedInfo
    }

    /**
     * TNT爆発の原因プレイヤーを検索
     * @param deathLocation 死亡位置
     * @return 点火者のUUID、見つからない場合はnull
     */
    fun findTntIgniter(deathLocation: Location): UUID? {
        val now = Instant.now()
        val worldName = deathLocation.world.name
        val x = deathLocation.blockX
        val y = deathLocation.blockY
        val z = deathLocation.blockZ

        // 直近の爆発を検索
        for ((_, info) in recentExplosions) {
            val explodedAt = info.explodedAt ?: continue
            if (Duration.between(explodedAt, now).seconds >= trackingDurationSeconds) continue
            if (info.worldName != worldName) continue

            // 爆発位置から32ブロック以内か
            val dx = info.x - x
            val dy = info.y - y
            val dz = info.z - z
            if (dx * dx + dy * dy + dz * dz <= maxDistanceBlocks * maxDistanceBlocks) {
                return info.igniterUuid
            }
        }

        return null
    }

    // ========== レッドストーントリガー追跡 ==========

    /**
     * レッドストーントリガー（ボタン/レバー/感圧板）の作動を記録
     * @param activator 作動させたプレイヤー
     * @param location トリガー位置
     */
    fun trackRedstoneActivation(activator: Player, location: Location) {
        val key = locationKey(location)
        redstoneActivations[key] = RedstoneActivation(
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ,
            activatorUuid = activator.uniqueId
        )
    }

    /**
     * 最後にトリガーを作動させたプレイヤーを検索
     * @param triggerLocation トリガー位置
     * @return 作動者のUUID、見つからない場合はnull
     */
    fun findLastTriggerActivator(triggerLocation: Location): UUID? {
        val key = locationKey(triggerLocation)
        val activation = redstoneActivations[key] ?: return null
        val now = Instant.now()

        return if (Duration.between(activation.activatedAt, now).seconds < trackingDurationSeconds) {
            activation.activatorUuid
        } else {
            null
        }
    }

    // ========== 水流・ピストン追跡（P3: 将来対応） ==========

    /**
     * 水源設置を記録
     * @param placer 設置したプレイヤー
     * @param location 設置位置
     */
    fun trackWaterPlacement(placer: Player, location: Location) {
        val key = locationKey(location)
        hazardPlacements[key] = HazardPlacement(
            type = HazardType.WATER,
            placerUuid = placer.uniqueId,
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }

    /**
     * ピストン設置を記録
     * @param placer 設置したプレイヤー
     * @param location 設置位置
     */
    fun trackPistonPlacement(placer: Player, location: Location) {
        val key = locationKey(location)
        hazardPlacements[key] = HazardPlacement(
            type = HazardType.PISTON,
            placerUuid = placer.uniqueId,
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }

    // ========== クリーンアップ ==========

    /**
     * 期限切れの追跡データを削除
     * 1秒ごとに呼び出されることを想定
     */
    fun cleanupExpiredEntries() {
        val now = Instant.now()

        // HazardPlacements: placedAt + 追跡期限で期限切れ
        hazardPlacements.entries.removeIf {
            Duration.between(it.value.placedAt, now).seconds >= trackingDurationSeconds
        }

        // TNT Igniters: 長時間爆発していないものを削除（30秒以上）
        tntIgniters.entries.removeIf {
            Duration.between(it.value.ignitedAt, now).seconds >= 30
        }

        // Recent Explosions: 爆発後の追跡期限で期限切れ
        recentExplosions.entries.removeIf {
            it.value.explodedAt?.let { explodedAt ->
                Duration.between(explodedAt, now).seconds >= trackingDurationSeconds
            } ?: true
        }

        // Redstone Activations: activatedAt + 追跡期限で期限切れ
        redstoneActivations.entries.removeIf {
            Duration.between(it.value.activatedAt, now).seconds >= trackingDurationSeconds
        }
    }

    // ========== 設定 ==========

    /**
     * 追跡期限（秒）
     */
    fun getTrackingDurationSeconds(): Long = trackingDurationSeconds

    /**
     * 最大追跡距離（ブロック）
     */
    fun getMaxDistanceBlocks(): Int = maxDistanceBlocks

    // ========== ユーティリティ ==========

    private fun locationKey(location: Location): String {
        return "${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"
    }
}
