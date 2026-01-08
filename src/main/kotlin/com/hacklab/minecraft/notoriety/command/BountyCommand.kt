package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BountyCommand(private val plugin: Notoriety) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        return when (args[0].lowercase()) {
            "add", "set" -> addBounty(sender, args)  // set は後方互換性のため残す
            "list" -> listBounties(sender)
            "check" -> checkBounty(sender, args)
            else -> {
                showUsage(sender)
                true
            }
        }
    }

    private fun addBounty(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        if (args.size < 3) {
            sender.sendMessage("Usage: /bounty add <player> <amount>")
            return true
        }

        val target = Bukkit.getOfflinePlayer(args[1])
        val amount = args[2].toDoubleOrNull() ?: run {
            sender.sendMessage("Invalid amount")
            return true
        }

        if (plugin.bountyService.setBounty(player.uniqueId, target.uniqueId, amount)) {
            sender.sendMessage("${target.name} に ${amount.toLong()} の懸賞金をかけました")
            Bukkit.broadcast(
                Component.text("${player.name} が ${target.name} に ${amount.toLong()} の懸賞金をかけました！")
                    .color(NamedTextColor.GOLD)
            )
        } else {
            sender.sendMessage("懸賞金の設定に失敗しました（対象が赤プレイヤーでないか、金額が不足しています）")
        }
        return true
    }

    private fun listBounties(sender: CommandSender): Boolean {
        val bounties = plugin.bountyService.getBountyList()

        if (bounties.isEmpty()) {
            sender.sendMessage("現在、懸賞金はかけられていません")
            return true
        }

        sender.sendMessage("=== 懸賞金リスト ===")
        bounties.forEachIndexed { index, bounty ->
            val name = Bukkit.getOfflinePlayer(bounty.target).name
            sender.sendMessage("${index + 1}. $name - ${bounty.total.toLong()}")
        }
        return true
    }

    private fun checkBounty(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("Usage: /noty bounty check <player>")
            return true
        }

        val target = Bukkit.getOfflinePlayer(args[1])
        val bounty = plugin.bountyService.getBounty(target.uniqueId)

        if (bounty == null) {
            sender.sendMessage("${target.name} には懸賞金がかけられていません")
        } else {
            sender.sendMessage("=== ${target.name} の懸賞金 ===")
            sender.sendMessage("合計: ${bounty.total.toLong()}")
            sender.sendMessage("出資者:")
            bounty.contributors.forEach { (uuid, amount) ->
                val name = Bukkit.getOfflinePlayer(uuid).name
                sender.sendMessage("  - $name: ${amount.toLong()}")
            }
        }
        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("=== Bounty Commands ===")
        sender.sendMessage("/bounty add <player> <amount> - 懸賞金をかける")
        sender.sendMessage("/bounty list - 懸賞金リスト表示")
        sender.sendMessage("/bounty check <player> - 懸賞金を確認")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "list", "check")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0] in listOf("add", "set", "check")) {
                // 赤プレイヤー（Murderer）のみ補完対象
                plugin.playerManager.findAllRedPlayers()
                    .mapNotNull { Bukkit.getOfflinePlayer(it.uuid).name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
