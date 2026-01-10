package com.hacklab.minecraft.notoriety.achievement.model

import com.hacklab.minecraft.notoriety.achievement.service.*
import com.hacklab.minecraft.notoriety.reputation.NameColor

/**
 * 全アチーブメント定義
 */
object Achievements {
    // ===== 名声系 (REPUTATION) =====

    val PATH_OF_THE_SQUIRE = Achievement(
        id = "REP-001",
        nameKey = "achievement.rep001.name",
        descriptionKey = "achievement.rep001.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.COMMON,
        condition = FameThresholdCondition(100),
        fameReward = 10,
        advancementKey = "reputation/path_of_the_squire"
    )

    val PATH_OF_THE_KNIGHT = Achievement(
        id = "REP-002",
        nameKey = "achievement.rep002.name",
        descriptionKey = "achievement.rep002.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.COMMON,
        condition = FameThresholdCondition(250),
        fameReward = 15,
        advancementKey = "reputation/path_of_the_knight"
    )

    val PATH_OF_THE_COMMANDER = Achievement(
        id = "REP-003",
        nameKey = "achievement.rep003.name",
        descriptionKey = "achievement.rep003.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.UNCOMMON,
        condition = FameThresholdCondition(500),
        fameReward = 25,
        advancementKey = "reputation/path_of_the_commander"
    )

    val MARK_OF_THE_HERO = Achievement(
        id = "REP-004",
        nameKey = "achievement.rep004.name",
        descriptionKey = "achievement.rep004.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.RARE,
        condition = FameThresholdCondition(750),
        fameReward = 50,
        advancementKey = "reputation/mark_of_the_hero"
    )

    val PINNACLE_OF_VIRTUE = Achievement(
        id = "REP-005",
        nameKey = "achievement.rep005.name",
        descriptionKey = "achievement.rep005.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.EPIC,
        condition = AlignmentThresholdCondition(1000),
        fameReward = 50,
        announceOnUnlock = true,
        advancementKey = "reputation/pinnacle_of_virtue"
    )

    val FALL_FROM_GRACE = Achievement(
        id = "REP-006",
        nameKey = "achievement.rep006.name",
        descriptionKey = "achievement.rep006.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.UNCOMMON,
        condition = PKCountThresholdCondition(1),
        advancementKey = "reputation/fall_from_grace"
    )

    val DREAD_SOVEREIGN = Achievement(
        id = "REP-007",
        nameKey = "achievement.rep007.name",
        descriptionKey = "achievement.rep007.desc",
        category = AchievementCategory.REPUTATION,
        rarity = AchievementRarity.LEGENDARY,
        condition = PKCountThresholdCondition(200),
        announceOnUnlock = true,
        advancementKey = "reputation/dread_sovereign"
    )

    // ===== 戦闘・賞金稼ぎ系 (COMBAT) =====

    val BOUNTY_HUNTER = Achievement(
        id = "CMB-001",
        nameKey = "achievement.cmb001.name",
        descriptionKey = "achievement.cmb001.desc",
        category = AchievementCategory.COMBAT,
        rarity = AchievementRarity.COMMON,
        condition = FirstTimeCondition("RED_KILL"),
        fameReward = 10,
        advancementKey = "combat/bounty_hunter"
    )

    val LEGENDARY_BOUNTY_HUNTER = Achievement(
        id = "CMB-002",
        nameKey = "achievement.cmb002.name",
        descriptionKey = "achievement.cmb002.desc",
        category = AchievementCategory.COMBAT,
        rarity = AchievementRarity.RARE,
        condition = RedKillsCondition(50),
        fameReward = 50,
        advancementKey = "combat/legendary_bounty_hunter"
    )

    val FORTUNE_SEEKER = Achievement(
        id = "CMB-003",
        nameKey = "achievement.cmb003.name",
        descriptionKey = "achievement.cmb003.desc",
        category = AchievementCategory.COMBAT,
        rarity = AchievementRarity.RARE,
        condition = TotalBountyEarnedCondition(100_000),
        fameReward = 30,
        advancementKey = "combat/fortune_seeker"
    )

    val GOLEM_SLAYER = Achievement(
        id = "CMB-004",
        nameKey = "achievement.cmb004.name",
        descriptionKey = "achievement.cmb004.desc",
        category = AchievementCategory.COMBAT,
        rarity = AchievementRarity.UNCOMMON,
        condition = GolemKillsCondition(10),
        alignmentReward = -100,
        advancementKey = "combat/golem_slayer"
    )

    val GOLEM_BANE = Achievement(
        id = "CMB-005",
        nameKey = "achievement.cmb005.name",
        descriptionKey = "achievement.cmb005.desc",
        category = AchievementCategory.COMBAT,
        rarity = AchievementRarity.EPIC,
        condition = GolemKillsCondition(100),
        announceOnUnlock = true,
        advancementKey = "combat/golem_bane"
    )

    // ===== ギルド系 (GUILD) =====

    val GUILD_MEMBER = Achievement(
        id = "GLD-001",
        nameKey = "achievement.gld001.name",
        descriptionKey = "achievement.gld001.desc",
        category = AchievementCategory.GUILD,
        rarity = AchievementRarity.COMMON,
        condition = GuildJoinCondition,
        fameReward = 5,
        advancementKey = "guild/guild_member"
    )

