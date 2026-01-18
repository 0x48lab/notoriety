package com.hacklab.minecraft.notoriety.territory.event

import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * シギルへのテレポート時に発火されるイベント
 */
class SigilTeleportEvent(
    val player: Player,
    val sigil: TerritorySigil,
    val guildId: Long
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
