package com.hacklab.minecraft.notoriety.inspect

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class InspectionStick(private val plugin: JavaPlugin, private val i18n: I18nManager) {

    companion object {
        private const val STICK_KEY = "inspection_stick"
    }

    private val namespacedKey = NamespacedKey(plugin, STICK_KEY)

    fun createStick(): ItemStack {
        val stick = ItemStack(Material.STICK)
        val meta = stick.itemMeta

        // Set display name with gradient-like effect
        meta.displayName(
            Component.text(i18n.get("inspect.stick_name", "Inspection Stick"))
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Set lore
        meta.lore(listOf(
            Component.empty(),
            Component.text(i18n.get("inspect.stick_lore1", "Right-click a block to check its owner"))
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(i18n.get("inspect.stick_lore2", "No inspect mode required!"))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("✦ ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(i18n.get("inspect.stick_lore3", "Special Item"))
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, true))
        ))

        // Add enchantment glow effect (hidden)
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        // Add persistent data to identify this as an inspection stick
        meta.persistentDataContainer.set(namespacedKey, PersistentDataType.BYTE, 1)

        stick.itemMeta = meta
        return stick
    }

    fun isInspectionStick(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.STICK) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(namespacedKey, PersistentDataType.BYTE)
    }
}
