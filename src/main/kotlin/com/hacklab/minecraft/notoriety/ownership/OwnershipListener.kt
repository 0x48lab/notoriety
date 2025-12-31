package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.core.BlockLocation
import com.hacklab.minecraft.notoriety.core.toBlockLoc
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class OwnershipListener(
    private val plugin: Notoriety,
    private val ownershipService: OwnershipService,
    private val guildService: GuildService,
    private val crimeService: CrimeService,
    private val chatService: ChatService
) : Listener {

    // 警告のクールダウン（同じブロックに対して連続警告を防ぐ）
    private data class WarningKey(val playerUuid: UUID, val location: BlockLocation)
    private val warningCooldowns = ConcurrentHashMap<WarningKey, Instant>()
    private val WARNING_COOLDOWN_SECONDS = 10L

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

    /**
     * コンテナを開いた時に警告を表示
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        // 警告がOFFの場合はスキップ
        if (!chatService.isWarningsEnabled(player.uniqueId)) return

        val holder = event.inventory.holder
        if (holder !is Container) return

        val location = holder.block.location
        val owner = ownershipService.getOwner(location) ?: return

        // アクセス権がある場合は警告不要（取り出しも可能な場合）
        if (ownershipService.canTakeFromContainer(location, player.uniqueId, guildService)) return

        // クールダウンチェック
        val warningKey = WarningKey(player.uniqueId, location.toBlockLoc())
        val lastWarning = warningCooldowns[warningKey]
        val now = Instant.now()
        if (lastWarning != null && java.time.Duration.between(lastWarning, now).seconds < WARNING_COOLDOWN_SECONDS) {
            return
        }
        warningCooldowns[warningKey] = now

        // 所有者名を取得
        val ownerName = Bukkit.getOfflinePlayer(owner).name ?: "???"

        // 警告メッセージを表示
        val message = plugin.i18nManager.get("warning.chest_open", "warning.chest_open").format(ownerName)
        player.sendMessage(message)
    }

    /**
     * ブロックを壊し始めた時に警告を表示
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        // 警告がOFFの場合はスキップ
        if (!chatService.isWarningsEnabled(player.uniqueId)) return

        val block = event.block
        val owner = ownershipService.getOwner(block.location) ?: return

        // アクセス権がある場合は警告不要
        if (ownershipService.canAccess(block.location, player.uniqueId, guildService)) return

        // クールダウンチェック（同じブロックに対する連続警告を防ぐ）
        val warningKey = WarningKey(player.uniqueId, block.location.toBlockLoc())
        val lastWarning = warningCooldowns[warningKey]
        val now = Instant.now()
        if (lastWarning != null && java.time.Duration.between(lastWarning, now).seconds < WARNING_COOLDOWN_SECONDS) {
            return
        }
        warningCooldowns[warningKey] = now

        // 所有者名を取得
        val ownerName = Bukkit.getOfflinePlayer(owner).name ?: "???"

        // 警告メッセージを表示
        val messageKey = if (block.state is Container) "warning.container_break" else "warning.block_break"
        val message = plugin.i18nManager.get(messageKey, messageKey).format(ownerName)
        player.sendMessage(message)
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

        // コンテナは即時犯罪
        if (block.state is Container) {
            crimeService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.DESTROY,
                alignmentPenalty = 10,
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
                alignmentPenalty = 10
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

        // 本人または信頼されたプレイヤーは許可（ギルドメンバー含む）
        // ただし、不信頼に設定されている場合は取り出し不可
        if (ownershipService.canTakeFromContainer(location, player.uniqueId, guildService)) return

        // 窃盗として犯罪確定
        crimeService.commitCrime(
            criminal = player.uniqueId,
            crimeType = CrimeType.THEFT,
            alignmentPenalty = 50,
            victim = owner,
            location = location
        )
        plugin.reputationService.updateDisplay(player)
    }
}
