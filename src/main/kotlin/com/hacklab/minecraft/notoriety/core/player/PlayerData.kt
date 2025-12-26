package com.hacklab.minecraft.notoriety.core.player

import com.hacklab.minecraft.notoriety.reputation.NameColor
import java.time.Instant
import java.util.*

data class PlayerData(
    val uuid: UUID,
    var alignment: Int = 0,      // -1000〜+1000（善悪軸）
    var pkCount: Int = 0,        // 殺人回数
    var fame: Int = 0,           // 名声
    var playTimeMinutes: Long = 0,
    var lastSeen: Instant = Instant.now()
) {
    fun getNameColor(): NameColor = when {
        pkCount >= 1 -> NameColor.RED
        alignment < 0 -> NameColor.GRAY
        else -> NameColor.BLUE
    }

    /**
     * Alignmentを増減する
     * 善行で+、悪行で-
     */
    fun addAlignment(amount: Int) {
        alignment = (alignment + amount).coerceIn(-1000, 1000)
    }

    /**
     * 時間経過による回復処理
     * Alignmentは0に向かって回復する
     * 赤プレイヤーはAlignmentが0以上になるとPKCount -1、Alignment -1000にリセット
     * @return 状態が変化したかどうか
     */
    fun decayAlignment(amount: Int): Boolean {
        if (pkCount >= 1) {
            // 赤プレイヤーの場合: 0に向かって回復
            if (alignment < 0) {
                alignment = (alignment + amount).coerceAtMost(0)
            }
            if (alignment >= 0) {
                pkCount--
                alignment = -1000  // 常に-1000にリセット（PKCount=0でも灰色期間が必要）
                return true // 状態変化あり
            }
        } else if (alignment < 0) {
            // 灰プレイヤーの場合: 0に向かって回復
            val oldAlignment = alignment
            alignment = (alignment + amount).coerceAtMost(0)
            if (oldAlignment < 0 && alignment >= 0) {
                return true // 灰→青の変化
            }
        }
        // 青プレイヤーはalignmentが正の場合も0に向かって減少
        else if (alignment > 0) {
            alignment = (alignment - amount).coerceAtLeast(0)
        }
        return false
    }

    fun addFame(amount: Int) {
        fame = (fame + amount).coerceIn(0, 1000)
    }

    fun resetAlignment() {
        alignment = 0
    }
}
