package com.hacklab.minecraft.notoriety.inspect

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

class InspectListener(
    private val inspectService: InspectService,
    private val inspectionStick: InspectionStick
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!inspectService.isInspecting(player)) return

        // Cancel block breaking when in inspect mode
        event.isCancelled = true

        // Show block info
        val messages = inspectService.formatBlockInfo(player, event.block.location)
        messages.forEach { player.sendMessage(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return

        // Check if using inspection stick
        val usingStick = inspectionStick.isInspectionStick(event.item)

        // Only process if in inspect mode or using inspection stick
        if (!inspectService.isInspecting(player) && !usingStick) return

        // Only handle block interactions
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.LEFT_CLICK_BLOCK) return

        // Cancel the interaction
        event.isCancelled = true

        // Show block info
        val messages = inspectService.formatBlockInfo(player, block.location)
        messages.forEach { player.sendMessage(it) }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        inspectService.onPlayerQuit(event.player)
    }
}
