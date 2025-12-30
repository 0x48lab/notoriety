package com.hacklab.minecraft.notoriety.guild.model

import java.time.Instant
import java.util.UUID

data class GuildMembership(
    val id: Long,
    val guildId: Long,
    val playerUuid: UUID,
    val role: GuildRole,
    val joinedAt: Instant
)
