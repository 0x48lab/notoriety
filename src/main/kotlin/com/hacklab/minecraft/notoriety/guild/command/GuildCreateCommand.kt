package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.core.economy.EconomyService
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GuildCreateCommand(
    private val guildService: GuildService,
    private val economyService: EconomyService,
    private val i18n: I18nManager
) : GuildSubCommand {

    companion object {
        /** ギルド作成費用 */
        const val GUILD_CREATION_COST = 50000.0
    }

    override val name = "create"
    override val description = "Create a new guild"
    override val usage = "/guild create <name> <tag> [description]"
    override val aliases = listOf("new")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        if (args.size < 2) {
            player.sendError(i18n.get(uuid, "guild.create_usage", "Usage: $usage"))
            player.sendInfo(i18n.get(uuid, "guild.create_name_hint", "Name: 3-32 characters"))
            player.sendInfo(i18n.get(uuid, "guild.create_tag_hint", "Tag: 2-4 characters"))
            player.sendInfo(i18n.get(uuid, "guild.create_cost_hint", "Cost: %s", economyService.format(GUILD_CREATION_COST)))
            return true
        }

        val name = args[0]
        val tag = args[1]
        val description = if (args.size > 2) args.drop(2).joinToString(" ") else null

        // 所持金チェック
        val balance = economyService.getBalance(uuid)
        if (balance < GUILD_CREATION_COST) {
            player.sendError(i18n.get(
                uuid,
                "guild.create_insufficient_funds",
                "Insufficient funds. Required: %s, You have: %s",
                economyService.format(GUILD_CREATION_COST),
                economyService.format(balance)
            ))
            return true
        }

        try {
            val guild = guildService.createGuild(uuid, name, tag, description)

            // ギルド作成成功後に費用を徴収
            if (!economyService.withdraw(uuid, GUILD_CREATION_COST)) {
                // 万が一の失敗時（通常は発生しない）
                player.sendError(i18n.get(uuid, "guild.create_withdraw_failed", "Failed to withdraw funds. Please try again."))
                return true
            }

            val successMsg = i18n.get(uuid, "guild.create_success", "Guild created!")
            val costDisplay = i18n.get(uuid, "guild.create_cost_display", "(Cost: %s)", economyService.format(GUILD_CREATION_COST))

            player.sendMessage(Component.text()
                .append(Component.text(successMsg + " ").color(NamedTextColor.GREEN))
                .append(Component.text("[${guild.tag}] ").color(NamedTextColor.GOLD))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))
                .append(Component.text(" ").color(NamedTextColor.GREEN))
                .append(Component.text(costDisplay).color(NamedTextColor.GRAY))
                .build())
        } catch (e: GuildException.InvalidName) {
            player.sendError(i18n.get(uuid, "guild.error_invalid_name", "Invalid guild name"))
        } catch (e: GuildException.InvalidTag) {
            player.sendError(i18n.get(uuid, "guild.error_invalid_tag", "Invalid guild tag"))
        } catch (e: GuildException.NameTaken) {
            player.sendError(i18n.get(uuid, "guild.error_name_taken", "A guild with this name already exists"))
        } catch (e: GuildException.TagTaken) {
            player.sendError(i18n.get(uuid, "guild.error_tag_taken", "A guild with this tag already exists"))
        } catch (e: GuildException.AlreadyInGuild) {
            player.sendError(i18n.get(uuid, "guild.error_already_in_guild", "You are already in a guild"))
        } catch (e: GuildException) {
            player.sendError(i18n.get(uuid, "guild.error_generic", "Failed to create guild: %s", e.message ?: "Unknown error"))
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("<name>")
            2 -> listOf("<tag>")
            3 -> listOf("[description]")
            else -> emptyList()
        }
    }
}
