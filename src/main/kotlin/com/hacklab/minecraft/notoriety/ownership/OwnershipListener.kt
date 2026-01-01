package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.CrimeCheckResult
import com.hacklab.minecraft.notoriety.NotorietyService
import com.hacklab.minecraft.notoriety.core.toBlockLoc
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.GameMode
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant

class OwnershipListener(
    private val plugin: JavaPlugin,
    private val ownershipService: OwnershipService,
    private val guildService: GuildService,
    private val notorietyService: NotorietyService,
    private val getPlayerNameColor: (Player) -> NameColor?
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player

        // クリエイティブモードは所有権システムをスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val color = getPlayerNameColor(player) ?: return

        // 青プレイヤーのみ所有権を登録
        if (color == NameColor.BLUE) {
            ownershipService.registerOwnership(event.block.location, player.uniqueId)
        }

        // 保留犯罪のキャンセルチェック
        val location = event.block.location.toBlockLoc()
        if (ownershipService.cancelPendingCrime(location, player.uniqueId, event.block.type)) {
            // キャンセル成功 - 犯罪にならなかった
        }
    }

    /**
     * コンテナを開いた時に警告を表示
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val holder = event.inventory.holder
        if (holder !is Container) return

        val location = holder.block.location

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkContainerAccessCrime(player, location)
    }

    /**
     * ブロックを壊し始めた時に警告を表示
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val isContainer = block.state is Container

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkBlockBreakCrime(player, block.location, isContainer)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val owner = ownershipService.getOwner(block.location) ?: return

        // 本人または信頼されたプレイヤーは許可（ギルドメンバー含む）
        if (ownershipService.canAccess(block.location, player.uniqueId, guildService)) {
            ownershipService.removeOwnership(block.location)
            return
        }

        val isContainer = block.state is Container

        // コンテナは即時犯罪
        if (isContainer) {
            notorietyService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.DESTROY,
                alignmentPenalty = 5,
                victim = owner,
                location = block.location,
                detail = block.type.name
            )
        } else {
            // 保留犯罪として登録
            ownershipService.addPendingCrime(PendingCrime(
                location = block.location.toBlockLoc(),
                blockType = block.type,
                playerUuid = player.uniqueId,
                ownerUuid = owner,
                brokenAt = Instant.now(),
                alignmentPenalty = 5
            ))
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val inventory = event.inventory
        val holder = inventory.holder

        // コンテナのみ対象
        if (holder !is Container) return

        val location = holder.block.location
        val owner = ownershipService.getOwner(location) ?: return

        // 本人は許可
        if (owner == player.uniqueId) return

        // アイテムを実際に取り出す操作かチェック
        // 1. Shift+クリックでプレイヤーインベントリに移動
        // 2. コンテナからカーソルに取ったアイテムをプレイヤーインベントリに置く
        val isExtraction = when {
            // Shift+クリックでコンテナからプレイヤーインベントリへ移動
            event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                event.clickedInventory == inventory -> true
            // カーソルにアイテムを持った状態でプレイヤーインベントリに置く
            event.action in listOf(
                InventoryAction.PLACE_ALL,
                InventoryAction.PLACE_ONE,
                InventoryAction.PLACE_SOME,
                InventoryAction.SWAP_WITH_CURSOR
            ) && event.clickedInventory == player.inventory -> true
            else -> false
        }

        if (!isExtraction) return

        // DISTRUST（不信頼）の場合は取り出し不可
        if (!ownershipService.canTakeFromContainer(location, player.uniqueId, guildService)) {
            event.isCancelled = true
            return
        }

        // TRUSTまたは同じギルドメンバーの場合は犯罪なし
        if (guildService.isAccessAllowed(owner, player.uniqueId)) return

        // 未設定の場合: 取り出せるが窃盗として犯罪確定（名声マイナス）
        notorietyService.commitCrime(
            criminal = player.uniqueId,
            crimeType = CrimeType.THEFT,
            alignmentPenalty = 50,
            victim = owner,
            location = location
        )
    }
}
