package com.hacklab.minecraft.notoriety.zone.service

import com.hacklab.minecraft.notoriety.zone.model.ProtectedZone
import com.hacklab.minecraft.notoriety.zone.repository.ProtectedZoneRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class ZoneServiceImpl(
    private val repository: ProtectedZoneRepository
) : ZoneService {

    private val zones = CopyOnWriteArrayList<ProtectedZone>()

    override fun loadAll() {
        zones.clear()
        zones.addAll(repository.findAll())
    }

    override fun createZone(
        name: String,
        worldName: String,
        x1: Int, y1: Int, z1: Int,
        x2: Int, y2: Int, z2: Int,
        creatorUuid: UUID
    ): ZoneCreateResult {
        if (name.isBlank() || name.length > 32) return ZoneCreateResult.INVALID_NAME
        if (zones.any { it.name.equals(name, ignoreCase = true) }) return ZoneCreateResult.DUPLICATE_NAME
        if (Bukkit.getWorld(worldName) == null) return ZoneCreateResult.INVALID_WORLD

        val zone = ProtectedZone(
            name = name,
            worldName = worldName,
            x1 = minOf(x1, x2),
            y1 = minOf(y1, y2),
            z1 = minOf(z1, z2),
            x2 = maxOf(x1, x2),
            y2 = maxOf(y1, y2),
            z2 = maxOf(z1, z2),
            creatorUuid = creatorUuid
        )

        val id = repository.insert(zone)
        zones.add(zone.copy(id = id))
        return ZoneCreateResult.SUCCESS
    }

    override fun removeZone(name: String): Boolean {
        val removed = repository.deleteByName(name)
        if (removed) {
            zones.removeAll { it.name.equals(name, ignoreCase = true) }
        }
        return removed
    }

    override fun getAllZones(): List<ProtectedZone> = zones.toList()

    override fun getZoneByName(name: String): ProtectedZone? {
        return zones.find { it.name.equals(name, ignoreCase = true) }
    }

    override fun isProtected(worldName: String, x: Int, y: Int, z: Int): Boolean {
        return zones.any { it.contains(worldName, x, y, z) }
    }

    override fun getZoneAt(worldName: String, x: Int, y: Int, z: Int): ProtectedZone? {
        return zones.find { it.contains(worldName, x, y, z) }
    }

    override fun isProtected(location: Location): Boolean {
        return isProtected(location.world.name, location.blockX, location.blockY, location.blockZ)
    }

    override fun getZoneAt(location: Location): ProtectedZone? {
        return getZoneAt(location.world.name, location.blockX, location.blockY, location.blockZ)
    }
}
