package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.GuildMembership
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
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
 * ギルドメンバー一覧GUI
 */
class GuildMembersGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val page: Int = 0
) : GuildGUI(
    player,
    Component.text("メンバー一覧").color(NamedTextColor.AQUA),
    54
) {

    private val guild = guildService.getPlayerGuild(player.uniqueId)
    private val myMembership = guildService.getMembership(player.uniqueId)
    private val myRole = myMembership?.role
    private val members = guild?.let { guildService.getMembers(it.id, page * ITEMS_PER_PAGE, ITEMS_PER_PAGE) } ?: emptyList()
    private val totalMembers = guild?.let { guildService.getMemberCount(it.id) } ?: 0
    private val maxPage = ((totalMembers - 1) / ITEMS_PER_PAGE).coerceAtLeast(0)

    companion object {
        const val ITEMS_PER_PAGE = 28 // 7x4 slots
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

        // メンバーを表示
        members.forEachIndexed { index, member ->
            if (index < CONTENT_SLOTS.size) {
                _inventory.setItem(CONTENT_SLOTS[index], createMemberHead(member))
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

        // ページ情報
        _inventory.setItem(4, createItem(
            Material.PAPER,
            Component.text("ページ ${page + 1}/${maxPage + 1}").color(NamedTextColor.WHITE),
            Component.text("メンバー数: $totalMembers/${guild.maxMembers}").color(NamedTextColor.GRAY)
        ))
    }

    private fun createMemberHead(member: GuildMembership): ItemStack {
        val offlinePlayer = Bukkit.getOfflinePlayer(member.playerUuid)
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = offlinePlayer

        val roleColor = when (member.role) {
            GuildRole.MASTER -> NamedTextColor.GOLD
            GuildRole.VICE_MASTER -> NamedTextColor.YELLOW
            GuildRole.MEMBER -> NamedTextColor.WHITE
        }

        val roleName = when (member.role) {
            GuildRole.MASTER -> "ギルドマスター"
            GuildRole.VICE_MASTER -> "副マスター"
            GuildRole.MEMBER -> "メンバー"
        }

        meta.displayName(Component.text(offlinePlayer.name ?: "Unknown").color(roleColor)
            .decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()
        lore.add(Component.text("役職: $roleName").color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false))

        val isOnline = offlinePlayer.isOnline
        lore.add(Component.text(if (isOnline) "オンライン" else "オフライン")
            .color(if (isOnline) NamedTextColor.GREEN else NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false))

        // 操作オプション
        if (myRole != null && member.playerUuid != player.uniqueId) {
            lore.add(Component.empty())

            if (myRole.canKick() && member.role != GuildRole.MASTER) {
                lore.add(Component.text("左クリック: キック").color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false))
            }

            if (myRole.canPromote() && member.role.level < myRole.level - 1) {
                lore.add(Component.text("右クリック: 昇格").color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false))
            }

            if (myRole.canDemote() && member.role.level > GuildRole.MEMBER.level && member.role != GuildRole.MASTER) {
                lore.add(Component.text("シフト+右クリック: 降格").color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false))
            }

            if (myRole.canTransfer() && member.role != GuildRole.MASTER) {
                lore.add(Component.text("シフト+左クリック: マスター譲渡").color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false))
            }
        }

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()
            SLOT_PREV_PAGE -> if (page > 0) guiManager.openMembersList(player, page - 1)
            SLOT_NEXT_PAGE -> if (page < maxPage) guiManager.openMembersList(player, page + 1)
            in CONTENT_SLOTS -> handleMemberClick(event)
        }
    }

    private fun handleMemberClick(event: InventoryClickEvent) {
        val index = CONTENT_SLOTS.indexOf(event.slot)
        if (index < 0 || index >= members.size) return

        val member = members[index]
        if (member.playerUuid == player.uniqueId) return
        if (myRole == null) return

        val guild = this.guild ?: return

        when {
            event.isLeftClick && event.isShiftClick && myRole.canTransfer() -> {
                // マスター譲渡
                val targetName = Bukkit.getOfflinePlayer(member.playerUuid).name ?: "Unknown"
                guiManager.openConfirmDialog(
                    player,
                    "マスター譲渡の確認",
                    listOf(
                        "「$targetName」にマスターを譲渡しますか？",
                        "あなたは副マスターになります。"
                    ),
                    onConfirm = {
                        try {
                            guildService.transferMaster(guild.id, member.playerUuid, player.uniqueId)
                            player.sendMessage(Component.text("マスターを譲渡しました").color(NamedTextColor.GREEN))
                            guiManager.openMembersList(player, page)
                        } catch (e: Exception) {
                            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                        }
                    }
                )
            }
            event.isLeftClick && !event.isShiftClick && myRole.canKick() && member.role != GuildRole.MASTER -> {
                // キック
                val targetName = Bukkit.getOfflinePlayer(member.playerUuid).name ?: "Unknown"
                guiManager.openConfirmDialog(
                    player,
                    "キックの確認",
                    listOf("「$targetName」をギルドからキックしますか？"),
                    onConfirm = {
                        try {
                            guildService.kickMember(guild.id, member.playerUuid, player.uniqueId)
                            player.sendMessage(Component.text("${targetName}をキックしました").color(NamedTextColor.GREEN))
                            guiManager.openMembersList(player, page)
                        } catch (e: Exception) {
                            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                        }
                    }
                )
            }
            event.isRightClick && !event.isShiftClick && myRole.canPromote() -> {
                // 昇格
                try {
                    guildService.promoteToViceMaster(guild.id, member.playerUuid, player.uniqueId)
                    val targetName = Bukkit.getOfflinePlayer(member.playerUuid).name ?: "Unknown"
                    player.sendMessage(Component.text("${targetName}を昇格させました").color(NamedTextColor.GREEN))
                    guiManager.openMembersList(player, page)
                } catch (e: Exception) {
                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                }
            }
            event.isRightClick && event.isShiftClick && myRole.canDemote() && member.role != GuildRole.MASTER -> {
                // 降格
                try {
                    guildService.demoteToMember(guild.id, member.playerUuid, player.uniqueId)
                    val targetName = Bukkit.getOfflinePlayer(member.playerUuid).name ?: "Unknown"
                    player.sendMessage(Component.text("${targetName}を降格させました").color(NamedTextColor.GREEN))
                    guiManager.openMembersList(player, page)
                } catch (e: Exception) {
                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                }
            }
        }
    }
}