    val GUILD_FOUNDER = Achievement(
        id = "GLD-002",
        nameKey = "achievement.gld002.name",
        descriptionKey = "achievement.gld002.desc",
        category = AchievementCategory.GUILD,
        rarity = AchievementRarity.UNCOMMON,
        condition = GuildCreateCondition,
        fameReward = 20,
        advancementKey = "guild/guild_founder"
    )

    // ===== 贖罪・復帰系 (REDEMPTION) =====

    val PATH_TO_REDEMPTION = Achievement(
        id = "RDM-001",
        nameKey = "achievement.rdm001.name",
        descriptionKey = "achievement.rdm001.desc",
        category = AchievementCategory.REDEMPTION,
        rarity = AchievementRarity.UNCOMMON,
        condition = ColorTransitionCondition(NameColor.RED, NameColor.GRAY),
        fameReward = 10,
        alignmentReward = 50,
        advancementKey = "redemption/path_to_redemption"
    )

    val ABSOLUTION = Achievement(
        id = "RDM-002",
        nameKey = "achievement.rdm002.name",
        descriptionKey = "achievement.rdm002.desc",
        category = AchievementCategory.REDEMPTION,
        rarity = AchievementRarity.RARE,
        condition = ColorTransitionCondition(NameColor.GRAY, NameColor.BLUE),
        fameReward = 25,
        alignmentReward = 100,
        advancementKey = "redemption/absolution"
    )

    val FALLEN_ANGEL = Achievement(
        id = "RDM-003",
        nameKey = "achievement.rdm003.name",
        descriptionKey = "achievement.rdm003.desc",
        category = AchievementCategory.REDEMPTION,
        rarity = AchievementRarity.EPIC,
        condition = AndCondition(listOf(
            ColorTransitionCondition(NameColor.GRAY, NameColor.BLUE),
            FameThresholdCondition(500)
        )),
        fameReward = 50,
        announceOnUnlock = true,
        advancementKey = "redemption/fallen_angel"
    )

    // ===== 村人・取引系 (VILLAGE) =====

    val TRUSTED_MERCHANT = Achievement(
        id = "VLG-001",
        nameKey = "achievement.vlg001.name",
        descriptionKey = "achievement.vlg001.desc",
        category = AchievementCategory.VILLAGE,
        rarity = AchievementRarity.COMMON,
        condition = TradeCountCondition(100),
        fameReward = 10,
        alignmentReward = 20,
        advancementKey = "village/trusted_merchant"
    )

    val MASTER_TRADER = Achievement(
        id = "VLG-002",
        nameKey = "achievement.vlg002.name",
        descriptionKey = "achievement.vlg002.desc",
        category = AchievementCategory.VILLAGE,
        rarity = AchievementRarity.RARE,
        condition = TradeCountCondition(1000),
        fameReward = 30,
        alignmentReward = 50,
        advancementKey = "village/master_trader"
    )

    val LEGEND_OF_COMMERCE = Achievement(
        id = "VLG-003",
        nameKey = "achievement.vlg003.name",
        descriptionKey = "achievement.vlg003.desc",
        category = AchievementCategory.VILLAGE,
        rarity = AchievementRarity.EPIC,
        condition = TradeCountCondition(10000),
        fameReward = 100,
        alignmentReward = 100,
        announceOnUnlock = true,
        advancementKey = "village/legend_of_commerce"
    )

    val VILLAGE_HERO = Achievement(
        id = "VLG-004",
        nameKey = "achievement.vlg004.name",
        descriptionKey = "achievement.vlg004.desc",
        category = AchievementCategory.VILLAGE,
        rarity = AchievementRarity.RARE,
        condition = AndCondition(listOf(
            AlignmentThresholdCondition(500),
            TradeCountCondition(500)
        )),
        fameReward = 40,
        alignmentReward = 50,
        advancementKey = "village/village_hero"
    )

    /**
     * 全アチーブメントのリスト
     */
    private val allAchievements: List<Achievement> = listOf(
        // 名声系
        PATH_OF_THE_SQUIRE,
        PATH_OF_THE_KNIGHT,
        PATH_OF_THE_COMMANDER,
        MARK_OF_THE_HERO,
        PINNACLE_OF_VIRTUE,
        FALL_FROM_GRACE,
        DREAD_SOVEREIGN,
        // 戦闘系
        BOUNTY_HUNTER,
        LEGENDARY_BOUNTY_HUNTER,
        FORTUNE_SEEKER,
        GOLEM_SLAYER,
        GOLEM_BANE,
        // ギルド系
        GUILD_MEMBER,
        GUILD_FOUNDER,
        // 贖罪系
        PATH_TO_REDEMPTION,
        ABSOLUTION,
        FALLEN_ANGEL,
        // 村人系
        TRUSTED_MERCHANT,
        MASTER_TRADER,
        LEGEND_OF_COMMERCE,
        VILLAGE_HERO
    )

    /**
     * 全アチーブメントを取得
     */
    fun all(): List<Achievement> = allAchievements

    /**
     * IDでアチーブメントを検索
     */
    fun findById(id: String): Achievement? = allAchievements.find { it.id == id }

    /**
     * カテゴリでアチーブメントをフィルタ
     */
    fun byCategory(category: AchievementCategory): List<Achievement> =
        allAchievements.filter { it.category == category }

    /**
     * カテゴリごとのアチーブメント数を取得
     */
    fun countByCategory(): Map<AchievementCategory, Int> =
        allAchievements.groupBy { it.category }.mapValues { it.value.size }
}
