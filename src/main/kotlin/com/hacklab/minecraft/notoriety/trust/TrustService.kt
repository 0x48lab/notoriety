package com.hacklab.minecraft.notoriety.trust

import java.util.*

class TrustService(private val repository: TrustRepository) {

    fun addTrust(truster: UUID, trusted: UUID) {
        repository.setTrustState(truster, trusted, TrustState.TRUST)
    }

    fun removeTrust(truster: UUID, trusted: UUID) {
        repository.removeTrust(truster, trusted)
    }

    fun isTrusted(owner: UUID, accessor: UUID): Boolean {
        return repository.getTrustState(owner, accessor) == TrustState.TRUST
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

    // === 三段階信頼システム ===

    /**
     * 信頼状態を取得
     * @return TrustState（TRUST, DISTRUST）またはnull（UNSET = レコードなし）
     */
    fun getTrustState(owner: UUID, accessor: UUID): TrustState? {
        return repository.getTrustState(owner, accessor)
    }

    /**
     * 信頼状態を設定（TRUST または DISTRUST）
     */
    fun setTrustState(owner: UUID, accessor: UUID, state: TrustState) {
        repository.setTrustState(owner, accessor, state)
    }

    /**
     * 信頼設定を解除（UNSET に戻す = レコード削除）
     */
    fun removeTrustState(owner: UUID, accessor: UUID) {
        repository.removeTrust(owner, accessor)
    }

    /**
     * 不信頼に設定しているプレイヤーのリスト
     */
    fun getDistrustedPlayers(owner: UUID): List<UUID> {
        return repository.getDistrustedPlayers(owner)
    }

    /**
     * 信頼関係のあるすべてのプレイヤーを取得（状態付き）
     */
    fun getAllTrustRelations(owner: UUID): Map<UUID, TrustState> {
        return repository.getAllTrustRelations(owner)
    }
}
