package com.hacklab.minecraft.notoriety.reputation

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

class TeamManager(private val plugin: JavaPlugin) {
    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    fun updatePlayerTeam(player: Player, color: NameColor, title: String?) {
        val teamName = getTeamName(player)
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)

        team.color(color.chatColor)

        if (title != null) {
            team.prefix(Component.text("[$title] ").color(color.prefixColor))
        } else {
            team.prefix(Component.empty())
        }

        if (!team.hasEntry(player.name)) {
            // Remove from other teams first
            scoreboard.teams.forEach { t ->
                if (t.hasEntry(player.name) && t.name != teamName) {
                    t.removeEntry(player.name)
                }
            }
            team.addEntry(player.name)
        }

        // Ensure player uses the main scoreboard
        if (player.scoreboard != scoreboard) {
            player.scoreboard = scoreboard
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
}
