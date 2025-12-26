package com.hacklab.minecraft.notoriety.village

import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import com.hacklab.minecraft.notoriety.reputation.NameColor
import com.hacklab.minecraft.notoriety.reputation.ReputationService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.MerchantInventory
import org.bukkit.inventory.MerchantRecipe
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TradeListener(
    private val playerManager: PlayerManager,
    private val reputationService: ReputationService
) : Listener {

    // 元のspecialPriceを保存（プレイヤーUUID -> レシピインデックス -> 元のspecialPrice）
    private val originalPrices = ConcurrentHashMap<UUID, Map<Int, Int>>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.player as? Player ?: return
        val data = playerManager.getPlayer(player) ?: return

        val merchantInventory = event.inventory as MerchantInventory
        val merchant = merchantInventory.merchant

        // 赤は取引画面を開けない
        if (data.getNameColor() == NameColor.RED) {
            event.isCancelled = true
            player.sendMessage(
                Component.text("村人は取引を拒否しました").color(NamedTextColor.RED)
            )
            return
        }

        // 元の価格を保存
        val savedPrices = mutableMapOf<Int, Int>()
        val recipes = merchant.recipes.toMutableList()

        for ((index, recipe) in recipes.withIndex()) {
            savedPrices[index] = recipe.specialPrice

            val baseAmount = recipe.ingredients.firstOrNull()?.amount ?: continue
            val newSpecialPrice: Int

            when (data.getNameColor()) {
                NameColor.GRAY -> {
                    // 灰は1.5倍価格（+50%）
                    val extraCost = (baseAmount * 0.5).toInt().coerceAtLeast(1)
                    newSpecialPrice = recipe.specialPrice + extraCost
                }
                NameColor.BLUE -> {
                    // 青は高Alignmentで割引
                    if (data.alignment >= 500) {
                        val discountPercent = 5 + (data.alignment - 500) * 15 / 500
                        val discount = (baseAmount * discountPercent / 100).coerceAtLeast(1)
                        newSpecialPrice = recipe.specialPrice - discount
                    } else {
                        newSpecialPrice = recipe.specialPrice
                    }
                }
                else -> {
                    newSpecialPrice = recipe.specialPrice
                }
            }

            // 新しいレシピを作成（specialPriceを変更）
            val newRecipe = MerchantRecipe(
                recipe.result,
                recipe.uses,
                recipe.maxUses,
                recipe.hasExperienceReward(),
                recipe.villagerExperience,
                recipe.priceMultiplier,
                recipe.demand,
                newSpecialPrice
            )
            newRecipe.ingredients = recipe.ingredients
            recipes[index] = newRecipe
        }

        originalPrices[player.uniqueId] = savedPrices
        merchant.recipes = recipes

        // 価格変更メッセージ
        when (data.getNameColor()) {
            NameColor.GRAY -> {
                player.sendMessage(
                    Component.text("(灰色プレイヤー: 取引価格が1.5倍になっています)")
                        .color(NamedTextColor.YELLOW)
                )
            }
            NameColor.BLUE -> {
                if (data.alignment >= 500) {
                    val discountPercent = 5 + (data.alignment - 500) * 15 / 500
                    player.sendMessage(
                        Component.text("(高名声による${discountPercent}%割引が適用されています)")
                            .color(NamedTextColor.GREEN)
                    )
                }
            }
            else -> {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.player as? Player ?: return

        // 元の価格を復元
        val savedPrices = originalPrices.remove(player.uniqueId) ?: return

        val merchantInventory = event.inventory as MerchantInventory
        val merchant = merchantInventory.merchant
        val recipes = merchant.recipes.toMutableList()

        for ((index, originalPrice) in savedPrices) {
            if (index >= recipes.size) continue
            val recipe = recipes[index]

            val restoredRecipe = MerchantRecipe(
                recipe.result,
                recipe.uses,
                recipe.maxUses,
                recipe.hasExperienceReward(),
                recipe.villagerExperience,
                recipe.priceMultiplier,
                recipe.demand,
                originalPrice
            )
            restoredRecipe.ingredients = recipe.ingredients
            recipes[index] = restoredRecipe
        }

        merchant.recipes = recipes
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onTrade(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.MERCHANT) return
        val player = event.whoClicked as? Player ?: return
        val data = playerManager.getPlayer(player) ?: return

        // Only process result slot clicks
        if (event.slot != 2) return
        if (event.currentItem == null || event.currentItem?.type?.isAir == true) return

        // 赤は取引不可（念のため）
        if (data.getNameColor() == NameColor.RED) {
            event.isCancelled = true
            return
        }

        // 善行として記録（灰も青も回復できる）
        reputationService.onGoodDeed(
            playerUuid = player.uniqueId,
            alignmentGain = 5,
            fameGain = 0
        )
    }
}
