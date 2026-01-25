package com.hacklab.minecraft.notoriety.hazard

import java.time.Instant
import java.util.UUID

/**
 * TNT点火者情報
 * TNTエンティティと点火者の紐付けを管理
 */
data class TntIgniterInfo(
    /** TNTエンティティのUUID */
    val tntEntityUuid: UUID,

    /** 点火したプレイヤーのUUID */
    val igniterUuid: UUID,

    /** TNT位置（ワールド名） */
    val worldName: String,

    /** TNT位置（X座標） */
    val x: Int,

    /** TNT位置（Y座標） */
    val y: Int,

    /** TNT位置（Z座標） */
    val z: Int,

    /** 点火時刻 */
    val ignitedAt: Instant = Instant.now(),

    /** 爆発時刻（爆発後に設定、爆発前はnull） */
    val explodedAt: Instant? = null
)
