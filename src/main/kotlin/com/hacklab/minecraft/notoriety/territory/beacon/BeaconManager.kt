package com.hacklab.minecraft.notoriety.territory.beacon

import org.bukkit.Location

/**
 * ビーコン管理インターフェース
 * 領地のシンボルとしてのビーコンブロック設置/削除を管理
 */
interface BeaconManager {
    /**
     * ビーコンを設置（3x3鉄ブロック土台含む）
     * @param location ビーコンの設置位置
     */
    fun placeBeacon(location: Location)

    /**
     * ビーコンを削除（土台含む）
     * @param location ビーコンの位置
     */
    fun removeBeacon(location: Location)

    /**
     * 指定位置がビーコンの土台かどうか
     * @param location チェックする位置
     * @return ビーコン土台ならtrue
     */
    fun isBeaconBase(location: Location): Boolean

    /**
     * 指定位置がビーコン本体かどうか
     * @param location チェックする位置
     * @return ビーコン本体ならtrue
     */
    fun isBeaconBlock(location: Location): Boolean
}
