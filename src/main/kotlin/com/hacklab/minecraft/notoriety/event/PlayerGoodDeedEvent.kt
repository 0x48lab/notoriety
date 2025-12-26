package com.hacklab.minecraft.notoriety.event

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

class PlayerGoodDeedEvent(
    val playerUuid: UUID,
    val alignmentGain: Int,
    val fameGain: Int
) : Event() {
    val player: Player? get() = Bukkit.getPlayer(playerUuid)

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLER_LIST
    }
    override fun getHandlers() = HANDLER_LIST
}
