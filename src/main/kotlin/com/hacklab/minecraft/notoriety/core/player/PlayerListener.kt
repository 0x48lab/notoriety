package com.hacklab.minecraft.notoriety.core.player

import com.hacklab.minecraft.notoriety.NotorietyService
import com.hacklab.minecraft.notoriety.reputation.TeamManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Instant

class PlayerListener(
    private val playerManager: PlayerManager,
    private val notorietyService: NotorietyService,
    private val teamManager: TeamManager
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // プレイヤーデータをロード
        val data = playerManager.loadPlayer(player.uniqueId)
        data.lastSeen = Instant.now()

        // 表示を更新
        notorietyService.updateDisplay(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // プレイヤーデータを保存してアンロード
        playerManager.unloadPlayer(player.uniqueId)

        // Teamから削除
        teamManager.removePlayerTeam(player)
    }
}
