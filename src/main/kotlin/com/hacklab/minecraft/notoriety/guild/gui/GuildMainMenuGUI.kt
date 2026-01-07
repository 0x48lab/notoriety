package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.GuildMembership
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * ギルドメインメニューGUI
 */
class GuildMainMenuGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager
) : GuildGUI(
    player,
    Component.text("ギルドメニュー").color(NamedTextColor.GOLD),
    54
) {

    private val guild: Guild? = guildService.getPlayerGuild(player.uniqueId)
    private val membership: GuildMembership? = guildService.getMembership(player.uniqueId)

    override fun setupItems() {
        fillBorder()

        if (guild != null && membership != null) {
            setupGuildMemberMenu()
        } else {
            setupNoGuildMenu()
        }

        // 閉じるボタン
        _inventory.setItem(SLOT_CLOSE, createCloseButton())
    }

    /**
     * ギルドに所属している場合のメニュー
     */
    private fun setupGuildMemberMenu() {
        val guild = this.guild!!
        val membership = this.membership!!
        val role = membership.role

        // ギルド情報（中央上）
        _inventory.setItem(4, createItem(
            Material.SHIELD,
            Component.text("[${guild.tag}] ${guild.name}").color(guild.tagColor.namedTextColor),
            Component.text("マスター: ${Bukkit.getOfflinePlayer(guild.masterUuid).name}").color(NamedTextColor.GRAY),
            Component.text("あなたの役職: ${getRoleDisplayName(role)}").color(NamedTextColor.GRAY),
            Component.text("メンバー数: ${guildService.getMemberCount(guild.id)}/${guild.maxMembers}").color(NamedTextColor.GRAY)
        ))

        // メンバー一覧（左上）
        _inventory.setItem(20, createItem(
            Material.PLAYER_HEAD,
            Component.text("メンバー一覧").color(NamedTextColor.AQUA),
            Component.text("ギルドメンバーを確認").color(NamedTextColor.GRAY)
        ))

        // 招待一覧（中央左）
        if (role.canInvite()) {
            _inventory.setItem(21, createItem(
                Material.WRITABLE_BOOK,
                Component.text("招待管理").color(NamedTextColor.GREEN),
                Component.text("保留中の招待を確認").color(NamedTextColor.GRAY),
                Component.text("オンラインプレイヤーを招待").color(NamedTextColor.GRAY)
            ))

            // 入会申請一覧（中央）
            val applicationCount = guildService.getPendingApplications(guild.id).size
            _inventory.setItem(22, createItem(
                Material.PAPER,
                Component.text("入会申請").color(NamedTextColor.AQUA),
                Component.text("保留中の申請: ${applicationCount}件").color(NamedTextColor.GRAY),
                Component.text("クリックで確認").color(NamedTextColor.GRAY)
            ))
        } else {
            _inventory.setItem(21, createItem(
                Material.BOOK,
                Component.text("招待確認").color(NamedTextColor.GRAY),
                Component.text("保留中の招待を確認").color(NamedTextColor.GRAY)
            ))
        }

        // タグカラー変更（右上）
        if (role.canChangeTagColor()) {
            _inventory.setItem(24, createItem(
                Material.LIME_DYE,
                Component.text("タグカラー変更").color(NamedTextColor.YELLOW),
                Component.text("現在: ").color(NamedTextColor.GRAY)
                    .append(Component.text(guild.tagColor.name).color(guild.tagColor.namedTextColor)),
                Component.text("クリックで変更").color(NamedTextColor.GRAY)
            ))
        }

        // ギルドチャット情報（左下）
        _inventory.setItem(29, createItem(
            Material.OAK_SIGN,
            Component.text("ギルドチャット").color(NamedTextColor.WHITE),
            Component.text("@をつけてチャットで発言").color(NamedTextColor.GRAY),
            Component.text("例: @こんにちは").color(NamedTextColor.DARK_GRAY)
        ))

        // ギルド一覧（中央下）
        _inventory.setItem(31, createItem(
            Material.COMPASS,
            Component.text("ギルド一覧").color(NamedTextColor.AQUA),
            Component.text("他のギルドを確認").color(NamedTextColor.GRAY)
        ))

        // 脱退（右下）
        if (role != GuildRole.MASTER) {
            _inventory.setItem(33, createItem(
                Material.IRON_DOOR,
                Component.text("ギルドを脱退").color(NamedTextColor.RED),
                Component.text("クリックで脱退").color(NamedTextColor.GRAY)
            ))
        } else {
            // マスターの場合は解散
            _inventory.setItem(33, createItem(
                Material.TNT,
                Component.text("ギルドを解散").color(NamedTextColor.DARK_RED),
                Component.text("警告: この操作は取り消せません").color(NamedTextColor.RED),
                Component.text("クリックで解散").color(NamedTextColor.GRAY)
            ))
        }
    }

    /**
     * ギルドに所属していない場合のメニュー
     */
    private fun setupNoGuildMenu() {
        // 説明
        _inventory.setItem(4, createItem(
            Material.PAPER,
            Component.text("ギルドに所属していません").color(NamedTextColor.YELLOW),
            Component.text("ギルドを作成するか、招待を受けてください").color(NamedTextColor.GRAY)
        ))

        // ギルド作成
        _inventory.setItem(20, createItem(
            Material.GOLDEN_SWORD,
            Component.text("ギルドを作成").color(NamedTextColor.GREEN),
            Component.text("新しいギルドを作成します").color(NamedTextColor.GRAY),
            Component.text("コマンド: /guild create <名前> <タグ>").color(NamedTextColor.DARK_GRAY)
        ))

        // 招待確認
        val pendingInvites = guildService.getPendingInvitations(player.uniqueId)
        _inventory.setItem(22, createItem(
            Material.PAPER,
            Component.text("招待一覧").color(NamedTextColor.AQUA),
            Component.text("受け取った招待: ${pendingInvites.size}件").color(NamedTextColor.GRAY)
        ))

        // ギルド一覧
        _inventory.setItem(24, createItem(
            Material.COMPASS,
            Component.text("ギルド一覧").color(NamedTextColor.AQUA),
            Component.text("存在するギルドを確認").color(NamedTextColor.GRAY)
        ))
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot

        when (slot) {
            SLOT_CLOSE -> player.closeInventory()

            4 -> {
                // ギルド情報クリック - 詳細表示
                if (guild != null) {
                    guiManager.openGuildInfo(player, guild.id)
                }
            }

            20 -> {
                if (guild != null) {
                    // メンバー一覧
                    guiManager.openMembersList(player)
                } else {
                    // ギルド作成 - コマンドで案内
                    player.closeInventory()
                    player.sendMessage(Component.text("ギルドを作成するには、以下のコマンドを使用してください:").color(NamedTextColor.YELLOW))
                    player.sendMessage(Component.text("/guild create <名前> <タグ> [色]").color(NamedTextColor.WHITE))
                }
            }

            21 -> {
                if (guild != null) {
                    // 招待管理
                    guiManager.openInvitesList(player)
                }
            }

            22 -> {
                if (guild != null && membership?.role?.canInvite() == true) {
                    // 入会申請一覧
                    guiManager.openApplicationsList(player)
                } else if (guild == null) {
                    // 招待一覧（受信）
                    guiManager.openInvitesList(player)
                }
            }

            24 -> {
                if (guild != null && membership?.role?.canChangeTagColor() == true) {
                    // タグカラー変更
                    guiManager.openColorSelect(player)
                } else if (guild == null) {
                    // ギルド一覧
                    guiManager.openGuildList(player)
                }
            }

            29 -> {
                // ギルドチャット情報 - 何もしない
            }

            31 -> {
                // ギルド一覧
                guiManager.openGuildList(player)
            }

            33 -> {
                if (guild != null) {
                    if (membership?.role == GuildRole.MASTER) {
                        // 解散確認
                        guiManager.openConfirmDialog(
                            player,
                            "ギルド解散の確認",
                            listOf(
                                "本当に「${guild.name}」を解散しますか？",
                                "この操作は取り消せません！",
                                "全メンバーがギルドから除外されます。"
                            ),
                            onConfirm = {
                                try {
                                    guildService.dissolveGuild(guild.id, player.uniqueId)
                                    player.sendMessage(Component.text("ギルドを解散しました").color(NamedTextColor.GREEN))
                                } catch (e: Exception) {
                                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                                }
                            }
                        )
                    } else {
                        // 脱退確認
                        guiManager.openConfirmDialog(
                            player,
                            "ギルド脱退の確認",
                            listOf(
                                "本当に「${guild.name}」を脱退しますか？"
                            ),
                            onConfirm = {
                                try {
                                    guildService.leaveGuild(player.uniqueId)
                                    player.sendMessage(Component.text("ギルドを脱退しました").color(NamedTextColor.GREEN))
                                } catch (e: Exception) {
                                    player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getRoleDisplayName(role: GuildRole): String {
        return when (role) {
            GuildRole.MASTER -> "ギルドマスター"
            GuildRole.VICE_MASTER -> "副マスター"
            GuildRole.MEMBER -> "メンバー"
        }
    }
}
