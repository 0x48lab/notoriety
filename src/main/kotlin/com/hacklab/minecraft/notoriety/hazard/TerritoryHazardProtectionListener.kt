package com.hacklab.minecraft.notoriety.hazard

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent

/**
 * 領地内での危険物設置防止リスナー
 * ギルド領地内で非メンバーによるマグマ/水バケツ、TNT設置を禁止する
 */
class TerritoryHazardProtectionListener(
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val i18nManager: I18nManager
) : Listener {

    /**
     * バケツ使用時のチェック（マグマ/水）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // 設置されるブロックの位置
        val location = event.blockClicked.getRelative(event.blockFace).location

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // ギルドメンバーかチェック
        val playerGuild = guildService.getPlayerGuild(player.uniqueId)
        if (playerGuild != null && playerGuild.id == territory.guildId) {
            return // メンバーは許可
        }

        // 非メンバーは危険物設置禁止
        when (event.bucket) {
            Material.LAVA_BUCKET -> {
                event.isCancelled = true
                player.sendMessage(i18nManager.get(player.uniqueId, "territory_hazard.lava_blocked", "⚠ You cannot use lava buckets in this territory"))
            }
            Material.WATER_BUCKET -> {
                event.isCancelled = true
                player.sendMessage(i18nManager.get(player.uniqueId, "territory_hazard.water_blocked", "⚠ You cannot use water buckets in this territory"))
            }
            else -> {
                // 他のバケツは許可
            }
        }
    }

    /**
     * TNT設置時のチェック
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player

        // クリエイティブモードは除外
        if (player.gameMode == GameMode.CREATIVE) return

        // TNTのみチェック
        if (event.block.type != Material.TNT) return

        val location = event.block.location

        // 領地内かチェック
        val territory = territoryService.getTerritoryAt(location) ?: return

        // ギルドメンバーかチェック
        val playerGuild = guildService.getPlayerGuild(player.uniqueId)
        if (playerGuild != null && playerGuild.id == territory.guildId) {
            return // メンバーは許可
        }

        // 非メンバーはTNT設置禁止
        event.isCancelled = true
        player.sendMessage(i18nManager.get(player.uniqueId, "territory_hazard.tnt_blocked", "⚠ You cannot place TNT in this territory"))
    }
}
