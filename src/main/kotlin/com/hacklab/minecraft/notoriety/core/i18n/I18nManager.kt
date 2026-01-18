package com.hacklab.minecraft.notoriety.core.i18n

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

/**
 * 国際化メッセージ管理
 *
 * 使用例:
 * ```
 * // メッセージ送信（推奨）
 * i18n.send(player, "key", "default", args...)
 * i18n.sendError(player, "key", "default", args...)
 * i18n.sendSuccess(player, "key", "default", args...)
 *
 * // 色指定
 * i18n.send(player, "key", "default", NamedTextColor.GOLD, args...)
 * ```
 */
class I18nManager(private val plugin: JavaPlugin, val defaultLocale: String) {
    private val messages = mutableMapOf<String, MutableMap<String, String>>()
    private val supportedLocales = listOf("ja", "en")

    /** プレイヤーのロケールを取得する関数（PlayerManagerから設定される） */
    var getPlayerLocale: ((UUID) -> String?)? = null

    init {
        loadAllMessages()
    }

    private fun loadAllMessages() {
        // 全ての言語ファイルを読み込む
        supportedLocales.forEach { locale ->
            saveDefaultLanguageFile(locale)
            loadMessagesForLocale(locale)
        }
        plugin.logger.info("Loaded messages for locales: ${supportedLocales.joinToString(", ")}")
    }

    private fun loadMessagesForLocale(locale: String) {
        val localeMessages = mutableMapOf<String, String>()

        val langFile = File(plugin.dataFolder, "lang/$locale.yml")
        if (langFile.exists()) {
            val config = YamlConfiguration.loadConfiguration(langFile)
            loadFromConfig(config, localeMessages)
        } else {
            // Fallback to embedded resource
            val resource = plugin.getResource("lang/$locale.yml")
            if (resource != null) {
                val config = YamlConfiguration.loadConfiguration(InputStreamReader(resource))
                loadFromConfig(config, localeMessages)
            }
        }

        messages[locale] = localeMessages
    }

    private fun saveDefaultLanguageFile(lang: String) {
        val langFile = File(plugin.dataFolder, "lang/$lang.yml")
        if (!langFile.exists()) {
            plugin.saveResource("lang/$lang.yml", false)
        }
    }

    private fun loadFromConfig(config: YamlConfiguration, target: MutableMap<String, String>) {
        for (key in config.getKeys(true)) {
            val value = config.getString(key)
            if (value != null) {
                target[key] = value
            }
        }
    }

    /**
     * プレイヤーのロケールを取得
     */
    fun getLocale(playerUuid: UUID): String {
        return getPlayerLocale?.invoke(playerUuid) ?: defaultLocale
    }

    /**
     * デフォルトロケールでメッセージを取得
     */
    fun get(key: String, default: String): String {
        return messages[defaultLocale]?.get(key) ?: default
    }

    /**
     * デフォルトロケールでメッセージを取得（フォーマット付き）
     */
    fun get(key: String, default: String, vararg args: Any): String {
        val template = messages[defaultLocale]?.get(key) ?: default
        return if (args.isEmpty()) template else template.format(*args)
    }

    /**
     * プレイヤーのロケールでメッセージを取得
     */
    fun get(playerUuid: UUID, key: String, default: String): String {
        val locale = getLocale(playerUuid)
        return messages[locale]?.get(key) ?: messages[defaultLocale]?.get(key) ?: default
    }

    /**
     * プレイヤーのロケールでメッセージを取得（フォーマット付き）
     */
    fun get(playerUuid: UUID, key: String, default: String, vararg args: Any): String {
        val locale = getLocale(playerUuid)
        val template = messages[locale]?.get(key) ?: messages[defaultLocale]?.get(key) ?: default
        return if (args.isEmpty()) template else template.format(*args)
    }

    /**
     * 指定ロケールでメッセージを取得
     */
    fun getForLocale(locale: String, key: String, default: String): String {
        return messages[locale]?.get(key) ?: messages[defaultLocale]?.get(key) ?: default
    }

