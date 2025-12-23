package com.hacklab.minecraft.notoriety.bounty

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BountyStorage(private val plugin: JavaPlugin) {
    private val bounties = ConcurrentHashMap<UUID, BountyEntry>()
    private val file: File = File(plugin.dataFolder, "bounties.yml")

    init {
        load()
    }

    fun addBounty(target: UUID, contributor: UUID, amount: Double) {
        val existing = bounties[target]
        if (existing != null) {
            val newContributors = existing.contributors.toMutableMap()
            newContributors[contributor] = (newContributors[contributor] ?: 0.0) + amount
            bounties[target] = BountyEntry(
                target = target,
                total = existing.total + amount,
                contributors = newContributors
            )
        } else {
            bounties[target] = BountyEntry(
                target = target,
                total = amount,
                contributors = mapOf(contributor to amount)
            )
        }
        save()
    }

    fun getBounty(target: UUID): BountyEntry? = bounties[target]

    fun removeBounty(target: UUID) {
        bounties.remove(target)
        save()
    }

    fun getAllBounties(): List<BountyEntry> = bounties.values.toList()

    fun getRankedBounties(): List<BountyEntry> =
        bounties.values.sortedByDescending { it.total }

    fun getBountyByRank(rank: Int): BountyEntry? {
        val ranked = getRankedBounties()
        return if (rank > 0 && rank <= ranked.size) ranked[rank - 1] else null
    }

    private fun load() {
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            try {
                val targetUuid = UUID.fromString(key)
                val section = config.getConfigurationSection(key) ?: continue
                val total = section.getDouble("total")

                val contributorsSection = section.getConfigurationSection("contributors")
                val contributors = mutableMapOf<UUID, Double>()
                contributorsSection?.getKeys(false)?.forEach { contribKey ->
                    contributors[UUID.fromString(contribKey)] = contributorsSection.getDouble(contribKey)
                }

                bounties[targetUuid] = BountyEntry(targetUuid, total, contributors)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load bounty for $key: ${e.message}")
            }
        }
        plugin.logger.info("Loaded ${bounties.size} bounties")
    }

    private fun save() {
        val config = YamlConfiguration()
        for ((uuid, entry) in bounties) {
            config.set("$uuid.total", entry.total)
            for ((contributor, amount) in entry.contributors) {
                config.set("$uuid.contributors.$contributor", amount)
            }
        }
        config.save(file)
    }
}
