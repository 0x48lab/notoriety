package com.hacklab.minecraft.notoriety.core

import org.bukkit.Bukkit
import org.bukkit.Location

data class BlockLocation(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int
)

fun Location.toBlockLoc(): BlockLocation {
    return BlockLocation(
        world = this.world.name,
        x = this.blockX,
        y = this.blockY,
        z = this.blockZ
    )
}

fun BlockLocation.toLocation(): Location? {
    val world = Bukkit.getWorld(this.world) ?: return null
    return Location(world, this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}
