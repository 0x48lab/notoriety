package com.hacklab.minecraft.notoriety.achievement.listener

import com.hacklab.minecraft.notoriety.achievement.service.AchievementContext
import com.hacklab.minecraft.notoriety.achievement.service.AchievementService
import com.hacklab.minecraft.notoriety.achievement.service.AchievementServiceImpl
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.event.BountyClaimedEvent
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.event.PlayerGoodDeedEvent
import com.hacklab.minecraft.notoriety.guild.event.GuildCreateEvent
import com.hacklab.minecraft.notoriety.guild.event.GuildMemberJoinEvent
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent

/**
 * アチーブメント関連イベントリスナー
 */
class AchievementListener(
    private val achievementService: AchievementService,
    private val playerManager: PlayerManager
) : Listener {

    /**
     * 善行イベント時にFame系アチーブメントをチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGoodDeed(event: PlayerGoodDeedEvent) {
        achievementService.checkAndUnlock(
            event.playerUuid,
            AchievementContext.forGoodDeed()
        )
    }

    /**
     * ネームカラー変更時に状態遷移アチーブメントをチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onColorChange(event: PlayerColorChangeEvent) {
        achievementService.checkAndUnlock(
            event.playerUuid,
            AchievementContext.forColorChange(event.oldColor)
        )
    }

    /**
     * 懸賞金獲得時に戦闘系アチーブメントをチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBountyClaimed(event: BountyClaimedEvent) {
        val playerData = playerManager.getPlayer(event.killerUuid) ?: return

        // 赤プレイヤー討伐回数を更新
        val newKillCount = playerData.incrementRedKills()

        // 懸賞金獲得額を更新
        playerData.addBountyEarned(event.bountyAmount.toLong())

        // 初回かどうかを判定
        val isFirstKill = newKillCount == 1

        achievementService.checkAndUnlock(
            event.killerUuid,
            AchievementContext.forRedKill(isFirstKill)
        )
    }

    /**
     * 村人取引時に取引カウントを更新してアチーブメントチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTrade(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.whoClicked as? Player ?: return

        // Result slotクリックのみカウント（実際に取引が成立した場合）
        if (event.slot != 2) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        val playerData = playerManager.getPlayer(player) ?: return
        playerData.incrementTradeCount()

        achievementService.checkAndUnlock(
            player.uniqueId,
            AchievementContext.forTrade()
        )
    }

    /**
     * エンティティ死亡時にゴーレム討伐をチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer ?: return

        // ゴーレム討伐
        if (entity is IronGolem) {
            val playerData = playerManager.getPlayer(killer) ?: return
            playerData.incrementGolemKills()

            achievementService.checkAndUnlock(
                killer.uniqueId,
                AchievementContext.EMPTY
            )
        }
    }

    /**
     * ギルド加入時にアチーブメントチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGuildJoin(event: GuildMemberJoinEvent) {
        achievementService.checkAndUnlock(
            event.member.uniqueId,
            AchievementContext.forGuildJoin()
        )
    }

    /**
     * ギルド創設時にアチーブメントチェック
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGuildCreate(event: GuildCreateEvent) {
        achievementService.checkAndUnlock(
            event.creatorUuid,
            AchievementContext.forGuildCreate()
        )
    }

    /**
     * プレイヤーログアウト時にキャッシュをクリア
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        (achievementService as? AchievementServiceImpl)?.clearCache(event.player.uniqueId)
    }
}
