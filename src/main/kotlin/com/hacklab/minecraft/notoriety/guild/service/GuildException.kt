package com.hacklab.minecraft.notoriety.guild.service

import java.util.UUID

sealed class GuildException(message: String) : Exception(message) {
    // ギルド関連
    class NotFound(guildId: Long) : GuildException("Guild not found: $guildId")
    class NotFoundByName(name: String) : GuildException("Guild not found: $name")
    class NameTaken(name: String) : GuildException("Guild name already taken: $name")
    class TagTaken(tag: String) : GuildException("Guild tag already taken: $tag")
    class InvalidName(name: String) : GuildException("Invalid guild name: $name")
    class InvalidTag(tag: String) : GuildException("Invalid guild tag: $tag")
    class InvalidColor(color: String) : GuildException("Invalid tag color: $color")
    class GuildFull(guildId: Long) : GuildException("Guild is full: $guildId")

    // メンバーシップ関連
    class AlreadyInGuild(playerUuid: UUID) : GuildException("Player already in a guild: $playerUuid")
    class NotInGuild(playerUuid: UUID) : GuildException("Player not in any guild: $playerUuid")
    class NotMember(playerUuid: UUID, guildId: Long) : GuildException("Player $playerUuid is not a member of guild $guildId")
    class NotMaster(playerUuid: UUID) : GuildException("Player is not the guild master: $playerUuid")
    class NotMasterOrVice(playerUuid: UUID) : GuildException("Player is not master or vice master: $playerUuid")
    class MasterCannotLeave : GuildException("Master cannot leave while other members exist")
    class CannotKickSelf : GuildException("Cannot kick yourself")
    class NotViceMaster(playerUuid: UUID) : GuildException("Player is not a vice master: $playerUuid")
    class AlreadyViceMaster(playerUuid: UUID) : GuildException("Player is already a vice master: $playerUuid")
    class AlreadyMember(playerUuid: UUID) : GuildException("Player is already a regular member: $playerUuid")

    // 経済関連
    class InsufficientFunds(required: Double, current: Double) : GuildException("Insufficient funds: required $required, have $current")

    // 招待関連
    class AlreadyInvited(playerUuid: UUID, guildId: Long) : GuildException("Player $playerUuid already invited to guild $guildId")
    class InvitationNotFound(invitationId: Long) : GuildException("Invitation not found: $invitationId")
    class InvitationNotFoundByGuild(guildName: String) : GuildException("No invitation from guild: $guildName")
    class InvitationNotFoundByGuildId(guildId: Long) : GuildException("No invitation from guild: $guildId")
    class InvitationExpired(invitationId: Long) : GuildException("Invitation expired: $invitationId")
    class NotInvitee(playerUuid: UUID, invitationId: Long) : GuildException("Player $playerUuid is not the invitee of invitation $invitationId")
    class PlayerNotFound(playerName: String) : GuildException("Player not found: $playerName")

    // 入会申請関連
    class AlreadyApplied(playerUuid: UUID, guildId: Long) : GuildException("Player $playerUuid already applied to guild $guildId")
    class ApplicationNotFound(applicationId: Long) : GuildException("Application not found: $applicationId")
    class ApplicationNotFoundByGuild(guildId: Long) : GuildException("No application for guild: $guildId")
    class ApplicationExpired(applicationId: Long) : GuildException("Application expired: $applicationId")
}
