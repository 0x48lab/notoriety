package com.hacklab.minecraft.notoriety.event

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

class BountyClaimedEvent(
    val killerUuid: UUID,
    val targetUuid: UUID,
    val bountyAmount: Double,
    val expReward: Int
) : Event() {
    val killer: Player? get() = Bukkit.getPlayer(killerUuid)
    val target: Player? get() = Bukkit.getPlayer(targetUuid)

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLER_LIST
    }
    override fun getHandlers() = HANDLER_LIST
}
