package com.hacklab.minecraft.notoriety.crime

enum class CrimeType(val displayKey: String, val defaultPenalty: Int) {
    THEFT("crime.theft", 50),
    DESTROY("crime.destroy", 5),
    ATTACK("crime.attack", 1),
    PK("crime.pk", 50),  // PKCountも増加
    KILL_VILLAGER("crime.kill_villager", 50),
    KILL_ANIMAL("crime.kill_animal", 10),
    HARVEST_CROP("crime.harvest_crop", 1),
    DESTROY_VILLAGER_BED("crime.destroy_villager_bed", 5),
    DESTROY_VILLAGER_WORKSTATION("crime.destroy_villager_workstation", 5),
    ATTACK_VILLAGER("crime.attack_villager", 1),
    KILL_GOLEM("crime.kill_golem", 50),
    INDIRECT_PK("crime.indirect_pk", 50)  // 間接PK（マグマ、TNT、落下等）
}
