package com.hacklab.minecraft.notoriety.crime

enum class CrimeType(val displayKey: String, val defaultPenalty: Int) {
    THEFT("crime.theft", 100),
    DESTROY("crime.destroy", 50),
    ATTACK("crime.attack", 150),
    PK("crime.pk", 0),  // PKCountで管理
    KILL_VILLAGER("crime.kill_villager", 200),
    KILL_ANIMAL("crime.kill_animal", 20),
    HARVEST_CROP("crime.harvest_crop", 10),
    DESTROY_VILLAGER_BED("crime.destroy_villager_bed", 5),
    DESTROY_VILLAGER_WORKSTATION("crime.destroy_villager_workstation", 10),
    ATTACK_VILLAGER("crime.attack_villager", 1),
    KILL_GOLEM("crime.kill_golem", 100)
}
