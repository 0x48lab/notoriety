package com.hacklab.minecraft.notoriety.bounty

import java.util.*

data class BountyEntry(
    val target: UUID,
    val total: Double,
    val contributors: Map<UUID, Double>
)
