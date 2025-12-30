package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/**
 * プレイヤー選択GUI（招待用）
 */
class GuildPlayerSelectGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val page: Int = 0
) : GuildGUI(
    player,
    Component.text("プレイヤーを招待").color(NamedTextColor.GREEN),
    54
) {

    private val guild = guildService.getPlayerGuild(player.uniqueId)

    // ギルドに未所属のオンラインプレイヤー一覧
    private val availablePlayers: List<Player> = Bukkit.getOnlinePlayers()
        .filter { it.uniqueId != player.uniqueId }
        .filter { guildService.getPlayerGuild(it.uniqueId) == null }
        .filter { guild?.let { g -> !guildService.hasInvitation(g.id, it.uniqueId) } ?: true }
        .toList()

    private val itemsPerPage = 28
    private val maxPage = ((availablePlayers.size - 1) / itemsPerPage).coerceAtLeast(0)
    private val pageStart = page * itemsPerPage
    private val pageEnd = minOf(pageStart + itemsPerPage, availablePlayers.size)
    private val currentPagePlayers = if (availablePlayers.isNotEmpty() && pageStart < availablePlayers.size) {
        availablePlayers.subList(pageStart, pageEnd)
    } else {
        emptyList()
    }

    companion object {
        private val CONTENT_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )
    }

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

        // タイトル
        _inventory.setItem(4, createItem(
            Material.EMERALD,
            Component.text("招待可能なプレイヤー").color(NamedTextColor.GOLD),
            Component.text("${availablePlayers.size}人がオンライン").color(NamedTextColor.GRAY)
        ))

        if (availablePlayers.isEmpty()) {
            _inventory.setItem(22, createItem(
                Material.PAPER,
                Component.text("招待可能なプレイヤーがいません").color(NamedTextColor.GRAY),
                Component.text("全員がギルドに所属しているか、").color(NamedTextColor.DARK_GRAY),
                Component.text("既に招待済みです").color(NamedTextColor.DARK_GRAY)
            ))
        } else {
            currentPagePlayers.forEachIndexed { index, targetPlayer ->
                if (index < CONTENT_SLOTS.size) {
                    _inventory.setItem(CONTENT_SLOTS[index], createPlayerHead(targetPlayer))
                }
            }
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())

        if (page > 0) {
            _inventory.setItem(SLOT_PREV_PAGE, createPrevPageButton(page, maxPage + 1))
        }
        if (page < maxPage) {
            _inventory.setItem(SLOT_NEXT_PAGE, createNextPageButton(page, maxPage + 1))
        }
    }

    private fun createPlayerHead(targetPlayer: Player): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = targetPlayer
        meta.displayName(Component.text(targetPlayer.name).color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false))

        val lore = listOf(
            Component.text("クリックで招待を送信").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_BACK -> guiManager.openInvitesList(player)
            SLOT_CLOSE -> player.closeInventory()
            SLOT_PREV_PAGE -> if (page > 0) GuildPlayerSelectGUI(player, guildService, guiManager, page - 1).open()
            SLOT_NEXT_PAGE -> if (page < maxPage) GuildPlayerSelectGUI(player, guildService, guiManager, page + 1).open()
            in CONTENT_SLOTS -> handlePlayerClick(event)
        }
    }

    private fun handlePlayerClick(event: InventoryClickEvent) {
        val index = CONTENT_SLOTS.indexOf(event.slot)
        if (index < 0 || index >= currentPagePlayers.size) return

        val targetPlayer = currentPagePlayers[index]
        val guild = this.guild ?: return

        guiManager.openConfirmDialog(
            player,
            "招待の確認",
            listOf("「${targetPlayer.name}」を招待しますか？"),
            onConfirm = {
                try {
                    guildService.invitePlayer(guild.id, targetPlayer.uniqueId, player.uniqueId)
                    player.sendMessage(Component.text("${targetPlayer.name}を招待しました").color(NamedTextColor.GREEN))
                    guiManager.openInvitesList(player)
                } catch (e: Exception) {
                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                }
            },
            onCancel = {
                GuildPlayerSelectGUI(player, guildService, guiManager, page).open()
            }
        )
    }
}
