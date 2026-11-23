package com.pixubi.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent discoveries survive the death and replacement of individual pets. */
data class RaisedPixu(
    val id: String,
    val species: Species,
    val sex: Sex,
    val colorIndex: Int,
    val name: String,
    val highest: GrowthStage,
    val activeDays: Long,
    val xp: Int,
    val miniGameWins: Int,
    val alive: Boolean
)

data class PlayerCollection(
    val nickname: String = "Мандрівник",
    val avatar: String = "🐾",
    val startedAt: Long = 0L,
    val discovered: Set<String> = emptySet(),
    val raised: List<RaisedPixu> = emptyList(),
    val bestStreak: Int = 0
) {
    val unlockedCount: Int get() = discovered.size
    val totalRaised: Int get() = raised.size
    val grownAdults: Int get() = raised.count { it.highest == GrowthStage.ADULT }
    val knownSpecies: Int get() = raised.map { it.species }.distinct().size
    val totalMiniGameWins: Int get() = raised.sumOf { it.miniGameWins }
    val accumulatedXp: Int get() = raised.fold(0L) { sum, record -> sum + record.xp }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val playerLevel: Int get() = (1 + accumulatedXp / 140).coerceIn(1, 50)
    val lifeDays: Long get() = raised.sumOf { it.activeDays }
}

object CollectionRules {
    const val ALL_FORMS = 5 * 2 * 6 * 5

    fun formKey(species: Species, sex: Sex, color: Int, stage: GrowthStage) =
        "${species.name}:${sex.name}:${color.coerceIn(0, 5)}:${stage.name}"

    fun hasForm(collection: PlayerCollection, species: Species, sex: Sex, color: Int, stage: GrowthStage) =
        formKey(species, sex, color, stage) in collection.discovered

    fun babyMayBePreviewed(stage: GrowthStage) =
        stage == GrowthStage.EGG || stage == GrowthStage.BABY

    fun canPreviewCurrent(collection: PlayerCollection, pet: Pet, stage: GrowthStage): Boolean =
        babyMayBePreviewed(stage) ||
            hasForm(collection, pet.species, pet.sex, pet.colorIndex, stage)

    fun recordId(pet: Pet) = "${pet.bornAt}:${pet.species.name}:${pet.geneA}:${pet.geneB}"

    /** Idempotent. If a pet jumped several stages while away, unlock all surpassed stages. */
    fun observe(source: PlayerCollection, pet: Pet): PlayerCollection {
        val current = PetGrowth.stage(pet)
        val ids = GrowthStage.entries
            .filter { it.ordinal <= current.ordinal }
            .map { formKey(pet.species, pet.sex, pet.colorIndex, it) }
            .toSet()
        val record = RaisedPixu(
            id = recordId(pet), species = pet.species, sex = pet.sex,
            colorIndex = pet.colorIndex, name = pet.name, highest = current,
            activeDays = pet.daysOld, xp = pet.xp, miniGameWins = pet.miniGameWins, alive = pet.alive
        )
        val previous = source.raised.firstOrNull { it.id == record.id }
        val merged = if (previous == null) record else record.copy(
            highest = if (previous.highest.ordinal > record.highest.ordinal) previous.highest else record.highest,
            activeDays = maxOf(previous.activeDays, record.activeDays),
            xp = maxOf(previous.xp, record.xp),
            miniGameWins = maxOf(previous.miniGameWins, record.miniGameWins)
        )
        val records = if (previous == null) source.raised + merged
            else source.raised.map { if (it.id == record.id) merged else it }
        return source.copy(
            startedAt = source.startedAt.takeIf { it > 0 } ?: pet.bornAt,
            discovered = source.discovered + ids,
            raised = records,
            bestStreak = maxOf(source.bestStreak, pet.careStreak)
        )
    }
}

/** Independent SharedPreferences file; never erased when the active pet is replaced. */
class PlayerCollectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_player_collection", Context.MODE_PRIVATE)

    fun load(): PlayerCollection {
        val raw = prefs.getString("collection_v1", null) ?: return PlayerCollection(
            startedAt = prefs.getLong("first_started_at", System.currentTimeMillis())
        )
        return runCatching {
            val json = JSONObject(raw)
            val forms = json.optJSONArray("discovered") ?: JSONArray()
            val seen = buildSet {
                for (i in 0 until forms.length()) {
                    val key = forms.optString(i)
                    if (key.isNotBlank() && size < CollectionRules.ALL_FORMS) add(key)
                }
            }
            val entries = json.optJSONArray("raised") ?: JSONArray()
            val records = buildList {
                for (i in 0 until minOf(entries.length(), 500)) {
                    val p = entries.optJSONObject(i) ?: continue
                    runCatching {
                        add(RaisedPixu(
                            id = p.getString("id"),
                            species = Species.valueOf(p.getString("species")),
                            sex = Sex.valueOf(p.getString("sex")),
                            colorIndex = p.optInt("colorIndex", 0).coerceIn(0,5),
                            name = p.optString("name", "?"),
                            highest = GrowthStage.valueOf(p.getString("highest")),
                            activeDays = p.optLong("activeDays", 0).coerceAtLeast(0),
                            xp = p.optInt("xp", 0).coerceAtLeast(0),
                            miniGameWins = p.optInt("miniGameWins", 0).coerceAtLeast(0),
                            alive = p.optBoolean("alive", false)
                        ))
                    }
                }
            }
            PlayerCollection(
                nickname = json.optString("nickname", "Мандрівник").take(24),
                avatar = json.optString("avatar", "🐾"),
                startedAt = json.optLong("startedAt", System.currentTimeMillis()),
                discovered = seen,
                raised = records,
                bestStreak = json.optInt("bestStreak", 0).coerceAtLeast(0)
            )
        }.getOrElse { PlayerCollection(startedAt = System.currentTimeMillis()) }
    }

    fun save(value: PlayerCollection) {
        val json = JSONObject().apply {
            put("version", 1)
            put("nickname", value.nickname.take(24))
            put("avatar", value.avatar)
            put("startedAt", value.startedAt)
            put("bestStreak", value.bestStreak)
            put("discovered", JSONArray().apply {
                value.discovered.sorted().forEach { put(it) }
            })
            put("raised", JSONArray().apply {
                value.raised.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("species", r.species.name)
                        put("sex", r.sex.name)
                        put("colorIndex", r.colorIndex)
                        put("name", r.name)
                        put("highest", r.highest.name)
                        put("activeDays", r.activeDays)
                        put("xp", r.xp)
                        put("miniGameWins", r.miniGameWins)
                        put("alive", r.alive)
                    })
                }
            })
        }
        prefs.edit().putLong("first_started_at", value.startedAt)
            .putString("collection_v1", json.toString()).apply()
    }

    fun observe(pet: Pet): PlayerCollection {
        val old = load()
        val next = CollectionRules.observe(old, pet)
        if (next != old) save(next)
        return next
    }
}
