package com.hacklab.minecraft.notoriety.guild.display

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.reputation.TeamManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.UUID

class GuildTagManager(private val teamManager: TeamManager) {

    fun setGuildTag(player: Player, guild: Guild) {
        val tagText = "[${guild.tag}] "
        teamManager.setTestGuildTag(player.uniqueId, guild.tag)
    }

    fun removeGuildTag(player: Player) {
        teamManager.setTestGuildTag(player.uniqueId, null)
    }

    fun getGuildTag(playerUuid: UUID): String? {
        return teamManager.getTestGuildTag(playerUuid)
    }
}
