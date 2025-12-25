package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.combat.CombatListener
import com.hacklab.minecraft.notoriety.command.NotorietyCommand
import com.hacklab.minecraft.notoriety.core.BlockLocation
import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.core.economy.EconomyService
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.core.player.PlayerListener
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.core.player.PlayerRepository
import com.hacklab.minecraft.notoriety.core.toLocation
import com.hacklab.minecraft.notoriety.crime.CrimeRepository
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.ownership.OwnershipListener
import com.hacklab.minecraft.notoriety.ownership.OwnershipRepository
import com.hacklab.minecraft.notoriety.ownership.OwnershipService
import com.hacklab.minecraft.notoriety.reputation.ReputationService
import com.hacklab.minecraft.notoriety.reputation.TeamManager
import com.hacklab.minecraft.notoriety.trust.TrustRepository
import com.hacklab.minecraft.notoriety.trust.TrustService
import com.hacklab.minecraft.notoriety.village.GolemService
import com.hacklab.minecraft.notoriety.village.TradeListener
import com.hacklab.minecraft.notoriety.village.VillagerListener
import com.hacklab.minecraft.notoriety.village.VillagerService
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class Notoriety : JavaPlugin() {
    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var i18nManager: I18nManager
        private set
    lateinit var economyService: EconomyService
        private set

    lateinit var playerManager: PlayerManager
        private set
    lateinit var ownershipService: OwnershipService
        private set
    lateinit var trustService: TrustService
        private set
    lateinit var crimeService: CrimeService
        private set
    lateinit var reputationService: ReputationService
        private set
    lateinit var villagerService: VillagerService
        private set
    lateinit var golemService: GolemService
        private set
    lateinit var bountyService: BountyService
        private set

    val api: NotorietyAPI by lazy { NotorietyAPIImpl(this) }

    override fun onEnable() {
        // 1. 設定読み込み
        configManager = ConfigManager(this)
        i18nManager = I18nManager(this, configManager.locale)

        // 2. データベース初期化
        databaseManager = DatabaseManager(this, configManager)
        databaseManager.initialize()

        // 3. 経済システム連携
        economyService = EconomyService(this)
        economyService.initialize()

        // 4. コアサービス初期化
        playerManager = PlayerManager(this, PlayerRepository(databaseManager))
        ownershipService = OwnershipService(OwnershipRepository(databaseManager))
        trustService = TrustService(TrustRepository(databaseManager))
        crimeService = CrimeService(CrimeRepository(databaseManager), playerManager)
        reputationService = ReputationService(playerManager, TeamManager(this))
        villagerService = VillagerService(playerManager, i18nManager)
        golemService = GolemService(playerManager)
        bountyService = BountyService(this, economyService)
        bountyService.initializeSignManager()

        // 5. イベントリスナー登録
        registerListeners()

        // 6. コマンド登録
        registerCommands()

        // 7. 定期タスク開始
        startScheduledTasks()

        logger.info("Notoriety has been enabled!")
    }

    override fun onDisable() {
        playerManager.saveAll()
        databaseManager.shutdown()
        logger.info("Notoriety has been disabled!")
    }

    private fun registerListeners() {
        val pm = server.pluginManager

        pm.registerEvents(PlayerListener(playerManager, reputationService), this)
        pm.registerEvents(OwnershipListener(this, ownershipService, trustService, crimeService), this)
        pm.registerEvents(VillagerListener(this, villagerService, golemService, crimeService), this)
        pm.registerEvents(TradeListener(playerManager, reputationService), this)
        pm.registerEvents(CombatListener(playerManager, crimeService, reputationService, bountyService, trustService), this)
    }

    private fun registerCommands() {
        val mainCommand = NotorietyCommand(this)

        getCommand("notoriety")?.let {
            it.setExecutor(mainCommand)
            it.tabCompleter = mainCommand
        }

        // Alias commands
        getCommand("bounty")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "bounty", arrayOf("bounty") + args)
            }
        }

        getCommand("trust")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "trust", arrayOf("trust") + args)
            }
        }

        getCommand("wanted")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "wanted", arrayOf("bounty") + args)
            }
        }
    }

    private fun startScheduledTasks() {
        // 1時間ごとの定期処理
        server.scheduler.runTaskTimer(this, Runnable {
            playerManager.getOnlinePlayers().forEach { data ->
                val oldColor = data.getNameColor()

                // Fame減少
                data.addFame(-1)

                // CrimePoint減少（1時間ごとに-10）
                data.addCrimePoint(-10)

                // 赤プレイヤーのPKCount減少チェック
                if (data.pkCount > 0 && data.crimePoint <= -1000) {
                    data.crimePoint += 1000  // 0にリセット
                    data.pkCount--
                }

                // 状態変化チェック
                val newColor = data.getNameColor()
                if (oldColor != newColor) {
                    data.resetKarma()
                    Bukkit.getPluginManager().callEvent(
                        PlayerColorChangeEvent(data.uuid, oldColor, newColor)
                    )
                }

                // 表示更新
                Bukkit.getPlayer(data.uuid)?.let {
                    reputationService.updateDisplay(it)
                }
            }
        }, 72000L, 72000L)  // 1時間 = 72000 ticks

        // 保留犯罪のチェック（1秒ごと）
        server.scheduler.runTaskTimer(this, Runnable {
            ownershipService.getExpiredPendingCrimes().forEach { pending ->
                crimeService.commitCrime(
                    criminal = pending.playerUuid,
                    crimeType = CrimeType.DESTROY,
                    crimePoint = pending.crimePoint,
                    victim = pending.ownerUuid,
                    location = pending.location.toLocation(),
                    detail = pending.blockType.name
                )
            }
        }, 20L, 20L)  // 1秒 = 20 ticks

        // 懸賞金看板の更新（10秒ごと）
        server.scheduler.runTaskTimer(this, Runnable {
            bountyService.signManager.updateAllSigns()
        }, 200L, 200L)  // 10秒 = 200 ticks

        // 定期保存（5分ごと）
        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            playerManager.saveAll()
        }, 6000L, 6000L)  // 5分 = 6000 ticks
    }
}
