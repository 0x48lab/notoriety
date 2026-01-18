package com.hacklab.minecraft.notoriety.territory.listener

import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.guild.service.GuildService
import com.hacklab.minecraft.notoriety.territory.event.TerritoryEnterEvent
import com.hacklab.minecraft.notoriety.territory.event.TerritoryLeaveEvent
import com.hacklab.minecraft.notoriety.territory.service.TerritoryService
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 領地出入りリスナー
 * プレイヤーが領地に出入りした時に通知を行う
 */
class TerritoryEntryListener(
    private val territoryService: TerritoryService,
    private val guildService: GuildService,
    private val i18n: I18nManager
) : Listener {

    // プレイヤーが現在いる領地のギルドID（キャッシュ）
    private val playerTerritoryCache = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // ブロック座標が変わった場合のみ処理
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val player = event.player
        val playerUuid = player.uniqueId

        // 現在の領地と以前の領地を比較
        val currentTerritory = territoryService.getTerritoryAt(to)
        val previousGuildId = playerTerritoryCache[playerUuid]

        val currentGuildId = currentTerritory?.guildId

        // 領地状態が変わっていない場合はスキップ
        if (currentGuildId == previousGuildId) {
            return
        }

        // 領地から出た
        if (previousGuildId != null && currentGuildId != previousGuildId) {
            val previousTerritory = territoryService.getTerritory(previousGuildId)
            val previousGuild = guildService.getGuild(previousGuildId)

            if (previousTerritory != null && previousGuild != null) {
                // イベント発火
                Bukkit.getPluginManager().callEvent(
                    TerritoryLeaveEvent(player, previousTerritory, previousGuild.name)
                )

                // 通知
                i18n.sendInfo(player, "territory.leave_territory",
                    "◀ Leaving %s's territory", previousGuild.name)
            }
        }

        // 領地に入った
        if (currentGuildId != null && currentGuildId != previousGuildId) {
            val currentGuild = guildService.getGuild(currentGuildId)

            if (currentTerritory != null && currentGuild != null) {
                // イベント発火
                Bukkit.getPluginManager().callEvent(
                    TerritoryEnterEvent(player, currentTerritory, currentGuild.name)
                )

                // 自分のギルドかどうかでメッセージを変える
                val playerGuild = guildService.getPlayerGuild(playerUuid)
                val isOwnTerritory = playerGuild?.id == currentGuildId

                if (isOwnTerritory) {
                    i18n.sendSuccess(player, "territory.enter_own_territory",
                        "▶ Entering your guild's territory")
                } else {
                    i18n.sendSuccess(player, "territory.enter_territory",
                        "▶ Entering %s's territory", currentGuild.name)
                }
            }
        }

        // キャッシュ更新
        if (currentGuildId != null) {
            playerTerritoryCache[playerUuid] = currentGuildId
        } else {
            playerTerritoryCache.remove(playerUuid)
        }
    }

    /**
     * プレイヤーのキャッシュをクリア（ログアウト時に呼び出し）
     */
    fun clearPlayerCache(playerUuid: UUID) {
        playerTerritoryCache.remove(playerUuid)
    }

    /**
     * ギルドマスターのログイン時に領地状態を通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // ギルドを取得
        val guild = guildService.getPlayerGuild(playerUuid) ?: return

        // ギルドマスターかどうかをチェック
        val membership = guildService.getMembership(playerUuid) ?: return
        if (membership.role.name != "MASTER") return

        // 1秒後に通知（他のメッセージと被らないように）
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().plugins.first { it.name == "Notoriety" }, Runnable {
            if (!player.isOnline) return@Runnable

            val territory = territoryService.getTerritory(guild.id)
            val memberCount = guildService.getMemberCount(guild.id)
            val allowedChunks = territoryService.calculateAllowedChunks(memberCount)

            // 領地状態ヘッダー
            i18n.sendHeader(player, "territory.gm_territory_status", "=== Guild Territory Status ===")

            if (territory == null || territory.chunkCount == 0) {
                // 領地なし
                if (memberCount >= TerritoryService.MIN_MEMBERS_FOR_TERRITORY) {
                    i18n.sendWarning(player, "territory.gm_no_territory_can_claim",
                        "You have no territory. You can claim up to %d chunks! (/guild territory set)",
                        allowedChunks)
                } else {
                    val needed = TerritoryService.MIN_MEMBERS_FOR_TERRITORY - memberCount
                    i18n.sendWarning(player, "territory.gm_no_territory_need_members",
                        "You have no territory. Need %d more members to claim territory.",
                        needed)
                }
            } else {
                // 領地情報
                i18n.send(player, "territory.gm_chunks_info",
                    "Territory: %d / %d chunks",
                    territory.chunkCount, allowedChunks)

                // 追加可能なチャンク
                val canClaim = allowedChunks - territory.chunkCount
                if (canClaim > 0) {
                    i18n.sendSuccess(player, "territory.gm_can_expand",
                        "You can expand by %d more chunks", canClaim)
                } else if (memberCount < TerritoryService.MIN_MEMBERS_FOR_TERRITORY) {
                    // メンバー不足で領地が縮小される可能性
                    i18n.sendError(player, "territory.gm_member_warning",
                        "⚠ Warning: With only %d members, you may lose territory if members leave",
                        memberCount)
                }
            }
        }, 20L)  // 1秒後
    }

    /**
     * プレイヤーログアウト時にキャッシュをクリア
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearPlayerCache(event.player.uniqueId)
    }
}
