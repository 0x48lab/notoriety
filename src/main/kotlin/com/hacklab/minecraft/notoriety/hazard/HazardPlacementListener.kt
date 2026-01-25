package com.hacklab.minecraft.notoriety.hazard

import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.type.Switch
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 危険物設置リスナー
 * マグマバケツ、TNT点火等の危険物設置を検知し、追跡サービスに記録する
 */
class HazardPlacementListener(
    private val hazardTrackingService: HazardTrackingService,
    private val configManager: ConfigManager
) : Listener {

    // TNT点火を追跡するためのマップ（TNTブロック位置 → 点火者UUID）
    private val pendingTntIgnitions = mutableMapOf<String, Player>()

    /**
     * マグマバケツ使用時の追跡
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player

        // クリエイティブモードは追跡から除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 間接PK検知が無効の場合は追跡しない
        if (!configManager.isIndirectPkEnabled()) return

        // 設置されたブロックの位置
        val location = event.blockClicked.getRelative(event.blockFace).location

        when (event.bucket) {
            Material.LAVA_BUCKET -> {
                if (configManager.isLavaTrackingEnabled()) {
                    hazardTrackingService.trackLavaPlacement(player, location)
                }
            }
            Material.WATER_BUCKET -> {
                if (configManager.isWaterTrackingEnabled()) {
                    hazardTrackingService.trackWaterPlacement(player, location)
                }
            }
            else -> {
                // 他のバケツは追跡しない
            }
        }
    }

    /**
     * TNT点火時の追跡（火打ち石と打ち金）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // クリエイティブモードは追跡から除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 間接PK検知が無効の場合は追跡しない
        if (!configManager.isIndirectPkEnabled()) return
        if (!configManager.isTntTrackingEnabled()) return

        val block = event.clickedBlock ?: return

        when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> {
                // 火打ち石と打ち金でTNTを点火
                val itemInHand = event.item
                if (itemInHand?.type == Material.FLINT_AND_STEEL && block.type == Material.TNT) {
                    val key = locationKey(block.location)
                    pendingTntIgnitions[key] = player
                }

                // レッドストーントリガー（ボタン/レバー）の作動を追跡
                if (isRedstoneSwitch(block.type)) {
                    hazardTrackingService.trackRedstoneActivation(player, block.location)
                }
            }
            Action.PHYSICAL -> {
                // 感圧板の作動を追跡
                if (isPressurePlate(block.type)) {
                    hazardTrackingService.trackRedstoneActivation(player, block.location)
                }
            }
            else -> { /* 他のアクションは無視 */ }
        }
    }

    /**
     * TNTエンティティがスポーンする直前（点火時）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplosionPrime(event: ExplosionPrimeEvent) {
        val entity = event.entity
        if (entity !is TNTPrimed) return

        // クリエイティブモードは追跡から除外
        if (!configManager.isIndirectPkEnabled()) return
        if (!configManager.isTntTrackingEnabled()) return

        val location = entity.location
        val key = locationKey(location)

        // 直接点火者を探す
        val igniter = pendingTntIgnitions.remove(key)
        if (igniter != null) {
            hazardTrackingService.trackTntIgnition(igniter, entity)
            return
        }

        // TNTPrimedのソース（点火者）を取得
        val source = entity.source
        if (source is Player && source.gameMode != GameMode.CREATIVE) {
            hazardTrackingService.trackTntIgnition(source, entity)
            return
        }

        // レッドストーン経由の点火を検索（周囲のトリガーを確認）
        val nearbyActivator = findNearbyRedstoneActivator(location)
        if (nearbyActivator != null) {
            hazardTrackingService.trackTntIgnition(nearbyActivator, entity)
        }
    }

    /**
     * TNT爆発時の記録
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        if (entity !is TNTPrimed) return

        // 間接PK検知が無効の場合は処理しない
        if (!configManager.isIndirectPkEnabled()) return
        if (!configManager.isTntTrackingEnabled()) return

        // 爆発を記録
        hazardTrackingService.onTntExplode(entity, event.location)
    }

    /**
     * 周囲のレッドストーントリガー作動者を検索
     */
    private fun findNearbyRedstoneActivator(location: org.bukkit.Location): Player? {
        // 周囲16ブロック以内のトリガー作動者を検索
        val world = location.world
        val centerX = location.blockX
        val centerY = location.blockY
        val centerZ = location.blockZ

        for (dx in -16..16) {
            for (dy in -16..16) {
                for (dz in -16..16) {
                    val checkLoc = world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz).location
                    val activatorUuid = hazardTrackingService.findLastTriggerActivator(checkLoc)
                    if (activatorUuid != null) {
                        return org.bukkit.Bukkit.getPlayer(activatorUuid)
                    }
                }
            }
        }
        return null
    }

    private fun locationKey(location: org.bukkit.Location): String {
        return "${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"
    }

    private fun isRedstoneSwitch(material: Material): Boolean {
        return material.name.endsWith("_BUTTON") || material == Material.LEVER
    }

    private fun isPressurePlate(material: Material): Boolean {
        return material.name.endsWith("_PRESSURE_PLATE")
    }
}
