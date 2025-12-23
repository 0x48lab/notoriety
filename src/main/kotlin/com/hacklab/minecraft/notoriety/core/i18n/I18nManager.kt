package com.hacklab.minecraft.notoriety.core.i18n

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

class I18nManager(private val plugin: JavaPlugin, private val locale: String) {
    private val messages = mutableMapOf<String, String>()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        // Save default language files
        saveDefaultLanguageFile("ja")
        saveDefaultLanguageFile("en")

        // Load the selected locale
        val langFile = File(plugin.dataFolder, "lang/$locale.yml")
        if (langFile.exists()) {
            val config = YamlConfiguration.loadConfiguration(langFile)
            loadFromConfig(config)
        } else {
            // Fallback to embedded resource
            val resource = plugin.getResource("lang/$locale.yml")
            if (resource != null) {
                val config = YamlConfiguration.loadConfiguration(InputStreamReader(resource))
                loadFromConfig(config)
            }
        }

        plugin.logger.info("Loaded ${messages.size} messages for locale: $locale")
    }

    private fun saveDefaultLanguageFile(lang: String) {
        val langFile = File(plugin.dataFolder, "lang/$lang.yml")
        if (!langFile.exists()) {
            plugin.saveResource("lang/$lang.yml", false)
        }
    }

    private fun loadFromConfig(config: YamlConfiguration) {
        for (key in config.getKeys(true)) {
            val value = config.getString(key)
            if (value != null) {
                messages[key] = value
            }
        }
    }

    fun get(key: String, vararg args: Any): String {
        val template = messages[key] ?: return key
        return if (args.isEmpty()) template else template.format(*args)
    }

    operator fun get(key: String): String = get(key)

    fun reload() {
        messages.clear()
        loadMessages()
    }
}
