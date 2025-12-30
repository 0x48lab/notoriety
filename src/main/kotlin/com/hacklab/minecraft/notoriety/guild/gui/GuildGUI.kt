package com.hacklab.minecraft.notoriety.guild.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * ギルドGUIの基底クラス
 */
abstract class GuildGUI(
    protected val player: Player,
    private val title: Component,
    private val size: Int = 54
) : InventoryHolder {

    @Suppress("PropertyName")
    protected val _inventory: Inventory = Bukkit.createInventory(this, size, title)

    override fun getInventory(): Inventory = _inventory

    /**
     * GUIを開く
     */
    fun open() {
        setupItems()
        player.openInventory(_inventory)
    }

    /**
     * アイテムをセットアップする（サブクラスで実装）
     */
    protected abstract fun setupItems()

    /**
     * クリックイベントを処理する（サブクラスで実装）
     */
    abstract fun handleClick(event: InventoryClickEvent)

    // ========== ユーティリティメソッド ==========

    /**
     * アイテムを作成する
     */
    protected fun createItem(
        material: Material,
        name: Component,
        vararg lore: Component
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(name.decoration(TextDecoration.ITALIC, false))
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { it.decoration(TextDecoration.ITALIC, false) })
        }
        item.itemMeta = meta
        return item
    }

    /**
     * 装飾用のガラスパネルを作成
     */
    protected fun createFiller(color: Material = Material.GRAY_STAINED_GLASS_PANE): ItemStack {
        return createItem(color, Component.empty())
    }

    /**
     * 戻るボタンを作成
     */
    protected fun createBackButton(): ItemStack {
        return createItem(
            Material.ARROW,
            Component.text("戻る").color(NamedTextColor.YELLOW),
            Component.text("クリックで前の画面に戻る").color(NamedTextColor.GRAY)
        )
    }

    /**
     * 閉じるボタンを作成
     */
    protected fun createCloseButton(): ItemStack {
        return createItem(
            Material.BARRIER,
            Component.text("閉じる").color(NamedTextColor.RED),
            Component.text("クリックでメニューを閉じる").color(NamedTextColor.GRAY)
        )
    }

    /**
     * 次ページボタンを作成
     */
    protected fun createNextPageButton(currentPage: Int, maxPage: Int): ItemStack {
        return createItem(
            Material.SPECTRAL_ARROW,
            Component.text("次のページ →").color(NamedTextColor.GREEN),
            Component.text("ページ ${currentPage + 1}/$maxPage").color(NamedTextColor.GRAY)
        )
    }

    /**
     * 前ページボタンを作成
     */
    protected fun createPrevPageButton(currentPage: Int, maxPage: Int): ItemStack {
        return createItem(
            Material.SPECTRAL_ARROW,
            Component.text("← 前のページ").color(NamedTextColor.GREEN),
            Component.text("ページ ${currentPage + 1}/$maxPage").color(NamedTextColor.GRAY)
        )
    }

    /**
     * 枠を埋める
     */
    protected fun fillBorder(filler: ItemStack = createFiller()) {
        // 上段
        for (i in 0..8) {
            _inventory.setItem(i, filler)
        }
        // 下段
        for (i in (size - 9) until size) {
            _inventory.setItem(i, filler)
        }
        // 左右
        for (i in 1 until (size / 9) - 1) {
            _inventory.setItem(i * 9, filler)
            _inventory.setItem(i * 9 + 8, filler)
        }
    }

    /**
     * 全体を埋める
     */
    protected fun fillAll(filler: ItemStack = createFiller()) {
        for (i in 0 until size) {
            _inventory.setItem(i, filler)
        }
    }

    companion object {
        // 共通のスロット位置
        const val SLOT_BACK = 45
        const val SLOT_CLOSE = 49
        const val SLOT_PREV_PAGE = 48
        const val SLOT_NEXT_PAGE = 50
    }
}
