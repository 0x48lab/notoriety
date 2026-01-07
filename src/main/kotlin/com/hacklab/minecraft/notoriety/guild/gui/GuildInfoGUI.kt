package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.GuildRole
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
 * ギルド詳細情報GUI
 */
class GuildInfoGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val guildId: Long
) : GuildGUI(
    player,
    Component.text("ギルド情報").color(NamedTextColor.GOLD),
    54
) {

    private val guild = guildService.getGuild(guildId)
    private val myGuild = guildService.getPlayerGuild(player.uniqueId)
    private val isMyGuild = guild?.id == myGuild?.id

    override fun setupItems() {
        fillBorder()

        if (guild == null) {
            _inventory.setItem(22, createItem(
                Material.BARRIER,
                Component.text("ギルドが見つかりません").color(NamedTextColor.RED)
            ))
            _inventory.setItem(SLOT_BACK, createBackButton())
            _inventory.setItem(SLOT_CLOSE, createCloseButton())
            return
        }

        val memberCount = guildService.getMemberCount(guild.id)
        val masterName = Bukkit.getOfflinePlayer(guild.masterUuid).name ?: "Unknown"

        // ギルド情報（中央上）
        _inventory.setItem(4, createItem(
            Material.SHIELD,
            Component.text("[${guild.tag}] ${guild.name}").color(guild.tagColor.namedTextColor),
            Component.text("タグカラー: ${guild.tagColor.name}").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ))

        // マスター情報
        val masterHead = ItemStack(Material.PLAYER_HEAD)
        val masterMeta = masterHead.itemMeta as SkullMeta
        masterMeta.owningPlayer = Bukkit.getOfflinePlayer(guild.masterUuid)
        masterMeta.displayName(Component.text("ギルドマスター").color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false))
        masterMeta.lore(listOf(
            Component.text(masterName).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        ))
        masterHead.itemMeta = masterMeta
        _inventory.setItem(20, masterHead)

        // メンバー数
        _inventory.setItem(22, createItem(
            Material.PLAYER_HEAD,
            Component.text("メンバー").color(NamedTextColor.AQUA),
            Component.text("$memberCount / ${guild.maxMembers}").color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        ))

        // 作成日
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())
        _inventory.setItem(24, createItem(
            Material.CLOCK,
            Component.text("作成日").color(NamedTextColor.YELLOW),
            Component.text(formatter.format(guild.createdAt)).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
        ))

        // 説明
        if (!guild.description.isNullOrBlank()) {
            _inventory.setItem(31, createItem(
                Material.BOOK,
                Component.text("説明").color(NamedTextColor.WHITE),
                Component.text(guild.description).color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            ))
        }

        // 役職一覧
        val members = guildService.getMembers(guild.id, 0, 100)
        val viceMasters = members.filter { it.role == GuildRole.VICE_MASTER }
        val regularMembers = members.filter { it.role == GuildRole.MEMBER }

        // 副マスター一覧
        if (viceMasters.isNotEmpty()) {
            val vmNames = viceMasters.take(5).mapNotNull {
                Bukkit.getOfflinePlayer(it.playerUuid).name
            }
            _inventory.setItem(29, createItem(
                Material.GOLDEN_HELMET,
                Component.text("副マスター (${viceMasters.size})").color(NamedTextColor.YELLOW),
                *vmNames.map {
                    Component.text("- $it").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                }.toTypedArray()
            ))
        }

        // メンバー一覧（一部）
        if (regularMembers.isNotEmpty()) {
            val memNames = regularMembers.take(5).mapNotNull {
                Bukkit.getOfflinePlayer(it.playerUuid).name
            }
            val more = if (regularMembers.size > 5) "他 ${regularMembers.size - 5}人..." else ""
            val lore = memNames.map {
                Component.text("- $it").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            }.toMutableList()
            if (more.isNotEmpty()) {
                lore.add(Component.text(more).color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false))
            }
            _inventory.setItem(33, createItem(
                Material.IRON_HELMET,
                Component.text("メンバー (${regularMembers.size})").color(NamedTextColor.WHITE),
                *lore.toTypedArray()
            ))
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())

        // 自分のギルドの場合はメンバー管理へのリンク
        if (isMyGuild) {
            _inventory.setItem(49, createItem(
                Material.COMPASS,
                Component.text("メンバー管理").color(NamedTextColor.GREEN),
                Component.text("クリックでメンバー一覧へ").color(NamedTextColor.GRAY)
            ))
        } else if (myGuild == null && guild != null) {
            // 自分がどのギルドにも所属していない場合は入会申請ボタンを表示
            val hasInvitation = guildService.hasInvitation(guild.id, player.uniqueId)
            val hasApplication = guildService.hasApplication(guild.id, player.uniqueId)

            when {
                hasInvitation -> {
                    // 既に招待がある場合
                    _inventory.setItem(40, createItem(
                        Material.WRITABLE_BOOK,
                        Component.text("招待を受けています").color(NamedTextColor.GREEN),
                        Component.text("クリックで招待を承諾").color(NamedTextColor.GRAY)
                    ))
                }
                hasApplication -> {
                    // 既に申請済みの場合
                    _inventory.setItem(40, createItem(
                        Material.CLOCK,
                        Component.text("申請中").color(NamedTextColor.YELLOW),
                        Component.text("審査待ちです").color(NamedTextColor.GRAY),
                        Component.text("クリックで申請をキャンセル").color(NamedTextColor.RED)
                    ))
                }
                memberCount < guild.maxMembers -> {
                    // メンバーに空きがある場合のみ申請可能
                    _inventory.setItem(40, createItem(
                        Material.PAPER,
                        Component.text("入会申請").color(NamedTextColor.AQUA),
                        Component.text("クリックでこのギルドに入会申請").color(NamedTextColor.GRAY)
                    ))
                }
                else -> {
                    // ギルドが満員
                    _inventory.setItem(40, createItem(
                        Material.BARRIER,
                        Component.text("入会不可").color(NamedTextColor.RED),
                        Component.text("このギルドは満員です").color(NamedTextColor.GRAY)
                    ))
                }
            }
        }
    }

    override fun handleClick(event: InventoryClickEvent) {
        when (event.slot) {
            SLOT_BACK -> guiManager.openGuildList(player)
            SLOT_CLOSE -> player.closeInventory()
            40 -> {
                if (myGuild == null && guild != null) {
                    val hasInvitation = guildService.hasInvitation(guild.id, player.uniqueId)
                    val hasApplication = guildService.hasApplication(guild.id, player.uniqueId)

                    when {
                        hasInvitation -> {
                            // 招待を承諾
                            try {
                                guildService.acceptInvitationByGuildId(guild.id, player.uniqueId)
                                player.sendMessage(Component.text("${guild.name} に参加しました！")
                                    .color(NamedTextColor.GREEN))
                                player.closeInventory()
                            } catch (e: GuildException) {
                                player.sendMessage(Component.text(e.message ?: "エラーが発生しました")
                                    .color(NamedTextColor.RED))
                            }
                        }
                        hasApplication -> {
                            // 申請をキャンセル
                            guiManager.openConfirmDialog(
                                player,
                                "申請キャンセル",
                                listOf("${guild.name} への入会申請をキャンセルしますか？"),
                                onConfirm = {
                                    try {
                                        guildService.cancelApplication(guild.id, player.uniqueId)
                                        player.sendMessage(Component.text("入会申請をキャンセルしました")
                                            .color(NamedTextColor.YELLOW))
                                        guiManager.openGuildInfo(player, guildId)
                                    } catch (e: GuildException) {
                                        player.sendMessage(Component.text(e.message ?: "エラーが発生しました")
                                            .color(NamedTextColor.RED))
                                    }
                                },
                                onCancel = {
                                    guiManager.openGuildInfo(player, guildId)
                                }
                            )
                        }
                        else -> {
                            // 入会申請
                            guiManager.openConfirmDialog(
                                player,
                                "入会申請",
                                listOf("${guild.name} に入会申請しますか？"),
                                onConfirm = {
                                    try {
                                        guildService.applyToGuild(guild.id, player.uniqueId)
                                        player.sendMessage(Component.text("${guild.name} に入会申請しました")
                                            .color(NamedTextColor.GREEN))
                                        guiManager.openGuildInfo(player, guildId)
                                    } catch (e: GuildException) {
                                        player.sendMessage(Component.text(e.message ?: "エラーが発生しました")
                                            .color(NamedTextColor.RED))
                                    }
                                },
                                onCancel = {
                                    guiManager.openGuildInfo(player, guildId)
                                }
                            )
                        }
                    }
                }
            }
            49 -> {
                if (isMyGuild) {
                    guiManager.openMembersList(player)
                }
            }
        }
    }
}
