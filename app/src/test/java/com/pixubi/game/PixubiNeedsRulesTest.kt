package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PixubiNeedsRulesTest {
    private val now = 1_795_440_000_000L
    private val enabled = PixubiNotificationSettings(enabled = true)

    private fun pet(sex: Sex = Sex.GIRL): Pet =
        GameEngine.hatch(Species.LISYK, sex, Region.FOREST, "Пуха", now, Random(7))
            .copy(activeMinutes = 1500, xp = 100)

    @Test fun quietHoursRespectTheLocalClock() {
        assertTrue(PixubiNeedRules.isQuietHour(23))
        assertTrue(PixubiNeedRules.isQuietHour(0))
        assertTrue(PixubiNeedRules.isQuietHour(7))
        assertFalse(PixubiNeedRules.isQuietHour(8))
        assertFalse(PixubiNeedRules.isQuietHour(21))
    }

    @Test fun notificationsAreOptInAndPausedWhenPetHibernatesOrDies() {
        val needy = pet().copy(hunger = 20.0, thirst = 20.0, energy = 20.0, happiness = 20.0)
        assertTrue(PixubiNeedRules.findNeeds(needy, PixubiNotificationSettings()).isEmpty())
        assertTrue(PixubiNeedRules.findNeeds(needy.copy(mode = Mode.HIBERNATION), enabled).isEmpty())
        assertTrue(PixubiNeedRules.findNeeds(needy.copy(alive = false), enabled).isEmpty())
    }

    @Test fun urgentNeedsAreOrderedWaterFoodSleepPlay() {
        val needy = pet().copy(hunger = 20.0, thirst = 20.0, energy = 20.0, happiness = 20.0)
        assertEquals(
            listOf(PixubiNeed.WATER, PixubiNeed.FOOD, PixubiNeed.SLEEP, PixubiNeed.PLAY),
            PixubiNeedRules.findNeeds(needy, enabled).map { it.need }
        )
    }

    @Test fun playerCanDisableIndividualNeedsAndSleepingPetIsNotAskedToPlay() {
        val needy = pet().copy(hunger = 20.0, thirst = 20.0, energy = 20.0, happiness = 20.0)
        val prefs = enabled.copy(water = false, food = false, sleep = false)
        assertEquals(listOf(PixubiNeed.PLAY), PixubiNeedRules.findNeeds(needy, prefs).map { it.need })
        val asleep = PixubiNeedRules.findNeeds(needy.copy(mode = Mode.SLEEP), enabled)
        assertEquals(listOf(PixubiNeed.WATER, PixubiNeed.FOOD), asleep.map { it.need })
    }

    @Test fun femaleAndMaleGetMatchingPhrases() {
        val girl = PixubiNeedRules.findNeeds(pet().copy(hunger = 10.0), enabled)
        val boy = PixubiNeedRules.findNeeds(pet(Sex.BOY).copy(hunger = 10.0), enabled)
        assertTrue(girl.first().title.contains("зголодніла"))
        assertTrue(boy.first().title.contains("зголоднів"))
    }

    @Test fun socialReminderCanArriveAfterSixHoursApartEvenIfPetIsHappy() {
        val happyPet = pet().copy(happiness = 95.0, energy = 90.0)
        assertTrue(PixubiNeedRules.findNeeds(happyPet, enabled, 60L).isEmpty())
        val missed = PixubiNeedRules.findNeeds(happyPet, enabled, 6 * 60L)
        assertEquals(PixubiNeed.PLAY, missed.single().need)
        assertTrue(missed.single().message.contains("скучив"))
    }

    @Test fun bothBabyAndYoungCanAskToPlay() {
        val baby = pet().copy(activeMinutes = 0, xp = 0, happiness = 10.0)
        val young = pet().copy(activeMinutes = 4320, xp = 120, happiness = 10.0)
        assertTrue(PixubiNeedRules.findNeeds(baby, enabled).any { it.need == PixubiNeed.PLAY })
        assertTrue(PixubiNeedRules.findNeeds(young, enabled).any { it.need == PixubiNeed.PLAY })
    }
}
