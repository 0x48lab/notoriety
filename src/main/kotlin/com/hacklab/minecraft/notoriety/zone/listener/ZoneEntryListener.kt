package com.hacklab.minecraft.notoriety.zone.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.zone.service.ZoneService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ZoneEntryListener(
    private val zoneService: ZoneService,
    private val i18n: I18nManager
) : Listener {

    // Cache: player UUID -> current zone name ("" if not in any zone)
    // ConcurrentHashMap は null 値を許容しないため、ゾーン外は空文字で表現
    private val playerZones = ConcurrentHashMap<UUID, String>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to

        // Only check when block coordinates change
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

        val player = event.player
        val currentZone = zoneService.getZoneAt(to.world.name, to.blockX, to.blockY, to.blockZ)
        val currentZoneName = currentZone?.name ?: ""
        val previousZoneName = playerZones.put(player.uniqueId, currentZoneName) ?: ""

        // No change
        if (previousZoneName == currentZoneName) return

        // Only notify admins (OP)
        if (!player.isOp) return

        // Left a zone
        if (previousZoneName.isNotEmpty()) {
            i18n.sendInfo(player, "zone.leave_admin", "◀ 保護エリア [%s] から出ました", previousZoneName)
        }

        // Entered a zone
        if (currentZoneName.isNotEmpty()) {
            i18n.sendInfo(player, "zone.enter_admin", "▶ 保護エリア [%s] に入りました", currentZoneName)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerZones.remove(event.player.uniqueId)
    }
}
