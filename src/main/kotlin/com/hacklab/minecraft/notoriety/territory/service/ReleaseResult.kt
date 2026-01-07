package com.hacklab.minecraft.notoriety.territory.service

/**
 * 領地解放の結果を表すsealed class
 */
sealed class ReleaseResult {
    /** 解放成功 */
    data class Success(val releasedChunkCount: Int) : ReleaseResult()

    /** ギルドマスターではない */
    data object NotGuildMaster : ReleaseResult()

    /** 領地が設定されていない */
    data object NoTerritory : ReleaseResult()

    /** ギルドが見つからない */
    data object GuildNotFound : ReleaseResult()

    /** 指定番号のチャンクが見つからない */
    data class ChunkNotFound(val chunkNumber: Int) : ReleaseResult()
}

/**
 * 領地解放の理由
 */
enum class ReleaseReason {
    /** GMによる手動解放 */
    MANUAL,
    /** メンバー減少 */
    MEMBER_DECREASE,
    /** ギルド解散 */
    GUILD_DISSOLVED
}
