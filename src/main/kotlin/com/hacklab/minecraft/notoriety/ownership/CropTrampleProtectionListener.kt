package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 農地踏み荒らし保護リスナー
 *
 * 所有権のある耕地・ギルド領地内の耕地を、プレイヤーおよびMobの
 * ジャンプによる踏み荒らしから保護する。
 * 犯罪としては記録しない（物理的保護のみ）。
 */
class CropTrampleProtectionListener(
    private val ownershipService: OwnershipService,
    private val territoryService: TerritoryService
) : Listener {

    /**
     * プレイヤーによる耕地踏み荒らしを検知・防止する
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerTrampleFarmland(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.FARMLAND) return

        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        if (isProtectedFarmland(block.location)) {
            event.isCancelled = true
        }
    }

    /**
     * Mob（動物・モンスター）による耕地踏み荒らしを検知・防止する
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityTrampleFarmland(event: EntityInteractEvent) {
        if (event.block.type != Material.FARMLAND) return
        if (event.entity is Player) return

        if (isProtectedFarmland(event.block.location)) {
            event.isCancelled = true
        }
    }

    private fun isProtectedFarmland(location: Location): Boolean {
        if (ownershipService.getOwner(location) != null) return true
        if (territoryService.isInTerritory(location)) return true
        return false
    }
}
