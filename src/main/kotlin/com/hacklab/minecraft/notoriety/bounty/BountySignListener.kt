package com.hacklab.minecraft.notoriety.bounty

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent

class BountySignListener(
    private val signManager: BountySignManager
) : Listener {

    companion object {
        const val PERMISSION = "notoriety.bounty.sign"
        const val BOUNTY_TAG = "[bounty]"
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSignChange(event: SignChangeEvent) {
        val line0 = event.line(0)?.let {
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it)
        } ?: return

        if (!line0.equals(BOUNTY_TAG, ignoreCase = true)) return

        val player = event.player

        // 権限チェック
        if (!player.hasPermission(PERMISSION)) {
            event.isCancelled = true
            player.sendMessage(
                Component.text("懸賞金看板を設置する権限がありません").color(NamedTextColor.RED)
            )
            return
        }

        // ランク番号を取得
        val line1 = event.line(1)?.let {
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it)
        } ?: ""

        val rank = line1.trim().toIntOrNull()
        if (rank == null || rank < 1) {
            event.isCancelled = true
            player.sendMessage(
                Component.text("2行目にランキング番号（1以上の数字）を入力してください").color(NamedTextColor.RED)
            )
            return
        }

        // 看板を登録
        signManager.registerSign(event.block.location, rank)
        player.sendMessage(
            Component.text("懸賞金看板（ランク$rank）を設置しました").color(NamedTextColor.GREEN)
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block

        // 看板かどうかチェック
        if (block.state !is Sign) return

        // 登録済み看板かチェック
        if (!signManager.isRegisteredSign(block.location)) return

        val player = event.player

        // OPは破壊可能
        if (player.hasPermission(PERMISSION)) {
            signManager.unregisterSign(block.location)
            player.sendMessage(
                Component.text("懸賞金看板を削除しました").color(NamedTextColor.YELLOW)
            )
            return
        }

        // 一般プレイヤーは破壊不可
        event.isCancelled = true
        player.sendMessage(
            Component.text("この看板は破壊できません").color(NamedTextColor.RED)
        )
    }
}
