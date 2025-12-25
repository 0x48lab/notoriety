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
        teamManager.updateSidebar(player, data, title)
    }

    fun onGoodDeed(playerUuid: UUID, karmaGain: Int, fameGain: Int, crimeReduction: Int) {
        val data = playerManager.getPlayer(playerUuid) ?: return
        val oldColor = data.getNameColor()

        // 青プレイヤーのみKarmaが増加
        if (oldColor == NameColor.BLUE) {
            data.addKarma(karmaGain)
        }
        data.addFame(fameGain)
        data.addCrimePoint(-crimeReduction)

        val newColor = data.getNameColor()
        if (oldColor != newColor) {
            data.resetKarma()
            Bukkit.getPluginManager().callEvent(
                PlayerColorChangeEvent(playerUuid, oldColor, newColor)
            )
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            PlayerGoodDeedEvent(playerUuid, karmaGain, fameGain, crimeReduction)
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
                killerData.resetKarma()

                // 赤になったらイベント発火
                val newColor = killerData.getNameColor()
                if (oldColor != newColor) {
                    Bukkit.getPluginManager().callEvent(
                        PlayerColorChangeEvent(killerUuid, oldColor, newColor)
                    )
                }

                // 赤なら悪名も増加
                if (killerData.getNameColor() == NameColor.RED) {
                    killerData.addKarma(50)
                }
            }
            NameColor.RED -> {
                // 賞金稼ぎ報酬（青プレイヤーのみ）
                if (oldColor == NameColor.BLUE) {
                    killerData.addKarma(50)
                }
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
