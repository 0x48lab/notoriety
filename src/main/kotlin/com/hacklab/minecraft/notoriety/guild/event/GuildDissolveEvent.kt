package com.hacklab.minecraft.notoriety.guild.event

import com.hacklab.minecraft.notoriety.guild.model.Guild
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class GuildDissolveEvent(
    val guild: Guild,
    val dissolverUuid: UUID,
    val formerMembers: List<UUID>
) : Event() {

    val dissolver: Player?
        get() = Bukkit.getPlayer(dissolverUuid)

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
