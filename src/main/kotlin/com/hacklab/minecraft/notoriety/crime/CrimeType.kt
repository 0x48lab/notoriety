package com.hacklab.minecraft.notoriety.crime

enum class CrimeType(val displayKey: String, val defaultPoint: Int) {
    THEFT("crime.theft", 100),
    DESTROY("crime.destroy", 50),
    ATTACK("crime.attack", 150),
    PK("crime.pk", 0),  // PKCountで管理
    KILL_VILLAGER("crime.kill_villager", 200),
    KILL_ANIMAL("crime.kill_animal", 20),
    HARVEST_CROP("crime.harvest_crop", 10)
}
