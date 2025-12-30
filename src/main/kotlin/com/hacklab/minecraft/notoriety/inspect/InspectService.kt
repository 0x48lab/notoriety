package com.hacklab.minecraft.notoriety.inspect

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.ownership.BlockOwnershipInfo
import com.hacklab.minecraft.notoriety.ownership.OwnershipRepository
import com.hacklab.minecraft.notoriety.trust.TrustService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InspectService(
    private val plugin: JavaPlugin,
    private val ownershipRepository: OwnershipRepository,
    private val trustService: TrustService,
    private val i18n: I18nManager
) {
    private val inspectingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private var actionBarTask: Int = -1

    fun isInspecting(player: Player): Boolean = inspectingPlayers.contains(player.uniqueId)

    fun toggleInspectMode(player: Player): Boolean {
        return if (inspectingPlayers.contains(player.uniqueId)) {
            disableInspectMode(player)
            false
        } else {
            enableInspectMode(player)
            true
        }
    }

    fun enableInspectMode(player: Player) {
        inspectingPlayers.add(player.uniqueId)
        player.sendMessage(
            Component.text(i18n.get("inspect.enabled", "Inspect mode enabled"))
                .color(NamedTextColor.GREEN)
        )
        startActionBarTaskIfNeeded()
    }

    fun disableInspectMode(player: Player) {
        inspectingPlayers.remove(player.uniqueId)
        player.sendMessage(
            Component.text(i18n.get("inspect.disabled", "Inspect mode disabled"))
                .color(NamedTextColor.YELLOW)
        )
        // Clear action bar
        player.sendActionBar(Component.empty())
        stopActionBarTaskIfEmpty()
    }

    fun disableAllInspectModes() {
        inspectingPlayers.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { disableInspectMode(it) }
        }
    }

    fun onPlayerQuit(player: Player) {
        inspectingPlayers.remove(player.uniqueId)
        stopActionBarTaskIfEmpty()
    }

    fun getBlockInfo(location: Location): BlockOwnershipInfo? {
        return ownershipRepository.getOwnershipInfo(location)
    }

    fun formatBlockInfo(player: Player, location: Location): List<Component> {
        val block = location.block
        val ownershipInfo = getBlockInfo(location)

        val messages = mutableListOf<Component>()

        // Header
        messages.add(
            Component.text(i18n.get("inspect.header", "=== Block Info ==="))
                .color(NamedTextColor.GOLD)
        )

        // Block type
        val blockTypeName = block.type.name.lowercase().replace("_", " ")
            .replaceFirstChar { it.uppercase() }
        messages.add(
            Component.text(i18n.get("inspect.block_type", "Type: %s", blockTypeName))
                .color(NamedTextColor.WHITE)
        )

        // Location
        messages.add(
            Component.text(i18n.get("inspect.location", "Location: %s (%d, %d, %d)",
                location.world.name, location.blockX, location.blockY, location.blockZ))
                .color(NamedTextColor.GRAY)
        )

        if (ownershipInfo != null) {
            // Owner name
            val ownerName = Bukkit.getOfflinePlayer(ownershipInfo.ownerUuid).name ?: "???"
            messages.add(
                Component.text(i18n.get("inspect.owner", "Owner: %s", ownerName))
                    .color(NamedTextColor.AQUA)
            )

            // Placed at
            if (ownershipInfo.placedAt != null) {
                val formatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                val placedAtStr = formatter.format(ownershipInfo.placedAt)
                messages.add(
                    Component.text(i18n.get("inspect.placed_at", "Placed: %s", placedAtStr))
                        .color(NamedTextColor.GRAY)
                )
            }

            // Trust relationship
            if (ownershipInfo.ownerUuid != player.uniqueId) {
                val isTrusted = trustService.isTrusted(ownershipInfo.ownerUuid, player.uniqueId)
                if (isTrusted) {
                    messages.add(
                        Component.text(i18n.get("inspect.trusted", "You are trusted by the owner"))
                            .color(NamedTextColor.GREEN)
                    )
                } else {
                    messages.add(
                        Component.text(i18n.get("inspect.not_trusted", "You are NOT trusted"))
                            .color(NamedTextColor.RED)
                    )
                }
            } else {
                messages.add(
                    Component.text(i18n.get("inspect.your_block", "This is your block"))
                        .color(NamedTextColor.GREEN)
                )
            }
        } else {
            // No owner
            messages.add(
                Component.text(i18n.get("inspect.no_owner", "Owner: None (Free to use)"))
                    .color(NamedTextColor.GRAY)
            )
        }

        return messages
    }

    private fun startActionBarTaskIfNeeded() {
        if (actionBarTask != -1) return
        if (inspectingPlayers.isEmpty()) return

        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            inspectingPlayers.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    val actionBarMessage = Component.text()
                        .append(Component.text("\uD83D\uDD0D ").color(NamedTextColor.YELLOW))
                        .append(Component.text(i18n.get("inspect.actionbar", "Inspect Mode - Click blocks to check ownership")).color(NamedTextColor.GOLD))
                        .build()
                    player.sendActionBar(actionBarMessage)
                }
            }
        }, 0L, 40L).taskId  // 2 seconds interval
    }

    private fun stopActionBarTaskIfEmpty() {
        if (inspectingPlayers.isEmpty() && actionBarTask != -1) {
            Bukkit.getScheduler().cancelTask(actionBarTask)
            actionBarTask = -1
        }
    }
}
