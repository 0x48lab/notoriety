package com.hacklab.minecraft.notoriety.ownership

import com.hacklab.minecraft.notoriety.core.BlockLocation
import org.bukkit.Material
import java.time.Instant
import java.util.*

data class PendingCrime(
    val location: BlockLocation,
    val blockType: Material,
    val playerUuid: UUID,
    val ownerUuid: UUID,
    val brokenAt: Instant,
    val crimePoint: Int
)
