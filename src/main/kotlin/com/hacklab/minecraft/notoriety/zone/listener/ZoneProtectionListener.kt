package com.hacklab.minecraft.notoriety.zone.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.zone.service.ZoneService
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryClickEvent

class ZoneProtectionListener(
    private val zoneService: ZoneService,
    private val i18n: I18nManager
) : Listener {

    // === US1: Basic block protection ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.isOp) return
        if (!zoneService.isProtected(event.block.location)) return

        event.isCancelled = true
        i18n.sendError(player, "zone.protected_break", "ここではブロックを操作できません")
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.isOp) return
        if (!zoneService.isProtected(event.block.location)) return

        event.isCancelled = true
        i18n.sendError(player, "zone.protected_place", "ここではブロックを操作できません")
    }

    // === US4: Explosion protection ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeAll { block ->
            zoneService.isProtected(block.location)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeAll { block ->
            zoneService.isProtected(block.location)
        }
    }

    // === US4: Piston protection ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { zoneService.isProtected(it.location) }) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { zoneService.isProtected(it.location) }) {
            event.isCancelled = true
        }
    }

    // === US4: Fire, water, lava, burn protection ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        if (zoneService.isProtected(event.block.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        if (zoneService.isProtected(event.toBlock.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        if (zoneService.isProtected(event.block.location)) {
            event.isCancelled = true
        }
    }

    // === US4: Entity block change (enderman, etc.) ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (zoneService.isProtected(event.block.location)) {
            event.isCancelled = true
        }
    }

    // === US4: Container access protection ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (player.isOp) return

        val holder = event.inventory.holder
        if (holder !is Container) return

        // Only block taking items out (clicking the top inventory)
        if (event.clickedInventory != event.inventory) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        val containerLocation = holder.block.location
        if (!zoneService.isProtected(containerLocation)) return

        event.isCancelled = true
        i18n.sendError(player, "zone.protected_container", "ここではアイテムを取り出せません")
    }
}
