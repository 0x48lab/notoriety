package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.core.config.ConfigManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.beacon.BeaconManager
import com.hacklab.minecraft.notoriety.territory.cache.TerritoryCache
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import com.hacklab.minecraft.notoriety.territory.repository.SigilRepository
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * シギルサービスの実装
 */
class SigilServiceImpl(
    private val plugin: JavaPlugin,
    private val sigilRepository: SigilRepository,
    private val beaconManager: BeaconManager,
    private val guildService: GuildService,
    private val cache: TerritoryCache,
    private val configManager: ConfigManager? = null
) : SigilService {

    // テレポートクールダウン追跡（プレイヤーUUID -> 最終テレポート時刻）
    private val teleportCooldowns = ConcurrentHashMap<UUID, Long>()

    // === シギル作成・削除 ===

    override fun createSigil(territoryId: Long, location: Location, name: String?): TerritorySigil {
        val existingSigils = sigilRepository.getByTerritory(territoryId)
        val sigilName = name ?: generateDefaultName(existingSigils.size)

        val sigil = TerritorySigil(
            territoryId = territoryId,
            name = sigilName,
            worldName = location.world?.name ?: throw IllegalArgumentException("World is null"),
            x = location.x,
            y = location.y,
            z = location.z
        )

        val sigilId = sigilRepository.create(sigil)
        val savedSigil = sigil.copy(id = sigilId)

        // ビーコンブロックを設置
        placeBeaconBlock(location)

        // キャッシュ更新（ギルドIDを取得するためにterritoryから逆引き）
        val territory = cache.getAllTerritories().find { it.id == territoryId }
        territory?.let { cache.addSigil(savedSigil, it.guildId) }

        plugin.logger.info("Sigil created: '$sigilName' at (${location.blockX}, ${location.blockY}, ${location.blockZ}) for territory $territoryId")

        return savedSigil
    }

    override fun deleteSigil(sigilId: Long, guildId: Long) {
        val sigil = sigilRepository.getById(sigilId) ?: return

        // ビーコンブロックを削除
        sigil.location?.let { removeBeaconBlock(it) }

        // DBから削除
        sigilRepository.delete(sigilId)

        // キャッシュから削除
        cache.removeSigil(sigilId, guildId)

        plugin.logger.info("Sigil deleted: '${sigil.name}' (ID: $sigilId)")
    }

    // === シギル取得 ===

    override fun getSigil(sigilId: Long): TerritorySigil? {
        return cache.getSigilById(sigilId) ?: sigilRepository.getById(sigilId)
    }

    override fun getSigilsForTerritory(territoryId: Long): List<TerritorySigil> {
        return sigilRepository.getByTerritory(territoryId)
    }

    override fun findSigilByName(territoryId: Long, name: String): TerritorySigil? {
        return getSigilsForTerritory(territoryId).find {
            it.name.equals(name, ignoreCase = true)
        }
    }

    // === シギル名前変更 ===

    override fun renameSigil(sigilId: Long, newName: String, guildId: Long, requester: UUID): RenameResult {
        // 権限チェック
        val guild = guildService.getGuild(guildId) ?: return RenameResult.NotInGuild
        if (guild.masterUuid != requester) return RenameResult.NotGuildMaster

        // シギル存在チェック
        val sigil = getSigil(sigilId) ?: return RenameResult.SigilNotFound

        // 名前バリデーション
        if (!isValidName(newName)) {
            return RenameResult.InvalidName("名前は32文字以内で、英数字・ひらがな・カタカナ・漢字のみ使用可能です")
        }

        // 重複チェック
        if (sigilRepository.existsByName(sigil.territoryId, newName)) {
            return RenameResult.NameAlreadyExists(newName)
        }

        val oldName = sigil.name

        // DB更新
        sigilRepository.updateName(sigilId, newName)

        // キャッシュ更新
        val updatedSigil = sigil.copy(name = newName)
        cache.removeSigil(sigilId, guildId)
        cache.addSigil(updatedSigil, guildId)

        plugin.logger.info("Sigil renamed: '$oldName' -> '$newName' (ID: $sigilId)")

        return RenameResult.Success(updatedSigil, oldName, newName)
    }

    override fun isValidName(name: String): Boolean {
        return TerritorySigil.isValidName(name)
    }

    override fun generateDefaultName(existingCount: Int): String {
        return TerritorySigil.generateDefaultName(existingCount)
    }

    // === テレポート ===

    override fun teleportToSigil(player: Player, sigil: TerritorySigil): TeleportResult {
        // クールダウンチェック
        val cooldownResult = checkCooldown(player.uniqueId)
        if (cooldownResult != null) return cooldownResult

        val sigilLocation = sigil.location ?: return TeleportResult.WorldNotFound

        // 安全なテレポート位置を探す
        val safeLocation = findSafeTeleportLocation(sigilLocation)
            ?: return TeleportResult.NoSafeLocation

        player.teleport(safeLocation)

        // クールダウン開始
        teleportCooldowns[player.uniqueId] = System.currentTimeMillis()

        plugin.logger.info("Player ${player.name} teleported to sigil '${sigil.name}'")

        return TeleportResult.Success(sigil)
    }

    /**
     * テレポートクールダウンをチェック
     * @return クールダウン中なら TeleportResult.OnCooldown、そうでなければ null
     */
    private fun checkCooldown(playerUuid: UUID): TeleportResult.OnCooldown? {
        val cooldownSeconds = configManager?.sigilTeleportCooldown ?: 30
        if (cooldownSeconds <= 0) return null

        val lastTeleport = teleportCooldowns[playerUuid] ?: return null
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastTeleport) / 1000

        if (elapsedSeconds < cooldownSeconds) {
            return TeleportResult.OnCooldown((cooldownSeconds - elapsedSeconds).toInt())
        }

        return null
    }

    /**
     * プレイヤーのクールダウンをクリア（ログアウト時など）
     */
    fun clearCooldown(playerUuid: UUID) {
        teleportCooldowns.remove(playerUuid)
    }

    override fun teleportToSigilByName(player: Player, guildId: Long, sigilName: String): TeleportResult {
        // ギルドメンバーチェック
        val playerGuild = guildService.getPlayerGuild(player.uniqueId)
            ?: return TeleportResult.NotInGuild

        if (playerGuild.id != guildId) return TeleportResult.NotInGuild

        // 領地取得
        val territory = cache.getTerritoryByGuild(guildId)
            ?: return TeleportResult.NoTerritory

        // シギル検索
        val sigil = findSigilByName(territory.id, sigilName)
            ?: return TeleportResult.SigilNameNotFound(sigilName)

        return teleportToSigil(player, sigil)
    }

    /**
     * 安全なテレポート位置を探す
     * シギル位置から上方向に空きスペースを探す
     */
    private fun findSafeTeleportLocation(sigilLocation: Location): Location? {
        val world = sigilLocation.world ?: return null

        // シギルの1ブロック上から開始
        var checkY = sigilLocation.blockY + 1
        val maxY = world.maxHeight

        while (checkY < maxY - 1) {
            val checkLocation = Location(
                world,
                sigilLocation.x + 0.5,  // ブロック中央
                checkY.toDouble(),
                sigilLocation.z + 0.5
            )

            if (isSafeToStand(checkLocation)) {
                // 視線方向を設定（シギル向き）
                checkLocation.yaw = sigilLocation.yaw
                checkLocation.pitch = 0f
                return checkLocation
            }

            checkY++
        }

        return null
    }

    /**
     * 指定位置に立てるか確認
     * 足元と頭上が空気かどうか、足元の下が固体かどうか
     */
    private fun isSafeToStand(location: Location): Boolean {
        val world = location.world ?: return false
        val blockBelow = world.getBlockAt(location).getRelative(BlockFace.DOWN)
        val blockAtFeet = world.getBlockAt(location)
        val blockAtHead = blockAtFeet.getRelative(BlockFace.UP)

        return blockBelow.type.isSolid &&
               !blockAtFeet.type.isSolid &&
               !blockAtFeet.isLiquid &&
               !blockAtHead.type.isSolid &&
               !blockAtHead.isLiquid
    }

    // === ビーコンブロック管理 ===

    override fun placeBeaconBlock(location: Location) {
        beaconManager.placeBeacon(location)
    }

    override fun removeBeaconBlock(location: Location) {
        beaconManager.removeBeacon(location)
    }
}
