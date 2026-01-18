package com.hacklab.minecraft.notoriety.territory.command

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.command.GuildSubCommand
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.SigilRenameEvent
import com.hacklab.minecraft.notoriety.territory.service.RenameResult
import com.hacklab.minecraft.notoriety.territory.service.SigilService
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * ギルドシギルコマンド
 * /guild sigil <list|rename>
 */
class GuildSigilCommand(
    private val sigilService: SigilService,
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val i18n: I18nManager
) : GuildSubCommand {

    override val name = "sigil"
    override val description = "Manage guild sigils"
    override val usage = "/guild sigil <list|rename>"
    override val aliases = listOf("s", "シギル")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        if (args.isEmpty()) {
            showUsage(player)
            return true
        }

        return when (args[0].lowercase()) {
            "list", "一覧" -> handleList(player)
            "rename", "名前変更" -> handleRename(player, args.drop(1).toTypedArray())
            else -> {
                showUsage(player)
                true
            }
        }
    }

    private fun handleList(player: Player): Boolean {
        val uuid = player.uniqueId
        val guild = guildService.getPlayerGuild(uuid)

        if (guild == null) {
            player.sendError(i18n.get(uuid, "sigil.list_not_in_guild", "You are not in a guild"))
            return true
        }

        val territory = territoryService.getTerritory(guild.id)

        if (territory == null || territory.sigilCount == 0) {
            player.sendInfo("ギルドにシギルがありません")
            return true
        }

        player.sendMessage(Component.text("=== ${guild.name} のシギル一覧 ===").color(NamedTextColor.GOLD))

        territory.sigils.forEachIndexed { index, sigil ->
            val chunkCount = territory.getChunksForSigil(sigil.id).size
            val location = sigil.location
            val coordsText = if (location != null) {
                "(${location.blockX}, ${location.blockY}, ${location.blockZ})"
            } else {
                "(不明)"
            }

            player.sendMessage(Component.text("${index + 1}. ").color(NamedTextColor.YELLOW)
                .append(Component.text(sigil.name).color(NamedTextColor.AQUA))
                .append(Component.text(" - ${chunkCount}チャンク $coordsText").color(NamedTextColor.GRAY)))
        }

        player.sendMessage(Component.text("テレポート: /guild home <シギル名>").color(NamedTextColor.GRAY))

        return true
    }

    private fun handleRename(player: Player, args: Array<out String>): Boolean {
        val uuid = player.uniqueId

        if (args.size < 2) {
            player.sendMessage(Component.text("使用法: /guild sigil rename <旧名> <新名>").color(NamedTextColor.YELLOW))
            return true
        }

        val guild = guildService.getPlayerGuild(uuid)
        if (guild == null) {
            player.sendError("ギルドに所属していません")
            return true
        }

        val territory = territoryService.getTerritory(guild.id)
        if (territory == null) {
            player.sendError("ギルドに領地がありません")
            return true
        }

        val oldName = args[0]
        val newName = args.drop(1).joinToString(" ")

        // シギルを名前で検索
        val sigil = territory.getSigilByName(oldName)
        if (sigil == null) {
            player.sendError("シギル「$oldName」が見つかりません")
            return true
        }

        val result = sigilService.renameSigil(sigil.id, newName, guild.id, uuid)

        when (result) {
            is RenameResult.Success -> {
                player.sendSuccess("シギル「${result.oldName}」を「${result.newName}」に変更しました")

                // イベント発火
                Bukkit.getPluginManager().callEvent(
                    SigilRenameEvent(
                        guildId = guild.id,
                        guildName = guild.name,
                        sigil = result.sigil,
                        oldName = result.oldName,
                        newName = result.newName,
                        renamedBy = uuid
                    )
                )
            }
            is RenameResult.SigilNotFound -> {
                player.sendError("シギルが見つかりません")
            }
            is RenameResult.NotGuildMaster -> {
                player.sendError("ギルドマスターのみがシギル名を変更できます")
            }
            is RenameResult.NotInGuild -> {
                player.sendError("ギルドに所属していません")
            }
            is RenameResult.InvalidName -> {
                player.sendError("無効な名前: ${result.reason}")
            }
            is RenameResult.NameAlreadyExists -> {
                player.sendError("シギル名「${result.name}」は既に使用されています")
            }
            is RenameResult.NoTerritory -> {
                player.sendError("ギルドに領地がありません")
            }
        }

        return true
    }

    private fun showUsage(player: Player) {
        player.sendMessage(Component.text("=== Sigil Commands ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("/guild sigil list").color(NamedTextColor.YELLOW)
            .append(Component.text(" - シギル一覧を表示").color(NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/guild sigil rename <旧名> <新名>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - シギル名を変更").color(NamedTextColor.GRAY)))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()

        if (args.size == 1) {
            return listOf("list", "rename")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].lowercase() in listOf("rename", "名前変更")) {
            // シギル名の補完
            val guild = guildService.getPlayerGuild(player.uniqueId) ?: return emptyList()
            val territory = territoryService.getTerritory(guild.id) ?: return emptyList()
            return territory.sigils.map { it.name }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }

        return emptyList()
    }
}
