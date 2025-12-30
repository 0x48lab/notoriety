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

    // 確認待ちのプレイヤー（10秒間有効）
    private val pendingConfirmations = ConcurrentHashMap<UUID, Long>()

    companion object {
        private const val CONFIRMATION_TIMEOUT_MS = 10_000L
    }

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player

        val guild = guildService.getPlayerGuild(player.uniqueId)
        if (guild == null) {
            player.sendError("You are not in a guild")
            return true
        }

        // 確認チェック
        val confirmArg = args.getOrNull(0)?.lowercase()

        if (confirmArg == "confirm") {
            val confirmTime = pendingConfirmations[player.uniqueId]
            if (confirmTime == null || System.currentTimeMillis() - confirmTime > CONFIRMATION_TIMEOUT_MS) {
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
        pendingConfirmations[player.uniqueId] = System.currentTimeMillis()

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
        player.sendMessage(Component.text()
            .append(Component.text("[Click to Confirm]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/guild dissolve confirm")))
            .append(Component.text(" or type ").color(NamedTextColor.GRAY))
            .append(Component.text("/guild dissolve confirm").color(NamedTextColor.WHITE))
            .build())
        player.sendInfo("This confirmation expires in 10 seconds")

        return true
    }
}
