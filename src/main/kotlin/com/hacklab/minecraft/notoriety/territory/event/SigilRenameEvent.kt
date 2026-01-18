package com.hacklab.minecraft.notoriety.territory.event

import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

/**
 * シギル名前変更時に発火されるイベント
 */
class SigilRenameEvent(
    val guildId: Long,
    val guildName: String,
    val sigil: TerritorySigil,
    val oldName: String,
    val newName: String,
    val renamedBy: UUID
) : Event() {

    companion object {
        private val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers() = HANDLER_LIST
}
