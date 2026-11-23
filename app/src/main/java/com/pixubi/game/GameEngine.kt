package com.pixubi.game

import kotlin.math.min
import kotlin.random.Random

/** All game decisions are calculated locally; no server, ads or account. */
enum class Region(val title: String, val icon: String, val hint: String) {
    FOREST("Смарагдовий ліс", "🌲", "Тихі стежки, ягоди та секрети"),
    MOUNTAINS("Гірські вершини", "⛰️", "Скелі та блискавичні пригоди"),
    LAKES("Озерний край", "💧", "Вода, очерет і водоспади"),
    DESERT("Золоті дюни", "🏜️", "Пісок, зорі та маленькі оази"),
    SKY("Небесні острови", "☁️", "Хмаринки та вітряні польоти")
}

enum class Species(val title: String, val icon: String, val native: Region, val personality: String) {
    LISYK("Лісик", "🦊", Region.FOREST, "Допитливий і спритний"),
    ISKRYK("Іскрик", "⚡", Region.MOUNTAINS, "Енергійний і сміливий"),
    KRAPLYK("Краплик", "🦎", Region.LAKES, "Веселий і товариський"),
    PISHCHANYK("Піщаник", "🐾", Region.DESERT, "Хитруватий мандрівник"),
    KHMARYK("Хмарик", "🪽", Region.SKY, "Мрійливий і доброзичливий")
}

enum class Sex(val title: String) { BOY("Хлопчик"), GIRL("Дівчинка") }
enum class Mode { AWAKE, SLEEP, HIBERNATION }
enum class Action { FEED, WATER, SLEEP, WAKE, HIBERNATE, PLAY_WIN }

data class Pet(
    val species: Species,
    val sex: Sex,
    val region: Region,
    val name: String,
    val geneA: Int,
    val geneB: Int,
    val bornAt: Long,
    val lastUpdatedAt: Long,
    val mode: Mode = Mode.AWAKE,
    val alive: Boolean = true,
    val hunger: Double = 85.0,
    val thirst: Double = 85.0,
    val energy: Double = 85.0,
    val happiness: Double = 80.0,
    val activeMinutes: Long = 0,
    val dangerMinutes: Long = 0,
    val miniGameWins: Int = 0,
    val xp: Int = 0,
    val careDays: Int = 0,
    val careStreak: Int = 0,
    val lastCareDay: Long = -1L,
    val actionXpDay: Long = -1L,
    val actionXpToday: Int = 0,
    val legacyAdult: Boolean = false,
    val deathReason: String = "",
    val message: String = "Ура! Я з'явився на світ! 💜"
) {
    val colorIndex: Int get() = geneA % 6
    val daysOld: Long get() = activeMinutes / (24 * 60)
}

object GameEngine {
    // One real day outside hibernation counts as one day of life.
    const val NATURAL_LIFE_DAYS = 365L
    const val CRITICAL_LIMIT_MINUTES = 24L * 60
    private const val HOUR_MINUTES = 60.0

    fun hatch(species: Species, sex: Sex, region: Region, name: String, now: Long,
              random: Random = Random.Default): Pet {
        val firstGene = random.nextInt(6)
        val secondGene = random.nextInt(6)
        return Pet(
            species = species,
            sex = sex,
            region = region,
            name = name.trim().take(24).ifBlank { species.title },
            geneA = firstGene,
            geneB = secondGene,
            bornAt = now,
            lastUpdatedAt = now,
            message = "Моє цифрове яйце чекає на твою турботу! 🥚"
        )
    }

