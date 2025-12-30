package com.hacklab.minecraft.notoriety.inspect

import com.hacklab.minecraft.notoriety.command.SubCommand
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class InspectCommand(
    private val inspectService: InspectService,
    private val inspectionStick: InspectionStick,
    private val i18n: I18nManager
) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        if (args.isEmpty()) {
            // Toggle inspect mode
            inspectService.toggleInspectMode(player)
            return true
        }

        return when (args[0].lowercase()) {
            "tool" -> giveTool(player)
            else -> {
                showUsage(sender)
                true
            }
        }
    }

    private fun giveTool(player: Player): Boolean {
        val stick = inspectionStick.createStick()

        val leftover = player.inventory.addItem(stick)
        if (leftover.isEmpty()) {
            player.sendMessage(
                Component.text(i18n.get("inspect.stick_received", "You received an Inspection Stick!"))
                    .color(NamedTextColor.GREEN)
            )
        } else {
            leftover.values.forEach { item ->
                player.world.dropItemNaturally(player.location, item)
            }
            player.sendMessage(
                Component.text(i18n.get("inspect.stick_dropped", "Inventory full! Inspection Stick dropped at your feet."))
                    .color(NamedTextColor.YELLOW)
            )
        }
        return true
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("=== Inspect Commands ===")
        sender.sendMessage("/noty inspect - Toggle inspect mode")
        sender.sendMessage("/noty inspect tool - Get an inspection stick")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("tool").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
