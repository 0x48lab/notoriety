package com.hacklab.minecraft.notoriety.zone.command

import com.hacklab.minecraft.notoriety.command.SubCommand
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.zone.model.ZoneSelection
import com.hacklab.minecraft.notoriety.zone.service.ZoneCreateResult
import com.hacklab.minecraft.notoriety.zone.service.ZoneService
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ZoneCommand(
    private val zoneService: ZoneService,
    private val i18n: I18nManager
) : SubCommand {

    val selections = ConcurrentHashMap<UUID, ZoneSelection>()

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("notoriety.admin")) {
            i18n.sendError(sender, "general.no_permission", "You don't have permission")
            return true
        }

        if (args.isEmpty()) {
            i18n.send(sender, "zone.usage", "Usage: /noty zone <create|remove|list|info|tool>")
            return true
        }

        return when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            "tool" -> handleTool(sender)
            else -> {
                i18n.send(sender, "zone.usage", "Usage: /noty zone <create|remove|list|info|tool>")
                true
            }
        }
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        if (args.size < 2) {
            i18n.send(sender, "zone.usage", "Usage: /noty zone create <name> [x1 y1 z1 x2 y2 z2]")
            return true
        }

        val name = args[1]

        val worldName: String
        val x1: Int; val y1: Int; val z1: Int
        val x2: Int; val y2: Int; val z2: Int

        if (args.size >= 9) {
            // Coordinate-based creation
            worldName = player.world.name
            try {
                x1 = args[2].toInt(); y1 = args[3].toInt(); z1 = args[4].toInt()
                x2 = args[5].toInt(); y2 = args[6].toInt(); z2 = args[7].toInt()
            } catch (e: NumberFormatException) {
                i18n.sendError(player, "zone.usage", "Usage: /noty zone create <name> <x1> <y1> <z1> <x2> <y2> <z2>")
                return true
            }
        } else {
            // Selection-based creation
            val selection = selections[player.uniqueId]
            val pos1 = selection?.pos1
            val pos2 = selection?.pos2

            if (pos1 == null || pos2 == null) {
                i18n.sendError(player, "zone.no_selection", "座標が選択されていません。ゾーンツールで2点を選択してください")
                return true
            }

            if (pos1.world != pos2.world) {
                i18n.sendError(player, "zone.no_selection", "2点は同じワールドで選択してください")
                return true
            }

            worldName = pos1.world
            x1 = pos1.x; y1 = pos1.y; z1 = pos1.z
            x2 = pos2.x; y2 = pos2.y; z2 = pos2.z
        }

        val result = zoneService.createZone(name, worldName, x1, y1, z1, x2, y2, z2, player.uniqueId)

        when (result) {
            ZoneCreateResult.SUCCESS -> {
                // Clear selection after creation
                selections.remove(player.uniqueId)
                val minX = minOf(x1, x2); val minY = minOf(y1, y2); val minZ = minOf(z1, z2)
                val maxX = maxOf(x1, x2); val maxY = maxOf(y1, y2); val maxZ = maxOf(z1, z2)
                i18n.sendSuccess(player, "zone.created",
                    "保護エリア '%s' を作成しました (%s: %d,%d,%d - %d,%d,%d)",
                    name, worldName, minX, minY, minZ, maxX, maxY, maxZ)
            }
            ZoneCreateResult.DUPLICATE_NAME ->
                i18n.sendError(player, "zone.duplicate_name", "同じ名前の保護エリアが既に存在します")
            ZoneCreateResult.INVALID_NAME ->
                i18n.sendError(player, "zone.invalid_name", "エリア名は1〜32文字で指定してください")
            ZoneCreateResult.INVALID_WORLD ->
                i18n.sendError(player, "zone.usage", "無効なワールドです")
        }

        return true
    }

    private fun handleRemove(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            i18n.send(sender, "zone.usage", "Usage: /noty zone remove <name>")
            return true
        }

        val name = args[1]
        if (zoneService.removeZone(name)) {
            i18n.sendSuccess(sender, "zone.removed", "保護エリア '%s' を削除しました", name)
        } else {
            i18n.sendError(sender, "zone.not_found", "保護エリア '%s' が見つかりません", name)
        }

        return true
    }

    private fun handleList(sender: CommandSender): Boolean {
        val zones = zoneService.getAllZones()
        if (zones.isEmpty()) {
            i18n.send(sender, "zone.list_empty", "保護エリアはありません")
            return true
        }

        i18n.sendHeader(sender, "zone.list_header", "=== 保護エリア一覧 (%d件) ===", zones.size)
        zones.forEach { zone ->
            i18n.send(sender, "zone.list_item",
                "- %s [%s: %d,%d,%d - %d,%d,%d]",
                zone.name, zone.worldName, zone.x1, zone.y1, zone.z1, zone.x2, zone.y2, zone.z2)
        }

        return true
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            i18n.send(sender, "zone.usage", "Usage: /noty zone info <name>")
            return true
        }

        val zone = zoneService.getZoneByName(args[1])
        if (zone == null) {
            i18n.sendError(sender, "zone.not_found", "保護エリア '%s' が見つかりません", args[1])
            return true
        }

        val creatorName = Bukkit.getOfflinePlayer(zone.creatorUuid).name ?: zone.creatorUuid.toString()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

        i18n.sendHeader(sender, "zone.info_header", "=== 保護エリア: %s ===", zone.name)
        i18n.send(sender, "zone.info_world", "ワールド: %s", zone.worldName)
        i18n.send(sender, "zone.info_coords", "座標: %d,%d,%d - %d,%d,%d",
            zone.x1, zone.y1, zone.z1, zone.x2, zone.y2, zone.z2)
        i18n.send(sender, "zone.info_creator", "作成者: %s", creatorName)
        i18n.send(sender, "zone.info_created_at", "作成日時: %s", formatter.format(zone.createdAt))

        return true
    }

    private fun handleTool(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
        i18n.sendSuccess(player, "zone.tool_given", "ゾーンツールを取得しました")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (!sender.hasPermission("notoriety.admin")) return emptyList()

        return when (args.size) {
            1 -> listOf("create", "remove", "list", "info", "tool")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "remove", "info" -> zoneService.getAllZones().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
