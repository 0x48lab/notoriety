package com.hacklab.minecraft.notoriety.guild.service

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.GuildApplication
import com.hacklab.minecraft.notoriety.guild.model.GuildInvitation
import com.hacklab.minecraft.notoriety.guild.model.GuildMembership
import com.hacklab.minecraft.notoriety.guild.model.TagColor
import com.hacklab.minecraft.notoriety.trust.TrustState
import java.util.UUID

interface GuildService {
    // === ギルド管理 ===

    fun createGuild(creator: UUID, name: String, tag: String, description: String? = null): Guild

    /**
     * 政府ギルドを作成する（OP専用）
     * 政府ギルドは領地上限がなく、特別な権限を持つ
     * @param creator 作成者UUID
     * @param name ギルド名
     * @param tag ギルドタグ
     * @param description 説明
     * @return 作成された政府ギルド
     */
    fun createGovernmentGuild(creator: UUID, name: String, tag: String, description: String? = null): Guild
    fun setTagColor(guildId: Long, color: TagColor, requester: UUID)
    fun setName(guildId: Long, name: String, requester: UUID)
    fun setTag(guildId: Long, tag: String, requester: UUID)
    fun setDescription(guildId: Long, description: String?, requester: UUID)
    fun dissolveGuild(guildId: Long, requester: UUID)
    fun getGuild(guildId: Long): Guild?
    fun getGuildByName(name: String): Guild?
    fun getPlayerGuild(playerUuid: UUID): Guild?
    fun getAllGuilds(page: Int = 0, pageSize: Int = 10): List<Guild>
    fun getGuildCount(): Int

    // === メンバー管理 ===

    fun getMembers(guildId: Long, page: Int = 0, pageSize: Int = 45): List<GuildMembership>
    fun getMemberCount(guildId: Long): Int
    fun getMembership(playerUuid: UUID): GuildMembership?
    fun kickMember(guildId: Long, targetUuid: UUID, requester: UUID)
    fun leaveGuild(playerUuid: UUID)
    fun promoteToViceMaster(guildId: Long, targetUuid: UUID, requester: UUID)
    fun demoteToMember(guildId: Long, targetUuid: UUID, requester: UUID)
    fun transferMaster(guildId: Long, newMasterUuid: UUID, currentMaster: UUID)

    // === 招待管理 ===

    fun invitePlayer(guildId: Long, inviteeUuid: UUID, inviter: UUID): GuildInvitation
    fun acceptInvitation(invitationId: Long, playerUuid: UUID)
    fun acceptInvitationByGuildId(guildId: Long, playerUuid: UUID)
    fun acceptInvitationByGuildName(guildName: String, playerUuid: UUID)
    fun declineInvitation(invitationId: Long, playerUuid: UUID)
    fun declineInvitationByGuildId(guildId: Long, playerUuid: UUID)
    fun declineInvitationByGuildName(guildName: String, playerUuid: UUID)
    fun cancelInvitation(guildId: Long, inviter: UUID, inviteeUuid: UUID)
    fun getPendingInvitations(playerUuid: UUID): List<GuildInvitation>
    fun getSentInvitations(guildId: Long): List<GuildInvitation>
    fun hasInvitation(guildId: Long, playerUuid: UUID): Boolean
    fun cleanupExpiredInvitations()

    // === 入会申請管理 ===

    fun applyToGuild(guildId: Long, applicantUuid: UUID, message: String? = null): GuildApplication
    fun approveApplication(applicationId: Long, approverUuid: UUID)
    fun rejectApplication(applicationId: Long, rejectorUuid: UUID)
    fun cancelApplication(guildId: Long, applicantUuid: UUID)
    fun getPendingApplications(guildId: Long): List<GuildApplication>
    fun getPlayerApplications(playerUuid: UUID): List<GuildApplication>
    fun hasApplication(guildId: Long, playerUuid: UUID): Boolean
    fun cleanupExpiredApplications()

    // === 信頼システム統合 ===

    fun areInSameGuild(player1: UUID, player2: UUID): Boolean
    fun isAccessAllowed(owner: UUID, accessor: UUID): Boolean
    fun canTakeFromContainer(owner: UUID, accessor: UUID): Boolean
    fun setTrustState(truster: UUID, trusted: UUID, state: TrustState)
    fun removeTrustState(truster: UUID, trusted: UUID)
    fun getTrustState(truster: UUID, trusted: UUID): TrustState?
}
