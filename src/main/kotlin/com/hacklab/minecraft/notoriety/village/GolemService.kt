package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Location
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Monster
import org.bukkit.entity.Player

class GolemService(private val playerManager: PlayerManager) {
    companion object {
        const val GOLEM_SEARCH_RANGE = 64.0
        const val GOLEM_TELEPORT_THRESHOLD = 16.0
        const val ESCORT_ALIGNMENT_THRESHOLD = 500
        const val ESCORT_VILLAGER_RANGE = 32.0
    }

    fun callGolemToAttack(target: Player, nearVillager: Location): Boolean {
        val golem = findNearbyGolem(nearVillager) ?: return false

        // ゴーレムが遠い場合はテレポートさせる
        val distance = golem.location.distance(target.location)
        if (distance > GOLEM_TELEPORT_THRESHOLD) {
            val teleportLocation = findSafeTeleportLocation(target.location)
            golem.teleport(teleportLocation)
        }

        golem.target = target
        return true
    }

    private fun findSafeTeleportLocation(targetLocation: Location): Location {
        // プレイヤーの後ろ側（向いている方向の反対）にテレポート
        val direction = targetLocation.direction.normalize().multiply(-3.0)
        val teleportLoc = targetLocation.clone().add(direction)
        teleportLoc.y = targetLocation.world.getHighestBlockYAt(teleportLoc).toDouble() + 1.0
        return teleportLoc
    }

    fun orderGolemToProtect(protectedPlayer: Player, monster: Monster) {
        val data = playerManager.getPlayer(protectedPlayer) ?: return
        if (data.getNameColor() != NameColor.BLUE) return
        if (data.alignment < ESCORT_ALIGNMENT_THRESHOLD) return

        // Check if player is near a villager
        val nearVillager = protectedPlayer.world.getNearbyEntities(
            protectedPlayer.location,
            ESCORT_VILLAGER_RANGE,
            ESCORT_VILLAGER_RANGE,
            ESCORT_VILLAGER_RANGE
        ).any { it is org.bukkit.entity.Villager }

        if (!nearVillager) return

        val golem = findNearbyGolem(protectedPlayer.location) ?: return
        golem.target = monster
    }

    fun shouldAttackRedPlayer(player: Player): Boolean {
        val data = playerManager.getPlayer(player) ?: return false
        return data.getNameColor() == NameColor.RED
    }

    private fun findNearbyGolem(location: Location): IronGolem? {
        return location.world.getNearbyEntities(
            location,
            GOLEM_SEARCH_RANGE,
            GOLEM_SEARCH_RANGE,
            GOLEM_SEARCH_RANGE
        ).filterIsInstance<IronGolem>()
            .minByOrNull { it.location.distance(location) }
    }
}
