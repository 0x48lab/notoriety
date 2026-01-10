package com.hacklab.minecraft.notoriety.achievement.service

import com.hacklab.minecraft.notoriety.achievement.event.AchievementUnlockEvent
import com.hacklab.minecraft.notoriety.achievement.model.Achievement
import com.hacklab.minecraft.notoriety.achievement.model.AchievementCategory
import com.hacklab.minecraft.notoriety.achievement.model.Achievements
import com.hacklab.minecraft.notoriety.achievement.model.PlayerAchievement
import com.hacklab.minecraft.notoriety.achievement.repository.AchievementRepository
import com.hacklab.minecraft.notoriety.core.i18n.I18nManager
import com.hacklab.minecraft.notoriety.core.player.PlayerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * アチーブメントサービス実装
 */
class AchievementServiceImpl(
    private val plugin: JavaPlugin,
    private val repository: AchievementRepository,
    private val playerManager: PlayerManager,
    private val i18nManager: I18nManager
) : AchievementService {

    private val checker = AchievementChecker()

    // 達成済みアチーブメントIDのキャッシュ
    private val unlockedCache = ConcurrentHashMap<UUID, MutableSet<String>>()

    // プレイヤー単位のロック（並行チェック防止）
    private val playerLocks = ConcurrentHashMap<UUID, Any>()

    override fun checkAndUnlock(playerUuid: UUID, context: AchievementContext): List<Achievement> {
        val playerData = playerManager.getPlayer(playerUuid) ?: return emptyList()

        // プレイヤー単位でロックを取得
        val lock = playerLocks.computeIfAbsent(playerUuid) { Any() }

        return synchronized(lock) {
            val allNewUnlocks = mutableListOf<Achievement>()

            // カスケードアンロック: 報酬によって新しいアチーブメントがアンロックされる可能性がある
            var currentContext = context
            var iteration = 0
            val maxIterations = 10 // 無限ループ防止

            do {
                val unlockedIds = getUnlockedIds(playerUuid)
                val newUnlocks = checker.checkNewUnlocks(playerData, currentContext, unlockedIds)

                if (newUnlocks.isEmpty()) break

                newUnlocks.forEach { achievement ->
                    // DB保存
                    repository.save(playerUuid, achievement.id)

                    // キャッシュ更新
                    unlockedCache.computeIfAbsent(playerUuid) { mutableSetOf() }.add(achievement.id)

                    // 報酬付与（これによりFame/Alignmentが変化し、次のアチーブメントが解除される可能性）
                    grantRewards(playerData, achievement)

                    // 通知
                    notifyPlayer(playerUuid, achievement)

                    // サーバー全体アナウンス（Epic/Legendary）
                    if (achievement.shouldAnnounce()) {
                        announceToServer(playerUuid, achievement)
                    }

                    // イベント発火
                    Bukkit.getPluginManager().callEvent(
                        AchievementUnlockEvent(playerUuid, achievement, achievement.fameReward, achievement.alignmentReward)
                    )
                }

                allNewUnlocks.addAll(newUnlocks)

                // 次のイテレーションでは報酬による変化をチェック
                currentContext = AchievementContext.EMPTY
                iteration++
            } while (iteration < maxIterations)

            allNewUnlocks
        }
    }

    override fun getUnlockedAchievements(playerUuid: UUID): List<PlayerAchievement> {
        return repository.getAll(playerUuid)
    }

    override fun hasUnlocked(playerUuid: UUID, achievementId: String): Boolean {
        return getUnlockedIds(playerUuid).contains(achievementId)
    }

    override fun getProgress(playerUuid: UUID, category: AchievementCategory): Pair<Int, Int> {
        val unlockedIds = getUnlockedIds(playerUuid)
        val categoryAchievements = Achievements.byCategory(category)
        val unlockedCount = categoryAchievements.count { it.id in unlockedIds }
        return unlockedCount to categoryAchievements.size
    }

    override fun getAllAchievements(): List<Achievement> = Achievements.all()

    override fun getAchievementsByCategory(category: AchievementCategory): List<Achievement> =
        Achievements.byCategory(category)

    override fun getTotalProgress(playerUuid: UUID): Pair<Int, Int> {
        val unlockedIds = getUnlockedIds(playerUuid)
        return unlockedIds.size to Achievements.all().size
    }

    override fun getUnlockedIds(playerUuid: UUID): Set<String> {
        return unlockedCache.computeIfAbsent(playerUuid) {
            repository.getUnlockedIds(playerUuid).toMutableSet()
        }
    }

    /**
     * 報酬を付与
     */
    private fun grantRewards(playerData: com.hacklab.minecraft.notoriety.core.player.PlayerData, achievement: Achievement) {
        if (achievement.fameReward > 0) {
            playerData.addFame(achievement.fameReward)
        }
        if (achievement.alignmentReward != 0) {
            playerData.addAlignment(achievement.alignmentReward)
        }
    }

    /**
     * プレイヤーに通知（MinecraftネイティブAdvancementを使用）
     */
    private fun notifyPlayer(playerUuid: UUID, achievement: Achievement) {
        val player = Bukkit.getPlayer(playerUuid) ?: return

        // メインスレッドで実行
        Bukkit.getScheduler().runTask(plugin, Runnable {
            // MinecraftネイティブAdvancementを付与（トースト通知が自動表示される）
            if (achievement.advancementKey.isNotEmpty()) {
                val key = NamespacedKey(plugin, achievement.advancementKey)
                val advancement = Bukkit.getAdvancement(key)
                if (advancement != null) {
                    val progress = player.getAdvancementProgress(advancement)
                    // 全ての条件を達成状態にする
                    advancement.criteria.forEach { criterion ->
                        if (!progress.isDone) {
                            progress.awardCriteria(criterion)
                        }
                    }
                } else {
                    plugin.logger.warning("Advancement not found: ${achievement.advancementKey}")
                }
            }

            // ActionBar（報酬表示）- 報酬がある場合のみ表示
            val rewardParts = mutableListOf<String>()
            if (achievement.fameReward > 0) {
                rewardParts.add("Fame +${achievement.fameReward}")
            }
            if (achievement.alignmentReward > 0) {
                rewardParts.add("Alignment +${achievement.alignmentReward}")
            } else if (achievement.alignmentReward < 0) {
                rewardParts.add("Alignment ${achievement.alignmentReward}")
            }
            if (rewardParts.isNotEmpty()) {
                player.sendActionBar(
                    Component.text(rewardParts.joinToString(" | ")).color(NamedTextColor.GREEN)
                )
            }
        })
    }

    /**
     * サーバー全体にアナウンス
     */
    private fun announceToServer(playerUuid: UUID, achievement: Achievement) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val playerName = Bukkit.getOfflinePlayer(playerUuid).name ?: "Unknown"
            val locale = i18nManager.defaultLocale
            val achievementName = i18nManager.getForLocale(locale, achievement.nameKey, achievement.id)
            val announceTemplate = i18nManager.getForLocale(locale, "achievement.server_announce", "★ %s unlocked '%s'! ★")
            val message = String.format(announceTemplate, playerName, achievementName)

            Bukkit.broadcast(
                Component.text(message).color(NamedTextColor.GOLD)
            )
        })
    }

    /**
     * プレイヤーのキャッシュをクリア（ログアウト時に呼ばれる）
     */
    fun clearCache(playerUuid: UUID) {
        unlockedCache.remove(playerUuid)
        playerLocks.remove(playerUuid)
    }
}
