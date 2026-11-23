package com.pixubi.game

import java.time.Instant
import java.time.ZoneId

/**
 * Evolution is earned by both activity and time lived outside hibernation.
 * Sequence: egg -> baby -> youngster -> cocoon -> adult.
 */
enum class GrowthStage(val title: String, val icon: String, val minMinutes: Long, val minLevel: Int) {
    EGG("Яйце", "🥚", 0, 1),
    BABY("Малюк", "🐣", 15, 2),
    YOUNG("Підліток", "🐾", 1_440, 3),
    COCOON("Кокон", "🪺", 4_320, 5),
    ADULT("Дорослий", "🦊", 5_760, 6)
}

object PetGrowth {
    const val MAX_LEVEL = 50
    const val DAILY_CARE_XP_LIMIT = 50
    private const val MS_DAY = 86_400_000L

    /** XP total required to start each level. No unchecked multiplication. */
    fun xpForLevel(level: Int): Int {
        val n = (level.coerceIn(1, MAX_LEVEL) - 1).toLong()
        return (15L * n + 5L * n * (n - 1L)).toInt()
    }

    fun level(xp: Int): Int {
        var result = 1
        while (result < MAX_LEVEL && xp >= xpForLevel(result + 1)) result++
        return result
    }

    fun stage(pet: Pet): GrowthStage {
        if (pet.legacyAdult) return GrowthStage.ADULT
        val lvl = level(pet.xp)
        return GrowthStage.entries.lastOrNull {
            pet.activeMinutes >= it.minMinutes && lvl >= it.minLevel
        } ?: GrowthStage.EGG
    }

    fun nextStage(pet: Pet): GrowthStage? {
        if (pet.legacyAdult) return null
        val index = stage(pet).ordinal
        return GrowthStage.entries.getOrNull(index + 1)
    }

    fun xpToNextLevel(pet: Pet): Int {
        val lvl = level(pet.xp)
        return if (lvl == MAX_LEVEL) 0 else
            (xpForLevel(lvl + 1) - pet.xp).coerceAtLeast(0)
    }

    fun progressInLevel(pet: Pet): Float {
        val lvl = level(pet.xp)
        if (lvl == MAX_LEVEL) return 1f
        val min = xpForLevel(lvl)
        val max = xpForLevel(lvl + 1)
        return ((pet.xp - min).toFloat() / (max - min)).coerceIn(0f, 1f)
    }

    fun careDay(now: Long): Long = Instant.ofEpochMilli(now)
        .atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    /** Survival XP is paid only on crossing full active days, not while hibernating. */
    fun survivalBonus(before: Long, after: Long): Int {
        if (after <= before) return 0
        val newDays = after / 1_440L - before / 1_440L
        return (newDays.coerceIn(0, 365).toInt() * 8)
    }

    fun recordCare(pet: Pet, action: Action, now: Long, wasHibernating: Boolean): Pet {
        if (action == Action.HIBERNATE) return pet
        val daily = careDay(now)
        val newDay = daily != pet.lastCareDay
        val oldDailyXp = if (daily == pet.actionXpDay) pet.actionXpToday else 0
        val requested = when (action) {
            Action.FEED -> 3
            Action.WATER -> 2
            Action.CHARGE -> 2
            Action.EXPLORE -> 0
            Action.SLEEP -> 4
            Action.WAKE -> 1
            Action.HIBERNATE -> 0
            Action.PLAY_WIN -> 12
        }
        val earned = requested.coerceAtMost((DAILY_CARE_XP_LIMIT - oldDailyXp).coerceAtLeast(0))
        val streak = if (!newDay) pet.careStreak
            else if (pet.lastCareDay < 0L) 1
            else if (daily - pet.lastCareDay == 1L || wasHibernating) pet.careStreak + 1
            else 1
        return pet.copy(
            xp = (pet.xp.toLong() + earned).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            actionXpToday = oldDailyXp + earned,
            actionXpDay = daily,
            careDays = pet.careDays + if (newDay) 1 else 0,
            careStreak = streak,
            lastCareDay = if (newDay) daily else pet.lastCareDay
        )
    }

    fun stageHint(pet: Pet): String {
        val next = nextStage(pet) ?: return "Дорослий Піксубі — нові форми попереду!"
        val minutes = (next.minMinutes - pet.activeMinutes).coerceAtLeast(0L)
        val xp = (xpForLevel(next.minLevel) - pet.xp).coerceAtLeast(0)
        val dayText = when {
            minutes == 0L -> "достатньо часу"
            minutes < 60L -> "ще ${minutes} хв активного життя"
            else -> "ще ${(minutes + 1_439L) / 1_440L} дн. активного життя"
        }
        val label = if (next == GrowthStage.BABY) "До появи малюка" else "До таємної еволюції"
        return "$label: $dayText • ${xp} XP"
    }
}
