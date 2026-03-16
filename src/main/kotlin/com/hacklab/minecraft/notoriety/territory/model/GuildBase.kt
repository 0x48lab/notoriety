package com.hacklab.minecraft.notoriety.territory.model

import java.time.Instant
import java.util.UUID

data class GuildBase(
    val id: Long = 0,
    val guildId: Long,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val setBy: UUID,
    val createdAt: Instant = Instant.now()
)
