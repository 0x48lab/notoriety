package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

/**
 * シギルサービスインターフェース
 * シギルの作成・削除・名前変更・テレポートを管理
 */
interface SigilService {

    // === シギル作成・削除 ===

    /**
     * 新しいシギルを作成する
     * @param territoryId 領地ID
     * @param location シギルの位置
     * @param name シギル名（nullの場合は自動生成）
     * @return 作成されたシギル
     */
    fun createSigil(territoryId: Long, location: Location, name: String? = null): TerritorySigil

    /**
     * シギルを削除する
     * @param sigilId シギルID
     * @param guildId ギルドID（キャッシュ更新用）
     */
    fun deleteSigil(sigilId: Long, guildId: Long)

    // === シギル取得 ===

    /**
     * シギルIDからシギルを取得
     */
    fun getSigil(sigilId: Long): TerritorySigil?

    /**
     * 領地内の全シギルを取得
     */
    fun getSigilsForTerritory(territoryId: Long): List<TerritorySigil>

    /**
     * シギル名から検索（領地内）
     */
    fun findSigilByName(territoryId: Long, name: String): TerritorySigil?

    // === シギル名前変更 ===

    /**
     * シギルの名前を変更する
     * @param sigilId シギルID
     * @param newName 新しい名前
     * @param guildId ギルドID
     * @param requester 実行者UUID
     * @return 名前変更結果
     */
    fun renameSigil(sigilId: Long, newName: String, guildId: Long, requester: UUID): RenameResult

    /**
     * シギル名を検証
     */
    fun isValidName(name: String): Boolean

    /**
     * デフォルトのシギル名を生成
     */
    fun generateDefaultName(existingCount: Int): String

    // === テレポート ===

    /**
     * シギルにテレポートする
     * @param player テレポートするプレイヤー
     * @param sigil 対象シギル
     * @return テレポート結果
     */
    fun teleportToSigil(player: Player, sigil: TerritorySigil): TeleportResult

    /**
     * シギル名でテレポートする
     * @param player テレポートするプレイヤー
     * @param guildId ギルドID
     * @param sigilName シギル名
     * @return テレポート結果
     */
    fun teleportToSigilByName(player: Player, guildId: Long, sigilName: String): TeleportResult

    // === ビーコンブロック管理 ===

    /**
     * ビーコンブロックを設置
     * @param location 設置位置
     */
    fun placeBeaconBlock(location: Location)

    /**
     * ビーコンブロックを削除
     * @param location 削除位置
     */
    fun removeBeaconBlock(location: Location)
}

/**
 * テレポート結果を表すsealed class
 */
sealed class TeleportResult {
    /** テレポート成功 */
    data class Success(val sigil: TerritorySigil) : TeleportResult()

    /** シギルが見つからない */
    data object SigilNotFound : TeleportResult()

    /** シギル名が見つからない */
    data class SigilNameNotFound(val name: String) : TeleportResult()

    /** ギルドに所属していない */
    data object NotInGuild : TeleportResult()

    /** 領地がない */
    data object NoTerritory : TeleportResult()

    /** 安全なテレポート位置が見つからない */
    data object NoSafeLocation : TeleportResult()

    /** クールダウン中 */
    data class OnCooldown(val remainingSeconds: Int) : TeleportResult()

    /** ワールドが見つからない */
    data object WorldNotFound : TeleportResult()
}

/**
 * シギル名前変更結果を表すsealed class
 */
sealed class RenameResult {
    /** 名前変更成功 */
    data class Success(val sigil: TerritorySigil, val oldName: String, val newName: String) : RenameResult()

    /** シギルが見つからない */
    data object SigilNotFound : RenameResult()

    /** ギルドマスターではない */
    data object NotGuildMaster : RenameResult()

    /** ギルドに所属していない */
    data object NotInGuild : RenameResult()

    /** 無効な名前（長さや文字制限違反） */
    data class InvalidName(val reason: String) : RenameResult()

    /** 同じ名前が既に存在する */
    data class NameAlreadyExists(val name: String) : RenameResult()

    /** 領地がない */
    data object NoTerritory : RenameResult()
}
