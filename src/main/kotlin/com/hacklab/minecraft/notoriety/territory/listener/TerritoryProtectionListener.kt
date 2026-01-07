package com.hacklab.minecraft.notoriety.territory.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder

/**
 * 領地保護リスナー
 * 領地内のブロック破壊・設置・チェストアクセスを非メンバーからブロック
 */
class TerritoryProtectionListener(
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val beaconManager: BeaconManager,
    private val i18n: I18nManager
) : Listener {

    /**
     * ブロック破壊の保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val location = event.block.location

        // ビーコン本体または土台の破壊を防止（クリエイティブモード含む全員）
        if (beaconManager.isBeaconBlock(location) || beaconManager.isBeaconBase(location)) {
            val territory = territoryService.getTerritoryAt(location)
            if (territory != null) {
                val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
                player.sendMessage(Component.text(i18n.get(
                    player.uniqueId,
                    "territory.protected_beacon",
                    "§cTerritory beacon cannot be destroyed (%s)",
                    guildName
                )))
                event.isCancelled = true
                return
            }
        }

        // クリエイティブモードは以降の保護を除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // アクセス権チェック
        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            sendProtectedMessage(player, guildName)
            event.isCancelled = true
        }
    }

    /**
     * ブロック設置の保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val location = event.block.location

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // アクセス権チェック
        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            player.sendMessage(Component.text(i18n.get(
                player.uniqueId,
                "territory.protected_place",
                "§cYou cannot place blocks in %s's territory",
                guildName
            )))
            event.isCancelled = true
        }
    }

    /**
     * インベントリ（チェスト等）オープンの保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val holder = event.inventory.holder

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // コンテナの位置を取得
        val location = when (holder) {
            is org.bukkit.block.Container -> holder.location
            is org.bukkit.block.DoubleChest -> holder.location
            else -> return
        }

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // アクセス権チェック
        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            player.sendMessage(Component.text(i18n.get(
                player.uniqueId,
                "territory.protected_interact",
                "§cYou cannot interact with this in %s's territory",
                guildName
            )))
            event.isCancelled = true
        }
    }

    /**
     * インタラクト（ドア、ボタン等）の保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // インタラクト対象のブロックタイプをチェック
        if (!isProtectedInteractable(block.type)) return

        val location = block.location

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // アクセス権チェック
        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            player.sendMessage(Component.text(i18n.get(
                player.uniqueId,
                "territory.protected_interact",
                "§cYou cannot interact with this in %s's territory",
                guildName
            )))
            event.isCancelled = true
        }
    }

    /**
     * 爆発による破壊の保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        // 領地内のブロックとビーコン関連ブロックを爆発リストから除外
        event.blockList().removeIf { block ->
            val location = block.location
            // ビーコン本体または土台は常に保護
            if (beaconManager.isBeaconBlock(location) || beaconManager.isBeaconBase(location)) {
                return@removeIf true
            }
            // 領地内のブロックも保護
            val territory = territoryService.getTerritoryAt(location)
            territory != null
        }
    }

    private fun sendProtectedMessage(player: Player, guildName: String) {
        player.sendMessage(Component.text(i18n.get(
            player.uniqueId,
            "territory.protected_break",
            "§cThis block is protected by %s's territory",
            guildName
        )))
    }

    /**
     * 保護対象のインタラクト可能ブロック
     */
    private fun isProtectedInteractable(material: Material): Boolean {
        return material.name.contains("DOOR") ||
                material.name.contains("GATE") ||
                material.name.contains("BUTTON") ||
                material == Material.LEVER ||
                material == Material.REPEATER ||
                material == Material.COMPARATOR ||
                material == Material.DAYLIGHT_DETECTOR ||
                material == Material.NOTE_BLOCK ||
                material == Material.JUKEBOX ||
                material == Material.BELL ||
                material == Material.LECTERN
    }
}
