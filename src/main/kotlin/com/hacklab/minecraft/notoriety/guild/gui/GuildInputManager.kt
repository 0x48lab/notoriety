package com.hacklab.minecraft.notoriety.guild.gui

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ギルド設定のチャット入力を管理するクラス
 */
class GuildInputManager(private val plugin: JavaPlugin) : Listener {

    /**
     * 入力タイプ
     */
    enum class InputType {
        NAME,
        TAG,
        DESCRIPTION
    }

    /**
     * 入力待ち状態
     */
    data class PendingInput(
        val type: InputType,
        val guildId: Long,
        val callback: (String) -> Unit,
        val onCancel: () -> Unit
    )

    private val pendingInputs = ConcurrentHashMap<UUID, PendingInput>()

    /**
     * 入力待ちを開始する
     */
    fun startInput(
        player: Player,
        type: InputType,
        guildId: Long,
        callback: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        pendingInputs[player.uniqueId] = PendingInput(type, guildId, callback, onCancel)

        val typeText = when (type) {
            InputType.NAME -> "ギルド名"
            InputType.TAG -> "ギルドタグ（2-4文字の英数字）"
            InputType.DESCRIPTION -> "ギルド説明"
        }

        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("新しい$typeText を入力してください").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("キャンセルするには「cancel」と入力").color(NamedTextColor.GRAY))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD))
    }

    /**
     * 入力待ちをキャンセルする
     */
    fun cancelInput(playerUuid: UUID) {
        val pending = pendingInputs.remove(playerUuid)
        pending?.onCancel?.invoke()
    }

    /**
     * 入力待ち中かどうか
     */
    fun hasPendingInput(playerUuid: UUID): Boolean {
        return pendingInputs.containsKey(playerUuid)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val pending = pendingInputs.remove(player.uniqueId) ?: return

        // チャットイベントをキャンセル
        event.isCancelled = true

        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        // キャンセル処理
        if (message.equals("cancel", ignoreCase = true)) {
            player.sendMessage(Component.text("入力をキャンセルしました").color(NamedTextColor.YELLOW))
            // メインスレッドでコールバックを実行
            Bukkit.getScheduler().runTask(plugin, Runnable {
                pending.onCancel()
            })
            return
        }

        // メインスレッドでコールバックを実行（GUI操作などが含まれる可能性があるため）
        Bukkit.getScheduler().runTask(plugin, Runnable {
            pending.callback(message)
        })
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // ログアウト時に入力待ちをクリア
        pendingInputs.remove(event.player.uniqueId)
    }
}
