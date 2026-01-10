package com.hacklab.minecraft.notoriety.achievement.service

import com.hacklab.minecraft.notoriety.core.player.PlayerData
import com.hacklab.minecraft.notoriety.reputation.NameColor

/**
 * アチーブメント達成条件の判定インターフェース
 */
sealed interface AchievementCondition {
    /**
     * 条件を満たしているかチェック
     * @param playerData プレイヤーデータ
     * @param context 判定コンテキスト
     * @return 条件を満たしていればtrue
     */
    fun check(playerData: PlayerData, context: AchievementContext): Boolean

    /**
     * 進捗を取得（カウント系の条件用）
     * @return 現在値と目標値のペア（進捗表示不要な条件はnull）
     */
    fun getProgress(playerData: PlayerData): Pair<Int, Int>? = null
}

/**
 * Fame到達条件
 */
data class FameThresholdCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.fame >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.fame to threshold
    }
}

/**
 * Alignment到達条件
 */
data class AlignmentThresholdCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.alignment >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.alignment to threshold
    }
}

/**
 * PKCount到達条件
 */
data class PKCountThresholdCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.pkCount >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.pkCount to threshold
    }
}

/**
 * 赤プレイヤー討伐回数条件
 */
data class RedKillsCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.redKills >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.redKills to threshold
    }
}

/**
 * 村人取引回数条件
 */
data class TradeCountCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.tradeCount >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.tradeCount to threshold
    }
}

/**
 * ゴーレム討伐回数条件
 */
data class GolemKillsCondition(val threshold: Int) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.golemKills >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.golemKills to threshold
    }
}

/**
 * 懸賞金累計獲得額条件
 */
data class TotalBountyEarnedCondition(val threshold: Long) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return playerData.totalBountyEarned >= threshold
    }

    override fun getProgress(playerData: PlayerData): Pair<Int, Int> {
        return playerData.totalBountyEarned.toInt().coerceAtMost(Int.MAX_VALUE) to threshold.toInt().coerceAtMost(Int.MAX_VALUE)
    }
}

/**
 * 初回イベント条件（例: 初めて赤プレイヤーを討伐）
 */
data class FirstTimeCondition(val eventType: String) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return context.triggeredEvent == eventType && context.isFirstTime
    }
}

/**
 * 状態遷移条件（例: 赤→灰への遷移）
 */
data class ColorTransitionCondition(
    val from: NameColor,
    val to: NameColor
) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return context.previousColor == from && playerData.getNameColor() == to
    }
}

/**
 * ギルド加入条件
 */
data object GuildJoinCondition : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return context.triggeredEvent == "GUILD_JOIN"
    }
}

/**
 * ギルド創設条件
 */
data object GuildCreateCondition : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return context.triggeredEvent == "GUILD_CREATE"
    }
}

/**
 * 複合条件（AND）
 */
data class AndCondition(val conditions: List<AchievementCondition>) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return conditions.all { it.check(playerData, context) }
    }
}

/**
 * 複合条件（OR）
 */
data class OrCondition(val conditions: List<AchievementCondition>) : AchievementCondition {
    override fun check(playerData: PlayerData, context: AchievementContext): Boolean {
        return conditions.any { it.check(playerData, context) }
    }
}
