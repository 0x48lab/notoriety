package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.crime.CrimeRecord
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class NotorietyAPIImpl(private val plugin: Notoriety) : NotorietyAPI {

    // === Alignment ===
    override fun getAlignment(player: UUID): Int =
        plugin.playerManager.getPlayer(player)?.alignment ?: 0

    override fun setAlignment(player: UUID, value: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.alignment = value.coerceIn(-1000, 1000)
            updateDisplayIfOnline(player)
        }
    }

    override fun addAlignment(player: UUID, amount: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.addAlignment(amount)
            updateDisplayIfOnline(player)
        }
    }

    // === PKCount ===
    override fun getPKCount(player: UUID): Int =
        plugin.playerManager.getPlayer(player)?.pkCount ?: 0

    override fun setPKCount(player: UUID, value: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.pkCount = maxOf(0, value)
            updateDisplayIfOnline(player)
        }
    }

    override fun addPKCount(player: UUID, amount: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.pkCount = maxOf(0, it.pkCount + amount)
            updateDisplayIfOnline(player)
        }
    }

    // === Fame ===
    override fun getFame(player: UUID): Int =
        plugin.playerManager.getPlayer(player)?.fame ?: 0

    override fun setFame(player: UUID, value: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.fame = value.coerceIn(0, 1000)
            updateDisplayIfOnline(player)
        }
    }

    override fun addFame(player: UUID, amount: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.addFame(amount)
            updateDisplayIfOnline(player)
        }
    }

    // === 状態取得 ===
    override fun getNameColor(player: UUID): NameColor =
        plugin.playerManager.getPlayer(player)?.getNameColor() ?: NameColor.BLUE

    override fun getTitle(player: UUID): String? =
        plugin.notorietyService.getTitleKey(player)?.let { key ->
            // 英語版を返す（API互換性用）
            plugin.i18nManager.getForLocale("en", key, key)
        }

    override fun getLocalizedTitle(player: UUID): String? =
        plugin.notorietyService.getLocalizedTitle(player)

    // === 所有権操作 ===
    override fun getBlockOwner(location: Location): UUID? =
        plugin.ownershipService.getOwner(location)

    override fun setBlockOwner(location: Location, owner: UUID) =
        plugin.ownershipService.registerOwnership(location, owner)

    override fun removeBlockOwner(location: Location) =
        plugin.ownershipService.removeOwnership(location)

    // === 信頼操作 ===
    override fun isTrusted(owner: UUID, player: UUID): Boolean =
        plugin.trustService.isTrusted(owner, player)

    override fun addTrust(owner: UUID, player: UUID) =
        plugin.trustService.addTrust(owner, player)

    override fun removeTrust(owner: UUID, player: UUID) =
        plugin.trustService.removeTrust(owner, player)

    override fun getTrustedPlayers(owner: UUID): List<UUID> =
        plugin.trustService.getTrustedPlayers(owner)

    // === 犯罪履歴 ===
    override fun recordCrime(
        criminal: UUID,
        crimeType: CrimeType,
        victim: UUID?,
        location: Location?,
        detail: String?
    ) {
        plugin.notorietyService.commitCrime(
            criminal = criminal,
            crimeType = crimeType,
            alignmentPenalty = crimeType.defaultPenalty,
            victim = victim,
            location = location,
            detail = detail
        )
    }

    override fun getCrimeHistory(player: UUID, limit: Int): List<CrimeRecord> =
        plugin.notorietyService.getHistory(player, 1, limit)

    private fun updateDisplayIfOnline(player: UUID) {
        Bukkit.getPlayer(player)?.let {
            plugin.notorietyService.updateDisplay(it)
        }
    }
}
