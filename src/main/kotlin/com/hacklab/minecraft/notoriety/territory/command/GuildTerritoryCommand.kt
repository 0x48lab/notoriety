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
            else -> {
                showUsage(player)
                true
            }
        }
    }

    private fun handleSet(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId
        val location = player.location

        // オプションでシギル名を指定可能（飛び地の場合）
        val sigilName = if (args.isNotEmpty()) args.joinToString(" ") else null

        val guildId = getPlayerGuildId(player) ?: run {
            player.sendError(i18n.get(uuid, "territory.claim_no_guild", "You must be in a guild to claim territory"))
            return true
        }

        val result = territoryService.claimTerritory(
            guildId = guildId,
            location = location,
            requester = uuid,
            sigilName = sigilName
        )

        when (result) {
            is ClaimResult.Success -> {
                val guild = guildService.getPlayerGuild(uuid)
                val territory = territoryService.getTerritory(guild?.id ?: 0)
                val totalChunks = territory?.chunkCount ?: 1

                // 基本メッセージ
                player.sendSuccess(i18n.get(
                    uuid,
                    "territory.claim_success",
                    "Territory claimed! (%d/%d chunks)",
                    totalChunks,
                    territoryService.calculateAllowedChunks(guildService.getMemberCount(guild?.id ?: 0))
                ))

                // シギル関連メッセージ
                if (result.isNewSigil && result.sigil != null) {
                    player.sendMessage(Component.text("新しいシギル「${result.sigil.name}」を作成しました")
                        .color(NamedTextColor.GREEN))

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
                    player.sendMessage(Component.text("シギル「${result.sigil.name}」の領地に追加しました")
                        .color(NamedTextColor.GRAY))
                }

                // グループマージがあった場合
                if (result.mergedSigilIds.isNotEmpty()) {
                    player.sendMessage(Component.text("${result.mergedSigilIds.size}個のシギルが統合されました")
                        .color(NamedTextColor.YELLOW))
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
                player.sendError(i18n.get(uuid, "territory.claim_no_guild", "You must be in a guild to claim territory"))
            }
            is ClaimResult.NotGuildMaster -> {
                player.sendError(i18n.get(uuid, "territory.claim_not_master", "Only the guild master can claim territory"))
            }
            is ClaimResult.NotEnoughMembers -> {
                val guild = guildService.getPlayerGuild(uuid)
                val memberCount = guildService.getMemberCount(guild?.id ?: 0)
                player.sendError(i18n.get(
                    uuid,
                    "territory.claim_not_enough_members",
                    "You need at least %d members to claim territory (current: %d)",
                    TerritoryService.MIN_MEMBERS_FOR_TERRITORY,
                    memberCount
                ))
            }
            is ClaimResult.MaxChunksReached -> {
                val maxChunks = territoryService.calculateAllowedChunks(
                    guildService.getMemberCount(getPlayerGuildId(player) ?: 0)
                )
                player.sendError(i18n.get(
                    uuid,
                    "territory.claim_max_chunks",
                    "Your guild has reached the maximum territory limit (%d chunks)",
                    maxChunks
                ))
            }
            is ClaimResult.MemberChunkLimitReached -> {
                val guild = guildService.getPlayerGuild(uuid)
                val memberCount = guildService.getMemberCount(guild?.id ?: 0)
                val maxChunks = territoryService.calculateAllowedChunks(memberCount)
                player.sendError(i18n.get(
                    uuid,
                    "territory.claim_member_limit",
                    "Your current member count (%d) only allows %d chunks",
                    memberCount,
                    maxChunks
                ))
            }
            is ClaimResult.OverlapOtherGuild -> {
                player.sendError(i18n.get(
                    uuid,
                    "territory.claim_overlap",
                    "This location overlaps with %s's territory",
                    result.guildName
                ))
            }
            is ClaimResult.NotInGuild -> {
                player.sendError(i18n.get(uuid, "territory.claim_no_guild", "You must be in a guild to claim territory"))
            }
            is ClaimResult.AlreadyClaimed -> {
                player.sendError("このチャンクは既にあなたのギルドの領地です")
            }
            is ClaimResult.InvalidSigilName -> {
                player.sendError("無効なシギル名: ${result.reason}")
            }
            is ClaimResult.SigilNameAlreadyExists -> {
                player.sendError("シギル名「${result.name}」は既に使用されています")
            }
        }

        return true
    }

    private fun handleInfo(player: Player): Boolean {
        val uuid = player.uniqueId
        val guild = guildService.getPlayerGuild(uuid)

        if (guild == null) {
            player.sendError(i18n.get(uuid, "territory.info_not_in_guild", "You are not in a guild"))
            return true
        }

        val territory = territoryService.getTerritory(guild.id)
        val memberCount = guildService.getMemberCount(guild.id)
        val maxChunks = if (guild.isGovernment) "無制限" else territoryService.calculateAllowedChunks(memberCount).toString()

        player.sendMessage(Component.text(i18n.get(uuid, "territory.info_header", "=== %s Territory Info ===", guild.name))
            .color(NamedTextColor.GOLD))

        if (guild.isGovernment) {
            player.sendMessage(Component.text("  [政府ギルド - 領地制限なし]").color(NamedTextColor.LIGHT_PURPLE))
        }

        if (territory == null || territory.chunkCount == 0) {
            player.sendInfo(i18n.get(uuid, "territory.info_no_territory", "Your guild has no territory"))
        } else {
            player.sendInfo("チャンク数: ${territory.chunkCount} / $maxChunks")

            // シギル一覧を表示
            if (territory.sigilCount > 0) {
                player.sendMessage(Component.text("--- シギル一覧 ---").color(NamedTextColor.AQUA))
                territory.sigils.forEach { sigil ->
                    val chunkCount = territory.getChunksForSigil(sigil.id).size
                    player.sendMessage(Component.text(
                        "  ● ${sigil.name} (${chunkCount}チャンク) - ${sigil.worldName}"
                    ).color(NamedTextColor.AQUA))
                }
            }

            // チャンク一覧を番号順で表示
            player.sendMessage(Component.text("--- チャンク一覧 ---").color(NamedTextColor.GRAY))
            territory.chunks.sortedBy { it.addOrder }.forEach { chunk ->
                val isCurrentChunk = chunk.containsLocation(player.location)
                val marker = if (isCurrentChunk) " §a← 現在地" else ""
                val sigilName = chunk.sigilId?.let { sigilId ->
                    territory.getSigilById(sigilId)?.name
                } ?: "?"
                player.sendMessage(Component.text(
                    "  #${chunk.addOrder}: (${chunk.chunkX}, ${chunk.chunkZ}) [${sigilName}]$marker"
                ).color(if (isCurrentChunk) NamedTextColor.GREEN else NamedTextColor.YELLOW))
            }
        }

        if (!guild.isGovernment) {
            player.sendInfo(i18n.get(
                uuid,
                "territory.info_allowed_chunks",
                "Allowed chunks: %d (members: %d)",
                territoryService.calculateAllowedChunks(memberCount),
                memberCount
            ))
        }

        return true
    }

    private fun handleRelease(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId

        // 引数なしの場合は使用方法を表示
        if (args.isEmpty()) {
            player.sendMessage(Component.text("使用法: /guild territory release <番号|all>").color(NamedTextColor.YELLOW))
            player.sendMessage(Component.text("  番号: 特定のチャンクを解放（/guild territory info で確認）").color(NamedTextColor.GRAY))
            player.sendMessage(Component.text("  all: 全てのチャンクを解放").color(NamedTextColor.GRAY))
            return true
        }

        val guildId = getPlayerGuildId(player) ?: run {
            player.sendError(i18n.get(uuid, "territory.release_no_guild", "You are not in a guild"))
            return true
        }

        // "all" の場合は全解放
        if (args[0].lowercase() == "all") {
            return handleReleaseAll(player, guildId)
        }

        // 番号指定の場合
        val chunkNumber = args[0].toIntOrNull()
        if (chunkNumber == null) {
            player.sendError("無効な番号です: ${args[0]}")
            return true
        }

        return handleReleaseChunk(player, guildId, chunkNumber)
    }

    private fun handleReleaseAll(player: Player, guildId: Long): Boolean {
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()

        // 確認チェック
        val lastConfirm = releaseConfirmations[uuid]
        if (lastConfirm == null || now - lastConfirm > CONFIRM_TIMEOUT_MS) {
            // 初回または期限切れ - 確認を求める
            releaseConfirmations[uuid] = now
            player.sendMessage(Component.text(i18n.get(
                uuid,
                "territory.release_confirm",
                "§cWarning: §eRelease all territory? §7(Run again within 5 seconds to confirm)"
            )))
            return true
        }

        // 確認済み - 実行
        releaseConfirmations.remove(uuid)

        val result = territoryService.releaseAllTerritory(guildId, uuid)

        when (result) {
            is ReleaseResult.Success -> {
                player.sendSuccess(i18n.get(
                    uuid,
                    "territory.release_success",
                    "Released all territory (%d chunks)",
                    result.releasedChunkCount
                ))
            }
            is ReleaseResult.GuildNotFound -> {
                player.sendError(i18n.get(uuid, "territory.release_no_guild", "You are not in a guild"))
            }
            is ReleaseResult.NotGuildMaster -> {
                player.sendError(i18n.get(uuid, "territory.release_not_master", "Only the guild master can release territory"))
            }
            is ReleaseResult.NoTerritory -> {
                player.sendError(i18n.get(uuid, "territory.release_no_territory", "Your guild has no territory"))
            }
            is ReleaseResult.ChunkNotFound -> {
                // all では発生しない
            }
        }

        return true
    }

    private fun handleReleaseChunk(player: Player, guildId: Long, chunkNumber: Int): Boolean {
        val uuid = player.uniqueId

        val result = territoryService.releaseChunk(guildId, chunkNumber, uuid)

        when (result) {
            is ReleaseResult.Success -> {
                player.sendSuccess("チャンク #$chunkNumber を解放しました")
            }
            is ReleaseResult.GuildNotFound -> {
                player.sendError(i18n.get(uuid, "territory.release_no_guild", "You are not in a guild"))
            }
            is ReleaseResult.NotGuildMaster -> {
                player.sendError(i18n.get(uuid, "territory.release_not_master", "Only the guild master can release territory"))
            }
            is ReleaseResult.NoTerritory -> {
                player.sendError(i18n.get(uuid, "territory.release_no_territory", "Your guild has no territory"))
            }
            is ReleaseResult.ChunkNotFound -> {
                player.sendError("チャンク #${result.chunkNumber} が見つかりません")
            }
        }

        return true
    }

    private fun getPlayerGuildId(player: Player): Long? {
        return guildService.getPlayerGuild(player.uniqueId)?.id
    }

    private fun showUsage(player: Player) {
        player.sendMessage(Component.text("=== Territory Commands ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("/guild territory set").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Claim territory at current location").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/guild territory info").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Show territory information").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/guild territory release <番号|all>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Release territory").color(NamedTextColor.GRAY)))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("set", "info", "release")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].lowercase() in listOf("release", "abandon")) {
            val player = sender as? Player ?: return emptyList()
            val guildId = getPlayerGuildId(player) ?: return emptyList()
            val territory = territoryService.getTerritory(guildId) ?: return emptyList()

            // チャンク番号のリストと "all" を返す
            val numbers = territory.chunks.map { it.addOrder.toString() }
            return (numbers + "all").filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }
}
