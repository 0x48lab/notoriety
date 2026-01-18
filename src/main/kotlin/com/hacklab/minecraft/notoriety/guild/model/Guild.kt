package com.hacklab.minecraft.notoriety.guild.model

import java.time.Instant
import java.util.UUID

data class Guild(
    val id: Long,
    val name: String,
    val tag: String,
    val tagColor: TagColor = TagColor.WHITE,
    val description: String?,
    val masterUuid: UUID,
    val createdAt: Instant,
    val maxMembers: Int = 50,
    val isGovernment: Boolean = false
) {
    companion object {
        private val NAME_REGEX = Regex("^[a-zA-Z0-9_\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー]+$")
        private val TAG_REGEX = Regex("^[a-zA-Z0-9]+$")
    }

    fun isValidName(): Boolean =
        name.length in 2..32 && name.matches(NAME_REGEX)

    fun isValidTag(): Boolean =
        tag.length in 1..4 && tag.matches(TAG_REGEX)
}
