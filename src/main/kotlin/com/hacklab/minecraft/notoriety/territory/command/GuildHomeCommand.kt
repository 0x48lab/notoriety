package com.hacklab.minecraft.notoriety.territory.command

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.command.GuildSubCommand
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.SigilTeleportEvent
import com.hacklab.minecraft.notoriety.territory.service.SigilService
import com.hacklab.minecraft.notoriety.territory.service.TeleportResult
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * ギルドホームコマンド - シギルへのテレポート
 * /guild home [シギル名]
 */
class GuildHomeCommand(
    private val sigilService: SigilService,
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val i18n: I18nManager
) : GuildSubCommand {

    override val name = "home"
    override val description = "Teleport to a guild sigil"
    override val usage = "/guild home [sigil_name]"
    override val aliases = listOf("h", "tp", "warp", "ホーム")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        val guild = guildService.getPlayerGuild(uuid)
        if (guild == null) {
            player.sendError(i18n.get(uuid, "home.not_in_guild", "You are not in a guild"))
            return true
        }

        val territory = territoryService.getTerritory(guild.id)
        if (territory == null || territory.sigilCount == 0) {
            player.sendError(i18n.get(uuid, "home.no_sigils", "Your guild has no sigils"))
            return true
        }

        // シギル名が指定されていない場合
        if (args.isEmpty()) {
            if (territory.sigilCount == 1) {
                // 1つしかない場合は自動でそのシギルにテレポート
                val sigil = territory.sigils.first()
                return teleportToSigil(player, guild.id, sigil.name)
            } else {
                // 複数ある場合はリストを表示
                showSigilList(player, territory)
                return true
            }
        }

        // シギル名が指定されている場合
        val sigilName = args.joinToString(" ")
        return teleportToSigil(player, guild.id, sigilName)
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
                if (territory != null && territory.sigilCount > 0) {
                    player.sendMessage(Component.text("利用可能なシギル:").color(NamedTextColor.GRAY))
                    territory.sigils.forEach { sigil ->
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
        }

        return true
    }

    private fun showSigilList(player: Player, territory: com.hacklab.minecraft.notoriety.territory.model.GuildTerritory) {
        player.sendMessage(Component.text("=== テレポート先を選択 ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("使用法: /guild home <シギル名>").color(NamedTextColor.GRAY))
        player.sendMessage(Component.text("").color(NamedTextColor.GRAY))

        territory.sigils.forEach { sigil ->
            val chunkCount = territory.getChunksForSigil(sigil.id).size
            player.sendMessage(Component.text("  ● ").color(NamedTextColor.YELLOW)
                .append(Component.text(sigil.name).color(NamedTextColor.AQUA))
                .append(Component.text(" (${chunkCount}チャンク)").color(NamedTextColor.GRAY)))
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()

        if (args.size == 1) {
            // シギル名の補完
            val guild = guildService.getPlayerGuild(player.uniqueId) ?: return emptyList()
            val territory = territoryService.getTerritory(guild.id) ?: return emptyList()
            return territory.sigils.map { it.name }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }

        return emptyList()
    }
}
