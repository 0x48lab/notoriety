package com.hacklab.minecraft.notoriety.territory.model

import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant

/**
 * 領地シギル - 連続領地グループの目印となるビーコン
 * 名前とテレポート先を持つ
 */
data class TerritorySigil(
    val id: Long = 0,
    val territoryId: Long,
    val name: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val createdAt: Instant = Instant.now()
) {
    /** シギルのLocation */
    val location: Location?
        get() = Bukkit.getWorld(worldName)?.let { world ->
            Location(world, x, y, z)
        }

    companion object {
        /** 名前の最大長 */
        const val MAX_NAME_LENGTH = 32

        /** デフォルト名のプレフィックス */
        const val DEFAULT_NAME_PREFIX = "シギル"

        /** デフォルト名を生成 */
        fun generateDefaultName(existingCount: Int): String {
            return "$DEFAULT_NAME_PREFIX${existingCount + 1}"
        }

        /**
         * 名前のバリデーション
         * 32文字以内、英数字・ひらがな・カタカナ・漢字・スペースのみ許可
         */
        fun isValidName(name: String): Boolean {
            if (name.isBlank()) return false
            if (name.length > MAX_NAME_LENGTH) return false
            // 英数字・ひらがな・カタカナ・漢字・スペース・アンダースコア・ハイフンのみ
            return name.matches(
                Regex("^[a-zA-Z0-9\\p{IsHiragana}\\p{IsKatakana}\\p{IsCJKUnifiedIdeographs}\\s_\\-]+$")
            )
        }
    }
}
