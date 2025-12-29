package com.hacklab.minecraft.notoriety.crime

enum class CrimeType(val displayKey: String, val defaultPenalty: Int) {
    THEFT("crime.theft", 50),
    DESTROY("crime.destroy", 10),
    ATTACK("crime.attack", 1),
    PK("crime.pk", 0),  // PKCountで管理
    KILL_VILLAGER("crime.kill_villager", 50),
    KILL_ANIMAL("crime.kill_animal", 1),
    HARVEST_CROP("crime.harvest_crop", 1),
    DESTROY_VILLAGER_BED("crime.destroy_villager_bed", 1),
    DESTROY_VILLAGER_WORKSTATION("crime.destroy_villager_workstation", 1),
    ATTACK_VILLAGER("crime.attack_villager", 1),
    KILL_GOLEM("crime.kill_golem", 50)
}
