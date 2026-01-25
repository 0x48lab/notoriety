package com.hacklab.minecraft.notoriety.combat

import com.hacklab.minecraft.notoriety.CrimeCheckResult
import com.hacklab.minecraft.notoriety.NotorietyService
import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
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
    private val notorietyService: NotorietyService,
    private val bountyService: BountyService,
    private val trustService: TrustService,
    private val combatTagService: CombatTagService? = null
) : Listener {

    /**
     * プレイヤー攻撃時の警告表示（犯罪判定より先に実行）
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerDamageWarning(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkPlayerAttackCrime(attacker, victim)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        // 戦闘タグを記録（間接PK検知用）
        // ノックバックの有無を判定（ダメージ > 0 ならノックバックが発生する可能性がある）
        val hasKnockback = event.damage > 0 && !attacker.isSneaking
        combatTagService?.tagPlayer(victim, attacker, event.damage, hasKnockback)

        // 犯罪判定
        when (val result = notorietyService.checkPlayerAttackCrime(attacker, victim)) {
            is CrimeCheckResult.IsCrime -> {
                notorietyService.commitCrime(
                    criminal = attacker.uniqueId,
                    crimeType = CrimeType.ATTACK,
                    alignmentPenalty = result.penalty,
                    victim = result.victimUuid
                )
            }
            else -> { /* 犯罪にならない */ }
        }
    }

    /**
     * ペット攻撃時の警告表示（犯罪判定より先に実行）
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPetDamageWarning(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val pet = event.entity as? Tameable ?: return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkPetAttackCrime(attacker, pet)
    }

    // ペット攻撃判定
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPetDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val pet = event.entity as? Tameable ?: return

        // 犯罪判定
        when (val result = notorietyService.checkPetAttackCrime(attacker, pet)) {
            is CrimeCheckResult.IsCrime -> {
                notorietyService.commitCrime(
                    criminal = attacker.uniqueId,
                    crimeType = CrimeType.ATTACK,
                    alignmentPenalty = result.penalty,
                    victim = result.victimUuid,
                    detail = "Pet: ${pet.type.name}"
                )
            }
            else -> { /* 犯罪にならない */ }
        }
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
        // 自殺（killer == victim）の場合はPK判定しない
        val killer = victim.killer
        if (killer != null && killer.uniqueId != victim.uniqueId) {
            // 被害者が加害者を信頼していれば犯罪・PKにならない
            val isTrusted = trustService.isTrusted(victim.uniqueId, killer.uniqueId)

            if (!isTrusted) {
                // PK判定と報酬処理
                notorietyService.onPlayerKill(killer.uniqueId, victim.uniqueId)

                // 赤プレイヤーを倒した場合、懸賞金処理
                if (victimData.getNameColor() == NameColor.RED) {
                    bountyService.claimBounty(killer.uniqueId, victim.uniqueId)
                }
            }

            // 表示を更新
            notorietyService.updateDisplay(killer)
        } else if (killer == null) {
            // 直接のキラーがいない場合、間接PKをチェック
            val lastDamage = victim.lastDamageCause
            if (lastDamage != null) {
                notorietyService.checkAndProcessIndirectPK(
                    victim = victim,
                    deathLocation = victim.location,
                    deathCause = lastDamage.cause
                )
            }
        }

        notorietyService.updateDisplay(victim)
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
        notorietyService.updateDisplay(killer)
    }
}
