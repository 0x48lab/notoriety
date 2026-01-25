package com.hacklab.minecraft.notoriety.combat

import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 戦闘タグサービス
 * プレイヤーへの直接ダメージ履歴を管理し、落下死などの間接PKで原因プレイヤーを特定する
 */
class CombatTagService(
    /** タグ保持期間（秒） */
    private val tagDurationSeconds: Long = 10
) {
    // 戦闘タグマップ（被害者UUID → タグ情報）
    private val combatTags = ConcurrentHashMap<UUID, CombatTag>()

    /**
     * プレイヤーへの直接ダメージを記録
     * @param victim ダメージを受けたプレイヤー
     * @param attacker ダメージを与えたプレイヤー
     * @param damage ダメージ量
     * @param hasKnockback ノックバックが発生したか
     */
    fun tagPlayer(victim: Player, attacker: Player, damage: Double, hasKnockback: Boolean) {
        // 自己ダメージは追跡しない
        if (victim.uniqueId == attacker.uniqueId) return

        combatTags[victim.uniqueId] = CombatTag(
            victimUuid = victim.uniqueId,
            attackerUuid = attacker.uniqueId,
            damage = damage,
            hasKnockback = hasKnockback
        )
    }

    /**
     * 最後にダメージを与えたプレイヤーを検索
     * @param victimUuid 被害者のUUID
     * @return 攻撃者のUUID、見つからない場合はnull
     */
    fun findLastAttacker(victimUuid: UUID): UUID? {
        val tag = combatTags[victimUuid] ?: return null
        val now = Instant.now()

        return if (Duration.between(tag.taggedAt, now).seconds < tagDurationSeconds) {
            tag.attackerUuid
        } else {
            null
        }
    }

    /**
     * 最後の戦闘タグを取得
     * @param victimUuid 被害者のUUID
     * @return 戦闘タグ、見つからないか期限切れの場合はnull
     */
    fun getTag(victimUuid: UUID): CombatTag? {
        val tag = combatTags[victimUuid] ?: return null
        val now = Instant.now()

        return if (Duration.between(tag.taggedAt, now).seconds < tagDurationSeconds) {
            tag
        } else {
            null
        }
    }

    /**
     * 戦闘タグをクリア
     * @param victimUuid 被害者のUUID
     */
    fun clearTag(victimUuid: UUID) {
        combatTags.remove(victimUuid)
    }

    /**
     * 期限切れのタグを削除
     * 1秒ごとに呼び出されることを想定
     */
    fun cleanupExpiredTags() {
        val now = Instant.now()
        combatTags.entries.removeIf {
            Duration.between(it.value.taggedAt, now).seconds >= tagDurationSeconds
        }
    }

    /**
     * タグ保持期間（秒）
     */
    fun getTagDurationSeconds(): Long = tagDurationSeconds
}
