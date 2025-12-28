package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GolemService(private val playerManager: PlayerManager) {
    companion object {
        const val GOLEM_SEARCH_RANGE = 128.0
        const val GOLEM_DETECTION_RANGE = 32.0        // ゴーレムの視認範囲
        const val GOLEM_MELEE_RANGE = 2.5             // ゴーレムの通常攻撃リーチ
        const val ESCORT_ALIGNMENT_THRESHOLD = 500
        const val ESCORT_VILLAGER_RANGE = 32.0

        // 強化ゴーレムの設定
        const val ENHANCED_GOLEM_ATTACK_DAMAGE = 45.0  // 通常7-21 → 45（40-50）
        const val ENHANCED_GOLEM_MAX_HEALTH = 200.0    // 通常100 → 200
        const val ENHANCED_GOLEM_SPEED = 0.5           // 通常0.25 → 0.5（2倍速）
    }

    // ゴーレムのホーム位置を記録（UUID -> 元の位置）
    private val golemHomeLocations = ConcurrentHashMap<UUID, Location>()

    fun callGolemToAttack(target: Player, nearVillager: Location): Boolean {
        val golem = findNearbyGolem(nearVillager) ?: return false

        // ホーム位置を保存（まだ保存されていない場合）
        saveHomeLocation(golem)

        // ゴーレムをプレイヤーの位置にテレポート
        val teleportLocation = findSafeTeleportLocation(target.location)
        golem.teleport(teleportLocation)

        // 攻撃モード時にゴーレムを強化
        enhanceGolem(golem)

        golem.target = target
        return true
    }

    /**
     * ゴーレムのホーム位置を保存
     */
    private fun saveHomeLocation(golem: IronGolem) {
        if (!golemHomeLocations.containsKey(golem.uniqueId)) {
            golemHomeLocations[golem.uniqueId] = golem.location.clone()
        }
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

    /**
     * ゴーレムが赤プレイヤーを検知したら攻撃
     * @return 攻撃したゴーレムがあればtrue
     */
    fun checkGolemDetectsRedPlayer(player: Player): Boolean {
        val data = playerManager.getPlayer(player) ?: return false
        if (data.getNameColor() != NameColor.RED) return false

        // 近くのゴーレムを探す（視認範囲内）
        val golem = findNearbyGolemWithLineOfSight(player.location, player) ?: return false

        // ホーム位置を保存
        saveHomeLocation(golem)

        // ゴーレムを強化して攻撃
        val teleportLocation = findSafeTeleportLocation(player.location)
        golem.teleport(teleportLocation)
        enhanceGolem(golem)
        golem.target = player
        return true
    }

    /**
     * ゴーレムが灰色プレイヤーの犯罪を目撃したら攻撃
     * @return 目撃したゴーレムがあればtrue
     */
    fun checkGolemWitnessesCrime(player: Player, crimeLocation: Location): Boolean {
        val data = playerManager.getPlayer(player) ?: return false
        if (data.getNameColor() != NameColor.GRAY) return false

        // 犯罪現場の近くのゴーレムを探す（視認範囲内）
        val golem = findNearbyGolemWithLineOfSight(crimeLocation, player) ?: return false

        // ホーム位置を保存
        saveHomeLocation(golem)

        // ゴーレムを強化して攻撃
        val teleportLocation = findSafeTeleportLocation(player.location)
        golem.teleport(teleportLocation)
        enhanceGolem(golem)
        golem.target = player
        return true
    }

    /**
     * 視線が通るゴーレムを探す
     */
    private fun findNearbyGolemWithLineOfSight(location: Location, target: Player): IronGolem? {
        return location.world.getNearbyEntities(
            location,
            GOLEM_DETECTION_RANGE,
            GOLEM_DETECTION_RANGE,
            GOLEM_DETECTION_RANGE
        ).filterIsInstance<IronGolem>()
            .filter { it.hasLineOfSight(target) }
            .minByOrNull { it.location.distance(location) }
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

    /**
     * 村人殺害時に強化ゴーレムをスポーンさせる
     */
    fun spawnEnhancedGolem(location: Location, target: Player): IronGolem {
        val world = location.world
        val golem = world.spawn(location, IronGolem::class.java) { entity ->
            // プレイヤー生成ではない（自然スポーン扱い）
            entity.isPlayerCreated = false

            // HP強化
            entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = ENHANCED_GOLEM_MAX_HEALTH
            entity.health = ENHANCED_GOLEM_MAX_HEALTH

            // 攻撃力強化
            entity.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = ENHANCED_GOLEM_ATTACK_DAMAGE

            // 移動速度強化
            entity.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = ENHANCED_GOLEM_SPEED

            // ノックバック耐性を最大に
            entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 1.0
        }

        // 速度アップのポーション効果（保険として追加）
        golem.addPotionEffect(PotionEffect(
            PotionEffectType.SPEED,
            Int.MAX_VALUE,  // 永続
            1,              // Speed II
            false,
            false
        ))

        // ターゲットを設定
        golem.target = target

        return golem
    }

    /**
     * 周囲のすべてのゴーレムを召集して攻撃させる
     */
    fun callAllGolemsToAttack(target: Player, location: Location): Int {
        val golems = location.world.getNearbyEntities(
            location,
            GOLEM_SEARCH_RANGE,
            GOLEM_SEARCH_RANGE,
            GOLEM_SEARCH_RANGE
        ).filterIsInstance<IronGolem>()

        golems.forEach { golem ->
            // ホーム位置を保存
            saveHomeLocation(golem)
            // ゴーレムをプレイヤーの位置にテレポート
            val teleportLocation = findSafeTeleportLocation(target.location)
            golem.teleport(teleportLocation)
            // 攻撃モード時にゴーレムを強化
            enhanceGolem(golem)
            golem.target = target
        }

        return golems.size
    }

    /**
     * 既存のゴーレムを攻撃モード用に強化する
     */
    fun enhanceGolem(golem: IronGolem) {
        // 既に強化済みの場合はスキップ（Speed効果があるかで判定）
        if (golem.hasPotionEffect(PotionEffectType.SPEED)) return

        // 移動速度強化
        golem.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = ENHANCED_GOLEM_SPEED

        // 攻撃力強化
        golem.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = ENHANCED_GOLEM_ATTACK_DAMAGE

        // ノックバック耐性を最大に
        golem.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 1.0

        // 速度アップのポーション効果（視覚的にもわかりやすい）
        golem.addPotionEffect(PotionEffect(
            PotionEffectType.SPEED,
            Int.MAX_VALUE,  // 永続
            1,              // Speed II
            false,
            false
        ))
    }

    /**
     * 強化ゴーレムがダメージを受けた時の処理
     * 攻撃者がゴーレムのリーチ外から攻撃している場合、攻撃者の位置にテレポート
     */
    fun onGolemDamaged(golem: IronGolem, attacker: Player) {
        if (!isEnhancedGolem(golem)) return

        val distance = golem.location.distance(attacker.location)

        // ゴーレムの攻撃が届かない距離から攻撃されたらテレポート
        if (distance > GOLEM_MELEE_RANGE) {
            val teleportLoc = findSafeTeleportLocation(attacker.location)
            golem.teleport(teleportLoc)
            // ターゲットを攻撃者に設定
            golem.target = attacker
        }
    }

    /**
     * 強化済みゴーレムかどうか判定
     */
    fun isEnhancedGolem(golem: IronGolem): Boolean {
        return golem.hasPotionEffect(PotionEffectType.SPEED)
    }

    /**
     * ゴーレムを村（ホーム位置）に帰還させる
     */
    fun returnGolemToHome(golem: IronGolem) {
        val home = golemHomeLocations.remove(golem.uniqueId) ?: return

        // 強化を解除
        resetGolemEnhancement(golem)

        // ホーム位置にテレポート
        golem.teleport(home)

        // ターゲットをクリア
        golem.target = null
    }

    /**
     * ゴーレムの強化を解除する
     */
    private fun resetGolemEnhancement(golem: IronGolem) {
        // 速度効果を削除
        golem.removePotionEffect(PotionEffectType.SPEED)

        // ステータスを通常に戻す
        golem.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.25
        golem.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = 21.0
        golem.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 1.0
    }

    /**
     * ホーム位置が登録されているゴーレムかどうか
     */
    fun hasHomeLocation(golem: IronGolem): Boolean {
        return golemHomeLocations.containsKey(golem.uniqueId)
    }

    /**
     * ゴーレムが死亡した場合、ホーム位置を削除
     */
    fun onGolemDeath(golem: IronGolem) {
        golemHomeLocations.remove(golem.uniqueId)
    }
}
