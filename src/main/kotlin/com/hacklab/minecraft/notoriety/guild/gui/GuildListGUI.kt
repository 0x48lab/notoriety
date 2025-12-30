package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * ギルド一覧GUI
 */
class GuildListGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val page: Int = 0
) : GuildGUI(
    player,
    Component.text("ギルド一覧").color(NamedTextColor.AQUA),
    54
) {

    private val guilds: List<Guild> = guildService.getAllGuilds(page * ITEMS_PER_PAGE, ITEMS_PER_PAGE)
    private val totalGuilds: Int = guildService.getGuildCount()
    private val maxPage = ((totalGuilds - 1) / ITEMS_PER_PAGE).coerceAtLeast(0)

    companion object {
        const val ITEMS_PER_PAGE = 28
        private val CONTENT_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )
    }

    override fun setupItems() {
        fillBorder()

        // タイトル
        _inventory.setItem(4, createItem(
            Material.COMPASS,
            Component.text("ギルド一覧").color(NamedTextColor.GOLD),
            Component.text("全${totalGuilds}ギルド").color(NamedTextColor.GRAY),
            Component.text("ページ ${page + 1}/${maxPage + 1}").color(NamedTextColor.GRAY)
        ))

        if (guilds.isEmpty()) {
            _inventory.setItem(22, createItem(
                Material.PAPER,
                Component.text("ギルドがありません").color(NamedTextColor.GRAY)
            ))
        } else {
            guilds.forEachIndexed { index, guild ->
                if (index < CONTENT_SLOTS.size) {
                    _inventory.setItem(CONTENT_SLOTS[index], createGuildItem(guild))
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

    private fun createGuildItem(guild: Guild): org.bukkit.inventory.ItemStack {
        val memberCount = guildService.getMemberCount(guild.id)
        val masterName = Bukkit.getOfflinePlayer(guild.masterUuid).name ?: "Unknown"

        return createItem(
            Material.SHIELD,
            Component.text("[${guild.tag}] ${guild.name}").color(guild.tagColor.namedTextColor),
            Component.text("マスター: $masterName").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("メンバー: $memberCount/${guild.maxMembers}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("クリックで詳細を表示").color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
        )
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()
            SLOT_PREV_PAGE -> if (page > 0) guiManager.openGuildList(player, page - 1)
            SLOT_NEXT_PAGE -> if (page < maxPage) guiManager.openGuildList(player, page + 1)
            in CONTENT_SLOTS -> handleGuildClick(event)
        }
    }

    private fun handleGuildClick(event: InventoryClickEvent) {
        val index = CONTENT_SLOTS.indexOf(event.slot)
        if (index < 0 || index >= guilds.size) return

        val guild = guilds[index]
        guiManager.openGuildInfo(player, guild.id)
    }
}
