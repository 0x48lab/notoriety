package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.reputation.TitleResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StatusCommand(private val plugin: Notoriety) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val target = if (args.isEmpty()) {
            (sender as? Player)?.uniqueId ?: run {
                sender.sendMessage("Player name required")
                return true
            }
        } else {
            Bukkit.getOfflinePlayer(args[0]).uniqueId
        }

        val data = plugin.playerManager.getPlayer(target)
        if (data == null) {
            sender.sendMessage("Player not found or not loaded")
            return true
        }

        val title = TitleResolver.getTitle(data)
        val color = data.getNameColor()
        val playerName = Bukkit.getOfflinePlayer(target).name ?: "Unknown"

        sender.sendMessage("=== [$playerName] の名声情報 ===")
        sender.sendMessage("状態: ${color.name}")
        sender.sendMessage("称号: ${title ?: "なし"}")
        sender.sendMessage("CrimePoint: ${data.crimePoint} / 1000")
        sender.sendMessage("PKCount: ${data.pkCount}")
        sender.sendMessage("Karma: ${data.karma} / 1000")
        sender.sendMessage("Fame: ${data.fame} / 1000")

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
