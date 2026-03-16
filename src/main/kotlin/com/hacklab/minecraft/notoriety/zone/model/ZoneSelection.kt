package com.hacklab.minecraft.notoriety.zone.model

data class BlockPos(val world: String, val x: Int, val y: Int, val z: Int)

data class ZoneSelection(
    var pos1: BlockPos? = null,
    var pos2: BlockPos? = null
)
