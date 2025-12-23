package com.hacklab.minecraft.notoriety.core.player

import com.hacklab.minecraft.notoriety.reputation.NameColor
import java.time.Instant
import java.util.*

data class PlayerData(
    val uuid: UUID,
    var crimePoint: Int = 0,
    var pkCount: Int = 0,
    var karma: Int = 0,
    var fame: Int = 0,
    var playTimeMinutes: Long = 0,
    var lastSeen: Instant = Instant.now()
) {
    fun getNameColor(): NameColor = when {
        pkCount >= 1 -> NameColor.RED
        crimePoint >= 1 -> NameColor.GRAY
        else -> NameColor.BLUE
    }

    fun addCrimePoint(amount: Int) {
        crimePoint = (crimePoint + amount).coerceIn(0, 1000)
    }

    fun addKarma(amount: Int) {
        karma = (karma + amount).coerceIn(0, 1000)
    }

    fun addFame(amount: Int) {
        fame = (fame + amount).coerceIn(0, 1000)
    }

    fun resetKarma() {
        karma = 0
    }
}
