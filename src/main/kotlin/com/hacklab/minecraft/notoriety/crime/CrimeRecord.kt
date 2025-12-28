package com.hacklab.minecraft.notoriety.crime

import java.time.Instant
import java.util.*

data class CrimeRecord(
    val id: Long = 0,
    val criminalUuid: UUID,
    val crimeType: CrimeType,
    val victimUuid: UUID? = null,
    val victimName: String? = null,
    val world: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val z: Int? = null,
    val detail: String? = null,
    val alignmentPenalty: Int,
    val committedAt: Instant = Instant.now()
)
