package com.hacklab.minecraft.notoriety.chat

/**
 * ローマ字→ひらがな変換ユーティリティ
 * 入力: "konnichiha"
 * 出力: "こんにちは（konnichiha）"
 */
object RomajiConverter {

    // ローマ字→ひらがな変換テーブル（長い順にマッチさせるため順序重要）
    private val conversionTable = listOf(
        // 拗音（3文字）
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
        "sya" to "しゃ", "syu" to "しゅ", "syo" to "しょ",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
        "tya" to "ちゃ", "tyu" to "ちゅ", "tyo" to "ちょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "jya" to "じゃ", "jyu" to "じゅ", "jyo" to "じょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "tsu" to "つ", "chi" to "ち", "shi" to "し",
        "dzu" to "づ", "dzi" to "ぢ",

        // 2文字
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "ti" to "ち", "tu" to "つ", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "fu" to "ふ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wi" to "ゐ", "we" to "ゑ", "wo" to "を",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "ji" to "じ", "ju" to "じゅ", "ja" to "じゃ", "jo" to "じょ",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ",
        "va" to "ゔぁ", "vi" to "ゔぃ", "vu" to "ゔ", "ve" to "ゔぇ", "vo" to "ゔぉ",
        "la" to "ぁ", "li" to "ぃ", "lu" to "ぅ", "le" to "ぇ", "lo" to "ぉ",
        "xa" to "ぁ", "xi" to "ぃ", "xu" to "ぅ", "xe" to "ぇ", "xo" to "ぉ",
        "nn" to "ん",

        // 1文字（母音）
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        "n" to "ん"
    )

    // 促音（っ）になる子音
    private val doubleConsonants = setOf('k', 's', 't', 'p', 'c', 'g', 'z', 'd', 'b', 'j', 'f')

    /**
     * ローマ字をひらがなに変換する
     * @param input ローマ字文字列
     * @return 変換されたひらがな（変換できない部分はそのまま）
     */
    fun toHiragana(input: String): String {
        val lower = input.lowercase()
        val result = StringBuilder()
        var i = 0

        while (i < lower.length) {
            val remaining = lower.substring(i)

            // 促音のチェック（同じ子音が連続）
            if (i + 1 < lower.length &&
                lower[i] == lower[i + 1] &&
                lower[i] in doubleConsonants) {
                result.append("っ")
                i++
                continue
            }

            // "n" の特別処理（次が母音やy以外なら「ん」）
            if (remaining.startsWith("n") && remaining.length > 1) {
                val nextChar = remaining[1]
                if (nextChar !in "aiueoy" && nextChar != 'n') {
                    result.append("ん")
                    i++
                    continue
                }
            }

            // 変換テーブルから最長一致で探す
            var matched = false
            for ((romaji, hiragana) in conversionTable) {
                if (remaining.startsWith(romaji)) {
                    result.append(hiragana)
                    i += romaji.length
                    matched = true
                    break
                }
            }

            // 一致しなかった場合はそのまま追加
            if (!matched) {
                result.append(input[i])  // 元の大文字小文字を保持
                i++
            }
        }

        return result.toString()
    }

    /**
     * メッセージにローマ字が含まれていれば変換する
     * ローマ字部分のみ変換し、日本語や記号はそのまま
     * @return 変換後のひらがな（変換があった場合のみ元のローマ字も付加）
     */
    fun convert(message: String): String {
        // 既にひらがな/カタカナ/漢字が含まれている場合は変換不要
        if (containsJapanese(message)) {
            return message
        }

        // ASCII文字のみの場合、ローマ字として変換
        val converted = toHiragana(message)

        // 変換されていない（全く同じ）場合はそのまま返す
        if (converted == message.lowercase()) {
            return message
        }

        // 変換結果に元のローマ字を付加
        return "$converted（$message）"
    }

    /**
     * 日本語文字（ひらがな、カタカナ、漢字）が含まれているかチェック
     */
    private fun containsJapanese(text: String): Boolean {
        return text.any { char ->
            val codePoint = char.code
            // ひらがな: U+3040-U+309F
            // カタカナ: U+30A0-U+30FF
            // CJK統合漢字: U+4E00-U+9FFF
            (codePoint in 0x3040..0x309F) ||
            (codePoint in 0x30A0..0x30FF) ||
            (codePoint in 0x4E00..0x9FFF)
        }
    }
}
