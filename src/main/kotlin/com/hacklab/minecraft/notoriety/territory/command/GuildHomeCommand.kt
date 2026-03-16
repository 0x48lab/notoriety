package com.hacklab.minecraft.notoriety.territory.command

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.command.GuildSubCommand
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.SigilTeleportEvent
import com.hacklab.minecraft.notoriety.territory.model.GuildBase
import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.service.GuildBaseService
import com.hacklab.minecraft.notoriety.territory.service.SigilService
import com.hacklab.minecraft.notoriety.territory.service.TeleportResult
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * ギルドホームコマンド - シギル・拠点へのテレポート
 * /guild home [シギル名|base|拠点]
 */
class GuildHomeCommand(
    private val sigilService: SigilService,
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val guildBaseService: GuildBaseService,
    private val i18n: I18nManager
) : GuildSubCommand {

    companion object {
        val BASE_KEYWORDS = listOf("base", "拠点")
    }

    override val name = "home"
    override val description = "Teleport to a guild sigil or base"
    override val usage = "/guild home [sigil_name|base]"
    override val aliases = listOf("h", "tp", "warp", "ホーム")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        val (guild, cleanedArgs) = resolveTargetGuild(player, args, guildService)
        if (guild == null) {
            player.sendError(i18n.get(uuid, "home.not_in_guild", "You are not in a guild"))
            return true
        }

        val territory = territoryService.getTerritory(guild.id)
        val base = guildBaseService.getBase(guild.id)
        val sigilCount = territory?.sigilCount ?: 0
        val hasBase = base != null
        val totalDestinations = sigilCount + if (hasBase) 1 else 0

        if (totalDestinations == 0) {
            player.sendError(i18n.get(uuid, "guild.home_no_destinations", "テレポート先がありません"))
            return true
        }

        // 引数なし
        if (cleanedArgs.isEmpty()) {
            if (totalDestinations == 1) {
                // 1件のみ → 自動テレポート
                if (hasBase && sigilCount == 0) {
                    return teleportToBase(player, guild.id)
                } else {
                    val sigil = territory!!.sigils.first()
                    return teleportToSigil(player, guild.id, sigil.name)
                }
            } else {
                // 複数 → 一覧表示
                showTeleportList(player, territory, base)
                return true
            }
        }

        // 引数あり
        val inputName = cleanedArgs.joinToString(" ")
        if (BASE_KEYWORDS.any { it.equals(inputName, ignoreCase = true) }) {
            return teleportToBase(player, guild.id)
        }
        return teleportToSigil(player, guild.id, inputName)
    }

    private fun teleportToBase(player: Player, guildId: Long): Boolean {
        val uuid = player.uniqueId
        val result = guildBaseService.teleportToBase(player, guildId)

        when (result) {
            is TeleportResult.BaseSuccess -> {
                val base = result.base
                player.sendSuccess(i18n.get(uuid, "guild.base_teleported", "拠点にテレポートしました (%s: %d, %d, %d)",
                    base.worldName, base.x.toInt(), base.y.toInt(), base.z.toInt()))
            }
            is TeleportResult.NoBase -> {
                player.sendError(i18n.get(uuid, "guild.base_not_set", "拠点が設定されていません"))
            }
            is TeleportResult.OnCooldown -> {
                player.sendError(i18n.get(uuid, "guild.base_cooldown", "テレポートのクールダウン中です（残り%d秒）",
                    result.remainingSeconds))
            }
            is TeleportResult.WorldNotFound -> {
                player.sendError(i18n.get(uuid, "guild.base_world_not_found", "拠点のワールドが見つかりません"))
            }
            is TeleportResult.NoSafeLocation -> {
                player.sendError(i18n.get(uuid, "guild.base_no_safe_location", "安全なテレポート先が見つかりません"))
            }
            else -> {
                player.sendError("Teleport failed")
            }
        }
        return true
    }

    private fun teleportToSigil(player: Player, guildId: Long, sigilName: String): Boolean {
        val uuid = player.uniqueId

        val result = sigilService.teleportToSigilByName(player, guildId, sigilName)

        when (result) {
            is TeleportResult.Success -> {
                player.sendSuccess("シギル「${result.sigil.name}」にテレポートしました")

                // イベント発火
                Bukkit.getPluginManager().callEvent(
                    SigilTeleportEvent(
                        player = player,
                        sigil = result.sigil,
                        guildId = guildId
                    )
                )
            }
            is TeleportResult.SigilNotFound -> {
                player.sendError("シギルが見つかりません")
            }
            is TeleportResult.SigilNameNotFound -> {
                player.sendError("シギル「${result.name}」が見つかりません")
                // 候補を表示
                val guild = guildService.getGuild(guildId)
                val territory = guild?.let { territoryService.getTerritory(it.id) }
                val base = guild?.let { guildBaseService.getBase(it.id) }
                if ((territory != null && territory.sigilCount > 0) || base != null) {
                    player.sendMessage(Component.text(
                        i18n.get(uuid, "guild.home_available_destinations", "利用可能なテレポート先:")
                    ).color(NamedTextColor.GRAY))
                    if (base != null) {
                        val baseTag = i18n.get(uuid, "guild.home_base_tag", "[拠点]")
                        val baseKeyword = i18n.get(uuid, "guild.home_base_keyword", "base")
                        player.sendMessage(Component.text("  - ").color(NamedTextColor.GRAY)
                            .append(Component.text(baseTag).color(NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(" ($baseKeyword)").color(NamedTextColor.GRAY)))
                    }
                    territory?.sigils?.forEach { sigil ->
                        player.sendMessage(Component.text("  - ${sigil.name}").color(NamedTextColor.AQUA))
                    }
                }
            }
            is TeleportResult.NotInGuild -> {
                player.sendError("ギルドに所属していません")
            }
            is TeleportResult.NoTerritory -> {
                player.sendError("ギルドに領地がありません")
            }
            is TeleportResult.NoSafeLocation -> {
                player.sendError("安全なテレポート位置が見つかりません")
            }
            is TeleportResult.OnCooldown -> {
                player.sendError("クールダウン中です（残り${result.remainingSeconds}秒）")
            }
            is TeleportResult.WorldNotFound -> {
                player.sendError("ワールドが見つかりません")
            }
            else -> {
                player.sendError("テレポートに失敗しました")
            }
        }

        return true
    }

    private fun showTeleportList(player: Player, territory: GuildTerritory?, base: GuildBase?) {
        val uuid = player.uniqueId
        player.sendMessage(Component.text("=== テレポート先を選択 ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("使用法: /guild home <名前>").color(NamedTextColor.GRAY))
        player.sendMessage(Component.empty())

        // 拠点エントリー（先頭に表示）
        if (base != null) {
            val baseTag = i18n.get(uuid, "guild.home_base_tag", "[拠点]")
            player.sendMessage(Component.text("  ★ ").color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(baseTag).color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" (${base.worldName}: ${base.x.toInt()}, ${base.y.toInt()}, ${base.z.toInt()})").color(NamedTextColor.GRAY)))
        }

        // シギルエントリー
        territory?.sigils?.forEach { sigil ->
            val chunkCount = territory.getChunksForSigil(sigil.id).size
            player.sendMessage(Component.text("  ● ").color(NamedTextColor.YELLOW)
                .append(Component.text(sigil.name).color(NamedTextColor.AQUA))
                .append(Component.text(" (${chunkCount}チャンク)").color(NamedTextColor.GRAY)))
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        val cleanedArgs = stripGovFlag(args)

        if (cleanedArgs.size == 1) {
            val (guild, _) = resolveTargetGuild(player, args, guildService)
            if (guild == null) return emptyList()

            val territory = territoryService.getTerritory(guild.id)
            val base = guildBaseService.getBase(guild.id)
            val input = cleanedArgs[0].lowercase()

            val completions = mutableListOf<String>()

            // 拠点キーワード（ロケールに応じたキーワードのみ表示）
            if (base != null) {
                val localeKeyword = i18n.get(player.uniqueId, "guild.home_base_keyword", "base")
                if (localeKeyword.lowercase().startsWith(input)) {
                    completions.add(localeKeyword)
                }
            }

            // シギル名
            if (territory != null) {
                completions.addAll(territory.sigils.map { it.name }
                    .filter { it.lowercase().startsWith(input) })
            }

            // --gov フラグ
            if (args.size == 1 && !hasGovFlag(args)) {
                completions.addAll(listOf("--gov").filter { it.startsWith(input) })
            }

            return completions
        }

        return emptyList()
    }
}
