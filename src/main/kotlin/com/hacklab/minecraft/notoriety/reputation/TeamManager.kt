package com.hacklab.minecraft.notoriety.reputation

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard

class TeamManager(private val plugin: JavaPlugin) {
    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    // テスト用: プレイヤーUUID -> ギルドタグ
    private val testGuildTags = mutableMapOf<java.util.UUID, String>()

    companion object {
        private const val BELOW_NAME_OBJECTIVE = "noty_title"
    }

    // テスト用: ギルドタグを設定
    fun setTestGuildTag(playerUuid: java.util.UUID, tag: String?) {
        if (tag == null) {
            testGuildTags.remove(playerUuid)
        } else {
            testGuildTags[playerUuid] = tag
        }
    }

    // テスト用: ギルドタグを取得
    fun getTestGuildTag(playerUuid: java.util.UUID): String? = testGuildTags[playerUuid]

    init {
        // 既存のBELOW_NAME Objectiveを削除（古い実装のクリーンアップ）
        scoreboard.getObjective(BELOW_NAME_OBJECTIVE)?.unregister()
    }

    fun updatePlayerTeam(player: Player, color: NameColor, title: String?) {
        val teamName = getTeamName(player)

        // チームの色を設定（名前の色）
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        team.color(color.chatColor)

        // 称号をprefixに表示（名前の前）
        if (title != null) {
            team.prefix(Component.text("$title ").color(color.prefixColor))
        } else {
            team.prefix(Component.empty())
        }

        // ギルドタグをsuffixに表示（名前の後ろ）
        val guildTag = testGuildTags[player.uniqueId]
        if (guildTag != null) {
            team.suffix(Component.text(" [$guildTag]").color(NamedTextColor.GOLD))
        } else {
            team.suffix(Component.empty())
        }

        if (!team.hasEntry(player.name)) {
            // 他のチームから削除
            scoreboard.teams.forEach { t ->
                if (t.hasEntry(player.name) && t.name != teamName) {
                    t.removeEntry(player.name)
                }
            }
            team.addEntry(player.name)
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

        // 古いBELOW_NAMEスコアをクリーンアップ
        scoreboard.resetScores(player.name)
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
