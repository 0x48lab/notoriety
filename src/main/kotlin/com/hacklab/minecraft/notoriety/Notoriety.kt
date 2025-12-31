package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.bounty.BountyService
import com.hacklab.minecraft.notoriety.bounty.BountySignListener
import com.hacklab.minecraft.notoriety.chat.ChatListener
import com.hacklab.minecraft.notoriety.chat.command.ChatCommand
import com.hacklab.minecraft.notoriety.chat.command.WhisperCommand
import com.hacklab.minecraft.notoriety.chat.repository.ChatSettingsRepository
import com.hacklab.minecraft.notoriety.chat.service.ChatService
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
import com.hacklab.minecraft.notoriety.crime.CrimeNotificationListener
import com.hacklab.minecraft.notoriety.crime.CrimeRepository
import com.hacklab.minecraft.notoriety.crime.CrimeService
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.guild.cache.GuildCache
import com.hacklab.minecraft.notoriety.guild.command.GuildCommand
import com.hacklab.minecraft.notoriety.guild.display.GuildTagManager
import com.hacklab.minecraft.notoriety.guild.gui.GuildGUIManager
import com.hacklab.minecraft.notoriety.guild.listener.GuildEventListener
import com.hacklab.minecraft.notoriety.guild.repository.GuildInvitationRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildMembershipRepository
import com.hacklab.minecraft.notoriety.guild.repository.GuildRepository
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.guild.service.GuildServiceImpl
import com.hacklab.minecraft.notoriety.inspect.InspectCommand
import com.hacklab.minecraft.notoriety.inspect.InspectListener
import com.hacklab.minecraft.notoriety.inspect.InspectService
import com.hacklab.minecraft.notoriety.inspect.InspectionStick
import com.hacklab.minecraft.notoriety.ownership.ExplosionProtectionListener
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
    lateinit var inspectService: InspectService
        private set
    lateinit var inspectionStick: InspectionStick
        private set

    // ギルドシステム
    lateinit var guildService: GuildService
        private set
    lateinit var chatService: ChatService
        private set
    lateinit var guildGUIManager: GuildGUIManager
        private set
    private lateinit var guildCache: GuildCache
    private lateinit var whisperCommand: WhisperCommand
    private lateinit var guildTagManager: GuildTagManager

    private lateinit var ownershipRepository: OwnershipRepository

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
        ownershipRepository = OwnershipRepository(databaseManager)
        ownershipService = OwnershipService(ownershipRepository)
        trustService = TrustService(TrustRepository(databaseManager))
        crimeService = CrimeService(CrimeRepository(databaseManager), playerManager)
        reputationService = ReputationService(playerManager, TeamManager(this))
        villagerService = VillagerService(playerManager, i18nManager)
        golemService = GolemService(playerManager)
        bountyService = BountyService(this, economyService)
        bountyService.initializeSignManager()
        inspectService = InspectService(this, ownershipRepository, trustService, i18nManager)
        inspectionStick = InspectionStick(this, i18nManager)

        // ギルドシステム初期化
        val guildRepository = GuildRepository(databaseManager)
        val membershipRepository = GuildMembershipRepository(databaseManager)
        val invitationRepository = GuildInvitationRepository(databaseManager)
        val chatSettingsRepository = ChatSettingsRepository(databaseManager)
        guildCache = GuildCache()
        guildTagManager = GuildTagManager(reputationService.teamManager)
        guildService = GuildServiceImpl(
            plugin = this,
            guildRepository = guildRepository,
            membershipRepository = membershipRepository,
            invitationRepository = invitationRepository,
            trustService = trustService,
            guildCache = guildCache,
            guildTagManager = guildTagManager
        )
        chatService = ChatService(chatSettingsRepository, guildService)
        guildGUIManager = GuildGUIManager(this, guildService)

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
        pm.registerEvents(OwnershipListener(this, ownershipService, guildService, crimeService, chatService), this)
        pm.registerEvents(ExplosionProtectionListener(ownershipService), this)
        pm.registerEvents(VillagerListener(this, villagerService, golemService, crimeService, chatService, i18nManager), this)
        pm.registerEvents(TradeListener(playerManager, reputationService), this)
        pm.registerEvents(CombatListener(playerManager, crimeService, reputationService, bountyService, trustService, chatService, i18nManager), this)
        pm.registerEvents(CrimeNotificationListener(i18nManager), this)
        pm.registerEvents(BountySignListener(bountyService.signManager), this)
        pm.registerEvents(InspectListener(inspectService, inspectionStick), this)

        // ギルドシステムリスナー
        pm.registerEvents(GuildEventListener(guildService, chatService, guildTagManager, reputationService), this)
        pm.registerEvents(ChatListener(chatService, guildService), this)
        pm.registerEvents(guildGUIManager, this)
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
            it.tabCompleter = TabCompleter { sender, _, _, args ->
                mainCommand.onTabComplete(sender, it, "trust", arrayOf("trust") + args)
            }
        }

        getCommand("wanted")?.let {
            it.setExecutor { sender, _, _, args ->
                mainCommand.onCommand(sender, it, "wanted", arrayOf("bounty") + args)
            }
        }

        getCommand("inspect")?.let {
            val inspectCommand = InspectCommand(inspectService, inspectionStick, i18nManager)
            it.setExecutor { sender, _, _, args ->
                inspectCommand.execute(sender, args)
            }
            it.tabCompleter = org.bukkit.command.TabCompleter { sender, _, _, args ->
                inspectCommand.tabComplete(sender, args)
            }
        }

        // ギルドコマンド
        val guildCommand = GuildCommand(this, guildService, guildGUIManager)
        getCommand("guild")?.let {
            it.setExecutor(guildCommand)
            it.tabCompleter = guildCommand
        }

        // チャットコマンド
        val chatCommand = ChatCommand(chatService)
        getCommand("chat")?.let {
            it.setExecutor(chatCommand)
            it.tabCompleter = chatCommand
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
    }
}
