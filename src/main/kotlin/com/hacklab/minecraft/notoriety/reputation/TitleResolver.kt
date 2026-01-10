package com.hacklab.minecraft.notoriety.reputation

import com.hacklab.minecraft.notoriety.core.player.PlayerData

/**
 * 称号判定ロジック
 * 称号キーを返し、実際の翻訳はI18nManagerで行う
 */
object TitleResolver {

    /**
     * プレイヤーの称号キーを取得
     * @return 称号キー（例: "title.notable"）、称号なしの場合はnull
     */
    fun getTitleKey(data: PlayerData): String? {
        return when (data.getNameColor()) {
            NameColor.BLUE -> getBlueTitleKey(data.fame)
            NameColor.RED -> getRedTitleKey(data.pkCount)
            NameColor.GRAY -> getGrayTitleKey(data.fame)
        }
    }

    /**
     * 青プレイヤーの称号キー（Fame基準）
     */
    private fun getBlueTitleKey(fame: Int): String? {
        return when {
            fame >= 750 -> "title.glorious_lord"
            fame >= 500 -> "title.great_lord"
            fame >= 250 -> "title.lord"
            fame >= 100 -> "title.notable"
            else -> null
        }
    }

    /**
     * 赤プレイヤーの称号キー（PKCount基準）
     */
    private fun getRedTitleKey(pkCount: Int): String {
        return when {
            pkCount >= 200 -> "title.dread_lord"
            pkCount >= 100 -> "title.dark_lord"
            pkCount >= 50 -> "title.infamous"
            pkCount >= 30 -> "title.notorious"
            pkCount >= 10 -> "title.wicked"
            else -> "title.outcast"
        }
    }

    /**
     * 灰プレイヤーの称号キー（Fame基準）
     */
    private fun getGrayTitleKey(fame: Int): String? {
        return when {
            fame >= 750 -> "title.renegade"
            fame >= 500 -> "title.outlaw"
            fame >= 250 -> "title.rogue"
            fame >= 100 -> "title.scoundrel"
            else -> null
        }
    }
}
