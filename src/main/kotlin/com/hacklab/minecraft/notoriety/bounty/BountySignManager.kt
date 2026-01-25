package com.hacklab.minecraft.notoriety.bounty

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Rotatable
import org.bukkit.block.sign.Side
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.NumberFormat
import java.util.*

data class BountySign(
    val location: Location,
    val rank: Int
)

class BountySignManager(
    private val plugin: JavaPlugin,
    private val bountyService: BountyService
) {
    private val signs = mutableListOf<BountySign>()
    private val file: File = File(plugin.dataFolder, "bounty_signs.yml")
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    init {
        load()
    }

    fun registerSign(location: Location, rank: Int) {
        // Remove existing sign at this location
        signs.removeIf { it.location == location }
        signs.add(BountySign(location, rank))
        save()
        updateSign(location, rank)
    }

    fun unregisterSign(location: Location) {
        signs.removeIf { it.location == location }
        save()
        removePlayerHead(location)
    }

    fun isRegisteredSign(location: Location): Boolean =
        signs.any { it.location == location }

    fun updateAllSigns() {
        signs.forEach { sign ->
            updateSign(sign.location, sign.rank)
        }
    }

    private fun updateSign(location: Location, rank: Int) {
        val block = location.block
        val sign = block.state as? Sign ?: return

        // 赤プレイヤー（アクティブなPK）のみを表示
        val bounty = bountyService.getActiveBountyByRank(rank)

        val frontSide = sign.getSide(Side.FRONT)

        if (bounty != null) {
            val playerName = Bukkit.getOfflinePlayer(bounty.target).name ?: "???"
            frontSide.line(0, Component.text("[$rank]").color(NamedTextColor.GOLD))
            frontSide.line(1, Component.text(playerName).color(NamedTextColor.RED))
            frontSide.line(2, Component.text(numberFormat.format(bounty.total.toLong())).color(NamedTextColor.YELLOW))
            frontSide.line(3, Component.text("WANTED").color(NamedTextColor.DARK_RED))

            // Set player head
            setPlayerHead(location, bounty.target)
        } else {
            frontSide.line(0, Component.text("[$rank]").color(NamedTextColor.GOLD))
            frontSide.line(1, Component.text("---").color(NamedTextColor.GRAY))
            frontSide.line(2, Component.text("---").color(NamedTextColor.GRAY))
            frontSide.line(3, Component.text("WANTED").color(NamedTextColor.DARK_RED))

            removePlayerHead(location)
        }

        sign.update()
    }

    private fun setPlayerHead(signLocation: Location, playerUuid: UUID) {
        val headLocation = signLocation.clone().add(0.0, 1.0, 0.0)
        val block = headLocation.block
        val signBlock = signLocation.block

        // 看板の向きを取得
        val signRotation = getSignRotation(signBlock)

        if (block.type != Material.PLAYER_HEAD && block.type != Material.PLAYER_WALL_HEAD) {
            block.type = Material.PLAYER_HEAD
        }

        val skull = block.state as? org.bukkit.block.Skull ?: return
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid))

        // ヘッドの向きを看板に合わせる
        val skullData = skull.blockData
        if (skullData is Rotatable && signRotation != null) {
            skullData.rotation = signRotation
            skull.blockData = skullData
        }

        skull.update()
    }

    /**
     * 看板の向きを取得（16方向のBlockFaceで返す）
     * プレイヤーヘッドが看板の正面と同じ方向を向くように調整
     */
    private fun getSignRotation(signBlock: org.bukkit.block.Block): BlockFace? {
        val blockData = signBlock.blockData

        return when (blockData) {
            // 立て看板: 直接回転を取得
            is Rotatable -> blockData.rotation
            // 壁看板: facingは看板が向いている方向だが、ヘッドの回転は逆方向で解釈されるため反転
            is Directional -> blockData.facing.oppositeFace
            else -> null
        }
    }

    private fun removePlayerHead(signLocation: Location) {
        val headLocation = signLocation.clone().add(0.0, 1.0, 0.0)
        val block = headLocation.block
        if (block.type == Material.PLAYER_HEAD || block.type == Material.PLAYER_WALL_HEAD) {
            block.type = Material.AIR
        }
    }

    private fun load() {
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)
        val signsList = config.getMapList("signs")

        for (signData in signsList) {
            try {
                val world = Bukkit.getWorld(signData["world"] as String) ?: continue
                val x = (signData["x"] as Number).toInt()
                val y = (signData["y"] as Number).toInt()
                val z = (signData["z"] as Number).toInt()
                val rank = (signData["rank"] as Number).toInt()

                signs.add(BountySign(Location(world, x.toDouble(), y.toDouble(), z.toDouble()), rank))
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load bounty sign: ${e.message}")
            }
        }
        plugin.logger.info("Loaded ${signs.size} bounty signs")
    }

    private fun save() {
        val config = YamlConfiguration()
        val signsList = signs.map { sign ->
            mapOf(
                "world" to sign.location.world.name,
                "x" to sign.location.blockX,
                "y" to sign.location.blockY,
                "z" to sign.location.blockZ,
                "rank" to sign.rank
            )
        }
        config.set("signs", signsList)
        config.save(file)
    }
}
