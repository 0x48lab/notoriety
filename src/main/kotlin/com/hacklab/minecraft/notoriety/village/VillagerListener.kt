package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.Notoriety
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Tameable
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.block.Container
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerMoveEvent

class VillagerListener(
    private val plugin: Notoriety,
    private val villagerService: VillagerService,
    private val golemService: GolemService,
    private val crimeService: CrimeService
) : Listener {

    // 赤・灰プレイヤーの移動検知
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!hasBlockChanged(event)) return

        val player = event.player
        val data = plugin.playerManager.getPlayer(player) ?: return

        when (data.getNameColor()) {
            NameColor.RED -> {
                // 村人の反応
                val villager = villagerService.checkRedPlayerProximity(player)
                if (villager != null) {
                    villagerService.shoutMurderer(villager)
                    golemService.callGolemToAttack(player, villager.location)
                } else {
                    // 村人がいなくても、ゴーレムが独自に赤プレイヤーを検知
                    golemService.checkGolemDetectsRedPlayer(player)
                }
            }
            NameColor.GRAY -> {
                // 灰色プレイヤーにはまれに警告（ゴーレムは呼ばない）
                villagerService.checkGrayPlayerProximity(player)
            }
            else -> {}
        }
    }

    // 村人殺害
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVillagerDeath(event: EntityDeathEvent) {
        val victim = event.entity
        if (victim !is Villager) return

        val killer = victim.killer ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (killer.gameMode == GameMode.CREATIVE) return

        val data = plugin.playerManager.getPlayer(killer) ?: return

        // 殺された村人の断末魔
        villagerService.dyingMessage(victim, killer)

        // 目撃した村人の叫び
        val witnesses = victim.world.getNearbyEntities(
            victim.location, 32.0, 32.0, 32.0
        ).filterIsInstance<Villager>()
            .filter { it != victim && it.hasLineOfSight(killer) }

        witnesses.firstOrNull()?.let { witness ->
            villagerService.shoutWitnessedMurder(witness, killer.name)
        }

        // 周囲のゴーレムを全て召集して強化
        golemService.callAllGolemsToAttack(killer, victim.location)

        crimeService.commitCrime(
            criminal = killer.uniqueId,
            crimeType = CrimeType.KILL_VILLAGER,
            alignmentPenalty = 200,
            location = victim.location
        )
        plugin.reputationService.updateDisplay(killer)
    }

    // 灰色・赤色プレイヤーの動物殺害
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnimalDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (killer.gameMode == GameMode.CREATIVE) return

        val entity = event.entity

        // 動物のみ
        if (entity !is Animals) return

        // ペットは別処理（CombatListener）
        if (entity is Tameable && entity.isTamed) return

        val data = plugin.playerManager.getPlayer(killer) ?: return

        // 青色プレイヤーは犯罪にならない
        if (data.getNameColor() == NameColor.BLUE) return

        // 灰色・赤色プレイヤーのみ処理
        val witness = villagerService.checkGrayCrimeWitness(killer, entity.location)
        val golemWitnessed = if (witness == null) {
            // 村人が目撃していなくても、ゴーレムが目撃しているかチェック
            golemService.checkGolemWitnessesCrime(killer, entity.location)
        } else {
            villagerService.shoutCrime(witness, killer.name, "kill_animal")
            golemService.callGolemToAttack(killer, witness.location)
            true
        }

        if (witness != null || golemWitnessed) {
            // Alignment -20（動物殺害の軽微な犯罪）
            data.addAlignment(-20)

            // 犯罪履歴には記録
            crimeService.recordCrimeHistory(
                criminal = killer.uniqueId,
                crimeType = CrimeType.KILL_ANIMAL,
                alignmentPenalty = 0,
                location = entity.location,
                detail = entity.type.name
            )
            plugin.reputationService.updateDisplay(killer)
        }
    }

    // 灰プレイヤーの作物収穫
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCropBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block

        if (!isCrop(block.type)) return

        val data = plugin.playerManager.getPlayer(player) ?: return
        if (data.getNameColor() != NameColor.GRAY) return

        val witness = villagerService.checkGrayCrimeWitness(player, block.location)
        val golemWitnessed = if (witness == null) {
            // 村人が目撃していなくても、ゴーレムが目撃しているかチェック
            golemService.checkGolemWitnessesCrime(player, block.location)
        } else {
            villagerService.shoutCrime(witness, player.name, "theft")
            golemService.callGolemToAttack(player, witness.location)
            true
        }

        if (witness != null || golemWitnessed) {
            crimeService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.HARVEST_CROP,
                alignmentPenalty = 10,
                location = block.location,
                detail = block.type.name
            )
            plugin.reputationService.updateDisplay(player)
        }
    }

    // 灰プレイヤーの所有権なしコンテナ・家具破壊（村人またはゴーレム目撃下）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onContainerOrFurnitureBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block

        // コンテナまたは家具のみ対象
        if (block.state !is Container && !isFurniture(block.type)) return

        val data = plugin.playerManager.getPlayer(player) ?: return
        if (data.getNameColor() != NameColor.GRAY) return

        // 所有権があるブロックはOwnershipListenerで処理
        if (plugin.ownershipService.isProtected(block.location)) return

        val witness = villagerService.checkGrayCrimeWitness(player, block.location)
        val golemWitnessed = if (witness == null) {
            // 村人が目撃していなくても、ゴーレムが目撃しているかチェック
            golemService.checkGolemWitnessesCrime(player, block.location)
        } else {
            villagerService.shoutCrime(witness, player.name, "destroy")
            golemService.callGolemToAttack(player, witness.location)
            true
        }

        if (witness != null || golemWitnessed) {
            crimeService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.DESTROY,
                alignmentPenalty = 30,
                location = block.location,
                detail = block.type.name
            )
            plugin.reputationService.updateDisplay(player)
        }
    }

    // 灰プレイヤーの所有権なしコンテナからのアイテム取り出し（村人またはゴーレム目撃下）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onContainerAccess(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val holder = event.inventory.holder as? Container ?: return

        if (event.clickedInventory != event.inventory) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        val data = plugin.playerManager.getPlayer(player) ?: return
        if (data.getNameColor() != NameColor.GRAY) return

        val location = holder.block.location

        // 所有権があるブロックはOwnershipListenerで処理
        if (plugin.ownershipService.isProtected(location)) return

        val witness = villagerService.checkGrayCrimeWitness(player, location)
        val golemWitnessed = if (witness == null) {
            // 村人が目撃していなくても、ゴーレムが目撃しているかチェック
            golemService.checkGolemWitnessesCrime(player, location)
        } else {
            villagerService.shoutCrime(witness, player.name, "theft")
            golemService.callGolemToAttack(player, witness.location)
            true
        }

        if (witness != null || golemWitnessed) {
            crimeService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.THEFT,
                alignmentPenalty = 50,
                location = location
            )
            plugin.reputationService.updateDisplay(player)
        }
    }

    // 強化ゴーレムがプレイヤーから攻撃を受けた時
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onGolemDamaged(event: EntityDamageByEntityEvent) {
        val golem = event.entity as? IronGolem ?: return
        if (!golemService.isEnhancedGolem(golem)) return

        val damager = event.damager

        // 矢などの飛び道具は無効化
        if (damager is Projectile) {
            event.isCancelled = true
            return
        }

        // 近接攻撃の場合、リーチ外からならテレポート
        val attacker = damager as? Player ?: return
        golemService.onGolemDamaged(golem, attacker)
    }

    // ゴーレムがターゲットを失った時（倒した、見失ったなど）
    @EventHandler(priority = EventPriority.MONITOR)
    fun onGolemTargetChange(event: EntityTargetEvent) {
        val golem = event.entity as? IronGolem ?: return

        // ホーム位置が登録されているゴーレムのみ処理
        if (!golemService.hasHomeLocation(golem)) return

        // ターゲットがnullになった（倒した、見失った）場合、帰還
        if (event.target == null) {
            golemService.returnGolemToHome(golem)
        }
    }

    // ゴーレムのターゲット（プレイヤー）が死亡した時
    @EventHandler(priority = EventPriority.MONITOR)
    fun onTargetPlayerDeath(event: EntityDeathEvent) {
        val deadPlayer = event.entity as? Player ?: return

        // このプレイヤーをターゲットにしている強化ゴーレムを探して帰還させる
        deadPlayer.world.getNearbyEntities(
            deadPlayer.location, 64.0, 64.0, 64.0
        ).filterIsInstance<IronGolem>()
            .filter { golemService.hasHomeLocation(it) && it.target == deadPlayer }
            .forEach { golemService.returnGolemToHome(it) }
    }

    // ゴーレムが死亡した時
    @EventHandler(priority = EventPriority.MONITOR)
    fun onGolemDeath(event: EntityDeathEvent) {
        val golem = event.entity as? IronGolem ?: return
        golemService.onGolemDeath(golem)
    }

    // ゴーレムがスポーンした時に強化（攻撃された時にテレポートできるように）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGolemSpawn(event: CreatureSpawnEvent) {
        val golem = event.entity as? IronGolem ?: return
        // プレイヤーが作成したゴーレムも含め、すべてのゴーレムを強化
        golemService.enhanceGolem(golem)
    }

    private fun hasBlockChanged(event: PlayerMoveEvent): Boolean {
        val from = event.from
        val to = event.to
        return from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ
    }

    private fun isCrop(material: Material): Boolean {
        return material in listOf(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.MELON, Material.PUMPKIN,
            Material.SWEET_BERRY_BUSH, Material.NETHER_WART, Material.COCOA
        )
    }

    private fun isFurniture(material: Material): Boolean {
        return material in listOf(
            // ベッド
            Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED,
            Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED,
            Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED,
            Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED,
            // 作業台・テーブル系
            Material.CRAFTING_TABLE, Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE,
            Material.SMITHING_TABLE, Material.LOOM, Material.STONECUTTER, Material.GRINDSTONE,
            // かまど系
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            // その他家具
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.BREWING_STAND, Material.ENCHANTING_TABLE, Material.LECTERN,
            Material.BELL, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.CAULDRON, Material.COMPOSTER, Material.BEEHIVE, Material.BEE_NEST
        )
    }
}
