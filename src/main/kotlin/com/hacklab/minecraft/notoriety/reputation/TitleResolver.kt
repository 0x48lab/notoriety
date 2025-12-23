package com.hacklab.minecraft.notoriety.reputation

import com.hacklab.minecraft.notoriety.core.player.PlayerData

object TitleResolver {
    fun getTitle(data: PlayerData): String? {
        return when (data.getNameColor()) {
            NameColor.BLUE -> getBlueTitle(data.fame, data.karma)
            NameColor.RED -> getRedTitle(data.fame, data.karma)
            NameColor.GRAY -> null
        }
    }

    private fun getBlueTitle(fame: Int, karma: Int): String? {
        return when {
            fame >= 750 && karma >= 1000 -> "Glorious Lord"
            fame >= 750 && karma >= 500 -> "Great Lord"
            fame >= 750 -> "Famous"
            fame >= 250 && karma >= 1000 -> "Great Lord"
            fame >= 250 && karma >= 500 -> "Lord"
            fame >= 250 -> "Notable"
            karma >= 1000 -> "Lord"
            karma >= 500 -> "Trustworthy"
            else -> null
        }
    }

    private fun getRedTitle(fame: Int, karma: Int): String? {
        return when {
            fame >= 750 && karma >= 1000 -> "Dread Lord"
            fame >= 750 && karma >= 500 -> "Dark Lord"
            fame >= 750 -> "Infamous"
            fame >= 250 && karma >= 1000 -> "Dread Lord"
            fame >= 250 && karma >= 500 -> "Dark Lord"
            fame >= 250 -> "Notorious"
            karma >= 1000 -> "Dark Lord"
            karma >= 500 -> "Wicked"
            else -> "Outcast"
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
            "Trustworthy" -> "義人"
            "Notable" -> "功士"
            "Famous" -> "豪傑"
            "Lord" -> "聖騎士"
            "Great Lord" -> "聖将"
            "Glorious Lord" -> "勇者"
            "Outcast" -> "罪人"
            "Notorious" -> "凶漢"
            "Infamous" -> "悪鬼"
            "Wicked" -> "外道"
            "Dark Lord" -> "殺人鬼"
            "Dread Lord" -> "殺戮者"
            else -> title
        }
    }
}
