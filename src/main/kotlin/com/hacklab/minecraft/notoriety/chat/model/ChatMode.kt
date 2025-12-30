package com.hacklab.minecraft.notoriety.chat.model

enum class ChatMode(val prefix: String, val displayKey: String) {
    LOCAL("", "chat.mode.local"),       // ローカルチャット（50ブロック範囲）
    GLOBAL("!", "chat.mode.global"),    // グローバルチャット（サーバー全体）
    GUILD("@", "chat.mode.guild");      // ギルドチャット（ギルドメンバーのみ）

    companion object {
        fun fromPrefix(message: String): ChatMode? {
            return when {
                message.startsWith("!") -> GLOBAL
                message.startsWith("@") -> GUILD
                else -> null
            }
        }

        fun fromString(value: String): ChatMode? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
