package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil

/**
 * 領地設定の結果を表すsealed class
 */
sealed class ClaimResult {
    /**
     * 設定成功
     * @param chunk 追加されたチャンク
     * @param sigil シギル（新規作成された場合はnew sigil、既存グループに追加の場合はそのグループのシギル）
     * @param isNewSigil 新規シギルが作成されたか（飛び地の場合true）
     * @param mergedSigilIds マージされたシギルIDのリスト（複数グループを接続した場合）
     */
    data class Success(
        val chunk: TerritoryChunk,
        val sigil: TerritorySigil? = null,
        val isNewSigil: Boolean = false,
        val mergedSigilIds: List<Long> = emptyList()
    ) : ClaimResult()

    /** ギルドマスターではない */
    data object NotGuildMaster : ClaimResult()

    /** メンバーが不足 */
    data object NotEnoughMembers : ClaimResult()

    /** configの最大チャンク数に達している */
    data object MaxChunksReached : ClaimResult()

    /** メンバー数に応じた最大チャンク数に達している */
    data object MemberChunkLimitReached : ClaimResult()

    /** 他ギルドの領地と重複 */
    data class OverlapOtherGuild(val guildName: String) : ClaimResult()

    /** ギルドが見つからない */
    data object GuildNotFound : ClaimResult()

    /** ギルドに所属していない */
    data object NotInGuild : ClaimResult()

    /** 既に同じチャンクを領地として保持している */
    data object AlreadyClaimed : ClaimResult()

    /** シギル名が無効 */
    data class InvalidSigilName(val reason: String) : ClaimResult()

    /** シギル名が既に存在する */
    data class SigilNameAlreadyExists(val name: String) : ClaimResult()
}
