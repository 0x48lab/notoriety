package com.hacklab.minecraft.notoriety.zone.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.zone.command.ZoneCommand
import com.hacklab.minecraft.notoriety.zone.model.BlockPos
import com.hacklab.minecraft.notoriety.zone.model.ZoneSelection
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class ZoneToolListener(
    private val zoneCommand: ZoneCommand,
    private val i18n: I18nManager
) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!player.isOp) return
        if (player.inventory.itemInMainHand.type != Material.WOODEN_AXE) return

        val block = event.clickedBlock ?: return

        val selection = zoneCommand.selections.computeIfAbsent(player.uniqueId) { ZoneSelection() }

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> {
                selection.pos1 = BlockPos(block.world.name, block.x, block.y, block.z)
                i18n.sendSuccess(player, "zone.pos1_set", "第1座標を設定しました (%d, %d, %d)",
                    block.x, block.y, block.z)
                event.isCancelled = true
            }
            Action.RIGHT_CLICK_BLOCK -> {
                selection.pos2 = BlockPos(block.world.name, block.x, block.y, block.z)
                i18n.sendSuccess(player, "zone.pos2_set", "第2座標を設定しました (%d, %d, %d)",
                    block.x, block.y, block.z)
                event.isCancelled = true
            }
            else -> {}
        }
    }
}
