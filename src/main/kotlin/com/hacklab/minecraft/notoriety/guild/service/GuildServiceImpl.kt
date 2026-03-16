package com.hacklab.minecraft.notoriety.guild.service

import com.hacklab.minecraft.notoriety.guild.cache.GuildCache
import com.hacklab.minecraft.notoriety.guild.display.GuildTagManager
import com.hacklab.minecraft.notoriety.guild.event.*
import com.hacklab.minecraft.notoriety.guild.model.*
import com.hacklab.minecraft.notoriety.guild.repository.GuildApplicationRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildInvitationRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildMembershipRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildRepository
import com.hacklab.minecraft.notoriety.trust.TrustService
import com.hacklab.minecraft.notoriety.trust.TrustState
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class GuildServiceImpl(
    private val plugin: JavaPlugin,
    private val guildRepository: GuildRepository,
    private val membershipRepository: GuildMembershipRepository,
    private val invitationRepository: GuildInvitationRepository,
    private val applicationRepository: GuildApplicationRepository,
    private val trustService: TrustService,
    private val guildCache: GuildCache,
    private val guildTagManager: GuildTagManager
) : GuildService {

    companion object {
        // 日本語（漢字・ひらがな・カタカナ）、英数字、スペース、アンダースコア、ハイフンを許可
        private val NAME_PATTERN = Regex("^[a-zA-Z0-9_\\-\\s\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー]{3,32}$")
        private val TAG_PATTERN = Regex("^[a-zA-Z0-9]{2,4}$")
    }

    // === ギルド管理 ===

    override fun createGuild(creator: UUID, name: String, tag: String, description: String?): Guild {
        // バリデーション
        if (!NAME_PATTERN.matches(name)) {
            throw GuildException.InvalidName(name)
        }
        if (!TAG_PATTERN.matches(tag)) {
            throw GuildException.InvalidTag(tag)
        }
        if (guildRepository.existsByName(name)) {
            throw GuildException.NameTaken(name)
        }
        if (guildRepository.existsByTag(tag)) {
            throw GuildException.TagTaken(tag)
        }
        // Check if player already has a civilian guild (government guild is OK)
        val existingMemberships = membershipRepository.findAllByPlayerUuid(creator)
        val hasCivilianGuild = existingMemberships.any { membership ->
            getGuild(membership.guildId)?.isGovernment == false
        }
        if (hasCivilianGuild) {
            throw GuildException.AlreadyInGuild(creator)
        }

        // ギルド作成
        val guild = Guild(
            id = 0, // auto-generated
            name = name,
            tag = tag.uppercase(),
            tagColor = TagColor.WHITE,
            description = description,
            masterUuid = creator,
            createdAt = Instant.now(),
            maxMembers = 50
        )
        val guildId = guildRepository.insert(guild)
        val createdGuild = guild.copy(id = guildId)

        // マスターをメンバーとして追加
        val membership = GuildMembership(
            id = 0,
            guildId = guildId,
            playerUuid = creator,
            role = GuildRole.MASTER,
            joinedAt = Instant.now()
        )
        membershipRepository.insert(membership)

        // キャッシュに追加
        guildCache.put(createdGuild)

        // ギルドタグを表示
        Bukkit.getPlayer(creator)?.let { player ->
            guildTagManager.setGuildTag(player, createdGuild)
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(GuildCreateEvent(createdGuild, creator))

        return createdGuild
    }

    override fun createGovernmentGuild(creator: UUID, name: String, tag: String, description: String?): Guild {
        // バリデーション
        if (!NAME_PATTERN.matches(name)) {
            throw GuildException.InvalidName(name)
        }
        if (!TAG_PATTERN.matches(tag)) {
            throw GuildException.InvalidTag(tag)
        }
        if (guildRepository.existsByName(name)) {
            throw GuildException.NameTaken(name)
        }
        if (guildRepository.existsByTag(tag)) {
            throw GuildException.TagTaken(tag)
        }
        // Check if player already has a government guild (civilian guild is OK)
        val existingMemberships = membershipRepository.findAllByPlayerUuid(creator)
        val hasGovernmentGuild = existingMemberships.any { membership ->
            getGuild(membership.guildId)?.isGovernment == true
        }
        if (hasGovernmentGuild) {
            throw GuildException.AlreadyInGuild(creator)
        }

        // 政府ギルド作成（isGovernment=true）
        val guild = Guild(
            id = 0,
            name = name,
            tag = tag.uppercase(),
            tagColor = TagColor.GOLD, // 政府ギルドはデフォルトで金色
            description = description,
            masterUuid = creator,
            createdAt = Instant.now(),
            maxMembers = 50,
            isGovernment = true
        )
        val guildId = guildRepository.insert(guild)
        val createdGuild = guild.copy(id = guildId)

        // マスターをメンバーとして追加
        val membership = GuildMembership(
            id = 0,
            guildId = guildId,
            playerUuid = creator,
            role = GuildRole.MASTER,
            joinedAt = Instant.now()
        )
        membershipRepository.insert(membership)

        // キャッシュに追加
        guildCache.put(createdGuild)

        // ギルドタグを表示
        Bukkit.getPlayer(creator)?.let { player ->
            guildTagManager.setGuildTag(player, createdGuild)
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(GuildCreateEvent(createdGuild, creator))

        plugin.logger.info("Government guild created: ${createdGuild.name} [${createdGuild.tag}] by ${Bukkit.getOfflinePlayer(creator).name}")

        return createdGuild
    }

    override fun setTagColor(guildId: Long, color: TagColor, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val membership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)

        if (!membership.role.canChangeTagColor()) {
            throw GuildException.NotMaster(requester)
        }

        guildRepository.updateTagColor(guildId, color)

        // キャッシュを更新
        val updatedGuild = guild.copy(tagColor = color)
        guildCache.put(updatedGuild)
    }

    override fun setName(guildId: Long, name: String, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val membership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)

        if (membership.role != GuildRole.MASTER) {
            throw GuildException.NotMaster(requester)
        }

        // バリデーション
        if (!NAME_PATTERN.matches(name)) {
            throw GuildException.InvalidName(name)
        }
        if (name != guild.name && guildRepository.existsByName(name)) {
            throw GuildException.NameTaken(name)
        }

        guildRepository.updateName(guildId, name)

        // キャッシュを更新
        val updatedGuild = guild.copy(name = name)
        guildCache.put(updatedGuild)
    }

    override fun setTag(guildId: Long, tag: String, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val membership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)

        if (membership.role != GuildRole.MASTER) {
            throw GuildException.NotMaster(requester)
        }

        val upperTag = tag.uppercase()

        // バリデーション
        if (!TAG_PATTERN.matches(tag)) {
            throw GuildException.InvalidTag(tag)
        }
        if (upperTag != guild.tag && guildRepository.existsByTag(upperTag)) {
            throw GuildException.TagTaken(tag)
        }

        guildRepository.updateTag(guildId, upperTag)

        // キャッシュを更新
        val updatedGuild = guild.copy(tag = upperTag)
        guildCache.put(updatedGuild)

        // オンラインメンバーのギルドタグを更新
        val members = membershipRepository.findAllByGuildId(guildId)
        members.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.let { player ->
                guildTagManager.setGuildTag(player, updatedGuild)
            }
        }
    }

    override fun setDescription(guildId: Long, description: String?, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val membership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)

        if (membership.role != GuildRole.MASTER) {
            throw GuildException.NotMaster(requester)
        }

        guildRepository.updateDescription(guildId, description)

        // キャッシュを更新
        val updatedGuild = guild.copy(description = description)
        guildCache.put(updatedGuild)
    }

    override fun dissolveGuild(guildId: Long, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val requesterMembership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)

        if (requesterMembership.role != GuildRole.MASTER) {
            throw GuildException.NotMaster(requester)
        }

        // 全メンバーを取得
        val members = membershipRepository.findAllByGuildId(guildId)
        val memberUuids = members.map { it.playerUuid }

        // メンバー離脱イベントを先に発火
        members.forEach { member ->
            Bukkit.getPluginManager().callEvent(
                GuildMemberLeaveEvent(guild, member.playerUuid, LeaveReason.DISSOLVED)
            )
        }

        // 招待を削除（CASCADE DELETEがあるが念のため）
        invitationRepository.deleteByGuildId(guildId)

        // メンバーシップを削除（CASCADE DELETEがあるが念のため）
        membershipRepository.deleteByGuildId(guildId)

        // ギルドを削除
        guildRepository.delete(guildId)

        // キャッシュから削除
        guildCache.remove(guildId)

        // 元メンバーのタグを更新（他のギルドがあればそちらのタグを設定）
        members.forEach { member ->
            Bukkit.getPlayer(member.playerUuid)?.let { player ->
                val remainingGuild = getPlayerGuild(member.playerUuid)
                if (remainingGuild != null) {
                    guildTagManager.setGuildTag(player, remainingGuild)
                } else {
                    guildTagManager.removeGuildTag(player)
                }
            }
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildDissolveEvent(guild, requester, memberUuids)
        )
    }

    override fun getGuild(guildId: Long): Guild? {
        // キャッシュを先にチェック
        guildCache.get(guildId)?.let { return it }

        // DBから取得してキャッシュ
        return guildRepository.findById(guildId)?.also { guildCache.put(it) }
    }

    override fun getGuildByName(name: String): Guild? {
        // キャッシュを先にチェック
        guildCache.getByName(name)?.let { return it }

        // DBから取得してキャッシュ
        return guildRepository.findByName(name)?.also { guildCache.put(it) }
    }

    override fun getPlayerGuild(playerUuid: UUID): Guild? {
        // Civilian guild priority, fallback to government guild
        val memberships = membershipRepository.findAllByPlayerUuid(playerUuid)
        if (memberships.isEmpty()) return null

        val guilds = memberships.mapNotNull { getGuild(it.guildId) }
        return guilds.firstOrNull { !it.isGovernment } ?: guilds.firstOrNull()
    }

    override fun getPlayerCivilianGuild(playerUuid: UUID): Guild? {
        val memberships = membershipRepository.findAllByPlayerUuid(playerUuid)
        return memberships.mapNotNull { getGuild(it.guildId) }
            .firstOrNull { !it.isGovernment }
    }

    override fun getPlayerGovernmentGuild(playerUuid: UUID): Guild? {
        val memberships = membershipRepository.findAllByPlayerUuid(playerUuid)
        return memberships.mapNotNull { getGuild(it.guildId) }
            .firstOrNull { it.isGovernment }
    }

    override fun getPlayerGuilds(playerUuid: UUID): List<Guild> {
        val memberships = membershipRepository.findAllByPlayerUuid(playerUuid)
        return memberships.mapNotNull { getGuild(it.guildId) }
    }

    override fun getAllGuilds(page: Int, pageSize: Int): List<Guild> {
        return guildRepository.findAll(page, pageSize).also { guilds ->
            guilds.forEach { guildCache.put(it) }
        }
    }

    override fun getGuildCount(): Int {
        return guildRepository.count()
    }

    // === メンバー管理 ===

    override fun getMembers(guildId: Long, page: Int, pageSize: Int): List<GuildMembership> {
        getGuildOrThrow(guildId)
        return membershipRepository.findByGuildId(guildId, page, pageSize)
    }

    override fun getMemberCount(guildId: Long): Int {
        return membershipRepository.countByGuildId(guildId)
    }

    override fun getMembership(playerUuid: UUID): GuildMembership? {
        return membershipRepository.findByPlayerUuid(playerUuid)
    }

    override fun getMembership(playerUuid: UUID, guildId: Long): GuildMembership? {
        return membershipRepository.findByPlayerAndGuild(playerUuid, guildId)
    }

    override fun kickMember(guildId: Long, targetUuid: UUID, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val requesterMembership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)
        val targetMembership = membershipRepository.findByPlayerAndGuild(targetUuid, guildId)
            ?: throw GuildException.NotMember(targetUuid, guildId)

        if (requester == targetUuid) {
            throw GuildException.CannotKickSelf()
        }
        if (!requesterMembership.role.canKick()) {
            throw GuildException.NotMaster(requester)
        }

        // メンバーシップを削除（このギルドのみ）
        membershipRepository.deleteByPlayerAndGuild(targetUuid, guildId)

        // ギルドタグを更新（他のギルドがあればそちらのタグを設定）
        Bukkit.getPlayer(targetUuid)?.let { player ->
            val remainingGuild = getPlayerGuild(targetUuid)
            if (remainingGuild != null) {
                guildTagManager.setGuildTag(player, remainingGuild)
            } else {
                guildTagManager.removeGuildTag(player)
            }
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildMemberLeaveEvent(guild, targetUuid, LeaveReason.KICKED)
        )
    }

    override fun leaveGuild(playerUuid: UUID) {
        // Delegate to guild-specific overload with civilian-priority guild
        val guild = getPlayerGuild(playerUuid) ?: throw GuildException.NotInGuild(playerUuid)
        leaveGuild(playerUuid, guild.id)
    }

    override fun leaveGuild(playerUuid: UUID, guildId: Long) {
        val membership = membershipRepository.findByPlayerAndGuild(playerUuid, guildId)
            ?: throw GuildException.NotMember(playerUuid, guildId)
        val guild = getGuildOrThrow(guildId)

        // マスターは他のメンバーがいる間は脱退できない
        if (membership.role == GuildRole.MASTER) {
            val memberCount = membershipRepository.countByGuildId(guildId)
            if (memberCount > 1) {
                throw GuildException.MasterCannotLeave()
            }
        }

        // メンバーシップを削除（ギルド指定）
        membershipRepository.deleteByPlayerAndGuild(playerUuid, guildId)

        // ギルドタグを更新（他のギルドがあればそちらのタグを設定）
        Bukkit.getPlayer(playerUuid)?.let { player ->
            val remainingGuild = getPlayerGuild(playerUuid)
            if (remainingGuild != null) {
                guildTagManager.setGuildTag(player, remainingGuild)
            } else {
                guildTagManager.removeGuildTag(player)
            }
        }

        // マスターが最後の一人だった場合、ギルドを解散
        if (membership.role == GuildRole.MASTER) {
            guildRepository.delete(guildId)
            guildCache.remove(guildId)
            Bukkit.getPluginManager().callEvent(
                GuildDissolveEvent(guild, playerUuid, listOf(playerUuid))
            )
        }

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildMemberLeaveEvent(guild, playerUuid, LeaveReason.LEAVE)
        )
    }

    override fun promoteToViceMaster(guildId: Long, targetUuid: UUID, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val requesterMembership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)
        val targetMembership = membershipRepository.findByPlayerAndGuild(targetUuid, guildId)
            ?: throw GuildException.NotMember(targetUuid, guildId)

        if (!requesterMembership.role.canPromote()) {
            throw GuildException.NotMaster(requester)
        }
        if (targetMembership.role == GuildRole.VICE_MASTER) {
            throw GuildException.AlreadyViceMaster(targetUuid)
        }
        if (targetMembership.role == GuildRole.MASTER) {
            throw GuildException.NotMember(targetUuid, guildId) // マスターは昇進できない
        }

        val oldRole = targetMembership.role
        membershipRepository.updateRole(targetUuid, guildId, GuildRole.VICE_MASTER)

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildRoleChangeEvent(guild, targetUuid, oldRole, GuildRole.VICE_MASTER)
        )
    }

    override fun demoteToMember(guildId: Long, targetUuid: UUID, requester: UUID) {
        val guild = getGuildOrThrow(guildId)
        val requesterMembership = membershipRepository.findByPlayerAndGuild(requester, guildId)
            ?: throw GuildException.NotMember(requester, guildId)
        val targetMembership = membershipRepository.findByPlayerAndGuild(targetUuid, guildId)
            ?: throw GuildException.NotMember(targetUuid, guildId)

        if (!requesterMembership.role.canDemote()) {
            throw GuildException.NotMaster(requester)
        }
        if (targetMembership.role != GuildRole.VICE_MASTER) {
            throw GuildException.NotViceMaster(targetUuid)
        }

        val oldRole = targetMembership.role
        membershipRepository.updateRole(targetUuid, guildId, GuildRole.MEMBER)

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildRoleChangeEvent(guild, targetUuid, oldRole, GuildRole.MEMBER)
        )
    }

    override fun transferMaster(guildId: Long, newMasterUuid: UUID, currentMaster: UUID) {
        val guild = getGuildOrThrow(guildId)
        val currentMasterMembership = membershipRepository.findByPlayerAndGuild(currentMaster, guildId)
            ?: throw GuildException.NotMember(currentMaster, guildId)
        val newMasterMembership = membershipRepository.findByPlayerAndGuild(newMasterUuid, guildId)
            ?: throw GuildException.NotMember(newMasterUuid, guildId)

        if (currentMasterMembership.role != GuildRole.MASTER) {
            throw GuildException.NotMaster(currentMaster)
        }

        // 新マスターをMASTERに
        membershipRepository.updateRole(newMasterUuid, guildId, GuildRole.MASTER)
        // 旧マスターをVICE_MASTERに
        membershipRepository.updateRole(currentMaster, guildId, GuildRole.VICE_MASTER)
        // ギルドのmaster_uuidを更新
        guildRepository.updateMaster(guildId, newMasterUuid)

        // キャッシュを更新
        val updatedGuild = guild.copy(masterUuid = newMasterUuid)
        guildCache.put(updatedGuild)

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildRoleChangeEvent(guild, currentMaster, GuildRole.MASTER, GuildRole.VICE_MASTER)
        )
        Bukkit.getPluginManager().callEvent(
            GuildRoleChangeEvent(updatedGuild, newMasterUuid, newMasterMembership.role, GuildRole.MASTER)
        )
    }

    // === 招待管理 ===

    override fun invitePlayer(guildId: Long, inviteeUuid: UUID, inviter: UUID): GuildInvitation {
        val guild = getGuildOrThrow(guildId)
        val inviterMembership = membershipRepository.findByPlayerAndGuild(inviter, guildId)
            ?: throw GuildException.NotMember(inviter, guildId)

        if (!inviterMembership.role.canInvite()) {
            throw GuildException.NotMasterOrVice(inviter)
        }
        // Check if invitee already has a guild of the same type
        if (hasGuildOfSameType(inviteeUuid, guild)) {
            throw GuildException.AlreadyInGuild(inviteeUuid)
        }
        if (invitationRepository.existsByGuildAndInvitee(guildId, inviteeUuid)) {
            throw GuildException.AlreadyInvited(inviteeUuid, guildId)
        }

        // ギルドが満員でないかチェック
        val memberCount = membershipRepository.countByGuildId(guildId)
        if (memberCount >= guild.maxMembers) {
            throw GuildException.GuildFull(guildId)
        }

        val invitation = GuildInvitation(
            id = 0,
            guildId = guildId,
            inviterUuid = inviter,
            inviteeUuid = inviteeUuid,
            invitedAt = Instant.now(),
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        )
        val invitationId = invitationRepository.insert(invitation)

        // イベント発火
        Bukkit.getPlayer(inviter)?.let { inviterPlayer ->
            Bukkit.getPluginManager().callEvent(
                GuildInviteEvent(guild, inviterPlayer, inviteeUuid)
            )
        }

        return invitation.copy(id = invitationId)
    }

    override fun acceptInvitation(invitationId: Long, playerUuid: UUID) {
        val invitation = invitationRepository.findById(invitationId)
            ?: throw GuildException.InvitationNotFound(invitationId)

        if (invitation.inviteeUuid != playerUuid) {
            throw GuildException.NotInvitee(playerUuid, invitationId)
        }
        if (invitation.expiresAt.isBefore(Instant.now())) {
            invitationRepository.delete(invitationId)
            throw GuildException.InvitationExpired(invitationId)
        }

        val guild = getGuildOrThrow(invitation.guildId)

        // Check if player already has a guild of the same type (civilian or government)
        if (hasGuildOfSameType(playerUuid, guild)) {
            throw GuildException.AlreadyInGuild(playerUuid)
        }

        // ギルドが満員でないかチェック
        val memberCount = membershipRepository.countByGuildId(invitation.guildId)
        if (memberCount >= guild.maxMembers) {
            throw GuildException.GuildFull(invitation.guildId)
        }

        // メンバーとして追加
        val membership = GuildMembership(
            id = 0,
            guildId = invitation.guildId,
            playerUuid = playerUuid,
            role = GuildRole.MEMBER,
            joinedAt = Instant.now()
        )
        membershipRepository.insert(membership)

        // 招待を削除
        invitationRepository.delete(invitationId)

        // ギルドタグを表示
        Bukkit.getPlayer(playerUuid)?.let { player ->
            guildTagManager.setGuildTag(player, guild)
            // イベント発火
            Bukkit.getPluginManager().callEvent(
                GuildMemberJoinEvent(guild, player, membership)
            )
        }
    }

    override fun acceptInvitationByGuildName(guildName: String, playerUuid: UUID) {
        val invitation = invitationRepository.findByGuildName(guildName, playerUuid)
            ?: throw GuildException.InvitationNotFoundByGuild(guildName)

        acceptInvitation(invitation.id, playerUuid)
    }

    override fun declineInvitation(invitationId: Long, playerUuid: UUID) {
        val invitation = invitationRepository.findById(invitationId)
            ?: throw GuildException.InvitationNotFound(invitationId)

        if (invitation.inviteeUuid != playerUuid) {
            throw GuildException.NotInvitee(playerUuid, invitationId)
        }

        invitationRepository.delete(invitationId)
    }

    override fun declineInvitationByGuildName(guildName: String, playerUuid: UUID) {
        val invitation = invitationRepository.findByGuildName(guildName, playerUuid)
            ?: throw GuildException.InvitationNotFoundByGuild(guildName)

        invitationRepository.delete(invitation.id)
    }

    override fun acceptInvitationByGuildId(guildId: Long, playerUuid: UUID) {
        val invitation = invitationRepository.findByGuildAndInvitee(guildId, playerUuid)
            ?: throw GuildException.InvitationNotFoundByGuildId(guildId)

        acceptInvitation(invitation.id, playerUuid)
    }

    override fun declineInvitationByGuildId(guildId: Long, playerUuid: UUID) {
        val invitation = invitationRepository.findByGuildAndInvitee(guildId, playerUuid)
            ?: throw GuildException.InvitationNotFoundByGuildId(guildId)

        invitationRepository.delete(invitation.id)
    }

    override fun cancelInvitation(guildId: Long, inviter: UUID, inviteeUuid: UUID) {
        val inviterMembership = membershipRepository.findByPlayerAndGuild(inviter, guildId)
            ?: throw GuildException.NotMember(inviter, guildId)

        if (!inviterMembership.role.canInvite()) {
            throw GuildException.NotMasterOrVice(inviter)
        }

        val invitation = invitationRepository.findByGuildAndInvitee(guildId, inviteeUuid)
            ?: throw GuildException.InvitationNotFoundByGuildId(guildId)

        invitationRepository.delete(invitation.id)
    }

    override fun getPendingInvitations(playerUuid: UUID): List<GuildInvitation> {
        return invitationRepository.findByInviteeUuid(playerUuid)
    }

    override fun getSentInvitations(guildId: Long): List<GuildInvitation> {
        return invitationRepository.findByGuildId(guildId)
    }

    override fun hasInvitation(guildId: Long, playerUuid: UUID): Boolean {
        return invitationRepository.existsByGuildAndInvitee(guildId, playerUuid)
    }

    override fun cleanupExpiredInvitations() {
        invitationRepository.deleteExpired()
    }

    // === 入会申請管理 ===

    override fun applyToGuild(guildId: Long, applicantUuid: UUID, message: String?): GuildApplication {
        val guild = getGuildOrThrow(guildId)

        // Check if player already has a guild of the same type
        if (hasGuildOfSameType(applicantUuid, guild)) {
            throw GuildException.AlreadyInGuild(applicantUuid)
        }
        // 既に申請済みの場合はエラー
        if (applicationRepository.existsByGuildAndApplicant(guildId, applicantUuid)) {
            throw GuildException.AlreadyApplied(applicantUuid, guildId)
        }
        // 招待がある場合は申請不要
        if (invitationRepository.existsByGuildAndInvitee(guildId, applicantUuid)) {
            throw GuildException.AlreadyInvited(applicantUuid, guildId)
        }
        // ギルドが満員でないかチェック
        val memberCount = membershipRepository.countByGuildId(guildId)
        if (memberCount >= guild.maxMembers) {
            throw GuildException.GuildFull(guildId)
        }

        val application = GuildApplication(
            id = 0,
            guildId = guildId,
            applicantUuid = applicantUuid,
            message = message,
            appliedAt = Instant.now(),
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        )
        val applicationId = applicationRepository.insert(application)

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            GuildApplicationEvent(guild, applicantUuid, message)
        )

        return application.copy(id = applicationId)
    }

    override fun approveApplication(applicationId: Long, approverUuid: UUID) {
        val application = applicationRepository.findById(applicationId)
            ?: throw GuildException.ApplicationNotFound(applicationId)

        val guild = getGuildOrThrow(application.guildId)
        val approverMembership = membershipRepository.findByPlayerAndGuild(approverUuid, application.guildId)
            ?: throw GuildException.NotMember(approverUuid, application.guildId)

        // マスターまたは副マスターのみ承認可能
        if (!approverMembership.role.canInvite()) {
            throw GuildException.NotMasterOrVice(approverUuid)
        }
        // 期限切れチェック
        if (application.isExpired()) {
            applicationRepository.delete(applicationId)
            throw GuildException.ApplicationExpired(applicationId)
        }
        // Check if player already has a guild of the same type
        if (hasGuildOfSameType(application.applicantUuid, guild)) {
            applicationRepository.delete(applicationId)
            throw GuildException.AlreadyInGuild(application.applicantUuid)
        }
        // ギルドが満員でないかチェック
        val memberCount = membershipRepository.countByGuildId(application.guildId)
        if (memberCount >= guild.maxMembers) {
            throw GuildException.GuildFull(application.guildId)
        }

        // メンバーとして追加
        val membership = GuildMembership(
            id = 0,
            guildId = application.guildId,
            playerUuid = application.applicantUuid,
            role = GuildRole.MEMBER,
            joinedAt = Instant.now()
        )
        membershipRepository.insert(membership)

        // 申請を削除
        applicationRepository.delete(applicationId)

        // ギルドタグを表示
        Bukkit.getPlayer(application.applicantUuid)?.let { player ->
            guildTagManager.setGuildTag(player, guild)
            // イベント発火
            Bukkit.getPluginManager().callEvent(
                GuildMemberJoinEvent(guild, player, membership)
            )
        }
    }

    override fun rejectApplication(applicationId: Long, rejectorUuid: UUID) {
        val application = applicationRepository.findById(applicationId)
            ?: throw GuildException.ApplicationNotFound(applicationId)

        val rejectorMembership = membershipRepository.findByPlayerAndGuild(rejectorUuid, application.guildId)
            ?: throw GuildException.NotMember(rejectorUuid, application.guildId)

        // マスターまたは副マスターのみ拒否可能
        if (!rejectorMembership.role.canInvite()) {
            throw GuildException.NotMasterOrVice(rejectorUuid)
        }

        applicationRepository.delete(applicationId)
    }

    override fun cancelApplication(guildId: Long, applicantUuid: UUID) {
        val application = applicationRepository.findByGuildAndApplicant(guildId, applicantUuid)
            ?: throw GuildException.ApplicationNotFoundByGuild(guildId)

        applicationRepository.delete(application.id)
    }

    override fun getPendingApplications(guildId: Long): List<GuildApplication> {
        return applicationRepository.findByGuildId(guildId)
    }

    override fun getPlayerApplications(playerUuid: UUID): List<GuildApplication> {
        return applicationRepository.findByApplicantUuid(playerUuid)
    }

    override fun hasApplication(guildId: Long, playerUuid: UUID): Boolean {
        return applicationRepository.existsByGuildAndApplicant(guildId, playerUuid)
    }

    override fun cleanupExpiredApplications() {
        applicationRepository.deleteExpired()
    }

    // === 信頼システム統合 ===

    override fun areInSameGuild(player1: UUID, player2: UUID): Boolean {
        return membershipRepository.areInSameGuild(player1, player2)
    }

    override fun isAccessAllowed(owner: UUID, accessor: UUID): Boolean {
        // 同じプレイヤーなら許可
        if (owner == accessor) return true

        // 信頼状態を確認
        val trustState = trustService.getTrustState(owner, accessor)
        when (trustState) {
            TrustState.TRUST -> return true
            TrustState.DISTRUST -> return false
            null -> {
                // UNSET: ギルドメンバーならアクセス許可
                return areInSameGuild(owner, accessor)
            }
        }
    }

    override fun canTakeFromContainer(owner: UUID, accessor: UUID): Boolean {
        // 同じプレイヤーなら許可
        if (owner == accessor) return true

        // DISTRUSTの場合のみ取り出し不可
        // TRUST → 取り出し可能（犯罪なし）
        // 未設定 → 取り出し可能（犯罪になる）
        // DISTRUST → 取り出し不可
        val trustState = trustService.getTrustState(owner, accessor)
        return trustState != TrustState.DISTRUST
    }

    override fun setTrustState(truster: UUID, trusted: UUID, state: TrustState) {
        trustService.setTrustState(truster, trusted, state)
    }

    override fun removeTrustState(truster: UUID, trusted: UUID) {
        trustService.removeTrustState(truster, trusted)
    }

    override fun getTrustState(truster: UUID, trusted: UUID): TrustState? {
        return trustService.getTrustState(truster, trusted)
    }

    // === ヘルパーメソッド ===

    private fun getGuildOrThrow(guildId: Long): Guild {
        return getGuild(guildId) ?: throw GuildException.NotFound(guildId)
    }

    /**
     * Check if a player already has a guild of the same type (civilian or government)
     * as the target guild. Used for membership validation during invite/apply/join.
     */
    private fun hasGuildOfSameType(playerUuid: UUID, targetGuild: Guild): Boolean {
        val existingMemberships = membershipRepository.findAllByPlayerUuid(playerUuid)
        return existingMemberships.any { membership ->
            val guild = getGuild(membership.guildId)
            guild != null && guild.isGovernment == targetGuild.isGovernment
        }
    }
}
