package com.hacklab.minecraft.notoriety.crime

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.event.PlayerCrimeEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.util.*

class CrimeService(
    private val repository: CrimeRepository,
    private val playerManager: PlayerManager
) {
    fun commitCrime(
        criminal: UUID,
        crimeType: CrimeType,
        crimePoint: Int,
        victim: UUID? = null,
        location: Location? = null,
        detail: String? = null
    ) {
        val player = playerManager.getPlayer(criminal) ?: return

        // Alignment減少（悪行なのでマイナス）
        player.addAlignment(-crimePoint)

        // 被害者名を取得
        val victimName = victim?.let { Bukkit.getOfflinePlayer(it).name }

        // 犯罪履歴に記録
        repository.recordCrime(CrimeRecord(
            criminalUuid = criminal,
            crimeType = crimeType,
            victimUuid = victim,
            victimName = victimName,
            world = location?.world?.name,
            x = location?.blockX,
            y = location?.blockY,
            z = location?.blockZ,
            detail = detail,
            crimePoint = crimePoint,
            committedAt = Instant.now()
        ))

        // カスタムイベント発火
        Bukkit.getPluginManager().callEvent(
            PlayerCrimeEvent(criminal, crimeType, crimePoint)
        )
    }

    fun getHistory(player: UUID, page: Int, pageSize: Int = 10): List<CrimeRecord> =
        repository.getHistory(player, page, pageSize)

    fun getHistoryCount(player: UUID): Int =
        repository.getHistoryCount(player)

    /**
     * 犯罪履歴のみを記録する（CrimePoint加算なし）
     * 動物殺害など、履歴は残すがCrimePointを加算しない場合に使用
     */
    fun recordCrimeHistory(
        criminal: UUID,
        crimeType: CrimeType,
        crimePoint: Int = 0,
        victim: UUID? = null,
        location: Location? = null,
        detail: String? = null
    ) {
        val victimName = victim?.let { Bukkit.getOfflinePlayer(it).name }

        repository.recordCrime(CrimeRecord(
            criminalUuid = criminal,
            crimeType = crimeType,
            victimUuid = victim,
            victimName = victimName,
            world = location?.world?.name,
            x = location?.blockX,
            y = location?.blockY,
            z = location?.blockZ,
            detail = detail,
            crimePoint = crimePoint,
            committedAt = Instant.now()
        ))
    }
}
