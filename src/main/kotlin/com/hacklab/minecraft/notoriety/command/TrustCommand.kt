package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.trust.TrustState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TrustCommand(private val plugin: Notoriety) : SubCommand {

    private val i18n: I18nManager get() = plugin.i18nManager

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            i18n.sendError(sender, "common.players_only", "This command can only be used by players")
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
            i18n.sendWarning(player, "trust.usage_add", "Usage: /trust add <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.addTrust(player.uniqueId, target.uniqueId)
        i18n.sendSuccess(player, "trust.added", "Added %s to your trust list", target.name ?: "???")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            i18n.sendSuccess(targetPlayer, "trust.you_are_trusted", "%s now trusts you", player.name)
            // 相互信頼でない場合、追加を促すメッセージ
            if (!plugin.trustService.isTrusted(target.uniqueId, player.uniqueId)) {
                i18n.sendInfo(targetPlayer, "trust.add_hint", "To trust them back: /trust add %s", player.name)
            }
        }
        return true
    }

    private fun removeTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            i18n.sendWarning(player, "trust.usage_remove", "Usage: /trust remove <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.removeTrustState(player.uniqueId, target.uniqueId)
        i18n.sendSuccess(player, "trust.removed", "Trust setting for %s has been reset to default", target.name ?: "???")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            i18n.sendInfo(targetPlayer, "trust.trust_removed_notification", "%s has reset their trust setting for you", player.name)
        }
        return true
    }

    private fun distrustPlayer(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            i18n.sendWarning(player, "trust.usage_distrust", "Usage: /trust distrust <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        plugin.trustService.setTrustState(player.uniqueId, target.uniqueId, TrustState.DISTRUST)
        i18n.sendWarning(player, "trust.distrusted", "Set %s as distrusted (access denied even for guild members)", target.name ?: "???")

        // 相手がオンラインなら通知を送る
        Bukkit.getPlayer(target.uniqueId)?.let { targetPlayer ->
            i18n.sendError(targetPlayer, "trust.distrust_notification", "%s has set you as distrusted", player.name)
        }
        return true
    }

    private fun listTrust(player: Player): Boolean {
        val myRelations = plugin.trustService.getAllTrustRelations(player.uniqueId)
        val trusters = plugin.trustService.getPlayersWhoTrust(player.uniqueId)
        val distrusted = plugin.trustService.getDistrustedPlayers(player.uniqueId)
        val trusted = myRelations.filterValues { it == TrustState.TRUST }.keys.toList()

        if (myRelations.isEmpty() && trusters.isEmpty()) {
            i18n.sendInfo(player, "trust.no_relations", "You have no trust relationships")
            return true
        }

        i18n.sendHeader(player, "trust.list_header", "=== Trust Relationships ===")

        // 不信頼プレイヤーを表示
        if (distrusted.isNotEmpty()) {
            i18n.sendError(player, "trust.distrust_section", "--- Distrusted ---")
            distrusted.forEach { uuid ->
                val name = Bukkit.getOfflinePlayer(uuid).name ?: "???"
                player.sendMessage(Component.text("✗ $name").color(NamedTextColor.RED))
            }
        }

        // 信頼プレイヤーを表示
        val allTrustPlayers = (trusted + trusters).distinct()
        if (allTrustPlayers.isNotEmpty()) {
            i18n.sendSuccess(player, "trust.trust_section", "--- Trusted ---")
            allTrustPlayers.forEach { uuid ->
                val name = Bukkit.getOfflinePlayer(uuid).name ?: "???"
                val iTrust = trusted.contains(uuid)
                val theyTrust = trusters.contains(uuid)

                val (symbol, color) = when {
                    iTrust && theyTrust -> "↔" to NamedTextColor.GREEN  // 相互
                    iTrust -> "→" to NamedTextColor.GRAY               // 自分だけが信頼
                    else -> "←" to NamedTextColor.YELLOW               // 相手だけが信頼
                }
                player.sendMessage(Component.text("$symbol $name").color(color))
            }
        }
        return true
    }

    private fun checkTrust(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            i18n.sendWarning(player, "trust.usage_check", "Usage: /trust check <player>")
            return true
        }
        val target = Bukkit.getOfflinePlayer(args[1])
        val myState = plugin.trustService.getTrustState(player.uniqueId, target.uniqueId)
        val theirState = plugin.trustService.getTrustState(target.uniqueId, player.uniqueId)

        val myStateColor = when (myState) {
            TrustState.TRUST -> NamedTextColor.GREEN
            TrustState.DISTRUST -> NamedTextColor.RED
            null -> NamedTextColor.GRAY
        }
        val myStateText = when (myState) {
            TrustState.TRUST -> i18n.get(player.uniqueId, "trust.state_trusted", "Trusted")
            TrustState.DISTRUST -> i18n.get(player.uniqueId, "trust.state_distrusted", "Distrusted")
            null -> i18n.get(player.uniqueId, "trust.state_unset", "Unset")
        }
        val theirStateColor = when (theirState) {
            TrustState.TRUST -> NamedTextColor.GREEN
            TrustState.DISTRUST -> NamedTextColor.RED
            null -> NamedTextColor.GRAY
        }
        val theirStateText = when (theirState) {
            TrustState.TRUST -> i18n.get(player.uniqueId, "trust.state_trusts_you", "Trusts you")
            TrustState.DISTRUST -> i18n.get(player.uniqueId, "trust.state_distrusts_you", "Distrusts you")
            null -> i18n.get(player.uniqueId, "trust.state_unset", "Unset")
        }

        i18n.sendHeader(player, "trust.check_header", "=== Trust Relationship with %s ===", target.name ?: "???")
        player.sendMessage(
            Component.text(i18n.get(player.uniqueId, "trust.you_to_them", "You → %s: ", target.name ?: "???"))
                .color(NamedTextColor.WHITE)
                .append(Component.text(myStateText).color(myStateColor))
        )
        player.sendMessage(
            Component.text(i18n.get(player.uniqueId, "trust.them_to_you", "%s → You: ", target.name ?: "???"))
                .color(NamedTextColor.WHITE)
                .append(Component.text(theirStateText).color(theirStateColor))
        )
        return true
    }

    private fun showUsage(sender: CommandSender) {
        i18n.sendHeader(sender, "trust.usage_header", "=== Trust Commands ===")
        i18n.send(sender, "trust.usage_cmd_add", "/trust add <player> - Trust a player")
        i18n.send(sender, "trust.usage_cmd_remove", "/trust remove <player> - Reset trust setting to default")
        i18n.send(sender, "trust.usage_cmd_distrust", "/trust distrust <player> - Set a player as distrusted")
        i18n.send(sender, "trust.usage_cmd_list", "/trust list - Show trust relationships")
        i18n.send(sender, "trust.usage_cmd_check", "/trust check <player> - Check trust relationship")
        sender.sendMessage(Component.empty())
        i18n.sendInfo(sender, "trust.usage_note", "Three states: Trust (allow access) / Unset (guild check) / Distrust (deny access)")
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
