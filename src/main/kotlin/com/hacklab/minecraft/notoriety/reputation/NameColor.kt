package com.hacklab.minecraft.notoriety.reputation

import net.kyori.adventure.text.format.NamedTextColor

enum class NameColor(val chatColor: NamedTextColor, val prefixColor: NamedTextColor) {
    BLUE(NamedTextColor.BLUE, NamedTextColor.AQUA),
    GRAY(NamedTextColor.GRAY, NamedTextColor.GRAY),
    RED(NamedTextColor.RED, NamedTextColor.DARK_RED)
}