    /** Incremental simulation ensures dangerous states cannot be skipped after long absences. */
    fun advance(source: Pet, now: Long): Pet {
        if (!source.alive) return source.copy(lastUpdatedAt = now.coerceAtLeast(source.lastUpdatedAt))
        if (source.mode == Mode.HIBERNATION) return source.copy(lastUpdatedAt = now.coerceAtLeast(source.lastUpdatedAt))
        val minutes = ((now - source.lastUpdatedAt).coerceAtLeast(0L) / 60_000L)
        if (minutes == 0L) return source
        var pet = source
        var remaining = minutes
        while (remaining > 0 && pet.alive) {
            val step = min(remaining, 30L)
            val h = step / HOUR_MINUTES
            val sleeping = pet.mode == Mode.SLEEP
            val hungry = (pet.hunger - (if (sleeping) 0.65 else 1.4) * h).coerceIn(0.0, 100.0)
            val thirsty = (pet.thirst - (if (sleeping) 1.2 else 2.2) * h).coerceIn(0.0, 100.0)
            val tired = (pet.energy + (if (sleeping) 11.0 else -1.35) * h).coerceIn(0.0, 100.0)
            val mood = (pet.happiness - (if (sleeping) 0.10 else 0.38) * h).coerceIn(0.0, 100.0)
            val danger = if (hungry <= 0.0 || thirsty <= 0.0) pet.dangerMinutes + step else 0
            val age = pet.activeMinutes + step
            val diedFromAge = age >= NATURAL_LIFE_DAYS * 24 * 60
            val diedFromNeglect = danger >= CRITICAL_LIMIT_MINUTES
            val oldStage = PetGrowth.stage(pet)
            val bonus = PetGrowth.survivalBonus(pet.activeMinutes, age)
            val nextXp = (pet.xp.toLong() + bonus).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val evolved = pet.copy(activeMinutes = age, xp = nextXp)
            val nextStage = PetGrowth.stage(evolved)
            pet = pet.copy(
                hunger = hungry, thirst = thirsty, energy = tired, happiness = mood,
                dangerMinutes = danger, activeMinutes = age, xp = nextXp,
                alive = !(diedFromAge || diedFromNeglect),
                deathReason = when {
                    diedFromAge -> "Прожив довге життя й помер від старості."
                    diedFromNeglect -> "Загинув через тривалу нестачу їжі або води."
                    else -> pet.deathReason
                },
                message = when {
                    diedFromAge || diedFromNeglect -> "Прощавай, маленький друже. 🕯️"
                    oldStage != nextStage -> "✨ Нова стадія: ${nextStage.title}! Я росту!"
                    else -> pet.message
                }
            )
            remaining -= step
        }
        // Residual seconds are kept so they are not lost on repeated reads.
        val accountedMinutes = minutes - remaining
        return pet.copy(lastUpdatedAt = source.lastUpdatedAt + accountedMinutes * 60_000L)
    }

    fun act(source: Pet, action: Action, now: Long): Pet {
        val pet = advance(source, now)
        if (!pet.alive) return pet
        // Reset the clock exactly when modes change, so elapsed fractions do not leak.
        if (pet.mode == Mode.HIBERNATION && action != Action.WAKE) return pet
        if (pet.mode == Mode.SLEEP && action !in listOf(Action.WAKE, Action.HIBERNATE)) return pet
        fun gain(value: Double, points: Double) = (value + points).coerceIn(0.0, 100.0)
        val updated = when (action) {
            Action.FEED -> pet.copy(hunger = gain(pet.hunger, 24.0), message = "Ням-ням! Смачно! 🍓")
            Action.WATER -> pet.copy(thirst = gain(pet.thirst, 30.0), message = "Водичка — це життя! 💧")
            Action.SLEEP -> pet.copy(mode = Mode.SLEEP, message = "На добраніч! 🌙")
            Action.WAKE -> pet.copy(mode = Mode.AWAKE, message = "Я прокинувся! Привіт! ☀️")
            Action.HIBERNATE -> pet.copy(mode = Mode.HIBERNATION, message = "Я чекатиму на тебе у сплячці. ❄️")
            Action.PLAY_WIN -> pet.copy(
                happiness = gain(pet.happiness, 18.0), energy = gain(pet.energy, -6.0),
                miniGameWins = pet.miniGameWins + 1, message = "Ого, ми виграли! ✨"
            )
        }
        val withCare = PetGrowth.recordCare(updated, action, now, pet.mode == Mode.HIBERNATION)
        val stageBefore = PetGrowth.stage(pet)
        val stageAfter = PetGrowth.stage(withCare)
        return withCare.copy(
            lastUpdatedAt = now,
            dangerMinutes = if (withCare.hunger > 0 && withCare.thirst > 0) 0 else withCare.dangerMinutes,
            message = if (stageAfter != stageBefore) "✨ ${stageAfter.title}! Я переходжу на новий етап життя!" else withCare.message
        )
    }
}
