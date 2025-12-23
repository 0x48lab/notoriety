package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TrustCommand(private val plugin: Notoriety) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        return when (args[0].lowercase()) {
            "add" -> addTrust(player, args)
            "remove" -> removeTrust(player, args)
            "list" -> listTrust(player)
            "check" -> checkTrust(player, args)
            else -> {
                showUsage(sender)
                true
            }
        }
    }

    private fun addTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /noty trust add <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.addTrust(player.uniqueId, target.uniqueId)
        player.sendMessage("${target.name} を信頼リストに追加しました")
        return true
    }

    private fun removeTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /noty trust remove <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.removeTrust(player.uniqueId, target.uniqueId)
        player.sendMessage("${target.name} を信頼リストから削除しました")
        return true
    }

    private fun listTrust(player: Player): Boolean {
        val trusted = plugin.trustService.getTrustedPlayers(player.uniqueId)
        val trusters = plugin.trustService.getPlayersWhoTrust(player.uniqueId)

        if (trusted.isEmpty() && trusters.isEmpty()) {
            player.sendMessage("信頼関係はありません")
            return true
        }

        player.sendMessage("=== 信頼関係 ===")

        // 全プレイヤーをまとめて表示
        val allPlayers = (trusted + trusters).distinct()
        allPlayers.forEach { uuid ->
            val name = Bukkit.getOfflinePlayer(uuid).name ?: "???"
            val iTrust = trusted.contains(uuid)
            val theyTrust = trusters.contains(uuid)

            val symbol = when {
                iTrust && theyTrust -> "§a↔" // 相互
                iTrust -> "§7→"              // 自分だけが信頼
                else -> "§e←"                // 相手だけが信頼
            }
            player.sendMessage("$symbol $name")
        }
        return true
    }

    private fun checkTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /noty trust check <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        val isTrusted = plugin.trustService.isTrusted(player.uniqueId, target.uniqueId)
        val trustedByTarget = plugin.trustService.isTrusted(target.uniqueId, player.uniqueId)

        player.sendMessage("=== ${target.name} との信頼関係 ===")
        player.sendMessage("あなた → ${target.name}: ${if (isTrusted) "信頼している" else "信頼していない"}")
        player.sendMessage("${target.name} → あなた: ${if (trustedByTarget) "信頼されている" else "信頼されていない"}")
        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("=== Trust Commands ===")
        sender.sendMessage("/noty trust add <player> - プレイヤーを信頼")
        sender.sendMessage("/noty trust remove <player> - 信頼を解除")
        sender.sendMessage("/noty trust list - 信頼リスト表示")
        sender.sendMessage("/noty trust check <player> - 信頼関係を確認")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "remove", "list", "check")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0] in listOf("add", "remove", "check")) {
                Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
