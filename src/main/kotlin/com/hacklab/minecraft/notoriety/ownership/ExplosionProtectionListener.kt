package com.hacklab.minecraft.notoriety.ownership

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

/**
 * 所有権のあるブロックを爆発から保護するリスナー
 * FR-005a: 所有権のあるブロックは爆発（クリーパー、TNT、ベッド等）で破壊されない
 */
class ExplosionProtectionListener(
    private val ownershipService: OwnershipService
) : Listener {

    /**
     * エンティティによる爆発（クリーパー、TNT、ウィザー等）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        // 所有権のあるブロックを爆発リストから除外
        event.blockList().removeIf { block ->
            ownershipService.isProtected(block.location)
        }
    }

    /**
     * ブロックによる爆発（ベッド、リスポーンアンカー等）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        // 所有権のあるブロックを爆発リストから除外
        event.blockList().removeIf { block ->
            ownershipService.isProtected(block.location)
        }
    }
}
