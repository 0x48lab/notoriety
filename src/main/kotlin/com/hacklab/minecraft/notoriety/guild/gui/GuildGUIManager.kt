package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * ギルドGUIの管理クラス
 */
class GuildGUIManager(
    private val plugin: JavaPlugin,
    private val guildService: GuildService,
    val territoryService: TerritoryService? = null
) : Listener {

    val inputManager = GuildInputManager(plugin)

    /**
     * メインメニューを開く
     */
    fun openMainMenu(player: Player) {
        GuildMainMenuGUI(player, guildService, this).open()
    }

    /**
     * メンバー一覧を開く
     */
    fun openMembersList(player: Player, page: Int = 0) {
        GuildMembersGUI(player, guildService, this, page).open()
    }

    /**
     * 招待一覧を開く
     */
    fun openInvitesList(player: Player, page: Int = 0) {
        GuildInvitesGUI(player, guildService, this, page).open()
    }

    /**
     * 入会申請一覧を開く
     */
    fun openApplicationsList(player: Player, page: Int = 0) {
        GuildApplicationsGUI(player, guildService, this, page).open()
    }

    /**
     * 色選択画面を開く
     */
    fun openColorSelect(player: Player) {
        GuildColorSelectGUI(player, guildService, this).open()
    }

    /**
     * ギルド設定画面を開く（マスター専用）
     */
    fun openSettings(player: Player) {
        GuildSettingsGUI(player, guildService, this, inputManager).open()
    }

    /**
     * 確認ダイアログを開く
     */
    fun openConfirmDialog(
        player: Player,
        title: String,
        description: List<String>,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        GuildConfirmGUI(player, title, description, onConfirm, onCancel, this).open()
    }

    /**
     * ギルド一覧を開く
     */
    fun openGuildList(player: Player, page: Int = 0) {
        GuildListGUI(player, guildService, this, page).open()
    }

    /**
     * ギルド情報を開く
     */
    fun openGuildInfo(player: Player, guildId: Long) {
        GuildInfoGUI(player, guildService, this, guildId).open()
    }

    // ========== イベントハンドラ ==========

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is GuildGUI) {
            event.isCancelled = true

            // 自分のインベントリ内でのクリックのみ処理
            if (event.clickedInventory == event.inventory) {
                holder.handleClick(event)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder
        if (holder is GuildGUI) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        // 必要に応じてクリーンアップ処理
    }
}
