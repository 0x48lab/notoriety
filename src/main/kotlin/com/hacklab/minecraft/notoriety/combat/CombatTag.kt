package com.hacklab.minecraft.notoriety.combat

import java.time.Instant
import java.util.UUID

/**
 * 戦闘タグ
 * プレイヤーへの直接ダメージ履歴を記録
 * 落下ダメージなどの間接PKで原因プレイヤーを特定するために使用
 */
data class CombatTag(
    /** ダメージを受けたプレイヤーのUUID */
    val victimUuid: UUID,

    /** ダメージを与えたプレイヤーのUUID */
    val attackerUuid: UUID,

    /** ダメージ量 */
    val damage: Double,

    /** タグ付け時刻 */
    val taggedAt: Instant = Instant.now(),

    /** ノックバックが発生したか */
    val hasKnockback: Boolean = false
)
