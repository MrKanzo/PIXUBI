package com.pixubi.game

import android.content.Context
import org.json.JSONObject

/** One locally saved pet for MVP. A version field enables future save migrations. */
class PetStorage(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_save", Context.MODE_PRIVATE)

    fun save(p: Pet) {
        val json = JSONObject().apply {
            put("version", 2)
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
                message = o.optString("message", "")
            )
        }.getOrNull()
    }

    fun erase() { prefs.edit().remove("pet").apply() }
}
