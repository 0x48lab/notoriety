package com.hacklab.minecraft.notoriety.trust

import java.util.*

class TrustService(private val repository: TrustRepository) {

    fun addTrust(truster: UUID, trusted: UUID) {
        repository.addTrust(truster, trusted)
    }

    fun removeTrust(truster: UUID, trusted: UUID) {
        repository.removeTrust(truster, trusted)
    }

    fun isTrusted(owner: UUID, accessor: UUID): Boolean {
        return repository.isTrusted(owner, accessor)
    }

    // 相互に信頼しているかチェック
    fun isMutuallyTrusted(player1: UUID, player2: UUID): Boolean {
        return isTrusted(player1, player2) && isTrusted(player2, player1)
    }

    fun getTrustedPlayers(truster: UUID): List<UUID> {
        return repository.getTrustedPlayers(truster)
    }

    fun getTrusterCount(trusted: UUID): Int {
        return repository.getTrusterCount(trusted)
    }

    // 指定プレイヤーを信頼しているプレイヤーのリスト
    fun getPlayersWhoTrust(trusted: UUID): List<UUID> {
        return repository.getPlayersWhoTrust(trusted)
    }
}
