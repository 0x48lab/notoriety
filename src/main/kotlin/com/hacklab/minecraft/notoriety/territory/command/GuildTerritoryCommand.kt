package com.hacklab.minecraft.notoriety.territory.command

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.command.GuildSubCommand
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.SigilCreateEvent
import com.hacklab.minecraft.notoriety.territory.event.TerritoryClaimEvent
import com.hacklab.minecraft.notoriety.territory.service.ClaimResult
import com.hacklab.minecraft.notoriety.territory.service.ReleaseResult
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ギルド領地コマンド
 * /guild territory <set|info|release>
 */
class GuildTerritoryCommand(
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val i18n: I18nManager
) : GuildSubCommand {

    override val name = "territory"
    override val description = "Manage guild territory"
    override val usage = "/guild territory <set|info|release>"
    override val aliases = listOf("t", "land")

    // 領地解放確認用タイムアウト（5秒）
    private val releaseConfirmations = ConcurrentHashMap<UUID, Long>()
    private val CONFIRM_TIMEOUT_MS = 5000L

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        if (args.isEmpty()) {
            showUsage(player)
            return true
        }

        return when (args[0].lowercase()) {
            "set", "claim" -> handleSet(player, args.drop(1).toTypedArray())
            "info", "status" -> handleInfo(player)
            "release", "abandon" -> handleRelease(player, args.drop(1).toTypedArray())
            "mobspawn", "mob" -> handleMobSpawn(player, args.drop(1).toTypedArray())
            else -> {
                showUsage(player)
                true
            }
        }
    }

    private fun handleSet(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId
        val location = player.location
        val isOp = player.isOp

        // OPの場合、最初の引数でギルド名を指定可能
        // 使用法: /guild territory set [ギルド名] [シギル名]
        val (guildId, sigilName, requester) = if (isOp && args.isNotEmpty()) {
            // OPの場合、最初の引数がギルド名かシギル名かを判定
            val firstArgGuild = guildService.getGuildByName(args[0])
            if (firstArgGuild != null) {
                // ギルド名として解釈
                val sigil = if (args.size > 1) args.drop(1).joinToString(" ") else null
                Triple(firstArgGuild.id, sigil, null as UUID?)
            } else {
                // シギル名として解釈（自分のギルド）
                val myGuildId = getPlayerGuildId(player)
                if (myGuildId == null) {
                    i18n.sendError(player, "territory.claim_no_guild", "You must be in a guild to claim territory")
                    return true
                }
                Triple(myGuildId, args.joinToString(" ").takeIf { it.isNotEmpty() }, uuid)
            }
        } else {
            // 通常プレイヤー
            val myGuildId = getPlayerGuildId(player) ?: run {
                i18n.sendError(player, "territory.claim_no_guild", "You must be in a guild to claim territory")
                return true
            }
            val sigil = if (args.isNotEmpty()) args.joinToString(" ") else null
            Triple(myGuildId, sigil, uuid)
        }

        val result = territoryService.claimTerritory(
            guildId = guildId,
            location = location,
            requester = requester,
            sigilName = sigilName
        )

        when (result) {
            is ClaimResult.Success -> {
                val guild = guildService.getPlayerGuild(uuid)
                val territory = territoryService.getTerritory(guild?.id ?: 0)
                val totalChunks = territory?.chunkCount ?: 1

                // 基本メッセージ
                i18n.sendSuccess(player, "territory.claim_success", "Territory claimed! (%d/%d chunks)",
                    totalChunks,
                    territoryService.calculateAllowedChunks(guildService.getMemberCount(guild?.id ?: 0)))

                // シギル関連メッセージ
                if (result.isNewSigil && result.sigil != null) {
                    i18n.sendSuccess(player, "territory.sigil_created",
                        "Created new sigil \"%s\"", result.sigil.name)

                    // シギル作成イベント発火
                    Bukkit.getPluginManager().callEvent(
                        SigilCreateEvent(
                            guildId = guild?.id ?: 0,
                            guildName = guild?.name ?: "",
                            sigil = result.sigil,
                            createdBy = uuid,
                            isEnclave = totalChunks > 1  // 2個目以降で新規シギル = 飛び地
                        )
                    )
                } else if (result.sigil != null) {
                    i18n.sendInfo(player, "territory.sigil_chunk_added",
                        "Added to sigil \"%s\"", result.sigil.name)
                }

                // グループマージがあった場合
                if (result.mergedSigilIds.isNotEmpty()) {
                    i18n.sendWarning(player, "territory.sigils_merged",
                        "%d sigils have been merged", result.mergedSigilIds.size)
                }

                // 領地確保イベント発火
                Bukkit.getPluginManager().callEvent(
                    TerritoryClaimEvent(
                        guildId = guild?.id ?: 0,
                        guildName = guild?.name ?: "",
                        claimedBy = uuid,
                        chunk = result.chunk,
                        totalChunks = totalChunks
                    )
                )
            }
            is ClaimResult.GuildNotFound -> {
                i18n.sendError(player, "territory.claim_no_guild", "You must be in a guild to claim territory")
            }
            is ClaimResult.NotGuildMaster -> {
                i18n.sendError(player, "territory.claim_not_master", "Only the guild master can claim territory")
            }
            is ClaimResult.NotEnoughMembers -> {
                val guild = guildService.getPlayerGuild(uuid)
                val memberCount = guildService.getMemberCount(guild?.id ?: 0)
                i18n.sendError(player, "territory.claim_not_enough_members",
                    "You need at least %d members to claim territory (current: %d)",
                    TerritoryService.MIN_MEMBERS_FOR_TERRITORY, memberCount)
            }
            is ClaimResult.MaxChunksReached -> {
                val maxChunks = territoryService.calculateAllowedChunks(
                    guildService.getMemberCount(getPlayerGuildId(player) ?: 0)
                )
                i18n.sendError(player, "territory.claim_max_chunks",
                    "Your guild has reached the maximum territory limit (%d chunks)", maxChunks)
            }
            is ClaimResult.MemberChunkLimitReached -> {
                val guild = guildService.getPlayerGuild(uuid)
                val memberCount = guildService.getMemberCount(guild?.id ?: 0)
                val maxChunks = territoryService.calculateAllowedChunks(memberCount)
                i18n.sendError(player, "territory.claim_member_limit",
                    "Your current member count (%d) only allows %d chunks", memberCount, maxChunks)
            }
            is ClaimResult.OverlapOtherGuild -> {
                i18n.sendError(player, "territory.claim_overlap",
                    "This location overlaps with %s's territory", result.guildName)
            }
            is ClaimResult.NotInGuild -> {
                i18n.sendError(player, "territory.claim_no_guild", "You must be in a guild to claim territory")
            }
            is ClaimResult.AlreadyClaimed -> {
                i18n.sendError(player, "territory.claim_already_claimed",
                    "This chunk is already part of your guild's territory")
            }
            is ClaimResult.InvalidSigilName -> {
                i18n.sendError(player, "territory.claim_invalid_sigil_name",
                    "Invalid sigil name: %s", result.reason)
            }
            is ClaimResult.SigilNameAlreadyExists -> {
                i18n.sendError(player, "territory.claim_sigil_name_exists",
                    "Sigil name \"%s\" is already in use", result.name)
            }
        }

        return true
    }

    private fun handleInfo(player: Player): Boolean {
        val uuid = player.uniqueId
        val isOp = player.isOp

        // ギルドを取得（OPの場合は現在地の領地からも取得可能）
        var guild = guildService.getPlayerGuild(uuid)

        if (guild == null && isOp) {
            // OPで自分のギルドがない場合、現在地の領地のギルドを取得
            val territoryAtLocation = territoryService.getTerritoryAt(player.location)
            if (territoryAtLocation != null) {
                guild = guildService.getGuild(territoryAtLocation.guildId)
            }
        }

        if (guild == null) {
            i18n.sendError(player, "territory.info_not_in_guild", "You are not in a guild")
            return true
        }

        val territory = territoryService.getTerritory(guild.id)
        val memberCount = guildService.getMemberCount(guild.id)
        val maxChunks = if (guild.isGovernment) {
            i18n.get(uuid, "territory.unlimited", "Unlimited")
        } else {
            territoryService.calculateAllowedChunks(memberCount).toString()
        }

        i18n.sendHeader(player, "territory.info_header", "=== %s Territory Info ===", guild.name)

        if (guild.isGovernment) {
            i18n.send(player, "territory.info_government_guild", "  [Government Guild - No territory limit]",
                net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
        }

        if (territory == null || territory.chunkCount == 0) {
            i18n.sendInfo(player, "territory.info_no_territory", "Your guild has no territory")
        } else {
            i18n.sendInfo(player, "territory.info_chunk_count", "Chunks: %s / %s",
                territory.chunkCount.toString(), maxChunks)

            // シギル一覧を表示
            if (territory.sigilCount > 0) {
                i18n.send(player, "territory.info_sigil_header", "--- Sigil List ---", NamedTextColor.AQUA)
                territory.sigils.forEach { sigil ->
                    val chunkCount = territory.getChunksForSigil(sigil.id).size
                    val chunkLabel = i18n.get(uuid, "territory.chunks_label", "%d chunks", chunkCount)
                    player.sendMessage(Component.text(
                        "  ● ${sigil.name} ($chunkLabel) - ${sigil.worldName}"
                    ).color(NamedTextColor.AQUA))
                }
            }

            // チャンク一覧を番号順で表示
            i18n.send(player, "territory.info_chunk_header", "--- Chunk List ---", NamedTextColor.GRAY)
            territory.chunks.sortedBy { it.addOrder }.forEach { chunk ->
                val isCurrentChunk = chunk.containsLocation(player.location)
                val sigilName = chunk.sigilId?.let { sigilId ->
                    territory.getSigilById(sigilId)?.name
                } ?: "?"
                val chunkInfo = Component.text(
                    "  #${chunk.addOrder}: (${chunk.chunkX}, ${chunk.chunkZ}) [${sigilName}]"
                ).color(if (isCurrentChunk) NamedTextColor.GREEN else NamedTextColor.YELLOW)
                if (isCurrentChunk) {
                    player.sendMessage(chunkInfo.append(
                        i18n.component(player, "territory.info_current_location", " ← Current", NamedTextColor.GREEN)
                    ))
                } else {
                    player.sendMessage(chunkInfo)
                }
            }
        }

        if (!guild.isGovernment) {
            i18n.sendInfo(player, "territory.info_allowed_chunks", "Allowed chunks: %d (members: %d)",
                territoryService.calculateAllowedChunks(memberCount), memberCount)
        }

        return true
    }

    private fun handleRelease(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId
        val isOp = player.isOp

        // ギルドIDを取得（OPの場合は現在地の領地からも取得可能）
        var guildId = getPlayerGuildId(player)

        if (guildId == null && isOp) {
            // OPで自分のギルドがない場合、現在地の領地のギルドを取得
            val territoryAtLocation = territoryService.getTerritoryAt(player.location)
            if (territoryAtLocation != null) {
                guildId = territoryAtLocation.guildId
            }
        }

        if (guildId == null) {
            i18n.sendError(player, "territory.release_no_guild", "You are not in a guild")
            return true
        }

        // OPの場合はrequesterをnullにして権限チェックをスキップ
        val requester = if (isOp) null else uuid

        // 引数なしの場合は現在地解放
        if (args.isEmpty()) {
            return handleReleaseCurrentLocation(player, guildId, requester)
        }

        // "all" の場合は全解放
        if (args[0].lowercase() == "all") {
            return handleReleaseAll(player, guildId, requester)
        }

        // 番号指定の場合
        val chunkNumber = args[0].toIntOrNull()
        if (chunkNumber == null) {
            i18n.sendError(player, "territory.release_invalid_number", "Invalid number: %s", args[0])
            return true
        }

        return handleReleaseChunk(player, guildId, chunkNumber, requester)
    }

    /**
     * 現在地のチャンクを解放
     */
    private fun handleReleaseCurrentLocation(player: Player, guildId: Long, requester: UUID?): Boolean {
        val territory = territoryService.getTerritory(guildId)
        if (territory == null) {
            i18n.sendError(player, "territory.release_no_territory", "Your guild has no territory")
            return true
        }

        // 現在地のチャンクを検索
        val currentChunk = territory.chunks.find { it.containsLocation(player.location) }
        if (currentChunk == null) {
            i18n.sendError(player, "territory.release_not_in_territory",
                "You are not standing in your guild's territory")
            return true
        }

        // 既存の releaseChunk を呼び出し
        return handleReleaseChunk(player, guildId, currentChunk.addOrder, requester)
    }

    private fun handleReleaseAll(player: Player, guildId: Long, requester: UUID? = player.uniqueId): Boolean {
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()

        // 確認チェック
        val lastConfirm = releaseConfirmations[uuid]
        if (lastConfirm == null || now - lastConfirm > CONFIRM_TIMEOUT_MS) {
            // 初回または期限切れ - 確認を求める
            releaseConfirmations[uuid] = now
            player.sendMessage(
                Component.text(i18n.get(uuid, "territory.release_confirm_warning", "Warning: "))
                    .color(NamedTextColor.RED)
                    .append(Component.text(i18n.get(uuid, "territory.release_confirm_question", "Release all territory? "))
                        .color(NamedTextColor.YELLOW))
                    .append(Component.text(i18n.get(uuid, "territory.release_confirm_hint", "(Run again within 5 seconds to confirm)"))
                        .color(NamedTextColor.GRAY))
            )
            return true
        }

        // 確認済み - 実行
        releaseConfirmations.remove(uuid)

        val result = territoryService.releaseAllTerritory(guildId, requester)

        when (result) {
            is ReleaseResult.Success -> {
                i18n.sendSuccess(player, "territory.release_success",
                    "Released all territory (%d chunks)", result.releasedChunkCount)
            }
            is ReleaseResult.GuildNotFound -> {
                i18n.sendError(player, "territory.release_no_guild", "You are not in a guild")
            }
            is ReleaseResult.NotGuildMaster -> {
                i18n.sendError(player, "territory.release_not_master", "Only the guild master can release territory")
            }
            is ReleaseResult.NoTerritory -> {
                i18n.sendError(player, "territory.release_no_territory", "Your guild has no territory")
            }
            is ReleaseResult.ChunkNotFound -> {
                // all では発生しない
            }
        }

        return true
    }

    private fun handleReleaseChunk(player: Player, guildId: Long, chunkNumber: Int, requester: UUID? = player.uniqueId): Boolean {
        val result = territoryService.releaseChunk(guildId, chunkNumber, requester)

        when (result) {
            is ReleaseResult.Success -> {
                i18n.sendSuccess(player, "territory.release_chunk_success",
                    "Released chunk #%d", chunkNumber)
            }
            is ReleaseResult.GuildNotFound -> {
                i18n.sendError(player, "territory.release_no_guild", "You are not in a guild")
            }
            is ReleaseResult.NotGuildMaster -> {
                i18n.sendError(player, "territory.release_not_master", "Only the guild master can release territory")
            }
            is ReleaseResult.NoTerritory -> {
                i18n.sendError(player, "territory.release_no_territory", "Your guild has no territory")
            }
            is ReleaseResult.ChunkNotFound -> {
                i18n.sendError(player, "territory.release_chunk_not_found",
                    "Chunk #%d not found", result.chunkNumber)
            }
        }

        return true
    }

    private fun handleMobSpawn(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId
        val isOp = player.isOp

        // ギルドIDを取得
        var guildId = getPlayerGuildId(player)

        if (guildId == null && isOp) {
            // OPで自分のギルドがない場合、現在地の領地のギルドを取得
            val territoryAtLocation = territoryService.getTerritoryAt(player.location)
            if (territoryAtLocation != null) {
                guildId = territoryAtLocation.guildId
            }
        }

        if (guildId == null) {
            i18n.sendError(player, "territory.mobspawn_no_guild", "You are not in a guild")
            return true
        }

        val territory = territoryService.getTerritory(guildId)
        if (territory == null) {
            i18n.sendError(player, "territory.mobspawn_no_territory", "Your guild has no territory")
            return true
        }

        // 引数なしの場合は現在の状態を表示
        if (args.isEmpty()) {
            val status = if (territory.mobSpawnEnabled) {
                i18n.get(uuid, "territory.mobspawn_status_enabled", "enabled")
            } else {
                i18n.get(uuid, "territory.mobspawn_status_disabled", "disabled")
            }
            i18n.sendInfo(player, "territory.mobspawn_current_status",
                "Monster spawn in territory: %s", status)
            i18n.sendInfo(player, "territory.mobspawn_usage",
                "Usage: /guild territory mobspawn <on|off>")
            return true
        }

        // マスター権限チェック（OPは除外）
        if (!isOp) {
            val membership = guildService.getMembership(uuid)
            if (membership == null || membership.role.name != "MASTER") {
                i18n.sendError(player, "territory.mobspawn_not_master",
                    "Only the guild master can change mob spawn settings")
                return true
            }
        }

        // on/off の設定
        val enabled = when (args[0].lowercase()) {
            "on", "true", "enable", "yes" -> true
            "off", "false", "disable", "no" -> false
            else -> {
                i18n.sendError(player, "territory.mobspawn_invalid_arg",
                    "Invalid argument. Use 'on' or 'off'")
                return true
            }
        }

        if (territoryService.setMobSpawnEnabled(guildId, enabled)) {
            if (enabled) {
                i18n.sendSuccess(player, "territory.mobspawn_enabled",
                    "Monster spawn in territory has been enabled")
            } else {
                i18n.sendSuccess(player, "territory.mobspawn_disabled",
                    "Monster spawn in territory has been disabled")
            }
        } else {
            i18n.sendError(player, "territory.mobspawn_failed",
                "Failed to update mob spawn settings")
        }

        return true
    }

    private fun getPlayerGuildId(player: Player): Long? {
        return guildService.getPlayerGuild(player.uniqueId)?.id
    }

    private fun showUsage(player: Player) {
        val uuid = player.uniqueId
        i18n.sendHeader(player, "territory.usage_header", "=== Territory Commands ===")
        player.sendMessage(Component.text("/guild territory set").color(NamedTextColor.YELLOW)
            .append(i18n.component(player, "territory.usage_set_desc", " - Claim territory at current location", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/guild territory info").color(NamedTextColor.YELLOW)
            .append(i18n.component(player, "territory.usage_info_desc", " - Show territory information", NamedTextColor.GRAY)))
        player.sendMessage(Component.text(i18n.get(uuid, "territory.usage_release_cmd", "/guild territory release <number|all>")).color(NamedTextColor.YELLOW)
            .append(i18n.component(player, "territory.usage_release_desc", " - Release territory", NamedTextColor.GRAY)))
        player.sendMessage(Component.text(i18n.get(uuid, "territory.usage_mobspawn_cmd", "/guild territory mobspawn <on|off>")).color(NamedTextColor.YELLOW)
            .append(i18n.component(player, "territory.usage_mobspawn_desc", " - Toggle monster spawn in territory", NamedTextColor.GRAY)))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("set", "info", "release", "mobspawn")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "release", "abandon" -> {
                    val player = sender as? Player ?: return emptyList()
                    val guildId = getPlayerGuildId(player) ?: return emptyList()
                    val territory = territoryService.getTerritory(guildId) ?: return emptyList()

                    // チャンク番号のリストと "all" を返す
                    val numbers = territory.chunks.map { it.addOrder.toString() }
                    return (numbers + "all").filter { it.startsWith(args[1].lowercase()) }
                }
                "mobspawn", "mob" -> {
                    return listOf("on", "off").filter { it.startsWith(args[1].lowercase()) }
                }
            }
        }
        return emptyList()
    }
}
