package com.hacklab.minecraft.notoriety.command

import com.hacklab.minecraft.notoriety.Notoriety
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LocaleCommand(private val plugin: Notoriety) : SubCommand {

    private val i18n get() = plugin.i18nManager

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        val playerData = plugin.playerManager.getPlayer(sender.uniqueId)
        if (playerData == null) {
            sender.sendMessage(Component.text(i18n.get("message.player_not_found", "Player data not found"))
                .color(NamedTextColor.RED))
            return true
        }

        // 引数なしの場合は現在の設定を表示
        if (args.isEmpty()) {
            showCurrentLocale(sender, playerData.locale)
            return true
        }

        val newLocale = args[0].lowercase()
        val supportedLocales = i18n.getSupportedLocales()

        // resetの場合はサーバーデフォルトに戻す
        if (newLocale == "reset" || newLocale == "default") {
            playerData.locale = null
            plugin.playerManager.savePlayer(sender.uniqueId)
            sender.sendMessage(Component.text(
                i18n.get(sender.uniqueId, "locale.reset", "Language reset to server default (%s)")
                    .format(i18n.defaultLocale)
            ).color(NamedTextColor.GREEN))
            return true
        }

        // サポートされているロケールかチェック
        if (newLocale !in supportedLocales) {
            sender.sendMessage(Component.text(
                i18n.get(sender.uniqueId, "locale.unsupported", "Unsupported language: %s")
                    .format(newLocale)
            ).color(NamedTextColor.RED))
            sender.sendMessage(Component.text(
                i18n.get(sender.uniqueId, "locale.supported", "Supported languages: %s")
                    .format(supportedLocales.joinToString(", "))
            ).color(NamedTextColor.YELLOW))
            return true
        }

        // ロケールを設定
        playerData.locale = newLocale
        plugin.playerManager.savePlayer(sender.uniqueId)

        // 新しいロケールでメッセージを表示
        sender.sendMessage(Component.text(
            i18n.get(sender.uniqueId, "locale.changed", "Language changed to %s")
                .format(getLocaleName(newLocale))
        ).color(NamedTextColor.GREEN))

        return true
    }

    private fun showCurrentLocale(player: Player, currentLocale: String?) {
        val effectiveLocale = currentLocale ?: i18n.defaultLocale
        val isDefault = currentLocale == null

        player.sendMessage(Component.text("=== ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(i18n.get(player.uniqueId, "locale.title", "Language Settings"))
                .color(NamedTextColor.WHITE))
            .append(Component.text(" ===").color(NamedTextColor.GOLD)))

        val currentText = if (isDefault) {
            i18n.get(player.uniqueId, "locale.current_default", "Current: %s (server default)")
                .format(getLocaleName(effectiveLocale))
        } else {
            i18n.get(player.uniqueId, "locale.current", "Current: %s")
                .format(getLocaleName(effectiveLocale))
        }
        player.sendMessage(Component.text(currentText).color(NamedTextColor.YELLOW))

        val supportedLocales = i18n.getSupportedLocales()
        player.sendMessage(Component.text(
            i18n.get(player.uniqueId, "locale.supported", "Supported languages: %s")
                .format(supportedLocales.joinToString(", ") { getLocaleName(it) })
        ).color(NamedTextColor.GRAY))

        player.sendMessage(Component.text(
            i18n.get(player.uniqueId, "locale.usage", "Usage: /noty locale <ja|en|reset>")
        ).color(NamedTextColor.GRAY))
    }

    private fun getLocaleName(locale: String): String {
        return when (locale) {
            "ja" -> "日本語 (ja)"
            "en" -> "English (en)"
            else -> locale
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val options = i18n.getSupportedLocales() + listOf("reset")
            return options.filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
