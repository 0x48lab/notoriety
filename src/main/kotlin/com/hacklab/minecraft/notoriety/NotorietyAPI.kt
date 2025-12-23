package com.hacklab.minecraft.notoriety

import com.hacklab.minecraft.notoriety.crime.CrimeRecord
import com.hacklab.minecraft.notoriety.crime.CrimeType
import com.hacklab.minecraft.notoriety.reputation.NameColor
import org.bukkit.Location
import java.util.*

interface NotorietyAPI {
    // === CrimePoint操作 ===
    fun getCrimePoint(player: UUID): Int
    fun setCrimePoint(player: UUID, value: Int)
    fun addCrimePoint(player: UUID, amount: Int)

    // === PKCount操作 ===
    fun getPKCount(player: UUID): Int
    fun setPKCount(player: UUID, value: Int)
    fun addPKCount(player: UUID, amount: Int)

    // === Karma操作 ===
    fun getKarma(player: UUID): Int
    fun setKarma(player: UUID, value: Int)
    fun addKarma(player: UUID, amount: Int)

    // === Fame操作 ===
    fun getFame(player: UUID): Int
    fun setFame(player: UUID, value: Int)
    fun addFame(player: UUID, amount: Int)

    // === 状態取得 ===
    fun getNameColor(player: UUID): NameColor
    fun getTitle(player: UUID): String?

    // === 所有権操作 ===
    fun getBlockOwner(location: Location): UUID?
    fun setBlockOwner(location: Location, owner: UUID)
    fun removeBlockOwner(location: Location)

    // === 信頼操作 ===
    fun isTrusted(owner: UUID, player: UUID): Boolean
    fun addTrust(owner: UUID, player: UUID)
    fun removeTrust(owner: UUID, player: UUID)
    fun getTrustedPlayers(owner: UUID): List<UUID>

    // === 犯罪履歴 ===
    fun recordCrime(criminal: UUID, crimeType: CrimeType, victim: UUID?, location: Location?, detail: String?)
    fun getCrimeHistory(player: UUID, limit: Int): List<CrimeRecord>
}
