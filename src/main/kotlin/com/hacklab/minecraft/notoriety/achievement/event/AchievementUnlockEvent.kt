package com.hacklab.minecraft.notoriety.achievement.event

import com.hacklab.minecraft.notoriety.achievement.model.Achievement
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

/**
 * アチーブメント達成時に発火されるイベント
 */
class AchievementUnlockEvent(
    val playerUuid: UUID,
    val achievement: Achievement,
    val fameReward: Int,
    val alignmentReward: Int
) : Event() {

    val player: Player? get() = Bukkit.getPlayer(playerUuid)

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}

/**
 * アチーブメント報酬データ
 */
data class AchievementRewards(
    val fame: Int = 0,
    val alignment: Int = 0
)
