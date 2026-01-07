package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.GuildTerritory
import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import org.bukkit.Location
import java.util.UUID

/**
 * 領地サービスインターフェース
 */
interface TerritoryService {

    companion object {
        /** 領地確保に必要な最小メンバー数 */
        const val MIN_MEMBERS_FOR_TERRITORY = 5

        /** 1チャンクあたりの必要メンバー数 */
        const val MEMBERS_PER_CHUNK = 5
    }

    // === 領地管理 ===

    /**
     * 指定位置に領地チャンクを設定する
     * @param guildId ギルドID
     * @param location プレイヤーの現在位置（チャンク中心）
     * @param requester 実行者UUID
     * @return 設定結果
     */
    fun claimTerritory(guildId: Long, location: Location, requester: UUID): ClaimResult

    /**
     * ギルドの全領地を解放する
     * @param guildId ギルドID
     * @param requester 実行者UUID
     * @return 解放結果
     */
    fun releaseAllTerritory(guildId: Long, requester: UUID): ReleaseResult

    /**
     * 指定番号のチャンクを解放する
     * @param guildId ギルドID
     * @param chunkNumber チャンク番号（addOrder）
     * @param requester 実行者UUID
     * @return 解放結果
     */
    fun releaseChunk(guildId: Long, chunkNumber: Int, requester: UUID): ReleaseResult

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
}
