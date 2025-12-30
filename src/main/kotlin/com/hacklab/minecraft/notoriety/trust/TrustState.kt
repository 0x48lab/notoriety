package com.hacklab.minecraft.notoriety.trust

/**
 * 三段階信頼システムの状態
 * - TRUST: 明示的に信頼（アクセス許可）
 * - DISTRUST: 明示的に不信頼（アクセス拒否、ギルド信頼より優先）
 * - UNSET（レコードなし）: 未設定（ギルド判定に委ねる）
 */
enum class TrustState {
    TRUST,      // 明示的に信頼
    DISTRUST;   // 明示的に不信頼

    companion object {
        fun fromString(value: String): TrustState? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
