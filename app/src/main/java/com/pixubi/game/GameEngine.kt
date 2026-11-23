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
enum class Action { FEED, WATER, CHARGE, EXPLORE, SLEEP, WAKE, HIBERNATE, PLAY_WIN }

enum class ShopItem(val title: String, val icon: String, val price: Int, val description: String) {
    FOOD("Їжа ×3", "🍓", 12, "Три порції для тарілочки"),
    WATER("Вода ×3", "💧", 10, "Три наповнення стаканчика"),
    CELLS("Заряд ×2", "🔋", 14, "Два елементи заряду"),
    HOME("Будиночок", "🏡", 55, "Затишний сон відновлює більше сил"),
    BOWL("Тарілочка", "🍽️", 18, "Більше ситості від кожної порції"),
    CUP("Стаканчик", "🥤", 18, "Більше користі від води"),
    CHARGER("Зарядна станція", "⚡", 65, "Підсилює кожен заряд")
}


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
    val message: String = "Ура! Я з'явився на світ! 💜",
    val coins: Int = 30,
    val foodStock: Int = 4,
    val waterStock: Int = 4,
    val chargeStock: Int = 2,
    val hasHome: Boolean = false,
    val hasBowl: Boolean = false,
    val hasCup: Boolean = false,
    val hasCharger: Boolean = false,
    val expeditionStartedAt: Long = 0L,
    val expeditionsDone: Int = 0,
    val expeditionDay: Long = -1L,
    val expeditionToday: Int = 0,
    val rewardDay: Long = -1L,
    val rewardCoinsToday: Int = 0
) {
    val colorIndex: Int get() = geneA % 6
    val daysOld: Long get() = activeMinutes / (24 * 60)
}

object GameEngine {
    // One real day outside hibernation counts as one day of life.
    const val NATURAL_LIFE_DAYS = 365L
    const val CRITICAL_LIMIT_MINUTES = 24L * 60
    private const val HOUR_MINUTES = 60.0
    const val OFFLINE_LIMIT_MINUTES = 720L
    const val EXPEDITION_MINUTES = 20L
    const val EXPEDITIONS_PER_DAY = 4
    const val GAME_COINS_PER_DAY = 30

    /** Time is reconciled when the app reopens. No background service or battery drain. */
    fun restoreAfterAbsence(source: Pet, now: Long): Pet {
        if (now <= source.lastUpdatedAt) return source
        val maxTime = source.lastUpdatedAt + OFFLINE_LIMIT_MINUTES * 60_000L
        val progressed = advance(source, min(now, maxTime))
        return progressed.copy(lastUpdatedAt = now)
    }

    private fun finishExpedition(pet: Pet, now: Long): Pet {
        if (!pet.alive) return pet
        if (pet.expeditionStartedAt <= 0L ||
            now - pet.expeditionStartedAt < EXPEDITION_MINUTES * 60_000L) return pet
        val completed = pet.expeditionsDone
        return pet.copy(
            expeditionStartedAt = 0L,
            expeditionsDone = completed + 1,
            coins = (pet.coins + 8).coerceAtMost(9999),
            foodStock = (pet.foodStock + if (completed % 3 == 0) 1 else 0).coerceAtMost(30),
            waterStock = (pet.waterStock + if (completed % 3 == 1) 1 else 0).coerceAtMost(30),
            chargeStock = (pet.chargeStock + if (completed % 3 == 2) 1 else 0).coerceAtMost(30),
            xp = (pet.xp.toLong() + 4L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            message = "🧭 Повернувся з прогулянки! +8 🪙 і корисна знахідка."
        )
    }

    fun buy(source: Pet, item: ShopItem, now: Long): Pet {
        val pet = advance(source, now)
        if (!pet.alive) return pet
        val owned = when (item) {
            ShopItem.HOME -> pet.hasHome
            ShopItem.BOWL -> pet.hasBowl
            ShopItem.CUP -> pet.hasCup
            ShopItem.CHARGER -> pet.hasCharger
            else -> false
        }
        if (owned) return pet.copy(message = "Цей предмет у мене вже є! 💜")
        val full = when (item) {
            ShopItem.FOOD -> pet.foodStock > 27
            ShopItem.WATER -> pet.waterStock > 27
            ShopItem.CELLS -> pet.chargeStock > 28
            else -> false
        }
        if (full) return pet.copy(message = "Звільни місце в запасах перед покупкою.")
        if (pet.coins < item.price) return pet.copy(message = "Не вистачає монеток на ${item.title}. 🪙")
        val paid = pet.copy(coins = pet.coins - item.price, lastUpdatedAt = now)
        return when (item) {
            ShopItem.FOOD -> paid.copy(foodStock = (pet.foodStock + 3).coerceAtMost(30), message = "🍓 Їжа куплена!")
            ShopItem.WATER -> paid.copy(waterStock = (pet.waterStock + 3).coerceAtMost(30), message = "💧 Вода куплена!")
            ShopItem.CELLS -> paid.copy(chargeStock = (pet.chargeStock + 2).coerceAtMost(30), message = "🔋 Заряд поповнено!")
            ShopItem.HOME -> paid.copy(hasHome = true, message = "🏡 Мій новий будиночок!")
            ShopItem.BOWL -> paid.copy(hasBowl = true, message = "🍽️ Моя нова тарілочка!")
            ShopItem.CUP -> paid.copy(hasCup = true, message = "🥤 Мій новий стаканчик!")
            ShopItem.CHARGER -> paid.copy(hasCharger = true, message = "⚡ Зарядна станція встановлена!")
        }
    }

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
            val tired = (pet.energy + (if (sleeping) (if (pet.hasHome) 15.0 else 11.0) else -1.35) * h).coerceIn(0.0, 100.0)
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
        val calculatedTime = source.lastUpdatedAt + accountedMinutes * 60_000L
        return finishExpedition(pet.copy(lastUpdatedAt = calculatedTime), calculatedTime)
    }

