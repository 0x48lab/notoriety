package com.hacklab.minecraft.notoriety.reputation

import com.hacklab.minecraft.notoriety.core.player.PlayerData

object TitleResolver {
    fun getTitle(data: PlayerData): String? {
        return when (data.getNameColor()) {
            NameColor.BLUE -> getBlueTitle(data.fame)
            NameColor.RED -> getRedTitle(data.pkCount)
            NameColor.GRAY -> getGrayTitle(data.fame)
        }
    }

    private fun getBlueTitle(fame: Int): String? {
        // 青プレイヤーの称号はFameのみで決定
        return when {
            fame >= 750 -> "Glorious Lord"
            fame >= 500 -> "Great Lord"
            fame >= 250 -> "Lord"
            fame >= 100 -> "Notable"
            else -> null
        }
    }

    private fun getRedTitle(pkCount: Int): String {
        // 赤プレイヤーはPKCountで称号が決まる
        return when {
            pkCount >= 200 -> "Dread Lord"    // 殺戮者
            pkCount >= 100 -> "Dark Lord"     // 殺人鬼
            pkCount >= 50 -> "Infamous"       // 悪鬼
            pkCount >= 30 -> "Notorious"      // 凶漢
            pkCount >= 10 -> "Wicked"         // 外道
            else -> "Outcast"                 // 罪人
        }
    }

    private fun getGrayTitle(fame: Int): String? {
        // 灰色はFameのみで称号が決まる（Karmaは使わない）
        return when {
            fame >= 750 -> "Renegade"      // 反逆者
            fame >= 500 -> "Outlaw"        // 無法者
            fame >= 250 -> "Rogue"         // ならず者
            fame >= 100 -> "Scoundrel"     // 悪党
            else -> null
        }
    }

    fun getLocalizedTitle(data: PlayerData, locale: String): String? {
        val title = getTitle(data) ?: return null
        return if (locale == "ja") {
            getJapaneseTitle(title)
        } else {
            title
        }
    }

    private fun getJapaneseTitle(title: String): String {
        return when (title) {
            // 青
            "Trustworthy" -> "義人"
            "Notable" -> "功士"
            "Famous" -> "豪傑"
            "Lord" -> "聖騎士"
            "Great Lord" -> "聖将"
            "Glorious Lord" -> "勇者"
            // 赤
            "Outcast" -> "罪人"
            "Notorious" -> "凶漢"
            "Infamous" -> "悪鬼"
            "Wicked" -> "外道"
            "Dark Lord" -> "殺人鬼"
            "Dread Lord" -> "殺戮者"
            // 灰
            "Scoundrel" -> "悪党"
            "Rogue" -> "ならず者"
            "Outlaw" -> "無法者"
            "Renegade" -> "反逆者"
            else -> title
        }
    }
}
