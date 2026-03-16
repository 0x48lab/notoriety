package com.hacklab.minecraft.notoriety.chat.model

enum class ChatMode(val prefix: String, val displayKey: String) {
    LOCAL("", "chat.mode.local"),           // ローカルチャット（50ブロック範囲）
    GLOBAL("!", "chat.mode.global"),        // グローバルチャット（サーバー全体）
    GUILD("@", "chat.mode.guild"),          // ギルドチャット（民間ギルド、フォールバック: 政府）
    GOV_GUILD("@@", "chat.mode.gov_guild"); // 政府ギルドチャット

    companion object {
        fun fromPrefix(message: String): ChatMode? {
            return when {
                message.startsWith("!") -> GLOBAL
                message.startsWith("@@") -> GOV_GUILD  // @@を@より先に検出
                message.startsWith("@") -> GUILD
                else -> null
            }
        }

        fun fromString(value: String): ChatMode? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
