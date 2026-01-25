package com.hacklab.minecraft.notoriety.territory.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import org.bukkit.GameMode
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

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
                i18n.sendError(player, "territory.protected_beacon",
                    "Territory beacon cannot be destroyed (%s)", guildName)
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
            i18n.sendError(player, "territory.protected_break",
                "This block is protected by %s's territory", guildName)
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
            i18n.sendError(player, "territory.protected_place",
                "You cannot place blocks in %s's territory", guildName)
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
            i18n.sendError(player, "territory.protected_interact",
                "You cannot interact with this in %s's territory", guildName)
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

    /**
     * 領地内のモンスタースポーン制御
     * mobSpawnEnabledがfalseの領地ではモンスターのスポーンをブロック
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // モンスターのみ対象
        if (event.entity !is Monster) return

        // スポナーからのスポーンは許可（トラップ用）
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.SPAWNER) return

        // コマンドやプラグインからのスポーンは許可
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return

        // 領地内でスポーンが許可されているかチェック
        if (!territoryService.isMobSpawnAllowed(event.location)) {
            event.isCancelled = true
        }
    }

    /**
     * アイテムフレームの操作保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // アイテムフレームのみ対象（GlowItemFrameはItemFrameのサブクラス）
        if (entity !is ItemFrame) return

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        val location = entity.location
        val territory = territoryService.getTerritoryAt(location) ?: return

        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            i18n.sendError(player, "territory.protected_item_frame",
                "You cannot interact with item frames in %s's territory", guildName)
            event.isCancelled = true
        }
    }

    /**
     * アイテムフレームの破壊保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        val entity = event.entity

        // アイテムフレームのみ対象
        if (entity !is ItemFrame) return

        val remover = event.remover
        val player = remover as? Player ?: return

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        val location = entity.location
        val territory = territoryService.getTerritoryAt(location) ?: return

        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            i18n.sendError(player, "territory.protected_item_frame",
                "You cannot interact with item frames in %s's territory", guildName)
            event.isCancelled = true
        }
    }

    /**
     * 看板編集の保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSignChange(event: SignChangeEvent) {
        val player = event.player
        val location = event.block.location

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        val territory = territoryService.getTerritoryAt(location) ?: return

        if (!territoryService.canAccessAt(location, player.uniqueId)) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            i18n.sendError(player, "territory.protected_sign",
                "You cannot edit signs in %s's territory", guildName)
            event.isCancelled = true
        }
    }

    /**
     * ブロックインタラクションの保護
     * ツールによるブロック状態変更、レッドストーン装置の操作等を保護
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 右クリックのみ対象（ブロック状態変更系）
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val location = block.location

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // アクセス権があれば許可
        if (territoryService.canAccessAt(location, player.uniqueId)) return

        // 保護が必要なインタラクションかチェック
        val item = event.item
        val blockType = block.type

        val shouldBlock = when {
            // ツールによるブロック変更
            isToolModification(item?.type, blockType) -> true
            // TNT点火
            item?.type == Material.FLINT_AND_STEEL && blockType == Material.TNT -> true
            // ブロック状態を変更するインタラクション
            isModifiableBlock(blockType) -> true
            else -> false
        }

        if (shouldBlock) {
            val guildName = guildService.getGuild(territory.guildId)?.name ?: "Unknown"
            i18n.sendError(player, "territory.protected_interact",
                "You cannot interact with this in %s's territory", guildName)
            event.isCancelled = true
        }
    }

    /**
     * ツールによるブロック変更かどうか判定
     */
    private fun isToolModification(itemType: Material?, blockType: Material): Boolean {
        if (itemType == null) return false

        return when {
            // スコップ → 土/草をパスに変更
            itemType.name.endsWith("_SHOVEL") && isPathConvertible(blockType) -> true
            // 斧 → 原木の皮を剥ぐ
            itemType.name.endsWith("_AXE") && isStrippable(blockType) -> true
            // クワ → 土を耕す
            itemType.name.endsWith("_HOE") && isFarmable(blockType) -> true
            else -> false
        }
    }

    /**
     * パスに変換可能なブロック
     */
    private fun isPathConvertible(type: Material): Boolean {
        return type in listOf(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.PODZOL,
            Material.MYCELIUM
        )
    }

    /**
     * 皮を剥げるブロック（原木・木材）
     */
    private fun isStrippable(type: Material): Boolean {
        val name = type.name
        return (name.endsWith("_LOG") || name.endsWith("_WOOD")) && !name.startsWith("STRIPPED_")
    }

    /**
     * 耕せるブロック
     */
    private fun isFarmable(type: Material): Boolean {
        return type in listOf(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.DIRT_PATH
        )
    }

    /**
     * 右クリックで状態変更可能なブロック
     */
    private fun isModifiableBlock(type: Material): Boolean {
        val name = type.name
        return when {
            // レッドストーン装置
            type == Material.REPEATER -> true
            type == Material.COMPARATOR -> true
            type == Material.DAYLIGHT_DETECTOR -> true
            type == Material.NOTE_BLOCK -> true
            // 消費可能
            type == Material.CAKE -> true
            name.endsWith("_CANDLE_CAKE") -> true
            // コンテナ的操作
            type == Material.FLOWER_POT -> true
            name.startsWith("POTTED_") -> true
            type == Material.COMPOSTER -> true
            type == Material.BEEHIVE -> true
            type == Material.BEE_NEST -> true
            type == Material.RESPAWN_ANCHOR -> true
            // キャンドル（点火/消火）
            name.endsWith("_CANDLE") || type == Material.CANDLE -> true
            // 大釜
            type == Material.CAULDRON -> true
            type == Material.WATER_CAULDRON -> true
            type == Material.LAVA_CAULDRON -> true
            type == Material.POWDER_SNOW_CAULDRON -> true
            // ジュークボックス
            type == Material.JUKEBOX -> true
            // 書見台（本の操作）
            type == Material.LECTERN -> true
            // チゼルドブックシェルフ
            type == Material.CHISELED_BOOKSHELF -> true
            // 装飾ポット
            type == Material.DECORATED_POT -> true
            else -> false
        }
    }
}
