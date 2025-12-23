package com.hacklab.minecraft.notoriety.core.economy

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class EconomyService(private val plugin: JavaPlugin) {
    private var economy: Economy? = null

    fun initialize(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.warning("Vault not found! Economy features will be disabled.")
            return false
        }

        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("No economy plugin found! Economy features will be disabled.")
            return false
        }

        economy = rsp.provider
        plugin.logger.info("Hooked into economy: ${economy?.name}")
        return true
    }

    fun isAvailable(): Boolean = economy != null

    fun getBalance(playerUuid: UUID): Double {
        val player = Bukkit.getOfflinePlayer(playerUuid)
        return economy?.getBalance(player) ?: 0.0
    }

    fun withdraw(playerUuid: UUID, amount: Double): Boolean {
        val econ = economy ?: return false
        val player = Bukkit.getOfflinePlayer(playerUuid)

        if (econ.getBalance(player) < amount) {
            return false
        }

        val response = econ.withdrawPlayer(player, amount)
        return response.transactionSuccess()
    }

    fun deposit(playerUuid: UUID, amount: Double): Boolean {
        val econ = economy ?: return false
        val player = Bukkit.getOfflinePlayer(playerUuid)
        val response = econ.depositPlayer(player, amount)
        return response.transactionSuccess()
    }

    fun format(amount: Double): String {
        return economy?.format(amount) ?: "$%.2f".format(amount)
    }
}
