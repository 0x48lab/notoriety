package com.hacklab.minecraft.notoriety.territory.beacon

import org.bukkit.Location
import org.bukkit.Material

/**
 * ビーコン管理の実装
 * ビーコン光線を表示するため、3x3の鉄ブロック土台を自動生成
 */
class BeaconManagerImpl : BeaconManager {

    companion object {
        /** ビーコン土台のブロック素材 */
        val BASE_MATERIAL = Material.IRON_BLOCK
    }

    override fun placeBeacon(location: Location) {
        val world = location.world ?: return
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        // 3x3 土台（1層、ビーコンの1ブロック下）
        for (dx in -1..1) {
            for (dz in -1..1) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(BASE_MATERIAL, false)
            }
        }

        // ビーコン設置
        world.getBlockAt(x, y, z).setType(Material.BEACON, false)
    }

    override fun removeBeacon(location: Location) {
        val world = location.world ?: return
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        // ビーコン削除
        val beaconBlock = world.getBlockAt(x, y, z)
        if (beaconBlock.type == Material.BEACON) {
            beaconBlock.setType(Material.AIR, false)
        }

        // 土台削除（鉄ブロックの場合のみ）
        for (dx in -1..1) {
            for (dz in -1..1) {
                val baseBlock = world.getBlockAt(x + dx, y - 1, z + dz)
                if (baseBlock.type == BASE_MATERIAL) {
                    baseBlock.setType(Material.AIR, false)
                }
            }
        }
    }

    override fun isBeaconBase(location: Location): Boolean {
        val world = location.world ?: return false
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        // 上のブロックがビーコンかチェック
        val blockAbove = world.getBlockAt(x, y + 1, z)
        if (blockAbove.type == Material.BEACON) {
            return true
        }

        // 周囲をチェック（斜めも含む3x3範囲）
        for (dx in -1..1) {
            for (dz in -1..1) {
                val potentialBeacon = world.getBlockAt(x + dx, y + 1, z + dz)
                if (potentialBeacon.type == Material.BEACON) {
                    // このビーコンの土台範囲内か確認
                    val beaconX = potentialBeacon.x
                    val beaconZ = potentialBeacon.z
                    if (x in (beaconX - 1)..(beaconX + 1) && z in (beaconZ - 1)..(beaconZ + 1)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    override fun isBeaconBlock(location: Location): Boolean {
        val world = location.world ?: return false
        return world.getBlockAt(location).type == Material.BEACON
    }
}
