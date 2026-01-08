package com.hacklab.minecraft.notoriety.core.player

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerManager(
    private val plugin: JavaPlugin,
    private val repository: PlayerRepository
) {
    private val cache = ConcurrentHashMap<UUID, PlayerData>()

    fun getPlayer(uuid: UUID): PlayerData? = cache[uuid]

    fun getPlayer(player: Player): PlayerData? = cache[player.uniqueId]

    /**
     * キャッシュまたはDBからプレイヤーデータを取得（オフラインプレイヤー対応）
     * キャッシュにあればキャッシュから、なければDBから読み込む（キャッシュには入れない）
     */
    fun getOrLoadPlayer(uuid: UUID): PlayerData? {
        cache[uuid]?.let { return it }
        return repository.load(uuid)
    }

    fun loadPlayer(uuid: UUID): PlayerData {
        val data = repository.load(uuid) ?: PlayerData(uuid)
        cache[uuid] = data
        return data
    }

    fun savePlayer(uuid: UUID) {
        cache[uuid]?.let { repository.save(it) }
    }

    /**
     * プレイヤーデータを直接DBに保存（オフラインプレイヤー対応）
     */
    fun savePlayerData(data: PlayerData) {
        repository.save(data)
    }

    fun unloadPlayer(uuid: UUID) {
        savePlayer(uuid)
        cache.remove(uuid)
    }

    fun saveAll() {
        cache.values.forEach { repository.save(it) }
    }

    fun getOnlinePlayers(): List<PlayerData> =
        plugin.server.onlinePlayers.mapNotNull { cache[it.uniqueId] }

    fun getCachedPlayers(): Collection<PlayerData> = cache.values

    /**
     * DBから全ての灰色プレイヤーを取得（オフライン含む）
     */
    fun findAllGrayPlayers(): List<PlayerData> = repository.findGrayPlayers()

    /**
     * DBから全ての赤プレイヤーを取得（オフライン含む）
     */
    fun findAllRedPlayers(): List<PlayerData> = repository.findRedPlayers()

    /**
     * 指定UUIDがオンラインかどうか
     */
    fun isOnline(uuid: UUID): Boolean = plugin.server.getPlayer(uuid) != null
}
