package com.hacklab.minecraft.notoriety.guild.event

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class GuildRoleChangeEvent(
    val guild: Guild,
    val memberUuid: UUID,
    val oldRole: GuildRole,
    val newRole: GuildRole
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
