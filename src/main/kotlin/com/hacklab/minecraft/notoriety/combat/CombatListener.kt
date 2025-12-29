package com.hacklab.minecraft.notoriety.combat

import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.ReputationService
import com.hacklab.minecraft.notoriety.trust.TrustService
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
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

        // 青を攻撃したらAlignment減少
        if (victimData.getNameColor() == NameColor.BLUE) {
            crimeService.commitCrime(
                criminal = attacker.uniqueId,
                crimeType = CrimeType.ATTACK,
                alignmentPenalty = 1,
                victim = victim.uniqueId
            )
            reputationService.updateDisplay(attacker)
        }
    }

    // ペット攻撃判定
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPetDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val pet = event.entity as? Tameable ?: return

        // テイムされていなければ対象外
        if (!pet.isTamed) return

        // 所有者がオンラインプレイヤーでなければ対象外
        val owner = pet.owner as? Player ?: return

        // 自分のペットは対象外
        if (owner.uniqueId == attacker.uniqueId) return

        // 所有者が攻撃者を信頼していれば犯罪にならない
        if (trustService.isTrusted(owner.uniqueId, attacker.uniqueId)) return

        val ownerData = playerManager.getPlayer(owner) ?: return

        // 青色プレイヤーのペットのみ保護
        if (ownerData.getNameColor() != NameColor.BLUE) return

        // Alignment -1
        crimeService.commitCrime(
            criminal = attacker.uniqueId,
            crimeType = CrimeType.ATTACK,
            alignmentPenalty = 1,
            victim = owner.uniqueId,
            detail = "Pet: ${pet.type.name}"
        )

        reputationService.updateDisplay(attacker)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val victimData = playerManager.getPlayer(victim) ?: return

        // Fame減少（死亡ペナルティ 10%）
        victimData.addFame(-(victimData.fame * 0.1).toInt())

        // 赤プレイヤーはアイテムを落とす、青/灰は保持
        if (victimData.getNameColor() == NameColor.RED) {
            event.keepInventory = false
            event.keepLevel = false
        } else {
            event.keepInventory = true
            event.keepLevel = true
            event.drops.clear()
            event.droppedExp = 0
        }

        // プレイヤーに殺された場合のみPK判定・懸賞金処理
        val killer = victim.killer
        if (killer != null) {
            // 被害者が加害者を信頼していれば犯罪・PKにならない
            val isTrusted = trustService.isTrusted(victim.uniqueId, killer.uniqueId)

            if (!isTrusted) {
                // PK判定と報酬処理
                reputationService.onPlayerKill(killer.uniqueId, victim.uniqueId)

                // 赤プレイヤーを倒した場合、懸賞金処理
                if (victimData.getNameColor() == NameColor.RED) {
                    bountyService.claimBounty(killer.uniqueId, victim.uniqueId)
                }
            }

            // 表示を更新
            reputationService.updateDisplay(killer)
        }

        reputationService.updateDisplay(victim)
    }

    // モンスター討伐でFame+1
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMonsterKill(event: EntityDeathEvent) {
        val entity = event.entity
        // モンスターのみ対象
        if (entity !is Monster) return

        val killer = entity.killer ?: return
        val killerData = playerManager.getPlayer(killer) ?: return

        // Fame +1
        killerData.addFame(1)

        // 表示を更新
        reputationService.updateDisplay(killer)
    }
}
