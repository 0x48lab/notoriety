package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.GuildApplication
import com.hacklab.minecraft.notoriety.guild.service.GuildException
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 入会申請一覧GUI（マスター/副マスター用）
 */
class GuildApplicationsGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val page: Int = 0
) : GuildGUI(
    player,
    Component.text("入会申請一覧").color(NamedTextColor.GOLD),
    54
) {

    private val guild = guildService.getPlayerGuild(player.uniqueId)
    private val applications: List<GuildApplication> = guild?.let {
        guildService.getPendingApplications(it.id)
    } ?: emptyList()

    private val itemsPerPage = 28
    private val maxPage = if (applications.isEmpty()) 1 else (applications.size - 1) / itemsPerPage + 1

    override fun setupItems() {
        fillBorder()

        if (guild == null) {
            _inventory.setItem(22, createItem(
                Material.BARRIER,
                Component.text("ギルドに所属していません").color(NamedTextColor.RED)
            ))
            _inventory.setItem(SLOT_CLOSE, createCloseButton())
            return
        }

        if (applications.isEmpty()) {
            _inventory.setItem(22, createItem(
                Material.PAPER,
                Component.text("入会申請はありません").color(NamedTextColor.GRAY)
            ))
        } else {
            // 申請を表示
            val startIndex = page * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, applications.size)
            val pageApplications = applications.subList(startIndex, endIndex)

            val slots = listOf(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            )

            pageApplications.forEachIndexed { index, application ->
                if (index < slots.size) {
                    _inventory.setItem(slots[index], createApplicationItem(application))
                }
            }
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())

        // ページネーション
        if (page > 0) {
            _inventory.setItem(SLOT_PREV_PAGE, createPrevPageButton(page + 1, maxPage))
        }
        if (page < maxPage - 1) {
            _inventory.setItem(SLOT_NEXT_PAGE, createNextPageButton(page + 1, maxPage))
        }
    }

    private fun createApplicationItem(application: GuildApplication): ItemStack {
        val offlinePlayer = Bukkit.getOfflinePlayer(application.applicantUuid)
        val playerName = offlinePlayer.name ?: "Unknown"

        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as SkullMeta
        meta.owningPlayer = offlinePlayer
        meta.displayName(
            Component.text(playerName).color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
        )

        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            .withZone(ZoneId.systemDefault())

        val lore = mutableListOf(
            Component.text("申請日: ${formatter.format(application.appliedAt)}")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("残り: ${application.remainingDays()}日")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        )

        if (!application.message.isNullOrBlank()) {
            lore.add(Component.empty())
            lore.add(Component.text("メッセージ:").color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(application.message).color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
        }

        lore.add(Component.empty())
        lore.add(Component.text("左クリック: 承認").color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("右クリック: 拒否").color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false))

        meta.lore(lore)
        head.itemMeta = meta
        return head
    }

    override fun handleClick(event: InventoryClickEvent) {
        when (event.slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()
            SLOT_PREV_PAGE -> {
                if (page > 0) {
                    guiManager.openApplicationsList(player, page - 1)
                }
            }
            SLOT_NEXT_PAGE -> {
                if (page < maxPage - 1) {
                    guiManager.openApplicationsList(player, page + 1)
                }
            }
            else -> {
                // 申請アイテムのクリック処理
                val item = event.currentItem ?: return
                if (item.type != Material.PLAYER_HEAD) return

                val slots = listOf(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
                )

                val slotIndex = slots.indexOf(event.slot)
                if (slotIndex == -1) return

                val applicationIndex = page * itemsPerPage + slotIndex
                if (applicationIndex >= applications.size) return

                val application = applications[applicationIndex]
                val applicantName = Bukkit.getOfflinePlayer(application.applicantUuid).name ?: "Unknown"

                if (event.isLeftClick) {
                    // 承認
                    guiManager.openConfirmDialog(
                        player,
                        "申請承認",
                        listOf("${applicantName} の入会申請を承認しますか？"),
                        onConfirm = {
                            try {
                                guildService.approveApplication(application.id, player.uniqueId)
                                player.sendMessage(
                                    Component.text("${applicantName} の入会申請を承認しました")
                                        .color(NamedTextColor.GREEN)
                                )
                                // 承認されたプレイヤーに通知
                                Bukkit.getPlayer(application.applicantUuid)?.sendMessage(
                                    Component.text("${guild?.name} への入会申請が承認されました！")
                                        .color(NamedTextColor.GREEN)
                                )
                                guiManager.openApplicationsList(player, page)
                            } catch (e: GuildException) {
                                player.sendMessage(
                                    Component.text(e.message ?: "エラーが発生しました")
                                        .color(NamedTextColor.RED)
                                )
                            }
                        },
                        onCancel = {
                            guiManager.openApplicationsList(player, page)
                        }
                    )
                } else if (event.isRightClick) {
                    // 拒否
                    guiManager.openConfirmDialog(
                        player,
                        "申請拒否",
                        listOf("${applicantName} の入会申請を拒否しますか？"),
                        onConfirm = {
                            try {
                                guildService.rejectApplication(application.id, player.uniqueId)
                                player.sendMessage(
                                    Component.text("${applicantName} の入会申請を拒否しました")
                                        .color(NamedTextColor.YELLOW)
                                )
                                // 拒否されたプレイヤーに通知
                                Bukkit.getPlayer(application.applicantUuid)?.sendMessage(
                                    Component.text("${guild?.name} への入会申請が拒否されました")
                                        .color(NamedTextColor.RED)
                                )
                                guiManager.openApplicationsList(player, page)
                            } catch (e: GuildException) {
                                player.sendMessage(
                                    Component.text(e.message ?: "エラーが発生しました")
                                        .color(NamedTextColor.RED)
                                )
                            }
                        },
                        onCancel = {
                            guiManager.openApplicationsList(player, page)
                        }
                    )
                }
            }
        }
    }
}
