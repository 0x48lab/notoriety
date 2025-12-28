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
        const val GRAY_WARNING_COOLDOWN = 120_000L  // 2分間のクールダウン
        const val GRAY_WARNING_CHANCE = 0.05        // 5%の確率で警告
        const val GRAY_WARNING_COUNT = 6            // 警告メッセージの数
    }

    // プレイヤーUUID -> 最終警告時刻
    private val lastWarningTime = mutableMapOf<java.util.UUID, Long>()

    fun checkRedPlayerProximity(player: Player): Villager? {
        val data = playerManager.getPlayer(player) ?: return null
        if (data.getNameColor() != NameColor.RED) return null

        return findNearbyVillager(player.location, RED_DETECTION_RANGE)
    }

    /**
     * 灰色プレイヤーが村人の近くにいるとき、まれに警告する
     * - 2分間のクールダウン
     * - 5%の確率
     */
    fun checkGrayPlayerProximity(player: Player): Boolean {
        val data = playerManager.getPlayer(player) ?: return false
        if (data.getNameColor() != NameColor.GRAY) return false

        // クールダウンチェック
        val now = System.currentTimeMillis()
        val lastWarning = lastWarningTime[player.uniqueId] ?: 0L
        if (now - lastWarning < GRAY_WARNING_COOLDOWN) return false

        // 確率チェック
        if (Math.random() > GRAY_WARNING_CHANCE) return false

        // 近くに村人がいるか確認
        val villager = findNearbyVillager(player.location, RED_DETECTION_RANGE) ?: return false

        // 視線が通るか確認
        if (!villager.hasLineOfSight(player)) return false

        // 警告メッセージを送信（ランダム選択）
        val index = (1..GRAY_WARNING_COUNT).random()
        val message = i18n.get("villager.gray_warning_$index", "%s...").format(player.name)
        villagerShout(villager, message)

        // クールダウン更新
        lastWarningTime[player.uniqueId] = now

        return true
    }

    fun checkGrayCrimeWitness(player: Player, crimeLocation: Location): Villager? {
        val data = playerManager.getPlayer(player) ?: return null
        if (data.getNameColor() != NameColor.GRAY) return null

        return player.world.getNearbyEntities(
            crimeLocation,
            GRAY_LINE_OF_SIGHT_RANGE,
            GRAY_LINE_OF_SIGHT_RANGE,
            GRAY_LINE_OF_SIGHT_RANGE
        ) { it is Villager && isValidVillager(it) }
            .filterIsInstance<Villager>()
            .firstOrNull { it.hasLineOfSight(player) }
    }

    fun villagerShout(villager: Villager, message: String) {
        val prefix = i18n.get("villager.prefix", "<Villager>")
        villager.world.players.filter {
            it.location.distance(villager.location) <= 32
        }.forEach {
            it.sendMessage(Component.text("$prefix $message").color(NamedTextColor.YELLOW))
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
