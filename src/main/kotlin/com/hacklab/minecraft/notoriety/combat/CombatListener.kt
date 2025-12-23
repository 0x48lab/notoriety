package com.hacklab.minecraft.notoriety.combat

import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.ReputationService
import com.hacklab.minecraft.notoriety.trust.TrustService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent

class CombatListener(
    private val playerManager: PlayerManager,
    private val crimeService: CrimeService,
    private val reputationService: ReputationService,
    private val bountyService: BountyService,
    private val trustService: TrustService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        // 被害者が加害者を信頼していれば犯罪にならない
        if (trustService.isTrusted(victim.uniqueId, attacker.uniqueId)) return

        val victimData = playerManager.getPlayer(victim) ?: return

        // 青を攻撃したらCrimePoint加算
        if (victimData.getNameColor() == NameColor.BLUE) {
            crimeService.commitCrime(
                criminal = attacker.uniqueId,
                crimeType = CrimeType.ATTACK,
                crimePoint = 150,
                victim = victim.uniqueId
            )
            reputationService.updateDisplay(attacker)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer ?: return

        val victimData = playerManager.getPlayer(victim) ?: return

        // 被害者が加害者を信頼していれば犯罪・PKにならない
        val isTrusted = trustService.isTrusted(victim.uniqueId, killer.uniqueId)

        // Fame減少（死亡ペナルティ 10%）- 信頼関係があっても適用
        victimData.addFame(-(victimData.fame * 0.1).toInt())

        if (!isTrusted) {
            // PK判定と報酬処理
            reputationService.onPlayerKill(killer.uniqueId, victim.uniqueId)

            // 赤プレイヤーを倒した場合、懸賞金処理
            if (victimData.getNameColor() == NameColor.RED) {
                bountyService.claimBounty(killer.uniqueId, victim.uniqueId)
            }
        }

        // 赤プレイヤーはアイテムを落とす
        if (victimData.getNameColor() == NameColor.RED) {
            event.keepInventory = false
            event.keepLevel = false
        } else {
            event.keepInventory = true
            event.keepLevel = true
            event.drops.clear()
            event.droppedExp = 0
        }

        // 表示を更新
        reputationService.updateDisplay(killer)
        reputationService.updateDisplay(victim)
    }
}
