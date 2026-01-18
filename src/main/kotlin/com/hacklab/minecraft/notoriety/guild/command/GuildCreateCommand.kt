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
        /** 政府ギルド作成フラグ */
        const val GOVERNMENT_FLAG = "--government"
        const val GOVERNMENT_FLAG_SHORT = "-g"
    }

    override val name = "create"
    override val description = "Create a new guild"
    override val usage = "/guild create <name> <tag> [description] [--government]"
    override val aliases = listOf("new")

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        val uuid = player.uniqueId

        if (args.size < 2) {
            player.sendError(i18n.get(uuid, "guild.create_usage", "Usage: $usage"))
            player.sendInfo(i18n.get(uuid, "guild.create_name_hint", "Name: 3-32 characters"))
            player.sendInfo(i18n.get(uuid, "guild.create_tag_hint", "Tag: 2-4 characters"))
            player.sendInfo(i18n.get(uuid, "guild.create_cost_hint", "Cost: %s", economyService.format(GUILD_CREATION_COST)))
            if (player.isOp) {
                player.sendInfo(i18n.get(uuid, "guild.create_government_hint", "OP only: Add --government for unlimited territory"))
            }
            return true
        }

        // --government フラグのチェック
        val isGovernment = args.any { it == GOVERNMENT_FLAG || it == GOVERNMENT_FLAG_SHORT }
        val filteredArgs = args.filter { it != GOVERNMENT_FLAG && it != GOVERNMENT_FLAG_SHORT }

        // 政府ギルド作成にはOP権限が必要
        if (isGovernment && !player.isOp) {
            player.sendError(i18n.get(uuid, "guild.create_government_op_only", "Only server operators can create government guilds"))
            return true
        }

        val name = filteredArgs[0]
        val tag = filteredArgs[1]
        val description = if (filteredArgs.size > 2) filteredArgs.drop(2).joinToString(" ") else null

        // 政府ギルドは費用無料
        if (!isGovernment) {
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
        }

        try {
            val guild = if (isGovernment) {
                guildService.createGovernmentGuild(uuid, name, tag, description)
            } else {
                guildService.createGuild(uuid, name, tag, description)
            }

            // 政府ギルド以外は費用を徴収
            if (!isGovernment) {
                if (!economyService.withdraw(uuid, GUILD_CREATION_COST)) {
                    // 万が一の失敗時（通常は発生しない）
                    player.sendError(i18n.get(uuid, "guild.create_withdraw_failed", "Failed to withdraw funds. Please try again."))
                    return true
                }
            }

            val successMsg = if (isGovernment) {
                i18n.get(uuid, "guild.create_government_success", "Government guild created!")
            } else {
                i18n.get(uuid, "guild.create_success", "Guild created!")
            }

            val messageBuilder = Component.text()
                .append(Component.text(successMsg + " ").color(if (isGovernment) NamedTextColor.GOLD else NamedTextColor.GREEN))
                .append(Component.text("[${guild.tag}] ").color(guild.tagColor.namedTextColor))
                .append(Component.text(guild.name).color(NamedTextColor.WHITE))

            if (isGovernment) {
                messageBuilder.append(Component.text(" [政府]").color(NamedTextColor.GOLD))
            } else {
                val costDisplay = i18n.get(uuid, "guild.create_cost_display", "(Cost: %s)", economyService.format(GUILD_CREATION_COST))
                messageBuilder.append(Component.text(" ").color(NamedTextColor.GREEN))
                    .append(Component.text(costDisplay).color(NamedTextColor.GRAY))
            }

            player.sendMessage(messageBuilder.build())
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
