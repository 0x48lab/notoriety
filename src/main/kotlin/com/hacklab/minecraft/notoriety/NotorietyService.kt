package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.chat.service.ChatService
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.core.player.PlayerData
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.crime.CrimeRecord
import com.hacklab.minecraft.notoriety.crime.CrimeRepository
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.event.PlayerColorChangeEvent
import com.hacklab.minecraft.notoriety.event.PlayerCrimeEvent
import com.hacklab.minecraft.notoriety.event.PlayerGoodDeedEvent
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.ownership.OwnershipService
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.TeamManager
import com.hacklab.minecraft.notoriety.reputation.TitleResolver
import com.hacklab.minecraft.notoriety.trust.TrustService
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.Villager
import org.bukkit.entity.memory.MemoryKey
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 名声システムの中核サービス
 * 犯罪判定、警告、Alignment/Fame操作、表示更新を一元管理
 */
class NotorietyService(
    private val playerManager: PlayerManager,
    private val crimeRepository: CrimeRepository,
    val teamManager: TeamManager,
    private val ownershipService: OwnershipService,
    private val trustService: TrustService,
    private val guildService: GuildService,
    private val chatService: ChatService,
    private val i18nManager: I18nManager
) {
    // 警告のクールダウン管理（一元化）
    private data class WarningKey(val playerUuid: UUID, val targetId: String)
    private val warningCooldowns = ConcurrentHashMap<WarningKey, Instant>()
    private val WARNING_COOLDOWN_SECONDS = 10L

    // ========== 称号取得（一元化） ==========

    /**
     * プレイヤーのローカライズ済み称号を取得（一元化されたAPI）
     * 全てのコンポーネントはこのメソッドを使用すること
     */
    fun getLocalizedTitle(playerUuid: UUID): String? {
        val data = playerManager.getPlayer(playerUuid) ?: return null
        val titleKey = TitleResolver.getTitleKey(data) ?: return null
        return i18nManager.get(playerUuid, titleKey, titleKey)
    }

    /**
     * プレイヤーの称号キーを取得
     */
    fun getTitleKey(playerUuid: UUID): String? {
        val data = playerManager.getPlayer(playerUuid) ?: return null
        return TitleResolver.getTitleKey(data)
    }

    // ========== 表示更新 ==========

    /**
     * プレイヤーの表示（名前色、称号）を更新
     */
    fun updateDisplay(player: Player) {
        val data = playerManager.getPlayer(player) ?: return
        val color = data.getNameColor()
        val title = getLocalizedTitle(player.uniqueId)
        teamManager.updatePlayerTeam(player, color, title)
    }

    // ========== 犯罪処理 ==========

    /**
     * 犯罪を確定する（Alignment減少、履歴記録、イベント発火）
     */
    fun commitCrime(
        criminal: UUID,
        crimeType: CrimeType,
        alignmentPenalty: Int,
        victim: UUID? = null,
        location: Location? = null,
        detail: String? = null
    ) {
        val player = playerManager.getPlayer(criminal) ?: return
        val oldColor = player.getNameColor()

        // Alignment減少
        player.addAlignment(-alignmentPenalty)

        // 色変化チェック
        val newColor = player.getNameColor()
        if (oldColor != newColor) {
            Bukkit.getPluginManager().callEvent(
                PlayerColorChangeEvent(criminal, oldColor, newColor)
            )
        }

        // 被害者名を取得
        val victimName = victim?.let { Bukkit.getOfflinePlayer(it).name }

        // 犯罪履歴に記録
        crimeRepository.recordCrime(CrimeRecord(
            criminalUuid = criminal,
            crimeType = crimeType,
            victimUuid = victim,
            victimName = victimName,
            world = location?.world?.name,
            x = location?.blockX,
            y = location?.blockY,
            z = location?.blockZ,
            detail = detail,
            alignmentPenalty = alignmentPenalty,
            committedAt = Instant.now()
        ))

        // イベント発火
        Bukkit.getPluginManager().callEvent(
            PlayerCrimeEvent(criminal, crimeType, alignmentPenalty, victim, victimName)
        )

        // 表示更新
        Bukkit.getPlayer(criminal)?.let { updateDisplay(it) }
    }

    /**
     * 犯罪履歴のみを記録する（Alignment減少なし）
     */
    fun recordCrimeHistory(
        criminal: UUID,
        crimeType: CrimeType,
        alignmentPenalty: Int = 0,
        victim: UUID? = null,
        location: Location? = null,
        detail: String? = null
    ) {
        val victimName = victim?.let { Bukkit.getOfflinePlayer(it).name }

        crimeRepository.recordCrime(CrimeRecord(
            criminalUuid = criminal,
            crimeType = crimeType,
            victimUuid = victim,
            victimName = victimName,
            world = location?.world?.name,
            x = location?.blockX,
            y = location?.blockY,
            z = location?.blockZ,
            detail = detail,
            alignmentPenalty = alignmentPenalty,
            committedAt = Instant.now()
        ))
    }

    // ========== 善行処理 ==========

    /**
     * 善行によるAlignment/Fame増加
     */
    fun onGoodDeed(playerUuid: UUID, alignmentGain: Int, fameGain: Int) {
        val data = playerManager.getPlayer(playerUuid) ?: return
        val oldColor = data.getNameColor()

        data.addAlignment(alignmentGain)
        data.addFame(fameGain)

        val newColor = data.getNameColor()
        if (oldColor != newColor) {
            Bukkit.getPluginManager().callEvent(
                PlayerColorChangeEvent(playerUuid, oldColor, newColor)
            )
        }

        Bukkit.getPluginManager().callEvent(
            PlayerGoodDeedEvent(playerUuid, alignmentGain, fameGain)
        )

        Bukkit.getPlayer(playerUuid)?.let { updateDisplay(it) }
    }

    // ========== PK処理 ==========

    /**
     * プレイヤー殺害時の処理
     */
    fun onPlayerKill(killerUuid: UUID, victimUuid: UUID) {
        val killerData = playerManager.getPlayer(killerUuid) ?: return
        val victimData = playerManager.getPlayer(victimUuid) ?: return
        val oldColor = killerData.getNameColor()

        when (victimData.getNameColor()) {
            NameColor.BLUE -> {
                killerData.pkCount++
                killerData.alignment = -1000

                val newColor = killerData.getNameColor()
                if (oldColor != newColor) {
                    Bukkit.getPluginManager().callEvent(
                        PlayerColorChangeEvent(killerUuid, oldColor, newColor)
                    )
                }
            }
            NameColor.RED -> {
                killerData.addAlignment(50)
                killerData.addFame(50)

                if (victimData.fame > killerData.fame) {
                    killerData.fame = victimData.fame
                }
            }
            NameColor.GRAY -> {
                // ペナルティなし
            }
        }

        Bukkit.getPlayer(killerUuid)?.let { updateDisplay(it) }
    }

    // ========== 履歴取得 ==========

    fun getHistory(player: UUID, page: Int, pageSize: Int = 10): List<CrimeRecord> =
        crimeRepository.getHistory(player, page, pageSize)

    fun getHistoryCount(player: UUID): Int =
        crimeRepository.getHistoryCount(player)

    // ========== 警告システム ==========

    /**
     * 警告を表示すべきかチェック（cooldown込み）
     */
    private fun shouldShowWarning(playerUuid: UUID, targetId: String): Boolean {
        if (!chatService.isWarningsEnabled(playerUuid)) return false

        val warningKey = WarningKey(playerUuid, targetId)
        val lastWarning = warningCooldowns[warningKey]
        val now = Instant.now()

        if (lastWarning != null && Duration.between(lastWarning, now).seconds < WARNING_COOLDOWN_SECONDS) {
            return false
        }

        warningCooldowns[warningKey] = now
        return true
    }

    /**
     * 警告メッセージを送信
     */
    private fun sendWarning(player: Player, messageKey: String, vararg args: Any) {
        val message = i18nManager.get(messageKey, messageKey).format(*args)
        player.sendMessage(message)
    }

    // ========== 所有権ベースの犯罪判定・警告 ==========

    /**
     * ブロック破壊が犯罪になるかチェックし、必要なら警告を出す
     * @return 犯罪になる場合true
     */
    fun checkBlockBreakCrime(player: Player, location: Location, isContainer: Boolean): CrimeCheckResult {
        val owner = ownershipService.getOwner(location) ?: return CrimeCheckResult.NOT_A_CRIME

        // アクセス権がある場合は犯罪にならない
        if (ownershipService.canAccess(location, player.uniqueId, guildService)) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val ownerName = Bukkit.getOfflinePlayer(owner).name ?: "???"
        val targetId = "block:${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"

        // 警告表示
        if (shouldShowWarning(player.uniqueId, targetId)) {
            val messageKey = if (isContainer) "warning.container_break" else "warning.block_break"
            sendWarning(player, messageKey, ownerName)
        }

        return CrimeCheckResult.IsCrime(owner, if (isContainer) 10 else 10)
    }

    /**
     * コンテナからのアイテム取り出しが犯罪になるかチェックし、必要なら警告を出す
     * @return 犯罪になる場合true
     */
    fun checkContainerAccessCrime(player: Player, location: Location): CrimeCheckResult {
        val owner = ownershipService.getOwner(location) ?: return CrimeCheckResult.NOT_A_CRIME

        // 取り出し権限がある場合は犯罪にならない
        if (ownershipService.canTakeFromContainer(location, player.uniqueId, guildService)) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val ownerName = Bukkit.getOfflinePlayer(owner).name ?: "???"
        val targetId = "container:${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"

        // 警告表示（コンテナを開いた時）
        if (shouldShowWarning(player.uniqueId, targetId)) {
            sendWarning(player, "warning.chest_open", ownerName)
        }

        return CrimeCheckResult.IsCrime(owner, 50)
    }

    // ========== プレイヤー攻撃の犯罪判定・警告 ==========

    /**
     * プレイヤー攻撃が犯罪になるかチェックし、必要なら警告を出す
     */
    fun checkPlayerAttackCrime(attacker: Player, victim: Player): CrimeCheckResult {
        // 被害者が加害者を信頼していれば犯罪にならない
        if (trustService.isTrusted(victim.uniqueId, attacker.uniqueId)) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val victimData = playerManager.getPlayer(victim) ?: return CrimeCheckResult.NOT_A_CRIME

        // 青プレイヤーへの攻撃のみ犯罪
        if (victimData.getNameColor() != NameColor.BLUE) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val targetId = "player:${victim.uniqueId}"

        if (shouldShowWarning(attacker.uniqueId, targetId)) {
            sendWarning(attacker, "warning.attack_player", victim.name)
        }

        return CrimeCheckResult.IsCrime(victim.uniqueId, 1)
    }

    /**
     * ペット攻撃が犯罪になるかチェックし、必要なら警告を出す
     */
    fun checkPetAttackCrime(attacker: Player, pet: Tameable): CrimeCheckResult {
        if (!pet.isTamed) return CrimeCheckResult.NOT_A_CRIME

        val owner = pet.owner as? Player ?: return CrimeCheckResult.NOT_A_CRIME

        if (owner.uniqueId == attacker.uniqueId) return CrimeCheckResult.NOT_A_CRIME

        if (trustService.isTrusted(owner.uniqueId, attacker.uniqueId)) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val ownerData = playerManager.getPlayer(owner) ?: return CrimeCheckResult.NOT_A_CRIME

        if (ownerData.getNameColor() != NameColor.BLUE) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val targetId = "pet:${pet.uniqueId}"

        if (shouldShowWarning(attacker.uniqueId, targetId)) {
            sendWarning(attacker, "warning.attack_pet", owner.name)
        }

        return CrimeCheckResult.IsCrime(owner.uniqueId, 1)
    }

    // ========== 村人関連の犯罪判定・警告 ==========

    /**
     * 村人のベッド破壊が犯罪になるかチェック
     */
    fun checkVillagerBedCrime(player: Player, location: Location): CrimeCheckResult {
        // 自分で設置したブロック、または信頼されたプレイヤーなら犯罪にならない
        val owner = ownershipService.getOwner(location)
        if (owner != null && (owner == player.uniqueId || trustService.isTrusted(owner, player.uniqueId))) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        // このベッドに紐づいている村人を探す
        val villager = findVillagerWithBed(location) ?: return CrimeCheckResult.NOT_A_CRIME

        val targetId = "villager_bed:${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"

        if (shouldShowWarning(player.uniqueId, targetId)) {
            sendWarning(player, "warning.villager_bed")
        }

        return CrimeCheckResult.VillageCrime(1)
    }

    /**
     * 村人の仕事場破壊が犯罪になるかチェック
     */
    fun checkVillagerWorkstationCrime(player: Player, location: Location): CrimeCheckResult {
        val owner = ownershipService.getOwner(location)
        if (owner != null && (owner == player.uniqueId || trustService.isTrusted(owner, player.uniqueId))) {
            return CrimeCheckResult.NOT_A_CRIME
        }

        val villager = findVillagerWithWorkstation(location) ?: return CrimeCheckResult.NOT_A_CRIME

        val targetId = "villager_workstation:${location.world.name}:${location.blockX}:${location.blockY}:${location.blockZ}"

        if (shouldShowWarning(player.uniqueId, targetId)) {
            sendWarning(player, "warning.villager_workstation")
        }

        return CrimeCheckResult.VillageCrime(1)
    }

    /**
     * 村人攻撃が犯罪になるかチェック
     */
    fun checkVillagerAttackCrime(attacker: Player, villager: Villager): CrimeCheckResult {
        val targetId = "villager:${villager.uniqueId}"

        if (shouldShowWarning(attacker.uniqueId, targetId)) {
            sendWarning(attacker, "warning.attack_villager")
        }

        return CrimeCheckResult.VillageCrime(1)
    }

    /**
     * ゴーレム攻撃が犯罪になるかチェック
     */
    fun checkGolemAttackCrime(attacker: Player, golem: IronGolem): CrimeCheckResult {
        val targetId = "golem:${golem.uniqueId}"

        if (shouldShowWarning(attacker.uniqueId, targetId)) {
            sendWarning(attacker, "warning.attack_golem")
        }

        return CrimeCheckResult.VillageCrime(50)
    }

    // ========== ヘルパーメソッド ==========

    private fun findVillagerWithBed(bedLocation: Location): Villager? {
        val searchRange = 64.0
        return bedLocation.world.getNearbyEntities(
            bedLocation, searchRange, searchRange, searchRange
        ).filterIsInstance<Villager>()
            .firstOrNull { villager ->
                val home = villager.getMemory(MemoryKey.HOME)
                home != null &&
                    home.blockX == bedLocation.blockX &&
                    home.blockY == bedLocation.blockY &&
                    home.blockZ == bedLocation.blockZ
            }
    }

    private fun findVillagerWithWorkstation(blockLocation: Location): Villager? {
        val searchRange = 64.0
        return blockLocation.world.getNearbyEntities(
            blockLocation, searchRange, searchRange, searchRange
        ).filterIsInstance<Villager>()
            .firstOrNull { villager ->
                val jobSite = villager.getMemory(MemoryKey.JOB_SITE)
                jobSite != null &&
                    jobSite.blockX == blockLocation.blockX &&
                    jobSite.blockY == blockLocation.blockY &&
                    jobSite.blockZ == blockLocation.blockZ
            }
    }
}

/**
 * 犯罪チェックの結果
 */
sealed class CrimeCheckResult {
    /** 犯罪にならない */
    object NOT_A_CRIME : CrimeCheckResult()

    /** 所有権ベースの犯罪 */
    data class IsCrime(val victimUuid: UUID, val penalty: Int) : CrimeCheckResult()

    /** 村人関連の犯罪（被害者なし） */
    data class VillageCrime(val penalty: Int) : CrimeCheckResult()
}
