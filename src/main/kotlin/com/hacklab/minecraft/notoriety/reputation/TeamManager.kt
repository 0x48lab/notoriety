package com.hacklab.minecraft.notoriety.reputation

import io.papermc.paper.scoreboard.numbers.NumberFormat
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard

class TeamManager(private val plugin: JavaPlugin) {
    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    companion object {
        private const val BELOW_NAME_OBJECTIVE = "noty_title"
    }

    init {
        // BELOW_NAME用のObjectiveを作成
        if (scoreboard.getObjective(BELOW_NAME_OBJECTIVE) == null) {
            val objective = scoreboard.registerNewObjective(
                BELOW_NAME_OBJECTIVE,
                Criteria.DUMMY,
                Component.empty()
            )
            objective.displaySlot = DisplaySlot.BELOW_NAME
        }
    }

    fun updatePlayerTeam(player: Player, color: NameColor, title: String?) {
        val teamName = getTeamName(player)

        // チームの色を設定（名前の色）
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        team.color(color.chatColor)
        // プレフィックスは使わない（BELOW_NAMEに称号を表示するため）
        team.prefix(Component.empty())

        if (!team.hasEntry(player.name)) {
            // 他のチームから削除
            scoreboard.teams.forEach { t ->
                if (t.hasEntry(player.name) && t.name != teamName) {
                    t.removeEntry(player.name)
                }
            }
            team.addEntry(player.name)
        }

        // BELOW_NAMEに称号を表示
        updateBelowName(player, color, title)
    }

    private fun updateBelowName(player: Player, color: NameColor, title: String?) {
        val objective = scoreboard.getObjective(BELOW_NAME_OBJECTIVE) ?: return
        val score = objective.getScore(player.name)
        score.score = 0

        if (title != null) {
            // 称号をカスタムフォーマットで表示
            score.numberFormat(NumberFormat.fixed(
                Component.text(title).color(color.prefixColor)
            ))
        } else {
            // 称号がない場合は空白
            score.numberFormat(NumberFormat.blank())
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

        // BELOW_NAMEのスコアをリセット
        scoreboard.getObjective(BELOW_NAME_OBJECTIVE)?.let { objective ->
            scoreboard.resetScores(player.name)
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
}
