package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.achievement.listener.AchievementListener
import com.hacklab.minecraft.notoriety.achievement.repository.AchievementRepository
import com.hacklab.minecraft.notoriety.achievement.service.AchievementService
import com.hacklab.minecraft.notoriety.achievement.service.AchievementServiceImpl
import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.bounty.BountySignListener
import com.hacklab.minecraft.notoriety.chat.ChatListener
import com.hacklab.minecraft.notoriety.chat.command.WhisperCommand
import com.hacklab.minecraft.notoriety.chat.repository.ChatSettingsRepository
import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.combat.CombatListener
import com.hacklab.minecraft.notoriety.command.NotorietyCommand
import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.core.economy.EconomyService
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.core.player.PlayerListener
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.core.player.PlayerRepository
import com.hacklab.minecraft.notoriety.core.toLocation
import com.hacklab.minecraft.notoriety.crime.CrimeNotificationListener
import com.hacklab.minecraft.notoriety.crime.CrimeRepository
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.guild.cache.GuildCache
import com.hacklab.minecraft.notoriety.guild.display.GuildTagManager
import com.hacklab.minecraft.notoriety.guild.gui.GuildGUIManager
import com.hacklab.minecraft.notoriety.guild.listener.GuildEventListener
import com.hacklab.minecraft.notoriety.guild.repository.GuildApplicationRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildInvitationRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildMembershipRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildRepository
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.guild.service.GuildServiceImpl
import com.hacklab.minecraft.notoriety.inspect.InspectListener
import com.hacklab.minecraft.notoriety.inspect.InspectService
import com.hacklab.minecraft.notoriety.inspect.InspectionStick
import com.hacklab.minecraft.notoriety.ownership.ExplosionProtectionListener
import com.hacklab.minecraft.notoriety.ownership.OwnershipListener
import com.hacklab.minecraft.notoriety.ownership.OwnershipRepository
import com.hacklab.minecraft.notoriety.ownership.OwnershipService
import com.hacklab.minecraft.notoriety.reputation.TeamManager
import com.hacklab.minecraft.notoriety.trust.TrustRepository
import com.hacklab.minecraft.notoriety.trust.TrustService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManagerImpl
import com.hacklab.minecraft.notoriety.territory.cache.TerritoryCache
import com.hacklab.minecraft.notoriety.territory.listener.BeaconVerifyListener
import com.hacklab.minecraft.notoriety.territory.listener.TerritoryEntryListener
import com.hacklab.minecraft.notoriety.territory.listener.TerritoryProtectionListener
import com.hacklab.minecraft.notoriety.territory.repository.SigilRepository
import com.hacklab.minecraft.notoriety.territory.repository.TerritoryRepository
import com.hacklab.minecraft.notoriety.territory.service.SigilService
import com.hacklab.minecraft.notoriety.territory.service.SigilServiceImpl
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import com.hacklab.minecraft.notoriety.territory.service.TerritoryServiceImpl
import com.hacklab.minecraft.notoriety.village.GolemService
import com.hacklab.minecraft.notoriety.village.TradeListener
import com.hacklab.minecraft.notoriety.village.VillagerListener
import com.hacklab.minecraft.notoriety.village.VillagerService
import org.bukkit.Bukkit
import org.bukkit.command.TabCompleter
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
    lateinit var notorietyService: NotorietyService
        private set
    lateinit var villagerService: VillagerService
        private set
    lateinit var golemService: GolemService
        private set
    lateinit var bountyService: BountyService
        private set
    lateinit var inspectService: InspectService
        private set
    lateinit var inspectionStick: InspectionStick
        private set

    // アチーブメントシステム
    lateinit var achievementService: AchievementService
        private set

    // ギルドシステム
    lateinit var guildService: GuildService
        private set
    lateinit var chatService: ChatService
        private set
    lateinit var guildGUIManager: GuildGUIManager
        private set

    // 領地システム
    lateinit var territoryService: TerritoryService
        private set
    lateinit var sigilService: SigilService
        private set
    private lateinit var beaconManager: BeaconManager
    private lateinit var territoryCache: TerritoryCache
    private lateinit var guildCache: GuildCache
    private lateinit var whisperCommand: WhisperCommand
    private lateinit var guildTagManager: GuildTagManager
    private lateinit var teamManager: TeamManager

    private lateinit var ownershipRepository: OwnershipRepository
    private lateinit var crimeRepository: CrimeRepository

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
        val playerRepository = PlayerRepository(databaseManager)
        playerManager = PlayerManager(this, playerRepository)

        // I18nManagerにプレイヤーロケール取得関数を設定
        i18nManager.getPlayerLocale = { uuid -> playerManager.getPlayer(uuid)?.locale }

        ownershipRepository = OwnershipRepository(databaseManager)
        ownershipService = OwnershipService(ownershipRepository, playerRepository, configManager)
        trustService = TrustService(TrustRepository(databaseManager))
        crimeRepository = CrimeRepository(databaseManager)
        teamManager = TeamManager(this)

        // ギルドシステム初期化（NotorietyServiceより先に初期化）
        val guildRepository = GuildRepository(databaseManager)
        val membershipRepository = GuildMembershipRepository(databaseManager)
        val invitationRepository = GuildInvitationRepository(databaseManager)
        val applicationRepository = GuildApplicationRepository(databaseManager)
        val chatSettingsRepository = ChatSettingsRepository(databaseManager)
        guildCache = GuildCache()
        guildTagManager = GuildTagManager(teamManager)
        guildService = GuildServiceImpl(
            plugin = this,
            guildRepository = guildRepository,
            membershipRepository = membershipRepository,
            invitationRepository = invitationRepository,
            applicationRepository = applicationRepository,
            trustService = trustService,
            guildCache = guildCache,
            guildTagManager = guildTagManager
        )
        // ChatService初期化（titleProviderは後で設定）
        chatService = ChatService(
            chatSettingsRepository = chatSettingsRepository,
            guildService = guildService,
            playerManager = playerManager,
            titleProvider = { uuid -> notorietyService.getLocalizedTitle(uuid) }
        )

        // 領地システム初期化
        val territoryRepository = TerritoryRepository(databaseManager)
        val sigilRepository = SigilRepository(databaseManager)
        territoryCache = TerritoryCache()
        beaconManager = BeaconManagerImpl()

        // TerritoryService初期化（内部でキャッシュを持つ）
        territoryService = TerritoryServiceImpl(
            plugin = this,
            repository = territoryRepository,
            sigilRepository = sigilRepository,
            guildService = guildService,
            beaconManager = beaconManager,
            configManager = configManager
        )

        // SigilService初期化（TerritoryServiceImplのキャッシュを取得）
        val territoryServiceImpl = territoryService as TerritoryServiceImpl
        sigilService = SigilServiceImpl(
            plugin = this,
            sigilRepository = sigilRepository,
            beaconManager = beaconManager,
            guildService = guildService,
            cache = territoryServiceImpl.getCache(),
            configManager = configManager
        )

        // 起動時ビーコン検証をスケジュール（ワールドロード後に実行）
        territoryServiceImpl.scheduleStartupBeaconVerification()

        // NotorietyService初期化（中央集約サービス）
        notorietyService = NotorietyService(
            playerManager = playerManager,
            crimeRepository = crimeRepository,
            teamManager = teamManager,
            ownershipService = ownershipService,
            trustService = trustService,
            guildService = guildService,
            chatService = chatService,
            i18nManager = i18nManager
        )

        // その他サービス初期化
        villagerService = VillagerService(playerManager, i18nManager)
        golemService = GolemService(playerManager)
        bountyService = BountyService(this, economyService)
        bountyService.initializeSignManager()
        inspectService = InspectService(this, ownershipRepository, trustService, i18nManager, territoryService, guildService)
        inspectionStick = InspectionStick(this, i18nManager)

        guildGUIManager = GuildGUIManager(this, guildService)

        // アチーブメントシステム初期化
        val achievementRepository = AchievementRepository(databaseManager)
        achievementService = AchievementServiceImpl(
            plugin = this,
            repository = achievementRepository,
            playerManager = playerManager,
            i18nManager = i18nManager
        )

        // 5. イベントリスナー登録
        registerListeners()

        // 6. コマンド登録
        registerCommands()

        // 7. 定期タスク開始
        startScheduledTasks()

        // 8. プラグイン起動後、全オンラインプレイヤーの表示を更新
        // （リロード時に既存プレイヤーの表示を正しく更新するため）
        server.scheduler.runTaskLater(this, Runnable {
            // 古い空のチームをクリーンアップ
            teamManager.cleanupEmptyTeams()

            Bukkit.getOnlinePlayers().forEach { player ->
                // プレイヤーデータをロード（まだロードされていない場合）
                playerManager.loadPlayer(player.uniqueId)
                // 表示を更新
                notorietyService.updateDisplay(player)
            }
            if (Bukkit.getOnlinePlayers().isNotEmpty()) {
                logger.info("Updated display for ${Bukkit.getOnlinePlayers().size} online player(s)")
            }
        }, 1L)  // 1 tick後に実行

        logger.info("Notoriety has been enabled!")
    }

    override fun onDisable() {
        playerManager.saveAll()
        databaseManager.shutdown()
        logger.info("Notoriety has been disabled!")
    }

    private fun registerListeners() {
        val pm = server.pluginManager

        pm.registerEvents(PlayerListener(playerManager, notorietyService, teamManager), this)
        pm.registerEvents(
            OwnershipListener(
                plugin = this,
                ownershipService = ownershipService,
                guildService = guildService,
                notorietyService = notorietyService,
                getPlayerNameColor = { player -> playerManager.getPlayer(player)?.getNameColor() }
            ),
            this
        )
        pm.registerEvents(ExplosionProtectionListener(ownershipService), this)
        pm.registerEvents(
            VillagerListener(
                playerManager = playerManager,
                villagerService = villagerService,
                golemService = golemService,
                notorietyService = notorietyService,
                ownershipService = ownershipService,
                guildService = guildService
            ),
            this
        )
        pm.registerEvents(TradeListener(playerManager, notorietyService), this)
        pm.registerEvents(
            CombatListener(
                playerManager = playerManager,
                notorietyService = notorietyService,
                bountyService = bountyService,
                trustService = trustService
            ),
            this
        )
        pm.registerEvents(CrimeNotificationListener(i18nManager), this)
        pm.registerEvents(BountySignListener(bountyService.signManager), this)
        pm.registerEvents(InspectListener(inspectService, inspectionStick), this)

        // ギルドシステムリスナー
        pm.registerEvents(GuildEventListener(guildService, chatService, guildTagManager, notorietyService, territoryService, i18nManager), this)
        pm.registerEvents(ChatListener(chatService, guildService), this)
        pm.registerEvents(guildGUIManager, this)
        pm.registerEvents(guildGUIManager.inputManager, this)

        // 領地システムリスナー
        pm.registerEvents(TerritoryProtectionListener(territoryService, guildService, beaconManager, i18nManager), this)
        pm.registerEvents(TerritoryEntryListener(territoryService, guildService, i18nManager), this)
        pm.registerEvents(BeaconVerifyListener(territoryService as TerritoryServiceImpl), this)

        // アチーブメントシステムリスナー
        pm.registerEvents(AchievementListener(achievementService, playerManager), this)
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
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "bounty", arrayOf("bounty") + args)
            }
        }

        getCommand("trust")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "trust", arrayOf("trust") + args)
            }
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "trust", arrayOf("trust") + args)
            }
        }

        getCommand("wanted")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "wanted", arrayOf("bounty") + args)
            }
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "wanted", arrayOf("bounty") + args)
            }
        }

        // /inspect - /noty inspect へのエイリアス
        getCommand("inspect")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "inspect", arrayOf("inspect") + args)
            }
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "inspect", arrayOf("inspect") + args)
            }
        }

        // /guild - /noty guild へのエイリアス
        getCommand("guild")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "guild", arrayOf("guild") + args)
            }
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "guild", arrayOf("guild") + args)
            }
        }

        // /chat - /noty chat へのエイリアス
        getCommand("chat")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "chat", arrayOf("chat") + args)
            }
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "chat", arrayOf("chat") + args)
            }
        }

        // ウィスパーコマンド
        whisperCommand = WhisperCommand(chatService)
        getCommand("w")?.let {
            it.setExecutor(whisperCommand)
            it.tabCompleter = whisperCommand
        }
        getCommand("r")?.let {
            it.setExecutor(whisperCommand)
            it.tabCompleter = whisperCommand
        }
    }

    private fun startScheduledTasks() {
        // 1時間ごとの定期処理
        server.scheduler.runTaskTimer(this, Runnable {
            playerManager.getOnlinePlayers().forEach { data ->
                val oldColor = data.getNameColor()

                // Fame減少
                data.addFame(-1)

                // Alignment回復（0に向かって+10/時間）
                // 赤プレイヤーはAlignmentが0になるとPKCount -1、Alignment -1000にリセット
                val stateChanged = data.decayAlignment(10)

                // 状態変化チェック
                val newColor = data.getNameColor()
                if (oldColor != newColor || stateChanged) {
                    Bukkit.getPluginManager().callEvent(
                        PlayerColorChangeEvent(data.uuid, oldColor, newColor)
                    )
                }

                // 表示更新
                Bukkit.getPlayer(data.uuid)?.let {
                    notorietyService.updateDisplay(it)
                }
            }
        }, 72000L, 72000L)  // 1時間 = 72000 ticks

        // 保留犯罪のチェック（1秒ごと）
        server.scheduler.runTaskTimer(this, Runnable {
            ownershipService.getExpiredPendingCrimes().forEach { pending ->
                notorietyService.commitCrime(
                    criminal = pending.playerUuid,
                    crimeType = CrimeType.DESTROY,
                    alignmentPenalty = pending.alignmentPenalty,
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

        // 期限切れ招待・申請のクリーンアップ（1時間ごと）
        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            guildService.cleanupExpiredInvitations()
            guildService.cleanupExpiredApplications()
        }, 72000L, 72000L)  // 1時間 = 72000 ticks
    }
}
