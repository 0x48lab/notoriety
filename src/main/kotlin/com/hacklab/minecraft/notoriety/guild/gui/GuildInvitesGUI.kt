package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.GuildInvitation
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
 * 招待一覧GUI
 * - ギルドに所属している場合: 送信した招待一覧 + オンラインプレイヤー招待
 * - ギルドに所属していない場合: 受信した招待一覧
 */
class GuildInvitesGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val page: Int = 0
) : GuildGUI(
    player,
    Component.text("招待一覧").color(NamedTextColor.GREEN),
    54
) {

    private val guild = guildService.getPlayerGuild(player.uniqueId)
    private val myMembership = guildService.getMembership(player.uniqueId)
    private val myRole = myMembership?.role
    private val isInGuild = guild != null

    // ギルドに所属している場合: 送信した招待
    // ギルドに所属していない場合: 受信した招待
    private val invitations: List<GuildInvitation> = if (isInGuild && guild != null) {
        guildService.getSentInvitations(guild.id)
    } else {
        guildService.getPendingInvitations(player.uniqueId)
    }

    companion object {
        private val CONTENT_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )
    }

    override fun setupItems() {
        fillBorder()

        if (isInGuild) {
            setupSentInvitesView()
        } else {
            setupReceivedInvitesView()
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())
    }

    /**
     * 送信した招待一覧（ギルドメンバー用）
     */
    private fun setupSentInvitesView() {
        val canInvite = myRole?.canInvite() == true

        // タイトル
        _inventory.setItem(4, createItem(
            Material.WRITABLE_BOOK,
            Component.text("送信した招待").color(NamedTextColor.GOLD),
            Component.text("保留中: ${invitations.size}件").color(NamedTextColor.GRAY)
        ))

        // 招待一覧
        invitations.forEachIndexed { index, invitation ->
            if (index < CONTENT_SLOTS.size) {
                _inventory.setItem(CONTENT_SLOTS[index], createSentInviteItem(invitation))
            }
        }

        // オンラインプレイヤーを招待ボタン
        if (canInvite) {
            _inventory.setItem(49, createItem(
                Material.EMERALD,
                Component.text("プレイヤーを招待").color(NamedTextColor.GREEN),
                Component.text("オンラインプレイヤーを招待").color(NamedTextColor.GRAY),
                Component.text("クリックで選択画面へ").color(NamedTextColor.DARK_GRAY)
            ))
        }
    }

    /**
     * 受信した招待一覧（ギルド未所属者用）
     */
    private fun setupReceivedInvitesView() {
        // タイトル
        _inventory.setItem(4, createItem(
            Material.PAPER,
            Component.text("受信した招待").color(NamedTextColor.GOLD),
            Component.text("保留中: ${invitations.size}件").color(NamedTextColor.GRAY)
        ))

        if (invitations.isEmpty()) {
            _inventory.setItem(22, createItem(
                Material.PAPER,
                Component.text("招待はありません").color(NamedTextColor.GRAY)
            ))
            return
        }

        // 招待一覧
        invitations.forEachIndexed { index, invitation ->
            if (index < CONTENT_SLOTS.size) {
                _inventory.setItem(CONTENT_SLOTS[index], createReceivedInviteItem(invitation))
            }
        }
    }

    private fun createSentInviteItem(invitation: GuildInvitation): ItemStack {
        val targetPlayer = Bukkit.getOfflinePlayer(invitation.inviteeUuid)
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = targetPlayer
        meta.displayName(Component.text(targetPlayer.name ?: "Unknown").color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false))

        val lore = listOf(
            Component.text("招待日時: ${formatTime(invitation.invitedAt)}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("有効期限: ${formatTime(invitation.expiresAt)}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("クリックで招待を取り消し").color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun createReceivedInviteItem(invitation: GuildInvitation): ItemStack {
        val inviterPlayer = Bukkit.getOfflinePlayer(invitation.inviterUuid)
        val guild = guildService.getGuild(invitation.guildId)

        val item = ItemStack(Material.SHIELD)
        val meta = item.itemMeta

        val guildName = guild?.let { "[${it.tag}] ${it.name}" } ?: "不明なギルド"
        val guildColor = guild?.tagColor?.namedTextColor ?: NamedTextColor.WHITE

        meta.displayName(Component.text(guildName).color(guildColor)
            .decoration(TextDecoration.ITALIC, false))

        val lore = listOf(
            Component.text("招待者: ${inviterPlayer.name ?: "Unknown"}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("有効期限: ${formatTime(invitation.expiresAt)}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("左クリック: 承諾").color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("右クリック: 拒否").color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        )
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun formatTime(instant: java.time.Instant): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()
            49 -> {
                if (isInGuild && myRole?.canInvite() == true) {
                    // プレイヤー招待画面へ
                    openPlayerSelectGUI()
                }
            }
            in CONTENT_SLOTS -> handleInviteClick(event)
        }
    }

    private fun handleInviteClick(event: InventoryClickEvent) {
        val index = CONTENT_SLOTS.indexOf(event.slot)
        if (index < 0 || index >= invitations.size) return

        val invitation = invitations[index]

        if (isInGuild) {
            // 送信した招待をキャンセル
            guiManager.openConfirmDialog(
                player,
                "招待取り消しの確認",
                listOf("この招待を取り消しますか？"),
                onConfirm = {
                    try {
                        guildService.cancelInvitation(invitation.guildId, player.uniqueId, invitation.inviteeUuid)
                        player.sendMessage(Component.text("招待を取り消しました").color(NamedTextColor.GREEN))
                        guiManager.openInvitesList(player, page)
                    } catch (e: Exception) {
                        player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                    }
                }
            )
        } else {
            // 受信した招待を処理
            if (event.isLeftClick) {
                // 承諾
                try {
                    guildService.acceptInvitationByGuildId(invitation.guildId, player.uniqueId)
                    player.sendMessage(Component.text("ギルドに参加しました！").color(NamedTextColor.GREEN))
                    player.closeInventory()
                } catch (e: Exception) {
                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                }
            } else if (event.isRightClick) {
                // 拒否
                try {
                    guildService.declineInvitationByGuildId(invitation.guildId, player.uniqueId)
                    player.sendMessage(Component.text("招待を拒否しました").color(NamedTextColor.YELLOW))
                    guiManager.openInvitesList(player, page)
                } catch (e: Exception) {
                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                }
            }
        }
    }

    private fun openPlayerSelectGUI() {
        GuildPlayerSelectGUI(player, guildService, guiManager).open()
    }
}
