package com.hacklab.minecraft.notoriety.event

import com.hacklab.minecraft.notoriety.crime.CrimeType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

class PlayerCrimeEvent(
    val criminalUuid: UUID,
    val crimeType: CrimeType,
    val alignmentPenalty: Int
) : Event() {
    val player: Player? get() = Bukkit.getPlayer(criminalUuid)

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLER_LIST
    }
    override fun getHandlers() = HANDLER_LIST
}
