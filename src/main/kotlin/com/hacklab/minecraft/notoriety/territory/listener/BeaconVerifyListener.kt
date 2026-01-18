package com.hacklab.minecraft.notoriety.territory.listener

import com.hacklab.minecraft.notoriety.territory.service.TerritoryServiceImpl
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

/**
 * チャンクロード時にシギルビーコンを検証・修復するリスナー
 */
class BeaconVerifyListener(
    private val territoryService: TerritoryServiceImpl
) : Listener {

    /**
     * チャンクロード時にそのチャンク内のシギルビーコンを検証
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChunkLoad(event: ChunkLoadEvent) {
        // 新規生成チャンクはスキップ（シギルがあるはずがない）
        if (event.isNewChunk) return

        val chunk = event.chunk
        val worldName = chunk.world.name

        // このチャンク内のシギルビーコンを検証・修復
        territoryService.verifyBeaconsInChunk(worldName, chunk.x, chunk.z)
    }
}
