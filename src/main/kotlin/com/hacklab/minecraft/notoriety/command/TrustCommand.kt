package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.trust.TrustState
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
            "distrust" -> distrustPlayer(player, args)
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
            player.sendMessage("Usage: /trust add <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.addTrust(player.uniqueId, target.uniqueId)
        player.sendMessage("${target.name} を信頼リストに追加しました")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            targetPlayer.sendMessage("§a${player.name} があなたを信頼しました")
            // 相互信頼でない場合、追加を促すメッセージ
            if (!plugin.trustService.isTrusted(target.uniqueId, player.uniqueId)) {
                targetPlayer.sendMessage("§7あなたも信頼するなら: §f/trust add ${player.name}")
            }
        }
        return true
    }

    private fun removeTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /trust remove <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.removeTrustState(player.uniqueId, target.uniqueId)
        player.sendMessage("${target.name} への信頼設定を解除しました（未設定に戻しました）")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            targetPlayer.sendMessage("§7${player.name} があなたへの信頼設定を解除しました")
        }
        return true
    }

    private fun distrustPlayer(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /trust distrust <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.setTrustState(player.uniqueId, target.uniqueId, TrustState.DISTRUST)
        player.sendMessage("${target.name} を不信頼に設定しました（ギルドメンバーでもアクセス拒否）")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            targetPlayer.sendMessage("§c${player.name} があなたを不信頼に設定しました")
        }
        return true
    }

    private fun listTrust(player: Player): Boolean {
        val myRelations = plugin.trustService.getAllTrustRelations(player.uniqueId)
        val trusters = plugin.trustService.getPlayersWhoTrust(player.uniqueId)
        val distrusted = plugin.trustService.getDistrustedPlayers(player.uniqueId)
        val trusted = myRelations.filterValues { it == TrustState.TRUST }.keys.toList()

        if (myRelations.isEmpty() && trusters.isEmpty()) {
            player.sendMessage("信頼関係はありません")
            return true
        }

        player.sendMessage("=== 信頼関係 ===")

        // 不信頼プレイヤーを表示
        if (distrusted.isNotEmpty()) {
            player.sendMessage("§c--- 不信頼 ---")
            distrusted.forEach { uuid ->
                val name = Bukkit.getOfflinePlayer(uuid).name ?: "???"
                player.sendMessage("§c✗ $name")
            }
        }

        // 信頼プレイヤーを表示
        val allTrustPlayers = (trusted + trusters).distinct()
        if (allTrustPlayers.isNotEmpty()) {
            player.sendMessage("§a--- 信頼 ---")
            allTrustPlayers.forEach { uuid ->
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
        }
        return true
    }

    private fun checkTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage("Usage: /trust check <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        val myState = plugin.trustService.getTrustState(player.uniqueId, target.uniqueId)
        val theirState = plugin.trustService.getTrustState(target.uniqueId, player.uniqueId)

        val myStateText = when (myState) {
            TrustState.TRUST -> "§a信頼"
            TrustState.DISTRUST -> "§c不信頼"
            null -> "§7未設定"
        }
        val theirStateText = when (theirState) {
            TrustState.TRUST -> "§a信頼されている"
            TrustState.DISTRUST -> "§c不信頼されている"
            null -> "§7未設定"
        }

        player.sendMessage("=== ${target.name} との信頼関係 ===")
        player.sendMessage("あなた → ${target.name}: $myStateText")
        player.sendMessage("${target.name} → あなた: $theirStateText")
        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("=== Trust Commands ===")
        sender.sendMessage("/trust add <player> - プレイヤーを信頼")
        sender.sendMessage("/trust remove <player> - 信頼設定を解除（未設定に戻す）")
        sender.sendMessage("/trust distrust <player> - プレイヤーを不信頼に設定")
        sender.sendMessage("/trust list - 信頼関係一覧")
        sender.sendMessage("/trust check <player> - 信頼関係を確認")
        sender.sendMessage("")
        sender.sendMessage("§7三段階: 信頼（アクセス許可）/ 未設定（ギルド判定）/ 不信頼（アクセス拒否）")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "remove", "distrust", "list", "check")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0] in listOf("add", "remove", "distrust", "check")) {
                Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
