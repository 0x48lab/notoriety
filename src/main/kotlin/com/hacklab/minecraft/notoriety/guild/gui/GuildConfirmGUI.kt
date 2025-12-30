package com.hacklab.minecraft.notoriety.guild.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * 確認ダイアログGUI
 */
class GuildConfirmGUI(
    player: Player,
    private val title: String,
    private val description: List<String>,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit,
    private val guiManager: GuildGUIManager
) : GuildGUI(
    player,
    Component.text(title).color(NamedTextColor.GOLD),
    27
) {

    companion object {
        private const val SLOT_CONFIRM = 11
        private const val SLOT_CANCEL = 15
        private const val SLOT_INFO = 13
    }

    override fun setupItems() {
        fillAll()

        // 説明
        val lore = description.map {
            Component.text(it).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        }

        _inventory.setItem(SLOT_INFO, createItem(
            Material.PAPER,
            Component.text(title).color(NamedTextColor.YELLOW),
            *lore.toTypedArray()
        ))

        // 確認ボタン
        _inventory.setItem(SLOT_CONFIRM, createItem(
            Material.LIME_WOOL,
            Component.text("確認").color(NamedTextColor.GREEN),
            Component.text("クリックで実行").color(NamedTextColor.GRAY)
        ))

        // キャンセルボタン
        _inventory.setItem(SLOT_CANCEL, createItem(
            Material.RED_WOOL,
            Component.text("キャンセル").color(NamedTextColor.RED),
            Component.text("クリックでキャンセル").color(NamedTextColor.GRAY)
        ))
    }

    override fun handleClick(event: InventoryClickEvent) {
        when (event.slot) {
            SLOT_CONFIRM -> {
                player.closeInventory()
                onConfirm()
            }
            SLOT_CANCEL -> {
                player.closeInventory()
                onCancel()
            }
        }
    }
}
