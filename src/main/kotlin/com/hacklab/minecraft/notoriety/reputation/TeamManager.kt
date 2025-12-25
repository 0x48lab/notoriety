package com.hacklab.minecraft.notoriety.reputation

import com.hacklab.minecraft.notoriety.core.player.PlayerData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard

class TeamManager(private val plugin: JavaPlugin) {
    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    fun updatePlayerTeam(player: Player, color: NameColor, title: String?) {
        val teamName = getTeamName(player)

        // メインスコアボードを更新
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        team.color(color.chatColor)
        if (title != null) {
            team.prefix(Component.text("[$title] ").color(color.prefixColor))
        } else {
            team.prefix(Component.empty())
        }

        if (!team.hasEntry(player.name)) {
            scoreboard.teams.forEach { t ->
                if (t.hasEntry(player.name) && t.name != teamName) {
                    t.removeEntry(player.name)
                }
            }
            team.addEntry(player.name)
        }

        // プレイヤーの現在のスコアボードにもチームを反映
        val playerBoard = player.scoreboard
        if (playerBoard != scoreboard) {
            val playerTeam = playerBoard.getTeam(teamName) ?: playerBoard.registerNewTeam(teamName)
            playerTeam.color(color.chatColor)
            if (title != null) {
                playerTeam.prefix(Component.text("[$title] ").color(color.prefixColor))
            } else {
                playerTeam.prefix(Component.empty())
            }
            if (!playerTeam.hasEntry(player.name)) {
                playerBoard.teams.forEach { t ->
                    if (t.hasEntry(player.name) && t.name != teamName) {
                        t.removeEntry(player.name)
                    }
                }
                playerTeam.addEntry(player.name)
            }
        }
    }

    fun removePlayerTeam(player: Player) {
        val teamName = getTeamName(player)
        scoreboard.getTeam(teamName)?.let { team ->
            team.removeEntry(player.name)
            if (team.entries.isEmpty()) {
                team.unregister()
            }
        }
    }

    private fun getTeamName(player: Player): String {
        return "noty_${player.uniqueId.toString().take(12).replace("-", "")}"
    }

    fun cleanupEmptyTeams() {
        scoreboard.teams
            .filter { it.name.startsWith("noty_") && it.entries.isEmpty() }
            .forEach { it.unregister() }
    }

    companion object {
        // Notorietyが使用するスコア範囲（2行固定: 100, 99）
        const val SIDEBAR_SCORE_START = 100
        const val SIDEBAR_LINE_COUNT = 2
    }

    fun updateSidebar(player: Player, data: PlayerData, title: String?, locale: String = "ja") {
        // プレイヤーの現在のスコアボードを使用（他プラグインとの競合を避ける）
        val playerBoard = player.scoreboard

        // 既存のNotorietyスコアをクリア
        playerBoard.entries
            .filter { it.startsWith("§n") }  // Notoriety用のプレフィックス
            .forEach { playerBoard.resetScores(it) }

        // Objectiveを取得または作成
        val objective = playerBoard.getObjective("skills_stats")
            ?: playerBoard.getObjective(DisplaySlot.SIDEBAR)
            ?: playerBoard.registerNewObjective(
                "notoriety_sidebar",
                Criteria.DUMMY,
                Component.text("§6§lStatus")
            ).also { it.displaySlot = DisplaySlot.SIDEBAR }

        val color = data.getNameColor()
        val statusText = when (color) {
            NameColor.BLUE -> "§9Innocent"
            NameColor.GRAY -> "§7Criminal"
            NameColor.RED -> "§cMurderer"
        }

        val titleText = title ?: "-"

        // 2行固定（スコア100, 99）
        objective.getScore("§nStatus: $statusText").score = SIDEBAR_SCORE_START
        objective.getScore("§nTitle: §f$titleText").score = SIDEBAR_SCORE_START - 1
    }

    fun removeSidebar(player: Player) {
        // Notorietyのスコアのみクリア（他プラグインのObjectiveは残す）
        player.scoreboard.entries
            .filter { it.startsWith("§n") }
            .forEach { player.scoreboard.resetScores(it) }
    }
}
