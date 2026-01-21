package com.hacklab.minecraft.notoriety.guild.gui

import com.hacklab.minecraft.notoriety.guild.model.Guild
import com.hacklab.minecraft.notoriety.guild.model.GuildRole
import com.hacklab.minecraft.notoriety.guild.service.GuildException
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * ギルド設定GUI（マスター専用）
 */
class GuildSettingsGUI(
    player: Player,
    private val guildService: GuildService,
    private val guiManager: GuildGUIManager,
    private val inputManager: GuildInputManager
) : GuildGUI(
    player,
    Component.text("ギルド設定").color(NamedTextColor.GOLD),
    54
) {

    private val guild: Guild? = guildService.getPlayerGuild(player.uniqueId)
    private val membership = guildService.getMembership(player.uniqueId)
    private val territoryService: TerritoryService? = guiManager.territoryService

    companion object {
        private const val SLOT_GUILD_INFO = 4
        private const val SLOT_NAME = 20
        private const val SLOT_TAG = 22
        private const val SLOT_DESCRIPTION = 24
        private const val SLOT_TAG_COLOR = 31
        private const val SLOT_MOB_SPAWN = 40
    }

    override fun setupItems() {
        fillBorder()

        if (guild == null || membership == null) {
            _inventory.setItem(22, createItem(
                Material.BARRIER,
                Component.text("ギルドに所属していません").color(NamedTextColor.RED)
            ))
            _inventory.setItem(SLOT_BACK, createBackButton())
            _inventory.setItem(SLOT_CLOSE, createCloseButton())
            return
        }

        if (membership.role != GuildRole.MASTER) {
            _inventory.setItem(22, createItem(
                Material.BARRIER,
                Component.text("ギルドマスターのみ設定を変更できます").color(NamedTextColor.RED)
            ))
            _inventory.setItem(SLOT_BACK, createBackButton())
            _inventory.setItem(SLOT_CLOSE, createCloseButton())
            return
        }

        // ギルド情報ヘッダー
        _inventory.setItem(SLOT_GUILD_INFO, createItem(
            Material.SHIELD,
            Component.text("[${guild.tag}] ${guild.name}").color(guild.tagColor.namedTextColor),
            Component.text("ギルド設定を変更できます").color(NamedTextColor.GRAY)
        ))

        // ギルド名変更
        _inventory.setItem(SLOT_NAME, createItem(
            Material.NAME_TAG,
            Component.text("ギルド名を変更").color(NamedTextColor.YELLOW),
            Component.text("現在: ${guild.name}").color(NamedTextColor.GRAY),
            Component.empty(),
            Component.text("クリックで変更").color(NamedTextColor.GREEN)
        ))

        // タグ変更
        _inventory.setItem(SLOT_TAG, createItem(
            Material.PAPER,
            Component.text("タグを変更").color(NamedTextColor.AQUA),
            Component.text("現在: ${guild.tag}").color(NamedTextColor.GRAY),
            Component.text("2-4文字の英数字").color(NamedTextColor.DARK_GRAY),
            Component.empty(),
            Component.text("クリックで変更").color(NamedTextColor.GREEN)
        ))

        // 説明変更
        val descLore = mutableListOf<Component>()
        if (guild.description != null) {
            descLore.add(Component.text("現在: ${guild.description}").color(NamedTextColor.GRAY))
        } else {
            descLore.add(Component.text("現在: (未設定)").color(NamedTextColor.DARK_GRAY))
        }
        descLore.add(Component.empty())
        descLore.add(Component.text("クリックで変更").color(NamedTextColor.GREEN))

        _inventory.setItem(SLOT_DESCRIPTION, createItem(
            Material.WRITABLE_BOOK,
            Component.text("説明を変更").color(NamedTextColor.LIGHT_PURPLE),
            *descLore.toTypedArray()
        ))

        // タグカラー変更
        _inventory.setItem(SLOT_TAG_COLOR, createItem(
            Material.LIME_DYE,
            Component.text("タグカラーを変更").color(NamedTextColor.WHITE),
            Component.text("現在: ").color(NamedTextColor.GRAY)
                .append(Component.text(guild.tagColor.name).color(guild.tagColor.namedTextColor)),
            Component.empty(),
            Component.text("クリックで変更").color(NamedTextColor.GREEN)
        ))

        // モンスタースポーン設定（領地がある場合のみ表示）
        if (territoryService != null) {
            val territory = territoryService.getTerritory(guild.id)
            if (territory != null) {
                val mobSpawnEnabled = territory.mobSpawnEnabled
                val statusText = if (mobSpawnEnabled) {
                    Component.text("有効").color(NamedTextColor.GREEN)
                } else {
                    Component.text("無効").color(NamedTextColor.RED)
                }
                val material = if (mobSpawnEnabled) Material.ZOMBIE_HEAD else Material.BARRIER

                _inventory.setItem(SLOT_MOB_SPAWN, createItem(
                    material,
                    Component.text("モンスタースポーン設定").color(NamedTextColor.DARK_PURPLE),
                    Component.text("現在: ").color(NamedTextColor.GRAY).append(statusText),
                    Component.empty(),
                    Component.text("領地内のモンスターのスポーンを").color(NamedTextColor.GRAY),
                    Component.text("許可または禁止します").color(NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("クリックで切り替え").color(NamedTextColor.YELLOW)
                ))
            }
        }

        // ナビゲーション
        _inventory.setItem(SLOT_BACK, createBackButton())
        _inventory.setItem(SLOT_CLOSE, createCloseButton())
    }

    override fun handleClick(event: InventoryClickEvent) {
        val slot = event.slot
        val guild = this.guild ?: return

        when (slot) {
            SLOT_BACK -> guiManager.openMainMenu(player)
            SLOT_CLOSE -> player.closeInventory()

            SLOT_NAME -> {
                player.closeInventory()
                inputManager.startInput(
                    player = player,
                    type = GuildInputManager.InputType.NAME,
                    guildId = guild.id,
                    callback = { newName ->
                        try {
                            guildService.setName(guild.id, newName, player.uniqueId)
                            player.sendMessage(
                                Component.text("ギルド名を「$newName」に変更しました").color(NamedTextColor.GREEN)
                            )
                        } catch (e: GuildException.InvalidName) {
                            player.sendMessage(Component.text("無効なギルド名です（3-32文字）").color(NamedTextColor.RED))
                        } catch (e: GuildException.NameTaken) {
                            player.sendMessage(Component.text("このギルド名は既に使用されています").color(NamedTextColor.RED))
                        } catch (e: Exception) {
                            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                        }
                        // GUIを再度開く
                        guiManager.openSettings(player)
                    },
                    onCancel = {
                        guiManager.openSettings(player)
                    }
                )
            }

            SLOT_TAG -> {
                player.closeInventory()
                inputManager.startInput(
                    player = player,
                    type = GuildInputManager.InputType.TAG,
                    guildId = guild.id,
                    callback = { newTag ->
                        try {
                            guildService.setTag(guild.id, newTag, player.uniqueId)
                            player.sendMessage(
                                Component.text("タグを「${newTag.uppercase()}」に変更しました").color(NamedTextColor.GREEN)
                            )
                        } catch (e: GuildException.InvalidTag) {
                            player.sendMessage(Component.text("無効なタグです（2-4文字の英数字）").color(NamedTextColor.RED))
                        } catch (e: GuildException.TagTaken) {
                            player.sendMessage(Component.text("このタグは既に使用されています").color(NamedTextColor.RED))
                        } catch (e: Exception) {
                            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                        }
                        guiManager.openSettings(player)
                    },
                    onCancel = {
                        guiManager.openSettings(player)
                    }
                )
            }

            SLOT_DESCRIPTION -> {
                player.closeInventory()
                inputManager.startInput(
                    player = player,
                    type = GuildInputManager.InputType.DESCRIPTION,
                    guildId = guild.id,
                    callback = { newDescription ->
                        try {
                            // 空の場合はnullにする
                            val desc = if (newDescription.isBlank()) null else newDescription
                            guildService.setDescription(guild.id, desc, player.uniqueId)
                            if (desc != null) {
                                player.sendMessage(
                                    Component.text("説明を変更しました").color(NamedTextColor.GREEN)
                                )
                            } else {
                                player.sendMessage(
                                    Component.text("説明を削除しました").color(NamedTextColor.YELLOW)
                                )
                            }
                        } catch (e: Exception) {
                            player.sendMessage(Component.text("エラー: ${e.message}").color(NamedTextColor.RED))
                        }
                        guiManager.openSettings(player)
                    },
                    onCancel = {
                        guiManager.openSettings(player)
                    }
                )
            }

            SLOT_TAG_COLOR -> {
                guiManager.openColorSelect(player)
            }

            SLOT_MOB_SPAWN -> {
                if (territoryService != null) {
                    val territory = territoryService.getTerritory(guild.id)
                    if (territory != null) {
                        // 現在の設定を反転
                        val newValue = !territory.mobSpawnEnabled
                        if (territoryService.setMobSpawnEnabled(guild.id, newValue)) {
                            if (newValue) {
                                player.sendMessage(
                                    Component.text("領地内のモンスタースポーンを有効にしました").color(NamedTextColor.GREEN)
                                )
                            } else {
                                player.sendMessage(
                                    Component.text("領地内のモンスタースポーンを無効にしました").color(NamedTextColor.YELLOW)
                                )
                            }
                            // GUIを更新
                            guiManager.openSettings(player)
                        } else {
                            player.sendMessage(
                                Component.text("設定の更新に失敗しました").color(NamedTextColor.RED)
                            )
                        }
                    }
                }
            }
        }
    }
}
