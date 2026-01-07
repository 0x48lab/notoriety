package com.hacklab.minecraft.notoriety.territory.event

import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.service.ReleaseReason
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 領地解放時に発火されるイベント
 */
class TerritoryReleaseEvent(
    val guildId: Long,
    val guildName: String,
    val releasedChunks: List<TerritoryChunk>,
    val reason: ReleaseReason,
    val remainingChunks: Int
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
