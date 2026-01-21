package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import org.bukkit.Location
import java.util.UUID

/**
 * 領地サービスインターフェース
 */
interface TerritoryService {

    companion object {
        /** 領地確保に必要な最小メンバー数（新方式: 不要） */
        const val MIN_MEMBERS_FOR_TERRITORY = 1

        /** 新方式: 1 + floor(memberCount / 3) */
        const val MEMBERS_PER_CHUNK_DIVISOR = 3

        /** 旧方式: 1チャンクあたりの必要メンバー数（後方互換用） */
        @Deprecated("Use MEMBERS_PER_CHUNK_DIVISOR instead")
        const val MEMBERS_PER_CHUNK = 5
    }

    // === 領地管理 ===

    /**
     * 指定位置に領地チャンクを設定する
     * @param guildId ギルドID
     * @param location プレイヤーの現在位置
     * @param requester 実行者UUID（nullの場合は権限チェックをスキップ=管理者モード）
     * @param sigilName シギル名（飛び地の場合、nullなら自動生成）
     * @return 設定結果
     */
    fun claimTerritory(guildId: Long, location: Location, requester: UUID?, sigilName: String? = null): ClaimResult

    // === 隣接・飛び地判定 ===

    /**
     * 指定位置に隣接する既存チャンクを検索
     * @param guildId ギルドID
     * @param worldName ワールド名
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     * @return 隣接するチャンクのリスト
     */
    fun findAdjacentChunks(guildId: Long, worldName: String, chunkX: Int, chunkZ: Int): List<TerritoryChunk>

    /**
     * 指定位置が飛び地（既存領地と非隣接）かどうか
     * @param guildId ギルドID
     * @param worldName ワールド名
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     * @return 飛び地ならtrue
     */
    fun isEnclave(guildId: Long, worldName: String, chunkX: Int, chunkZ: Int): Boolean

    /**
     * 連続グループをマージする（新チャンクが複数グループを接続する場合）
     * @param guildId ギルドID
     * @param adjacentSigilIds 隣接するシギルIDのセット
     * @return マージ後のシギルID
     */
    fun mergeGroups(guildId: Long, adjacentSigilIds: Set<Long>): Long?

    /**
     * ギルドの全領地を解放する
     * @param guildId ギルドID
     * @param requester 実行者UUID（nullの場合は権限チェックをスキップ=管理者モード）
     * @return 解放結果
     */
    fun releaseAllTerritory(guildId: Long, requester: UUID?): ReleaseResult

    /**
     * 指定番号のチャンクを解放する
     * @param guildId ギルドID
     * @param chunkNumber チャンク番号（addOrder）
     * @param requester 実行者UUID（nullの場合は権限チェックをスキップ=管理者モード）
     * @return 解放結果
     */
    fun releaseChunk(guildId: Long, chunkNumber: Int, requester: UUID?): ReleaseResult

    /**
     * 指定チャンク数まで領地を縮小する（LIFO順）
     * @param guildId ギルドID
     * @param targetChunkCount 目標チャンク数
     */
    fun shrinkTerritoryTo(guildId: Long, targetChunkCount: Int)

    // === 領地取得 ===

    /**
     * ギルドIDから領地を取得
     */
    fun getTerritory(guildId: Long): GuildTerritory?

    /**
     * 指定位置のチャンクを取得
     */
    fun getChunkAt(location: Location): TerritoryChunk?

    /**
     * 指定位置の領地を取得
     */
    fun getTerritoryAt(location: Location): GuildTerritory?

    /**
     * 全領地を取得
     */
    fun getAllTerritories(): List<GuildTerritory>

    // === 判定 ===

    /**
     * 指定位置が領地内かどうか
     */
    fun isInTerritory(location: Location): Boolean

    /**
     * 指定位置がギルドの領地内かどうか
     */
    fun isInGuildTerritory(location: Location, guildId: Long): Boolean

    /**
     * プレイヤーが指定位置でアクセス可能か
     * (ギルドメンバーまたは領地外)
     */
    fun canAccessAt(location: Location, playerUuid: UUID): Boolean

    // === 計算 ===

    /**
     * メンバー数から許可されるチャンク数を計算
     */
    fun calculateAllowedChunks(memberCount: Int): Int

    /**
     * ギルドが追加でチャンクを確保可能か
     */
    fun canClaimMoreChunks(guildId: Long): Boolean

    // === キャッシュ管理 ===

    /**
     * キャッシュを再読み込み
     */
    fun reloadCache()

    // === 領地設定 ===

    /**
     * モンスタースポーン設定を変更
     * @param guildId ギルドID
     * @param enabled true=スポーン許可, false=スポーン禁止
     * @return 成功したらtrue
     */
    fun setMobSpawnEnabled(guildId: Long, enabled: Boolean): Boolean

    /**
     * 指定位置でモンスターがスポーン可能か
     * @param location チェックする位置
     * @return スポーン可能ならtrue（領地外または設定で許可されている場合）
     */
    fun isMobSpawnAllowed(location: Location): Boolean
}
