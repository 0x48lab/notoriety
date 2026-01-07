package com.hacklab.minecraft.notoriety.territory.event

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

/**
 * 領地確保時に発火されるイベント
 */
class TerritoryClaimEvent(
    val guildId: Long,
    val guildName: String,
    val claimedBy: UUID,
    val chunk: TerritoryChunk,
    val totalChunks: Int
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
