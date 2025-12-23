package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.crime.CrimeRecord
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.TitleResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class NotorietyAPIImpl(private val plugin: Notoriety) : NotorietyAPI {

    // === CrimePoint ===
    override fun getCrimePoint(player: UUID): Int =
        plugin.playerManager.getPlayer(player)?.crimePoint ?: 0

    override fun setCrimePoint(player: UUID, value: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.crimePoint = value.coerceIn(0, 1000)
            updateDisplayIfOnline(player)
        }
    }

    override fun addCrimePoint(player: UUID, amount: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.addCrimePoint(amount)
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

    // === Karma ===
    override fun getKarma(player: UUID): Int =
        plugin.playerManager.getPlayer(player)?.karma ?: 0

    override fun setKarma(player: UUID, value: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.karma = value.coerceIn(0, 1000)
            updateDisplayIfOnline(player)
        }
    }

    override fun addKarma(player: UUID, amount: Int) {
        plugin.playerManager.getPlayer(player)?.let {
            it.addKarma(amount)
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
        plugin.playerManager.getPlayer(player)?.let { TitleResolver.getTitle(it) }

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
        plugin.crimeService.commitCrime(
            criminal = criminal,
            crimeType = crimeType,
            crimePoint = crimeType.defaultPoint,
            victim = victim,
            location = location,
            detail = detail
        )
    }

    override fun getCrimeHistory(player: UUID, limit: Int): List<CrimeRecord> =
        plugin.crimeService.getHistory(player, 1, limit)

    private fun updateDisplayIfOnline(player: UUID) {
        Bukkit.getPlayer(player)?.let {
            plugin.reputationService.updateDisplay(it)
        }
    }
}
