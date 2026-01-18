package com.hacklab.minecraft.notoriety.territory.repository

import com.hacklab.minecraft.notoriety.core.database.DatabaseManager
import com.hacklab.minecraft.notoriety.territory.model.TerritorySigil
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

/**
 * シギルデータのDB操作リポジトリ
 */
class SigilRepository(private val databaseManager: DatabaseManager) {

    private val provider get() = databaseManager.provider

    /**
     * シギルを作成
     * @param sigil シギルデータ
     * @return 作成されたシギルID
     */
    fun create(sigil: TerritorySigil): Long {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO territory_sigils
                (territory_id, name, world_name, x, y, z)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(), Statement.RETURN_GENERATED_KEYS)
            stmt.setLong(1, sigil.territoryId)
            stmt.setString(2, sigil.name)
            stmt.setString(3, sigil.worldName)
            stmt.setDouble(4, sigil.x)
            stmt.setDouble(5, sigil.y)
            stmt.setDouble(6, sigil.z)
            stmt.executeUpdate()

            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getLong(1)
                else throw IllegalStateException("Failed to create sigil")
            }
        }
    }

    /**
     * シギルを削除
     * @param sigilId シギルID
     */
    fun delete(sigilId: Long) {
        provider.useConnection { conn ->
            conn.prepareStatement("DELETE FROM territory_sigils WHERE id = ?").use {
                it.setLong(1, sigilId)
                it.executeUpdate()
            }
        }
    }

    /**
     * 領地に属する全シギルを削除
     * @param territoryId 領地ID
     */
    fun deleteByTerritory(territoryId: Long) {
        provider.useConnection { conn ->
            conn.prepareStatement("DELETE FROM territory_sigils WHERE territory_id = ?").use {
                it.setLong(1, territoryId)
                it.executeUpdate()
            }
        }
    }

    /**
     * IDからシギルを取得
     * @param sigilId シギルID
     * @return シギル、なければnull
     */
    fun getById(sigilId: Long): TerritorySigil? {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM territory_sigils WHERE id = ?"
            )
            stmt.setLong(1, sigilId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapSigil(rs) else null
            }
        }
    }

    /**
     * 領地に属する全シギルを取得
     * @param territoryId 領地ID
     * @return シギルのリスト
     */
    fun getByTerritory(territoryId: Long): List<TerritorySigil> {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM territory_sigils WHERE territory_id = ? ORDER BY created_at"
            )
            stmt.setLong(1, territoryId)
            stmt.executeQuery().use { rs ->
                val sigils = mutableListOf<TerritorySigil>()
                while (rs.next()) {
                    sigils.add(mapSigil(rs))
                }
                sigils
            }
        }
    }

    /**
     * 領地内でシギル名が存在するか確認
     * @param territoryId 領地ID
     * @param name シギル名
     * @return 存在する場合true
     */
    fun existsByName(territoryId: Long, name: String): Boolean {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM territory_sigils WHERE territory_id = ? AND name = ?"
            )
            stmt.setLong(1, territoryId)
            stmt.setString(2, name)
            stmt.executeQuery().use { rs ->
                rs.next() && rs.getInt(1) > 0
            }
        }
    }

    /**
     * シギル名を更新
     * @param sigilId シギルID
     * @param newName 新しい名前
     */
    fun updateName(sigilId: Long, newName: String) {
        provider.useConnection { conn ->
            conn.prepareStatement(
                "UPDATE territory_sigils SET name = ? WHERE id = ?"
            ).use {
                it.setString(1, newName)
                it.setLong(2, sigilId)
                it.executeUpdate()
            }
        }
    }

    /**
     * 領地内のシギル数を取得
     * @param territoryId 領地ID
     * @return シギル数
     */
    fun countByTerritory(territoryId: Long): Int {
        return provider.useConnection { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM territory_sigils WHERE territory_id = ?"
            )
            stmt.setLong(1, territoryId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    /**
     * ResultSetからTerritorySigilにマッピング
     */
    private fun mapSigil(rs: ResultSet): TerritorySigil {
        return TerritorySigil(
            id = rs.getLong("id"),
            territoryId = rs.getLong("territory_id"),
            name = rs.getString("name"),
            worldName = rs.getString("world_name"),
            x = rs.getDouble("x"),
            y = rs.getDouble("y"),
            z = rs.getDouble("z"),
            createdAt = parseTimestamp(rs.getString("created_at"))
        )
    }

    /**
     * タイムスタンプ文字列をパース
     */
    private fun parseTimestamp(str: String?): Instant {
        if (str == null) return Instant.now()
        return try {
            if (str.contains("T")) {
                Instant.parse(str)
            } else {
                // "yyyy-MM-dd HH:mm:ss" 形式の場合
                Instant.parse(str.replace(" ", "T") + "Z")
            }
        } catch (e: Exception) {
            Instant.now()
        }
    }
}
