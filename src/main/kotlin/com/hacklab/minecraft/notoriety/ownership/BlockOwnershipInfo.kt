package com.hacklab.minecraft.notoriety.ownership

import java.time.Instant
import java.util.*

data class BlockOwnershipInfo(
    val ownerUuid: UUID,
    val placedAt: Instant?
)
