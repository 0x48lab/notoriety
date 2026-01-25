package com.hacklab.minecraft.notoriety.hazard

import java.time.Instant
import java.util.UUID

/**
 * レッドストーントリガー作動記録
 * ボタン/レバー/感圧板の作動を追跡
 */
data class RedstoneActivation(
    /** トリガー位置（ワールド名） */
    val worldName: String,

    /** トリガー位置（X座標） */
    val x: Int,

    /** トリガー位置（Y座標） */
    val y: Int,

    /** トリガー位置（Z座標） */
    val z: Int,

    /** 作動させたプレイヤーのUUID */
    val activatorUuid: UUID,

    /** 作動時刻 */
    val activatedAt: Instant = Instant.now()
)
