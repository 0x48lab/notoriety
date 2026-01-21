package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.CrimeCheckResult
import com.hacklab.minecraft.notoriety.NotorietyService
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.ownership.OwnershipService
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Tameable
import org.bukkit.entity.Villager
import org.bukkit.entity.memory.MemoryKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.block.Container
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.Bukkit
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerMoveEvent

class VillagerListener(
    private val playerManager: PlayerManager,
    private val villagerService: VillagerService,
    private val golemService: GolemService,
    private val notorietyService: NotorietyService,
    private val ownershipService: OwnershipService,
    private val guildService: GuildService,
    private val territoryService: TerritoryService
) : Listener {

    /**
     * 村人のベッドを壊そうとした時の警告
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVillagerBedDamageWarning(event: BlockDamageEvent) {
        val player = event.player

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block

        // ベッドのみ対象
        if (!isBed(block.type)) return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkVillagerBedCrime(player, block.location)
    }

    /**
     * 村人の仕事場を壊そうとした時の警告
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVillagerWorkstationDamageWarning(event: BlockDamageEvent) {
        val player = event.player

        // クリエイティブモードはスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkVillagerWorkstationCrime(player, event.block.location)
    }

    /**
     * 村人を攻撃した時の警告
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVillagerAttackWarning(event: EntityDamageByEntityEvent) {
        val villager = event.entity as? Villager ?: return

        // 攻撃者を特定
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        } ?: return

        // クリエイティブモードはスキップ
        if (attacker.gameMode == GameMode.CREATIVE) return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkVillagerAttackCrime(attacker, villager)
    }

    /**
     * アイアンゴーレムを攻撃した時の警告
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onGolemAttackWarning(event: EntityDamageByEntityEvent) {
        val golem = event.entity as? IronGolem ?: return

        // 攻撃者を特定
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        } ?: return

        // クリエイティブモードはスキップ
        if (attacker.gameMode == GameMode.CREATIVE) return

        // NotorietyServiceで犯罪チェック（警告表示も含む）
        notorietyService.checkGolemAttackCrime(attacker, golem)
    }

    // 赤・灰プレイヤーの移動検知
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!hasBlockChanged(event)) return

        val player = event.player
        val data = playerManager.getPlayer(player) ?: return

        when (data.getNameColor()) {
            NameColor.RED -> {
                // 村人の反応
                val villager = villagerService.checkRedPlayerProximity(player)
                if (villager != null) {
                    // クールダウン中でなければ叫ぶ（ゴーレムは常に呼ぶ）
                    villagerService.shoutMurderer(villager, player.uniqueId)
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

        playerManager.getPlayer(killer) ?: return

        // ベッドに紐づいているかどうかでペナルティを変える
        val hasBed = victim.getMemory(MemoryKey.HOME) != null
        val penalty = if (hasBed) 50 else 10

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

        notorietyService.commitCrime(
            criminal = killer.uniqueId,
            crimeType = CrimeType.KILL_VILLAGER,
            alignmentPenalty = penalty,
            location = victim.location
        )
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

        val data = playerManager.getPlayer(killer) ?: return

        // 青色プレイヤーは犯罪にならない（灰色・赤色のみ対象）
        if (data.getNameColor() == NameColor.BLUE) return
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
            // 動物殺害の犯罪を記録（Alignment -10）
            notorietyService.commitCrime(
                criminal = killer.uniqueId,
                crimeType = CrimeType.KILL_ANIMAL,
                alignmentPenalty = 10,
                location = entity.location,
                detail = entity.type.name
            )
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

        val data = playerManager.getPlayer(player) ?: return

        // 青色プレイヤーは犯罪にならない（灰色・赤色のみ対象）
        if (data.getNameColor() == NameColor.BLUE) return

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
            notorietyService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.HARVEST_CROP,
                alignmentPenalty = 1,
                location = block.location,
                detail = block.type.name
            )
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

        val data = playerManager.getPlayer(player) ?: return

        // 青色プレイヤーは犯罪にならない（灰色・赤色のみ対象）
        if (data.getNameColor() == NameColor.BLUE) return

        // 所有権があるブロックはOwnershipListenerで処理
        if (ownershipService.isProtected(block.location)) return

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
            notorietyService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.DESTROY,
                alignmentPenalty = 5,
                location = block.location,
                detail = block.type.name
            )
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

        val data = playerManager.getPlayer(player) ?: return

        // 青色プレイヤーは犯罪にならない（灰色・赤色のみ対象）
        if (data.getNameColor() == NameColor.BLUE) return

        val location = holder.block.location

        // 所有権があるブロックはOwnershipListenerで処理
        if (ownershipService.isProtected(location)) return

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
            notorietyService.commitCrime(
                criminal = player.uniqueId,
                crimeType = CrimeType.THEFT,
                alignmentPenalty = 50,
                location = location
            )
        }
    }

    // 村人ベッド破壊（村人が紐づいているベッドを壊すと犯罪）- 全プレイヤー対象
    // 優先度NORMALで実行（OwnershipListenerのHIGHより前に所有権チェックを行う）
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onVillagerBedBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block

        // ベッドのみ対象
        if (!isBed(block.type)) return

        playerManager.getPlayer(player) ?: return

        // 自分で設置したブロック、信頼されたプレイヤー、またはギルドメンバーなら犯罪にならない
        val owner = ownershipService.getOwner(block.location)
        if (owner != null && (owner == player.uniqueId || guildService.isAccessAllowed(owner, player.uniqueId))) return

        // このベッドに紐づいている村人を探す
        val affectedVillager = findVillagerWithBed(block.location) ?: return

        // 村人が叫ぶ
        villagerService.shoutCrime(affectedVillager, player.name, "destroy_bed")

        // ゴーレムを呼ぶ
        golemService.callGolemToAttack(player, affectedVillager.location)

        // 犯罪記録（Alignment -5）
        notorietyService.commitCrime(
            criminal = player.uniqueId,
            crimeType = CrimeType.DESTROY_VILLAGER_BED,
            alignmentPenalty = 5,
            location = block.location
        )
    }

    // 村人仕事場破壊（村人が紐づいている職業ブロックを壊すと犯罪）- 全プレイヤー対象
    // 優先度NORMALで実行（OwnershipListenerのHIGHより前に所有権チェックを行う）
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onVillagerWorkstationBreak(event: BlockBreakEvent) {
        val player = event.player

        // クリエイティブモードは犯罪判定をスキップ
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        playerManager.getPlayer(player) ?: return

        // 自分で設置したブロック、信頼されたプレイヤー、またはギルドメンバーなら犯罪にならない
        val owner = ownershipService.getOwner(block.location)
        if (owner != null && (owner == player.uniqueId || guildService.isAccessAllowed(owner, player.uniqueId))) return

        // この職業ブロックに紐づいている村人を探す
        val affectedVillager = findVillagerWithWorkstation(block.location) ?: return

        // 村人が叫ぶ
        villagerService.shoutCrime(affectedVillager, player.name, "destroy_workstation")

        // ゴーレムを呼ぶ
        golemService.callGolemToAttack(player, affectedVillager.location)

        // 犯罪記録（Alignment -5）
        notorietyService.commitCrime(
            criminal = player.uniqueId,
            crimeType = CrimeType.DESTROY_VILLAGER_WORKSTATION,
            alignmentPenalty = 5,
            location = block.location,
            detail = block.type.name
        )
    }

    /**
     * 指定された位置のベッドに紐づいている村人を探す
     */
    private fun findVillagerWithBed(bedLocation: org.bukkit.Location): Villager? {
        val searchRange = 64.0
        return bedLocation.world.getNearbyEntities(
            bedLocation,
            searchRange,
            searchRange,
            searchRange
        ).filterIsInstance<Villager>()
            .firstOrNull { villager ->
                val home = villager.getMemory(MemoryKey.HOME)
                home != null &&
                    home.blockX == bedLocation.blockX &&
                    home.blockY == bedLocation.blockY &&
                    home.blockZ == bedLocation.blockZ
            }
    }

    /**
     * 指定された位置の職業ブロックに紐づいている村人を探す
     */
    private fun findVillagerWithWorkstation(blockLocation: org.bukkit.Location): Villager? {
        val searchRange = 64.0
        return blockLocation.world.getNearbyEntities(
            blockLocation,
            searchRange,
            searchRange,
            searchRange
        ).filterIsInstance<Villager>()
            .firstOrNull { villager ->
                val jobSite = villager.getMemory(MemoryKey.JOB_SITE)
                jobSite != null &&
                    jobSite.blockX == blockLocation.blockX &&
                    jobSite.blockY == blockLocation.blockY &&
                    jobSite.blockZ == blockLocation.blockZ
            }
    }

    /**
     * ベッドかどうか判定
     */
    private fun isBed(material: Material): Boolean {
        return material.name.endsWith("_BED")
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

    // ゴーレムが死亡した時（ホーム位置管理）
    @EventHandler(priority = EventPriority.MONITOR)
    fun onGolemDeath(event: EntityDeathEvent) {
        val golem = event.entity as? IronGolem ?: return
        golemService.onGolemDeath(golem)
    }

    // ゴーレム殺害（全プレイヤー対象）- Alignment -50
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGolemKill(event: EntityDeathEvent) {
        val golem = event.entity as? IronGolem ?: return
        val killer = golem.killer ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (killer.gameMode == GameMode.CREATIVE) return

        playerManager.getPlayer(killer) ?: return

        // 目撃した村人の叫び
        val witness = golem.world.getNearbyEntities(
            golem.location, 32.0, 32.0, 32.0
        ).filterIsInstance<Villager>()
            .firstOrNull { it.hasLineOfSight(killer) }

        witness?.let {
            villagerService.shoutCrime(it, killer.name, "kill_golem")
        }

        // 周囲のゴーレムを全て召集して強化
        golemService.callAllGolemsToAttack(killer, golem.location)

        // 犯罪記録（Alignment -50）
        notorietyService.commitCrime(
            criminal = killer.uniqueId,
            crimeType = CrimeType.KILL_GOLEM,
            alignmentPenalty = 50,
            location = golem.location
        )
    }

    // 村人攻撃（全プレイヤー対象）- Alignment -1（攻撃の度）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVillagerAttack(event: EntityDamageByEntityEvent) {
        val villager = event.entity as? Villager ?: return

        // 攻撃者を特定（直接攻撃または飛び道具）
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        } ?: return

        // クリエイティブモードは犯罪判定をスキップ
        if (attacker.gameMode == GameMode.CREATIVE) return

        playerManager.getPlayer(attacker) ?: return

        // 目撃した村人の叫び（攻撃された村人以外）
        val witness = villager.world.getNearbyEntities(
            villager.location, 32.0, 32.0, 32.0
        ).filterIsInstance<Villager>()
            .firstOrNull { it != villager && it.hasLineOfSight(attacker) }

        witness?.let {
            villagerService.shoutCrime(it, attacker.name, "attack_villager")
        }

        // ゴーレムを呼ぶ
        golemService.callGolemToAttack(attacker, villager.location)

        // 犯罪記録（Alignment -1）
        notorietyService.commitCrime(
            criminal = attacker.uniqueId,
            crimeType = CrimeType.ATTACK_VILLAGER,
            alignmentPenalty = 1,
            location = villager.location
        )
    }

    // モンスター討伐（全プレイヤー対象）- Alignment +1
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMonsterKill(event: EntityDeathEvent) {
        val monster = event.entity as? Monster ?: return
        val killer = monster.killer ?: return

        // クリエイティブモードはスキップ
        if (killer.gameMode == GameMode.CREATIVE) return

        val data = playerManager.getPlayer(killer) ?: return

        // Alignment +1
        data.addAlignment(1)
        notorietyService.updateDisplay(killer)
    }

    // ゴーレムがスポーンした時に強化（領地内のみ）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGolemSpawn(event: CreatureSpawnEvent) {
        val golem = event.entity as? IronGolem ?: return

        // 領地内でのみ強化
        if (!territoryService.isInTerritory(golem.location)) return

        // 1tick遅らせて強化（スポーン直後は属性が正しく設定されないことがあるため）
        val plugin = Bukkit.getPluginManager().getPlugin("Notoriety") ?: return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (golem.isValid) {
                golemService.enhanceGolem(golem)
            }
        }, 1L)
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
