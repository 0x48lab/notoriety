package com.hacklab.minecraft.notoriety.guild.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * ギルド入会申請データクラス
 */
data class GuildApplication(
    val id: Long,
    val guildId: Long,
    val applicantUuid: UUID,
    val message: String?,
    val appliedAt: Instant,
    val expiresAt: Instant
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun remainingDays(): Long {
        val remaining = Duration.between(Instant.now(), expiresAt)
        return remaining.toDays().coerceAtLeast(0)
    }

    companion object {
        val DEFAULT_DURATION: Duration = Duration.ofDays(7)
    }
}
