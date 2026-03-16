package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.GuildBase
import com.hacklab.minecraft.notoriety.territory.repository.GuildBaseRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class GuildBaseServiceImpl(
    private val repository: GuildBaseRepository,
    private val sigilService: SigilService
) : GuildBaseService {

    override fun setBase(guildId: Long, location: Location, setBy: UUID): GuildBase {
        val base = GuildBase(
            guildId = guildId,
            worldName = location.world?.name ?: throw IllegalArgumentException("World is null"),
            x = location.x,
            y = location.y,
            z = location.z,
            yaw = location.yaw,
            pitch = location.pitch,
            setBy = setBy
        )
        val id = repository.upsert(base)
        return base.copy(id = id)
    }

    override fun getBase(guildId: Long): GuildBase? {
        return repository.findByGuildId(guildId)
    }

    override fun removeBase(guildId: Long): Boolean {
        return repository.deleteByGuildId(guildId)
    }

    override fun teleportToBase(player: Player, guildId: Long): TeleportResult {
        // 拠点取得
        val base = repository.findByGuildId(guildId) ?: return TeleportResult.NoBase

        // クールダウンチェック（シギルTPと共有）
        val cooldownResult = sigilService.checkCooldown(player.uniqueId)
        if (cooldownResult != null) return cooldownResult

        // ワールド存在チェック
        val world = Bukkit.getWorld(base.worldName) ?: return TeleportResult.WorldNotFound

        // 基準位置を作成
        val baseLocation = Location(world, base.x, base.y, base.z, base.yaw, base.pitch)

        // 安全なテレポート位置を探す
        val safeLocation = sigilService.findSafeTeleportLocation(baseLocation)
            ?: return TeleportResult.NoSafeLocation

        // yaw/pitchを保持
        safeLocation.yaw = base.yaw
        safeLocation.pitch = base.pitch

        // テレポート実行
        player.teleport(safeLocation)

        // クールダウン設定（シギルTPと共有）
        sigilService.setCooldown(player.uniqueId)

        return TeleportResult.BaseSuccess(base)
    }
}
