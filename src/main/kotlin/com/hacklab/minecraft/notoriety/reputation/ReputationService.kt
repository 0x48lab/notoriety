package com.hacklab.minecraft.notoriety.reputation

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.event.PlayerGoodDeedEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class ReputationService(
    private val playerManager: PlayerManager,
    val teamManager: TeamManager
) {
    fun updateDisplay(player: Player) {
        val data = playerManager.getPlayer(player) ?: return
        val color = data.getNameColor()
        val title = TitleResolver.getTitle(data)
        teamManager.updatePlayerTeam(player, color, title)
    }

    fun onGoodDeed(playerUuid: UUID, alignmentGain: Int, fameGain: Int) {
        val data = playerManager.getPlayer(playerUuid) ?: return
        val oldColor = data.getNameColor()

        // Alignment増加（善行）
        data.addAlignment(alignmentGain)
        data.addFame(fameGain)

        val newColor = data.getNameColor()
        if (oldColor != newColor) {
            Bukkit.getPluginManager().callEvent(
                PlayerColorChangeEvent(playerUuid, oldColor, newColor)
            )
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            PlayerGoodDeedEvent(playerUuid, alignmentGain, fameGain)
        )

        // 表示を更新
        Bukkit.getPlayer(playerUuid)?.let { updateDisplay(it) }
    }

    fun onPlayerKill(killerUuid: UUID, victimUuid: UUID) {
        val killerData = playerManager.getPlayer(killerUuid) ?: return
        val victimData = playerManager.getPlayer(victimUuid) ?: return
        val oldColor = killerData.getNameColor()

        when (victimData.getNameColor()) {
            NameColor.BLUE -> {
                // PKカウント増加
                killerData.pkCount++
                // 赤になるとAlignment -1000からスタート
                killerData.alignment = -1000

                // 赤になったらイベント発火
                val newColor = killerData.getNameColor()
                if (oldColor != newColor) {
                    Bukkit.getPluginManager().callEvent(
                        PlayerColorChangeEvent(killerUuid, oldColor, newColor)
                    )
                }
            }
            NameColor.RED -> {
                // 賞金稼ぎ報酬: Alignment増加
                killerData.addAlignment(50)
                killerData.addFame(50)

                // Fame継承
                if (victimData.fame > killerData.fame) {
                    killerData.fame = victimData.fame
                }
            }
            NameColor.GRAY -> {
                // ペナルティなし
            }
        }

        // 表示を更新
        Bukkit.getPlayer(killerUuid)?.let { updateDisplay(it) }
    }
}
