package com.hacklab.minecraft.notoriety.core.i18n

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

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
}
