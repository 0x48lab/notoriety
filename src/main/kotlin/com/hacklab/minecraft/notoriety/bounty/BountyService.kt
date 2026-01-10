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

    /**
     * 赤プレイヤー（アクティブなPK）の懸賞金リストを取得
     * PKCountが0になった灰プレイヤーは除外される
     */
    fun getBountyList(): List<BountyEntry> = storage.getAllBounties()
        .filter { isActiveRedPlayer(it.target) }
        .sortedByDescending { it.total }

    /**
     * 指定したランクの赤プレイヤーの懸賞金を取得（看板表示用）
     * PKCountが0の灰プレイヤーは除外される
     */
    fun getActiveBountyByRank(rank: Int): BountyEntry? {
        val activeBounties = storage.getAllBounties()
            .filter { isActiveRedPlayer(it.target) }
            .sortedByDescending { it.total }
        return if (rank > 0 && rank <= activeBounties.size) activeBounties[rank - 1] else null
    }

    fun getBounty(target: UUID): BountyEntry? = storage.getBounty(target)

    fun hasBounty(target: UUID): Boolean = storage.getBounty(target) != null

    /**
     * 対象が赤プレイヤー（PKCount >= 1）かどうかを判定
     */
    private fun isActiveRedPlayer(targetUuid: UUID): Boolean {
        val data = plugin.playerManager.getOrLoadPlayer(targetUuid) ?: return false
        return data.getNameColor() == NameColor.RED
    }
}
