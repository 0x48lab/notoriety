package com.hacklab.minecraft.notoriety.guild.event

import com.hacklab.minecraft.notoriety.guild.model.Guild
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

/**
 * ギルドへの入会申請が行われた時に発火するイベント
 */
class GuildApplicationEvent(
    val guild: Guild,
    val applicantUuid: UUID,
    val message: String?
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