    operator fun get(key: String): String = messages[defaultLocale]?.get(key) ?: key

    fun reload() {
        messages.clear()
        loadAllMessages()
    }

    fun getSupportedLocales(): List<String> = supportedLocales

    // ========================================
    // 統一メッセージ送信API
    // ========================================

    /**
     * プレイヤーにメッセージを送信（デフォルト色: 白）
     */
    fun send(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.WHITE, *args)
    }

    /**
     * プレイヤーにメッセージを送信（色指定）
     */
    fun send(player: Player, key: String, default: String, color: TextColor, vararg args: Any) {
        val message = get(player.uniqueId, key, default, *args)
        player.sendMessage(Component.text(message).color(color))
    }

    /**
     * CommandSenderにメッセージを送信（デフォルト色: 白）
     */
    fun send(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.WHITE, *args)
    }

    /**
     * CommandSenderにメッセージを送信（色指定）
     */
    fun send(sender: CommandSender, key: String, default: String, color: TextColor, vararg args: Any) {
        val playerUuid = (sender as? Player)?.uniqueId
        val message = if (playerUuid != null) {
            get(playerUuid, key, default, *args)
        } else {
            get(key, default, *args)
        }
        sender.sendMessage(Component.text(message).color(color))
    }

    /**
     * Audienceにメッセージを送信（色指定）
     */
    fun send(audience: Audience, playerUuid: UUID?, key: String, default: String, color: TextColor, vararg args: Any) {
        val message = if (playerUuid != null) {
            get(playerUuid, key, default, *args)
        } else {
            get(key, default, *args)
        }
        audience.sendMessage(Component.text(message).color(color))
    }

    // ========================================
    // 便利メソッド（色付き）
    // ========================================

    /** エラーメッセージ（赤） */
    fun sendError(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.RED, *args)
    }

    /** エラーメッセージ（赤） */
    fun sendError(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.RED, *args)
    }

    /** 成功メッセージ（緑） */
    fun sendSuccess(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.GREEN, *args)
    }

    /** 成功メッセージ（緑） */
    fun sendSuccess(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.GREEN, *args)
    }

    /** 警告メッセージ（黄） */
    fun sendWarning(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.YELLOW, *args)
    }

    /** 警告メッセージ（黄） */
    fun sendWarning(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.YELLOW, *args)
    }

    /** 情報メッセージ（灰） */
    fun sendInfo(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.GRAY, *args)
    }

    /** 情報メッセージ（灰） */
    fun sendInfo(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.GRAY, *args)
    }

    /** ヘッダーメッセージ（金） */
    fun sendHeader(player: Player, key: String, default: String, vararg args: Any) {
        send(player, key, default, NamedTextColor.GOLD, *args)
    }

    /** ヘッダーメッセージ（金） */
    fun sendHeader(sender: CommandSender, key: String, default: String, vararg args: Any) {
        send(sender, key, default, NamedTextColor.GOLD, *args)
    }

    // ========================================
    // Component生成API（複雑なメッセージ用）
    // ========================================

    /**
     * メッセージをComponentとして取得
     */
    fun component(player: Player, key: String, default: String, color: TextColor, vararg args: Any): Component {
        val message = get(player.uniqueId, key, default, *args)
        return Component.text(message).color(color)
    }

    /**
     * メッセージをComponentとして取得（CommandSender用）
     */
    fun component(sender: CommandSender, key: String, default: String, color: TextColor, vararg args: Any): Component {
        val playerUuid = (sender as? Player)?.uniqueId
        val message = if (playerUuid != null) {
            get(playerUuid, key, default, *args)
        } else {
            get(key, default, *args)
        }
        return Component.text(message).color(color)
    }

    /**
     * メッセージをComponentとして取得（UUID指定）
     */
    fun component(playerUuid: UUID?, key: String, default: String, color: TextColor, vararg args: Any): Component {
        val message = if (playerUuid != null) {
            get(playerUuid, key, default, *args)
        } else {
            get(key, default, *args)
        }
        return Component.text(message).color(color)
    }
}
