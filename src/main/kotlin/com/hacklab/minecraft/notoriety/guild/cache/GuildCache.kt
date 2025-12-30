package com.hacklab.minecraft.notoriety.guild.cache

import com.hacklab.minecraft.notoriety.guild.model.Guild
import java.util.concurrent.ConcurrentHashMap

class GuildCache {
    private val byId = ConcurrentHashMap<Long, Guild>()
    private val byName = ConcurrentHashMap<String, Guild>()
    private val byTag = ConcurrentHashMap<String, Guild>()

    fun put(guild: Guild) {
        byId[guild.id] = guild
        byName[guild.name.lowercase()] = guild
        byTag[guild.tag.lowercase()] = guild
    }

    fun get(id: Long): Guild? = byId[id]

    fun getByName(name: String): Guild? = byName[name.lowercase()]

    fun getByTag(tag: String): Guild? = byTag[tag.lowercase()]

    fun remove(id: Long) {
        byId.remove(id)?.let { guild ->
            byName.remove(guild.name.lowercase())
            byTag.remove(guild.tag.lowercase())
        }
    }

    fun removeByName(name: String) {
        byName.remove(name.lowercase())?.let { guild ->
            byId.remove(guild.id)
            byTag.remove(guild.tag.lowercase())
        }
    }

    fun clear() {
        byId.clear()
        byName.clear()
        byTag.clear()
    }

    fun getAll(): Collection<Guild> = byId.values

    fun size(): Int = byId.size
}
