package com.hacklab.minecraft.notoriety.territory.event

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 領地に入った時に発火されるイベント
 */
class TerritoryEnterEvent(
    val player: Player,
    val territory: GuildTerritory,
    val guildName: String
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