    fun act(source: Pet, action: Action, now: Long): Pet {
        val pet = advance(source, now)
        if (!pet.alive) return pet
        // Reset the clock exactly when modes change, so elapsed fractions do not leak.
        if (pet.mode == Mode.HIBERNATION && action != Action.WAKE) return pet
        if (pet.mode == Mode.SLEEP && action !in listOf(Action.WAKE, Action.HIBERNATE)) return pet
        fun gain(value: Double, points: Double) = (value + points).coerceIn(0.0, 100.0)
        if (action == Action.FEED && pet.foodStock <= 0)
            return pet.copy(lastUpdatedAt = now, message = "🍓 Їжа закінчилася! Купи або знайди ще.")
        if (action == Action.WATER && pet.waterStock <= 0)
            return pet.copy(lastUpdatedAt = now, message = "💧 Вода закінчилася! Поповни запаси.")
        if (action == Action.CHARGE && pet.chargeStock <= 0)
            return pet.copy(lastUpdatedAt = now, message = "🔋 Закінчилися елементи заряду.")
        if (action == Action.EXPLORE) {
            val today = PetGrowth.careDay(now)
            val used = if (today == pet.expeditionDay) pet.expeditionToday else 0
            if (pet.expeditionStartedAt > 0L)
                return pet.copy(lastUpdatedAt = now, message = "🧭 Я ще досліджую світ!")
            if (used >= EXPEDITIONS_PER_DAY)
                return pet.copy(lastUpdatedAt = now, message = "🧭 Сьогодні я вже добре погуляв. Завтра знову!")
            if (PetGrowth.stage(pet) in listOf(GrowthStage.EGG, GrowthStage.COCOON))
                return pet.copy(lastUpdatedAt = now, message = "🥚 Для прогулянки я маю вилупитися!")
            if (pet.energy < 15.0)
                return pet.copy(lastUpdatedAt = now, message = "💤 Спочатку відпочину перед прогулянкою.")
        }
        val updated = when (action) {
            Action.FEED -> pet.copy(foodStock = pet.foodStock - 1, hunger = gain(pet.hunger, if (pet.hasBowl) 32.0 else 24.0),
                message = if (PetGrowth.stage(pet) in listOf(GrowthStage.EGG, GrowthStage.COCOON))
                    "Моя цифрова оболонка отримала живлення! ⚡" else "Ням-ням! Смачно! 🍓")
            Action.WATER -> pet.copy(waterStock = pet.waterStock - 1, thirst = gain(pet.thirst, if (pet.hasCup) 40.0 else 30.0),
                message = if (PetGrowth.stage(pet) in listOf(GrowthStage.EGG, GrowthStage.COCOON))
                    "Цифрова оболонка зволожена! 💧" else "Водичка — це життя! 💧")
            Action.CHARGE -> pet.copy(chargeStock = pet.chargeStock - 1,
                energy = gain(pet.energy, if (pet.hasCharger) 55.0 else 32.0),
                message = "⚡ Енергію відновлено!")
            Action.EXPLORE -> {
                val today = PetGrowth.careDay(now)
                pet.copy(expeditionStartedAt = now,
                    expeditionDay = today,
                    expeditionToday = (if (today == pet.expeditionDay) pet.expeditionToday else 0) + 1,
                    energy = gain(pet.energy, -8.0),
                    message = "🧭 Вирушаю на 20 хвилин шукати монетки та ресурси!")
            }
            Action.SLEEP -> pet.copy(mode = Mode.SLEEP, message = "На добраніч! 🌙")
            Action.WAKE -> pet.copy(mode = Mode.AWAKE, message = "Я прокинувся! Привіт! ☀️")
            Action.HIBERNATE -> pet.copy(mode = Mode.HIBERNATION, expeditionStartedAt = 0L,
                message = "Я чекатиму на тебе у сплячці. ❄️")
            Action.PLAY_WIN -> pet.copy(
                happiness = gain(pet.happiness, 18.0), energy = gain(pet.energy, -6.0),
                miniGameWins = pet.miniGameWins + 1,
                coins = (pet.coins + min(6, (GAME_COINS_PER_DAY -
                    (if (PetGrowth.careDay(now) == pet.rewardDay) pet.rewardCoinsToday else 0)).coerceAtLeast(0))).coerceAtMost(9999),
                rewardDay = PetGrowth.careDay(now),
                rewardCoinsToday = ((if (PetGrowth.careDay(now) == pet.rewardDay)
                    pet.rewardCoinsToday else 0) + 6).coerceAtMost(GAME_COINS_PER_DAY),
                message = "Виграли! ✨ +монетки (до 30 за ігри на день)."

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
