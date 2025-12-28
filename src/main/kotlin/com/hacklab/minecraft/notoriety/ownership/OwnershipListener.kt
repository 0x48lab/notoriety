package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.core.BlockLocation
import com.hacklab.minecraft.notoriety.core.toBlockLoc
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.trust.TrustService
import org.bukkit.GameMode
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Instant

class OwnershipListener(
    private val plugin: Notoriety,
    private val ownershipService: OwnershipService,
    private val trustService: TrustService,
    private val crimeService: CrimeService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player

        // クリエイティブモードは所有権システムをスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val data = plugin.playerManager.getPlayer(player) ?: return

        // 青プレイヤーのみ所有権を登録
        if (data.getNameColor() == NameColor.BLUE) {
            ownershipService.registerOwnership(event.block.location, player.uniqueId)
        }

        // 保留犯罪のキャンセルチェック
        val location = event.block.location.toBlockLoc()
        if (ownershipService.cancelPendingCrime(location, player.uniqueId, event.block.type)) {
            // キャンセル成功 - 犯罪にならなかった
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val owner = ownershipService.getOwner(block.location) ?: return

        // 本人または信頼されたプレイヤーは許可
        if (ownershipService.canAccess(block.location, player.uniqueId, trustService)) {
            ownershipService.removeOwnership(block.location)
            return
        }

        // コンテナは即時犯罪
        if (block.state is Container) {
            crimeService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.DESTROY,
                alignmentPenalty = 50,
                victim = owner,
                location = block.location,
                detail = block.type.name
            )
            plugin.reputationService.updateDisplay(player)
        } else {
            // 保留犯罪として登録
            ownershipService.addPendingCrime(PendingCrime(
                location = block.location.toBlockLoc(),
                blockType = block.type,
                playerUuid = player.uniqueId,
                ownerUuid = owner,
                brokenAt = Instant.now(),
                alignmentPenalty = 50
            ))
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val inventory = event.inventory
        val holder = inventory.holder

        // コンテナからアイテムを取り出す場合のみチェック
        if (holder !is Container) return
        if (event.clickedInventory != inventory) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        val location = holder.block.location
        val owner = ownershipService.getOwner(location) ?: return

        // 本人または信頼されたプレイヤーは許可
        if (ownershipService.canAccess(location, player.uniqueId, trustService)) return

        // 窃盗として犯罪確定
        crimeService.commitCrime(
            criminal = player.uniqueId,
            crimeType = CrimeType.THEFT,
            alignmentPenalty = 100,
            victim = owner,
            location = location
        )
        plugin.reputationService.updateDisplay(player)
    }
}
