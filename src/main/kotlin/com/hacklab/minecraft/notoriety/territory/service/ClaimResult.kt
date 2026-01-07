package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.TerritoryChunk

/**
 * 領地設定の結果を表すsealed class
 */
sealed class ClaimResult {
    /** 設定成功 */
    data class Success(val chunk: TerritoryChunk) : ClaimResult()

    /** ギルドマスターではない */
    data object NotGuildMaster : ClaimResult()

    /** メンバーが10人未満 */
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
}
