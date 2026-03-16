package com.hacklab.minecraft.notoriety.territory.command

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.command.GuildSubCommand
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.service.GuildBaseService
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GuildBaseCommand(
    private val guildBaseService: GuildBaseService,
    private val guildService: GuildService,
    private val i18n: I18nManager
) : GuildSubCommand {

    override val name = "base"
    override val description = "ギルド拠点の設定・管理"
    override val usage = "/guild base <set|remove|info> [--gov]"
    override val aliases = listOf("拠点")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: return true
        val uuid = player.uniqueId

        // --gov フラグ処理でギルドを解決
        val (guild, cleanedArgs) = resolveTargetGuild(player, args, guildService)

        if (guild == null) {
            player.sendError(i18n.get(uuid, "guild.base_not_in_guild", "ギルドに所属していません"))
            return true
        }

        // サブコマンドのディスパッチ
        val subAction = cleanedArgs.firstOrNull()?.lowercase()
        return when (subAction) {
            "set" -> handleSet(player, guild.id)
            "remove" -> handleRemove(player, guild.id)
            "info" -> handleInfo(player, guild.id)
            else -> {
                player.sendInfo(i18n.get(uuid, "guild.base_usage", "使用法: /guild base <set|remove|info>"))
                player.sendInfo(i18n.get(uuid, "guild.base_tp_hint", "テレポートは /guild home base を使用してください"))
                true
            }
        }
    }

    private fun handleSet(player: Player, guildId: Long): Boolean {
        val uuid = player.uniqueId

        // マスター権限チェック
        val membership = guildService.getMembership(player.uniqueId, guildId)
        if (membership == null || membership.role != GuildRole.MASTER) {
            player.sendError(i18n.get(uuid, "guild.base_not_master", "ギルドマスターのみが拠点を設定できます"))
            return true
        }

        // 既存の拠点があるか確認（新規 or 更新メッセージの分岐）
        val existingBase = guildBaseService.getBase(guildId)
        val location = player.location

        guildBaseService.setBase(guildId, location, player.uniqueId)

        val msgKey = if (existingBase != null) "guild.base_updated" else "guild.base_set"
        val msgDefault = if (existingBase != null) "ギルド拠点を更新しました (%s: %d, %d, %d)" else "ギルド拠点を設定しました (%s: %d, %d, %d)"
        player.sendSuccess(i18n.get(uuid, msgKey, msgDefault,
            location.world?.name ?: "?",
            location.blockX, location.blockY, location.blockZ))

        return true
    }

    private fun handleInfo(player: Player, guildId: Long): Boolean {
        val uuid = player.uniqueId
        val base = guildBaseService.getBase(guildId)
        if (base == null) {
            player.sendError(i18n.get(uuid, "guild.base_not_set", "拠点が設定されていません"))
            return true
        }

        player.sendInfo(i18n.get(uuid, "guild.base_info_header", "=== ギルド拠点情報 ==="))
        player.sendInfo(i18n.get(uuid, "guild.base_info_location", "場所: %s (%d, %d, %d)",
            base.worldName, base.x.toInt(), base.y.toInt(), base.z.toInt()))

        val setByPlayer = Bukkit.getOfflinePlayer(base.setBy)
        player.sendInfo(i18n.get(uuid, "guild.base_info_set_by", "設定者: %s",
            setByPlayer.name ?: base.setBy.toString()))

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())
        player.sendInfo(i18n.get(uuid, "guild.base_info_set_at", "設定日時: %s",
            dateFormatter.format(base.createdAt)))

        return true
    }

    private fun handleRemove(player: Player, guildId: Long): Boolean {
        val uuid = player.uniqueId

        // マスター権限チェック
        val membership = guildService.getMembership(player.uniqueId, guildId)
        if (membership == null || membership.role != GuildRole.MASTER) {
            player.sendError(i18n.get(uuid, "guild.base_not_master_remove", "ギルドマスターのみが拠点を削除できます"))
            return true
        }

        val removed = guildBaseService.removeBase(guildId)
        if (removed) {
            player.sendSuccess(i18n.get(uuid, "guild.base_removed", "ギルド拠点を削除しました"))
        } else {
            player.sendError(i18n.get(uuid, "guild.base_not_set", "拠点が設定されていません"))
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val completions = mutableListOf("set", "remove", "info", "--gov")
        return when {
            args.isEmpty() -> completions
            args.size == 1 -> completions.filter { it.startsWith(args[0].lowercase()) }
            args.size == 2 && args[0] == "--gov" -> listOf("set", "remove", "info")
                .filter { it.startsWith(args[1].lowercase()) }
            else -> emptyList()
        }
    }
}
