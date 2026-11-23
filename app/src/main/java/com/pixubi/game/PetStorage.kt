package com.pixubi.game

import android.content.Context
import org.json.JSONObject

/** One locally saved pet for MVP. A version field enables future save migrations. */
class PetStorage(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_save", Context.MODE_PRIVATE)

    fun save(p: Pet) {
        val json = JSONObject().apply {
            put("version", 3)
            put("species", p.species.name)
            put("sex", p.sex.name)
            put("region", p.region.name)
            put("name", p.name)
            put("geneA", p.geneA)
            put("geneB", p.geneB)
            put("bornAt", p.bornAt)
            put("lastUpdatedAt", p.lastUpdatedAt)
            put("mode", p.mode.name)
            put("alive", p.alive)
            put("hunger", p.hunger)
            put("thirst", p.thirst)
            put("energy", p.energy)
            put("happiness", p.happiness)
            put("activeMinutes", p.activeMinutes)
            put("dangerMinutes", p.dangerMinutes)
            put("miniGameWins", p.miniGameWins)
            put("xp", p.xp)
            put("careDays", p.careDays)
            put("careStreak", p.careStreak)
            put("lastCareDay", p.lastCareDay)
            put("actionXpDay", p.actionXpDay)
            put("actionXpToday", p.actionXpToday)
            put("legacyAdult", p.legacyAdult)
            put("deathReason", p.deathReason)
            put("message", p.message)
            put("coins", p.coins)
            put("foodStock", p.foodStock)
            put("waterStock", p.waterStock)
            put("chargeStock", p.chargeStock)
            put("hasHome", p.hasHome)
            put("hasBowl", p.hasBowl)
            put("hasCup", p.hasCup)
            put("hasCharger", p.hasCharger)
            put("expeditionStartedAt", p.expeditionStartedAt)
            put("expeditionsDone", p.expeditionsDone)
            put("expeditionDay", p.expeditionDay)
            put("expeditionToday", p.expeditionToday)
            put("rewardDay", p.rewardDay)
            put("rewardCoinsToday", p.rewardCoinsToday)
        }
        prefs.edit().putString("pet", json.toString()).apply()
    }

    fun load(): Pet? {
        val raw = prefs.getString("pet", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            Pet(
                species = Species.valueOf(o.getString("species")),
                sex = Sex.valueOf(o.getString("sex")),
                region = Region.valueOf(o.getString("region")),
                name = o.getString("name"),
                geneA = o.getInt("geneA"),
                geneB = o.getInt("geneB"),
                bornAt = o.getLong("bornAt"),
                lastUpdatedAt = o.getLong("lastUpdatedAt"),
                mode = Mode.valueOf(o.getString("mode")),
                alive = o.getBoolean("alive"),
                hunger = o.getDouble("hunger"),
                thirst = o.getDouble("thirst"),
                energy = o.getDouble("energy"),
                happiness = o.getDouble("happiness"),
                activeMinutes = o.getLong("activeMinutes"),
                dangerMinutes = o.getLong("dangerMinutes"),
                miniGameWins = o.getInt("miniGameWins"),
                xp = o.optInt("xp", 400).coerceAtLeast(0),
                careDays = o.optInt("careDays", 0).coerceAtLeast(0),
                careStreak = o.optInt("careStreak", 0).coerceAtLeast(0),
                lastCareDay = o.optLong("lastCareDay", -1L),
                actionXpDay = o.optLong("actionXpDay", -1L),
                actionXpToday = o.optInt("actionXpToday", 0).coerceIn(0, PetGrowth.DAILY_CARE_XP_LIMIT),
                legacyAdult = o.optBoolean("legacyAdult", !o.has("xp")),
                deathReason = o.optString("deathReason", ""),
                message = o.optString("message", ""),
                coins = o.optInt("coins", 30).coerceIn(0, 9999),
                foodStock = o.optInt("foodStock", 4).coerceIn(0, 30),
                waterStock = o.optInt("waterStock", 4).coerceIn(0, 30),
                chargeStock = o.optInt("chargeStock", 2).coerceIn(0, 30),
                hasHome = o.optBoolean("hasHome", false),
                hasBowl = o.optBoolean("hasBowl", false),
                hasCup = o.optBoolean("hasCup", false),
                hasCharger = o.optBoolean("hasCharger", false),
                expeditionStartedAt = o.optLong("expeditionStartedAt", 0L).coerceAtLeast(0L),
                expeditionsDone = o.optInt("expeditionsDone", 0).coerceAtLeast(0),
                expeditionDay = o.optLong("expeditionDay", -1L),
                expeditionToday = o.optInt("expeditionToday", 0).coerceIn(0, GameEngine.EXPEDITIONS_PER_DAY),
                rewardDay = o.optLong("rewardDay", -1L),
                rewardCoinsToday = o.optInt("rewardCoinsToday", 0).coerceIn(0, GameEngine.GAME_COINS_PER_DAY)
            )
        }.getOrNull()
    }

    fun erase() { prefs.edit().remove("pet").apply() }
}
