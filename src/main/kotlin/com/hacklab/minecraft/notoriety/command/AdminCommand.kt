package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class AdminCommand(private val plugin: Notoriety) : SubCommand {

    private val i18n get() = plugin.i18nManager

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("notoriety.admin")) {
            sender.sendMessage(i18n.get("message.no_permission", "Permission denied"))
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        // listgray サブコマンド
        if (args[0].lowercase() == "listgray") {
            return listGrayPlayers(sender)
        }

        // listred サブコマンド
        if (args[0].lowercase() == "listred") {
            return listRedPlayers(sender)
        }

        // guildtag テストコマンド
        if (args[0].lowercase() == "guildtag") {
            return testGuildTag(sender, args.drop(1).toTypedArray())
        }

        if (args.size < 4) {
            showUsage(sender)
            return true
        }

        val target = Bukkit.getOfflinePlayer(args[0]).uniqueId
        val data = plugin.playerManager.getPlayer(target) ?: run {
            sender.sendMessage(i18n.get("message.player_not_found", "Player not found"))
            return true
        }

        val param = args[1].lowercase()
        val operation = args[2].lowercase()
        val value = args[3].toIntOrNull() ?: run {
            sender.sendMessage(i18n.get("message.invalid_amount", "Invalid value"))
            return true
        }

        when (param) {
            "alignment" -> if (operation == "set") data.alignment = value.coerceIn(-1000, 1000)
                           else data.addAlignment(value)
            "fame" -> if (operation == "set") data.fame = value.coerceIn(0, 1000)
                      else data.addFame(value)
            "pk" -> if (operation == "set") data.pkCount = maxOf(0, value)
                    else data.pkCount = maxOf(0, data.pkCount + value)
            else -> {
                sender.sendMessage("Unknown parameter: $param")
                return true
            }
        }

        Bukkit.getPlayer(target)?.let {
            plugin.reputationService.updateDisplay(it)
        }
        sender.sendMessage(i18n.get("admin.updated", "Updated %s's %s").format(args[0], param))
        return true
    }

    private fun listGrayPlayers(sender: CommandSender): Boolean {
        val grayPlayers = plugin.playerManager.findAllGrayPlayers()

        if (grayPlayers.isEmpty()) {
            sender.sendMessage(Component.text(i18n.get("admin.gray_list_empty", "No gray players found")).color(NamedTextColor.GRAY))
            return true
        }

        val title = i18n.get("admin.gray_list_title", "=== Gray Players (%d) ===").format(grayPlayers.size)
        sender.sendMessage(Component.text(title).color(NamedTextColor.GRAY))

        val onlineText = i18n.get("admin.status_online", "Online")
        val offlineText = i18n.get("admin.status_offline", "Offline")

        grayPlayers.forEach { data ->
            val playerName = Bukkit.getOfflinePlayer(data.uuid).name ?: data.uuid.toString()
            val isOnline = plugin.playerManager.isOnline(data.uuid)
            val statusText = if (isOnline) onlineText else offlineText
            val statusColor = if (isOnline) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

            sender.sendMessage(
                Component.text("- $playerName ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("[$statusText] ")
                        .color(statusColor))
                    .append(Component.text("[Alignment: ${data.alignment}, Fame: ${data.fame}]")
                        .color(NamedTextColor.DARK_GRAY))
            )
        }
        return true
    }

    private fun testGuildTag(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /noty admin guildtag <player> <tag|clear>").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("Example: /noty admin guildtag Steve BC").color(NamedTextColor.GRAY))
            sender.sendMessage(Component.text("Example: /noty admin guildtag Steve clear").color(NamedTextColor.GRAY))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /noty admin guildtag <player> <tag|clear>").color(NamedTextColor.RED))
            return true
        }

        val targetPlayer = Bukkit.getPlayer(args[0])
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player ${args[0]} is not online").color(NamedTextColor.RED))
            return true
        }

        val tag = args[1]
        val teamManager = plugin.reputationService.teamManager

        if (tag.lowercase() == "clear") {
            teamManager.setTestGuildTag(targetPlayer.uniqueId, null)
            plugin.reputationService.updateDisplay(targetPlayer)
            sender.sendMessage(Component.text("Cleared guild tag for ${targetPlayer.name}").color(NamedTextColor.GREEN))
        } else {
            if (tag.length > 5) {
                sender.sendMessage(Component.text("Tag must be 5 characters or less").color(NamedTextColor.RED))
                return true
            }
            teamManager.setTestGuildTag(targetPlayer.uniqueId, tag)
            plugin.reputationService.updateDisplay(targetPlayer)
            sender.sendMessage(
                Component.text("Set guild tag for ${targetPlayer.name} to ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text("[$tag]").color(NamedTextColor.GOLD))
            )
        }
        return true
    }

    private fun listRedPlayers(sender: CommandSender): Boolean {
        val redPlayers = plugin.playerManager.findAllRedPlayers()

        if (redPlayers.isEmpty()) {
            sender.sendMessage(Component.text(i18n.get("admin.red_list_empty", "No red players found")).color(NamedTextColor.RED))
            return true
        }

        val title = i18n.get("admin.red_list_title", "=== Red Players (%d) ===").format(redPlayers.size)
        sender.sendMessage(Component.text(title).color(NamedTextColor.RED))

        val onlineText = i18n.get("admin.status_online", "Online")
        val offlineText = i18n.get("admin.status_offline", "Offline")

        redPlayers.forEach { data ->
            val playerName = Bukkit.getOfflinePlayer(data.uuid).name ?: data.uuid.toString()
            val isOnline = plugin.playerManager.isOnline(data.uuid)
            val statusText = if (isOnline) onlineText else offlineText
            val statusColor = if (isOnline) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

            sender.sendMessage(
                Component.text("- $playerName ")
                    .color(NamedTextColor.RED)
                    .append(Component.text("[$statusText] ")
                        .color(statusColor))
                    .append(Component.text("[PK: ${data.pkCount}, Alignment: ${data.alignment}, Fame: ${data.fame}]")
                        .color(NamedTextColor.DARK_RED))
            )
        }
        return true
    }

    private fun showUsage(sender: CommandSender) {
        val usage = i18n.get("admin.usage", """
            === Admin Commands ===
            /noty admin listgray - List gray players
            /noty admin listred - List red players
            /noty admin guildtag <player> <tag|clear> - Test guild tag display
            /noty admin <player> <alignment|fame|pk> <set|add> <value>
        """.trimIndent())
        sender.sendMessage(usage)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val commands = listOf("listgray", "listred", "guildtag")
                val players = Bukkit.getOnlinePlayers().map { it.name }
                (commands + players).filter { it.lowercase().startsWith(args[0].lowercase()) }
            }
            2 -> when (args[0].lowercase()) {
                "listgray", "listred" -> emptyList()
                "guildtag" -> Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> listOf("alignment", "fame", "pk")
                    .filter { it.startsWith(args[1].lowercase()) }
            }
            3 -> when (args[0].lowercase()) {
                "guildtag" -> listOf("clear").filter { it.startsWith(args[2].lowercase()) }
                "listgray", "listred" -> emptyList()
                else -> listOf("set", "add")
                    .filter { it.startsWith(args[2].lowercase()) }
            }
            else -> emptyList()
        }
    }
}
