package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class AdminCommand(private val plugin: Notoriety) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("notoriety.admin")) {
            sender.sendMessage("Permission denied")
            return true
        }

        if (args.size < 4) {
            sender.sendMessage("Usage: /noty admin <player> <karma|fame|crime|pk> <set|add> <value>")
            return true
        }

        val target = Bukkit.getOfflinePlayer(args[0]).uniqueId
        val data = plugin.playerManager.getPlayer(target) ?: run {
            sender.sendMessage("Player not found or not loaded")
            return true
        }

        val param = args[1].lowercase()
        val operation = args[2].lowercase()
        val value = args[3].toIntOrNull() ?: run {
            sender.sendMessage("Invalid value")
            return true
        }

        when (param) {
            "karma" -> if (operation == "set") data.karma = value.coerceIn(0, 1000)
                       else data.addKarma(value)
            "fame" -> if (operation == "set") data.fame = value.coerceIn(0, 1000)
                      else data.addFame(value)
            "crime" -> if (operation == "set") data.crimePoint = value.coerceIn(0, 1000)
                       else data.addCrimePoint(value)
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
        sender.sendMessage("Updated ${args[0]}'s $param")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers().map { it.name }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
            2 -> listOf("karma", "fame", "crime", "pk")
                .filter { it.startsWith(args[1].lowercase()) }
            3 -> listOf("set", "add")
                .filter { it.startsWith(args[2].lowercase()) }
            else -> emptyList()
        }
    }
}
