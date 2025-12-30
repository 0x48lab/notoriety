package com.hacklab.minecraft.notoriety.guild.model

enum class GuildRole(val level: Int, val displayKey: String) {
    MASTER(3, "role.master"),
    VICE_MASTER(2, "role.vice_master"),
    MEMBER(1, "role.member");

    fun hasPermission(required: GuildRole): Boolean = this.level >= required.level

    fun canInvite(): Boolean = this.level >= VICE_MASTER.level
    fun canKick(): Boolean = this == MASTER
    fun canPromote(): Boolean = this == MASTER
    fun canDemote(): Boolean = this == MASTER
    fun canDissolve(): Boolean = this == MASTER
    fun canTransfer(): Boolean = this == MASTER
    fun canChangeTagColor(): Boolean = this == MASTER

    companion object {
        fun fromString(value: String): GuildRole? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
