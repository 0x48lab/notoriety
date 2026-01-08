package com.hacklab.minecraft.notoriety.bounty

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.core.economy.EconomyService
import com.hacklab.minecraft.notoriety.event.BountyClaimedEvent
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Bukkit
import java.util.*

class BountyService(
    private val plugin: Notoriety,
    private val economy: EconomyService
) {
    val storage = BountyStorage(plugin)
    lateinit var signManager: BountySignManager

    companion object {
        const val MINIMUM_BOUNTY = 100.0
    }

    fun initializeSignManager() {
        signManager = BountySignManager(plugin, this)
    }

    fun setBounty(contributor: UUID, target: UUID, amount: Double): Boolean {
        // オフラインプレイヤーにも懸賞金を設定できるようにDBからも読み込む
        val targetData = plugin.playerManager.getOrLoadPlayer(target) ?: return false
        if (targetData.getNameColor() != NameColor.RED) return false
        if (amount < MINIMUM_BOUNTY) return false

        if (!economy.withdraw(contributor, amount)) return false

        storage.addBounty(target, contributor, amount)
        return true
    }

    fun claimBounty(killerUuid: UUID, targetUuid: UUID) {
        val bounty = storage.getBounty(targetUuid) ?: return
        val targetData = plugin.playerManager.getPlayer(targetUuid) ?: return

        // 経験値報酬
        val expReward = when (targetData.pkCount) {
            in 1..4 -> 50
            in 5..9 -> 100
            in 10..19 -> 300
            else -> 500
        }
        Bukkit.getPlayer(killerUuid)?.giveExp(expReward)

        // 懸賞金支払い
        economy.deposit(killerUuid, bounty.total)

        // 懸賞金クリア
        storage.removeBounty(targetUuid)

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            BountyClaimedEvent(killerUuid, targetUuid, bounty.total, expReward)
        )
    }

    fun getBountyList(): List<BountyEntry> = storage.getAllBounties()
        .sortedByDescending { it.total }

    fun getBounty(target: UUID): BountyEntry? = storage.getBounty(target)

    fun hasBounty(target: UUID): Boolean = storage.getBounty(target) != null

    /**
     * 懸賞金を返却して削除する（PKCount が 0 になったときに呼び出す）
     * 出資者に全額返金される
     */
    fun refundBounty(targetUuid: UUID) {
        val bounty = storage.getBounty(targetUuid) ?: return

        // 各出資者に返金
        bounty.contributors.forEach { (contributorUuid, amount) ->
            economy.deposit(contributorUuid, amount)
            // オンラインの出資者に通知
            Bukkit.getPlayer(contributorUuid)?.sendMessage(
                "§a${Bukkit.getOfflinePlayer(targetUuid).name} の懸賞金 ${amount.toLong()} が返金されました（対象が赤プレイヤーではなくなったため）"
            )
        }

        // 懸賞金を削除
        storage.removeBounty(targetUuid)
    }

    /**
     * サーバー起動時に不整合な懸賞金データをクリーンアップ
     * PKCount が 0 のプレイヤーに対する懸賞金を返金して削除する
     */
    fun cleanupInvalidBounties() {
        val allBounties = storage.getAllBounties()
        var cleanedCount = 0

        allBounties.forEach { bounty ->
            val targetData = plugin.playerManager.getOrLoadPlayer(bounty.target)
            // プレイヤーデータが存在しない、または PKCount が 0 の場合は返金
            if (targetData == null || targetData.pkCount == 0) {
                bounty.contributors.forEach { (contributorUuid, amount) ->
                    economy.deposit(contributorUuid, amount)
                }
                storage.removeBounty(bounty.target)
                cleanedCount++
                val targetName = Bukkit.getOfflinePlayer(bounty.target).name ?: bounty.target.toString()
                plugin.logger.info("Invalid bounty cleaned up: $targetName (refunded ${bounty.total.toLong()} to contributors)")
            }
        }

        if (cleanedCount > 0) {
            plugin.logger.info("Cleaned up $cleanedCount invalid bounties on startup")
        }
    }
}
