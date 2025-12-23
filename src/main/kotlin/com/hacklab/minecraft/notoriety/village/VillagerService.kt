package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.reputation.NameColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Villager

class VillagerService(
    private val playerManager: PlayerManager,
    private val i18n: I18nManager
) {
    companion object {
        const val RED_DETECTION_RANGE = 16.0
        const val GRAY_LINE_OF_SIGHT_RANGE = 32.0
    }

    fun checkRedPlayerProximity(player: Player): Villager? {
        val data = playerManager.getPlayer(player) ?: return null
        if (data.getNameColor() != NameColor.RED) return null

        return findNearbyVillager(player.location, RED_DETECTION_RANGE)
    }

    fun checkGrayCrimeWitness(player: Player, crimeLocation: Location): Villager? {
        val data = playerManager.getPlayer(player) ?: return null
        if (data.getNameColor() != NameColor.GRAY) return null

        return player.world.getNearbyEntities(
            crimeLocation,
            GRAY_LINE_OF_SIGHT_RANGE,
            GRAY_LINE_OF_SIGHT_RANGE,
            GRAY_LINE_OF_SIGHT_RANGE
        ) { it is Villager && isValidVillager(it as Villager) }
            .filterIsInstance<Villager>()
            .firstOrNull { it.hasLineOfSight(player) }
    }

    fun villagerShout(villager: Villager, message: String) {
        villager.world.players.filter {
            it.location.distance(villager.location) <= 32
        }.forEach {
            it.sendMessage(Component.text("<村人> $message").color(NamedTextColor.YELLOW))
        }
    }

    fun shoutMurderer(villager: Villager) {
        villagerShout(villager, i18n.get("villager.murderer", "人殺しが来た！"))
    }

    fun shoutCrime(villager: Villager, criminalName: String, crimeKey: String) {
        val message = i18n.get("villager.$crimeKey", "%s!").format(criminalName)
        villagerShout(villager, message)
    }

    // 村人が殺されたときの断末魔
    fun dyingMessage(villager: Villager, killer: Player) {
        val message = i18n.get("villager.dying", "%sは人殺しだ！").format(killer.name)
        villagerShout(villager, message)
    }

    // 村人殺害を目撃した村人の叫び
    fun shoutWitnessedMurder(witness: Villager, killerName: String) {
        villagerShout(witness, i18n.get("villager.witness_murder", "%sが村人を殺した！").format(killerName))
    }

    private fun findNearbyVillager(location: Location, range: Double): Villager? {
        return location.world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Villager>()
            .firstOrNull { isValidVillager(it) }
    }

    private fun isValidVillager(villager: Villager): Boolean {
        // カスタム名のない村人（NPCショップなどを除外）
        return villager.customName() == null
    }
}
