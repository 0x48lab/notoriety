package com.hacklab.minecraft.notoriety.guild.event

import com.hacklab.minecraft.notoriety.guild.model.Guild
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class GuildMemberLeaveEvent(
    val guild: Guild,
    val memberUuid: UUID,
    val reason: LeaveReason
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
