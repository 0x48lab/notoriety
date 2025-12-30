package com.hacklab.minecraft.notoriety.guild.command

import com.hacklab.minecraft.notoriety.guild.gui.GuildGUIManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * /guild menu - GUIメニューを開く
 */
class GuildMenuCommand(
    private val guiManager: GuildGUIManager
) : GuildSubCommand {

    override val name = "menu"
    override val description = "Open the guild GUI menu"
    override val usage = "/guild menu"
    override val aliases = listOf("gui", "m")
    override val requiresPlayer = true

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as Player
        guiManager.openMainMenu(player)
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}
