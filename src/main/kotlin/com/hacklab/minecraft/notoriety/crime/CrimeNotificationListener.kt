package com.hacklab.minecraft.notoriety.crime

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.event.PlayerCrimeEvent
import com.hacklab.minecraft.notoriety.reputation.NameColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class CrimeNotificationListener(
    private val i18n: I18nManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerCrime(event: PlayerCrimeEvent) {
        val player = Bukkit.getPlayer(event.criminalUuid) ?: return

        val crimeTypeName = i18n.get("crime.${event.crimeType.name.lowercase()}", event.crimeType.name)
        val message = i18n.get(
            "notification.crime_committed",
            "[Notoriety] %s (Alignment -%d)",
            crimeTypeName,
            event.alignmentPenalty
        )

        player.sendMessage(
            Component.text(message).color(NamedTextColor.RED)
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onColorChange(event: PlayerColorChangeEvent) {
        val player = Bukkit.getPlayer(event.playerUuid) ?: return

        val newColorName = when (event.newColor) {
            NameColor.BLUE -> i18n.get("status.innocent", "Innocent (Blue)")
            NameColor.GRAY -> i18n.get("status.criminal", "Criminal (Gray)")
            NameColor.RED -> i18n.get("status.murderer", "Murderer (Red)")
        }

        val messageKey = when {
            event.newColor == NameColor.RED -> "notification.became_red"
            event.newColor == NameColor.GRAY -> "notification.became_gray"
            event.oldColor == NameColor.RED && event.newColor != NameColor.RED -> "notification.no_longer_red"
            event.oldColor == NameColor.GRAY && event.newColor == NameColor.BLUE -> "notification.became_blue"
            else -> return
        }

        val message = i18n.get(messageKey, "You are now %s", newColorName)
        val color = when (event.newColor) {
            NameColor.BLUE -> NamedTextColor.BLUE
            NameColor.GRAY -> NamedTextColor.GRAY
            NameColor.RED -> NamedTextColor.RED
        }

        player.sendMessage(
            Component.text("[Notoriety] ").color(NamedTextColor.GOLD)
                .append(Component.text(message).color(color))
        )
    }
}
