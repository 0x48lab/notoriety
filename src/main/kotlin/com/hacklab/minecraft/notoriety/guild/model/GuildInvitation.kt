package com.hacklab.minecraft.notoriety.guild.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class GuildInvitation(
    val id: Long,
    val guildId: Long,
    val inviterUuid: UUID,
    val inviteeUuid: UUID,
    val invitedAt: Instant,
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
