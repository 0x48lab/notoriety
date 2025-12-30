package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.TagColor
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * タグカラー選択GUI
 */
class GuildColorSelectGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager
) : GuildGUI(
    player,
    Component.text("タグカラー選択").color(NamedTextColor.YELLOW),
    54
) {

    private val guild = guildService.getPlayerGuild(player.uniqueId)

    // 色とマテリアルのマッピング
    private val colorMaterials = mapOf(
        TagColor.WHITE to Material.WHITE_WOOL,
        TagColor.RED to Material.RED_WOOL,
        TagColor.BLUE to Material.BLUE_WOOL,
        TagColor.GREEN to Material.LIME_WOOL,
        TagColor.YELLOW to Material.YELLOW_WOOL,
        TagColor.GOLD to Material.ORANGE_WOOL,
        TagColor.AQUA to Material.LIGHT_BLUE_WOOL,
        TagColor.LIGHT_PURPLE to Material.MAGENTA_WOOL,
        TagColor.DARK_RED to Material.RED_TERRACOTTA,
        TagColor.DARK_BLUE to Material.BLUE_TERRACOTTA,
        TagColor.DARK_GREEN to Material.GREEN_WOOL,
        TagColor.DARK_AQUA to Material.CYAN_WOOL,
        TagColor.DARK_PURPLE to Material.PURPLE_WOOL,
        TagColor.GRAY to Material.LIGHT_GRAY_WOOL,
        TagColor.DARK_GRAY to Material.GRAY_WOOL,
        TagColor.BLACK to Material.BLACK_WOOL
    )

    // 色の日本語名
    private val colorNames = mapOf(
        TagColor.WHITE to "白",
        TagColor.RED to "赤",
        TagColor.BLUE to "青",
        TagColor.GREEN to "緑",
        TagColor.YELLOW to "黄",
        TagColor.GOLD to "金",
        TagColor.AQUA to "水色",
        TagColor.LIGHT_PURPLE to "ピンク",
        TagColor.DARK_RED to "暗い赤",
        TagColor.DARK_BLUE to "紺色",
        TagColor.DARK_GREEN to "深緑",
        TagColor.DARK_AQUA to "シアン",
        TagColor.DARK_PURPLE to "紫",
        TagColor.GRAY to "灰色",
        TagColor.DARK_GRAY to "暗い灰色",
        TagColor.BLACK to "黒"
    )

    // 配置するスロット
    private val colorSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29
    )

    override fun setupItems() {
        fillBorder()

        if (guild == null) {
            _inventory.setItem(22, createItem(
                Material.BARRIER,
                Component.text("ギルドに所属していません").color(NamedTextColor.RED)
            ))
            _inventory.setItem(SLOT_BACK, createBackButton())
            _inventory.setItem(SLOT_CLOSE, createCloseButton())
            return
        }

        // 現在の色
        _inventory.setItem(4, createItem(
            colorMaterials[guild.tagColor] ?: Material.WHITE_WOOL,
            Component.text("現在のタグカラー").color(NamedTextColor.GOLD),
            Component.text("[${guild.tag}]").color(guild.tagColor.namedTextColor),
            Component.text("色: ${colorNames[guild.tagColor]}").color(NamedTextColor.GRAY)
        ))

        // 色一覧
        TagColor.entries.forEachIndexed { index, color ->
            if (index < colorSlots.size) {
                val material = colorMaterials[color] ?: Material.WHITE_WOOL
                val isSelected = color == guild.tagColor

                val item = createItem(
                    material,
                    Component.text(colorNames[color] ?: color.name).color(color.namedTextColor),
                    Component.text("[${guild.tag}]").color(color.namedTextColor),
                    if (isSelected) {
                        Component.text("★ 現在選択中").color(NamedTextColor.GREEN)
                    } else {
                        Component.text("クリックで選択").color(NamedTextColor.GRAY)
                    }
                )

                // 選択中の場合は光らせる
                if (isSelected) {
                    val meta = item.itemMeta
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true)
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                    item.itemMeta = meta
                }

                _inventory.setItem(colorSlots[index], item)
            }
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()
            in colorSlots -> handleColorClick(event)
        }
    }

    private fun handleColorClick(event: InventoryClickEvent) {
        val index = colorSlots.indexOf(event.slot)
        if (index < 0 || index >= TagColor.entries.size) return

        val selectedColor = TagColor.entries[index]
        val guild = this.guild ?: return

        if (selectedColor == guild.tagColor) {
            // 既に選択中
            player.sendMessage(Component.text("既にこの色が選択されています").color(NamedTextColor.YELLOW))
            return
        }

        try {
            guildService.setTagColor(guild.id, selectedColor, player.uniqueId)
            player.sendMessage(
                Component.text("タグカラーを").color(NamedTextColor.GREEN)
                    .append(Component.text(colorNames[selectedColor] ?: selectedColor.name).color(selectedColor.namedTextColor))
                    .append(Component.text("に変更しました").color(NamedTextColor.GREEN))
            )
            guiManager.openColorSelect(player) // リフレッシュ
        } catch (e: Exception) {
            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
        }
    }
}
