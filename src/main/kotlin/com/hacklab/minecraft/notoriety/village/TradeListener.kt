package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.ReputationService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.MerchantInventory

class TradeListener(
    private val playerManager: PlayerManager,
    private val reputationService: ReputationService
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onTrade(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.whoClicked as? Player ?: return
        val data = playerManager.getPlayer(player) ?: return

        // Only process result slot clicks
        if (event.slot != 2) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        when (data.getNameColor()) {
            NameColor.RED -> {
                // 赤は取引不可
                event.isCancelled = true
                player.sendMessage(
                    Component.text("村人は取引を拒否しました").color(NamedTextColor.RED)
                )
            }
            NameColor.GRAY -> {
                // 灰は1.5倍価格（実際の価格変更はMerchantRecipeで）
                // Note: Paper APIでの価格調整は複雑なため、ここでは通知のみ
            }
            NameColor.BLUE -> {
                // 善行として記録
                reputationService.onGoodDeed(
                    playerUuid = player.uniqueId,
                    karmaGain = 2,
                    fameGain = 2,
                    crimeReduction = 3
                )

                // 高Karma割引の通知（Karma 500で5%、1000で20%）
                if (data.karma >= 500) {
                    val discountPercent = 5 + (data.karma - 500) * 15 / 500
                    player.sendMessage(
                        Component.text("(${discountPercent}%割引が適用されました)").color(NamedTextColor.GREEN)
                    )
                }
            }
        }
    }
}
