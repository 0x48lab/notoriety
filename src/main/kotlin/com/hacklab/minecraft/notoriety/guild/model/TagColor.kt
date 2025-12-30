package com.hacklab.minecraft.notoriety.guild.model

import net.kyori.adventure.text.format.NamedTextColor

enum class TagColor(val namedTextColor: NamedTextColor) {
    WHITE(NamedTextColor.WHITE),
    RED(NamedTextColor.RED),
    BLUE(NamedTextColor.BLUE),
    GREEN(NamedTextColor.GREEN),
    YELLOW(NamedTextColor.YELLOW),
    GOLD(NamedTextColor.GOLD),
    AQUA(NamedTextColor.AQUA),
    LIGHT_PURPLE(NamedTextColor.LIGHT_PURPLE),
    DARK_RED(NamedTextColor.DARK_RED),
    DARK_BLUE(NamedTextColor.DARK_BLUE),
    DARK_GREEN(NamedTextColor.DARK_GREEN),
    DARK_AQUA(NamedTextColor.DARK_AQUA),
    DARK_PURPLE(NamedTextColor.DARK_PURPLE),
    GRAY(NamedTextColor.GRAY),
    DARK_GRAY(NamedTextColor.DARK_GRAY),
    BLACK(NamedTextColor.BLACK);

    companion object {
        fun fromString(value: String): TagColor? =
            entries.find { it.name.equals(value, ignoreCase = true) }

        fun allNames(): List<String> = entries.map { it.name }
    }
}
