package com.hacklab.minecraft.notoriety.hazard

/**
 * 危険物の種別
 * 間接PK追跡で使用される危険物の分類
 */
enum class HazardType {
    /** マグマ（バケツ設置） */
    LAVA,

    /** 水流（P3: 将来対応） */
    WATER,

    /** TNT設置 */
    TNT_PLACED,

    /** TNT点火済み */
    TNT_IGNITED,

    /** ピストン（P3: 将来対応） */
    PISTON
}
