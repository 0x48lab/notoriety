package com.hacklab.minecraft.notoriety.guild.event

import com.hacklab.minecraft.notoriety.guild.model.Guild
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class GuildInviteEvent(
    val guild: Guild,
    val inviter: Player,
    val inviteeUuid: UUID
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
