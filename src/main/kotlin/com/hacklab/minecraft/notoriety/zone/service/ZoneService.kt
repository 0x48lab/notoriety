package com.hacklab.minecraft.notoriety.zone.service

import com.hacklab.minecraft.notoriety.zone.model.ProtectedZone
import org.bukkit.Location
import java.util.UUID

enum class ZoneCreateResult {
    SUCCESS,
    DUPLICATE_NAME,
    INVALID_NAME,
    INVALID_WORLD
}

interface ZoneService {
    fun loadAll()
    fun createZone(name: String, worldName: String, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, creatorUuid: UUID): ZoneCreateResult
    fun removeZone(name: String): Boolean
    fun getAllZones(): List<ProtectedZone>
    fun getZoneByName(name: String): ProtectedZone?
    fun isProtected(worldName: String, x: Int, y: Int, z: Int): Boolean
    fun getZoneAt(worldName: String, x: Int, y: Int, z: Int): ProtectedZone?
    fun isProtected(location: Location): Boolean
    fun getZoneAt(location: Location): ProtectedZone?
}
