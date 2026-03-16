package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuildDissolveCommand(
    private val guildService: GuildService
) : GuildSubCommand {

    override val name = "dissolve"
    override val description = "Dissolve your guild (permanently delete)"
    override val usage = "/guild dissolve"
    override val aliases = listOf("disband", "delete")

    // 確認待ちのプレイヤー（10秒間有効）- ギルドIDとタイムスタンプを保存
    private data class PendingDissolve(val guildId: Long, val timestamp: Long)
    private val pendingConfirmations = ConcurrentHashMap<UUID, PendingDissolve>()

    companion object {
        private const val CONFIRMATION_TIMEOUT_MS = 10_000L
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val (guild, cleanedArgs) = resolveTargetGuild(player, args, guildService)
        val useGov = hasGovFlag(args)

        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        // 確認チェック
        val confirmArg = cleanedArgs.getOrNull(0)?.lowercase()

        if (confirmArg == "confirm") {
            val pending = pendingConfirmations[player.uniqueId]
            if (pending == null || System.currentTimeMillis() - pending.timestamp > CONFIRMATION_TIMEOUT_MS) {
                player.sendError("Confirmation expired. Run /guild dissolve again")
                pendingConfirmations.remove(player.uniqueId)
                return true
            }

            // 確認時のギルドIDが一致することを検証
            if (pending.guildId != guild.id) {
                player.sendError("Confirmation expired. Run /guild dissolve again")
                pendingConfirmations.remove(player.uniqueId)
                return true
            }

            pendingConfirmations.remove(player.uniqueId)

            try {
                guildService.dissolveGuild(guild.id, player.uniqueId)

                player.sendMessage(Component.text()
                    .append(Component.text("Guild ").color(NamedTextColor.RED))
                    .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                    .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                    .append(Component.text(" has been dissolved").color(NamedTextColor.RED))
                    .build())
            } catch (e: GuildException.NotMaster) {
                player.sendError("Only the guild master can dissolve the guild")
            } catch (e: GuildException) {
                player.sendError("Failed to dissolve guild: ${e.message}")
            }

            return true
        }

        // 確認プロンプト
        val memberCount = guildService.getMemberCount(guild.id)
        pendingConfirmations[player.uniqueId] = PendingDissolve(guild.id, System.currentTimeMillis())

        player.sendMessage(Component.text()
            .append(Component.text("WARNING: ").color(NamedTextColor.DARK_RED))
            .append(Component.text("This will permanently delete your guild!").color(NamedTextColor.RED))
            .build())
        player.sendMessage(Component.text()
            .append(Component.text("Guild: ").color(NamedTextColor.GRAY))
            .append(Component.text("[${guild.tag}] ${guild.name}").color(NamedTextColor.WHITE))
            .build())
        player.sendMessage(Component.text()
            .append(Component.text("Members affected: ").color(NamedTextColor.GRAY))
            .append(Component.text("$memberCount").color(NamedTextColor.WHITE))
            .build())
        val govSuffix = if (useGov) " --gov" else ""
        player.sendMessage(Component.text()
            .append(Component.text("[Click to Confirm]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/guild dissolve${govSuffix} confirm")))
            .append(Component.text(" or type ").color(NamedTextColor.GRAY))
            .append(Component.text("/guild dissolve${govSuffix} confirm").color(NamedTextColor.WHITE))
            .build())
        player.sendInfo("This confirmation expires in 10 seconds")

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("--gov", "confirm").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
