package com.hacklab.minecraft.notoriety.territory.service

import com.hacklab.minecraft.notoriety.territory.model.GuildBase
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

interface GuildBaseService {

    /**
     * ギルドの拠点を設定（既存があれば上書き）
     */
    fun setBase(guildId: Long, location: Location, setBy: UUID): GuildBase

    /**
     * ギルドの拠点を取得
     * @return null if no base set
     */
    fun getBase(guildId: Long): GuildBase?

    /**
     * ギルドの拠点を削除
     * @return true if deleted, false if no base existed
     */
    fun removeBase(guildId: Long): Boolean

    /**
     * 拠点にテレポート
     * Uses shared cooldown with sigil TP
     */
    fun teleportToBase(player: Player, guildId: Long): TeleportResult
}
