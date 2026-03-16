package com.hacklab.minecraft.notoriety.zone.model

import java.time.Instant
import java.util.UUID

data class ProtectedZone(
    val id: Long = 0,
    val name: String,
    val worldName: String,
    val x1: Int, val y1: Int, val z1: Int,
    val x2: Int, val y2: Int, val z2: Int,
    val creatorUuid: UUID,
    val createdAt: Instant = Instant.now()
) {
    fun contains(world: String, x: Int, y: Int, z: Int): Boolean {
        return world == worldName &&
            x in x1..x2 &&
            y in y1..y2 &&
            z in z1..z2
    }
}
