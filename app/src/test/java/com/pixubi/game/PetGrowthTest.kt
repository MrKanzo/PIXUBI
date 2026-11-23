package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class PetGrowthTest {
    private val start = LocalDate.of(2026, 11, 23)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun egg(): Pet = GameEngine.hatch(
        Species.LISYK, Sex.GIRL, Region.FOREST, "Пух", start, Random(2)
    )

    @Test fun newPetStartsAsAnEggAndHasAColorGene() {
        val pet = egg()
        assertEquals(GrowthStage.EGG, PetGrowth.stage(pet))
        assertEquals(1, PetGrowth.level(pet.xp))
        assertTrue(pet.colorIndex in 0..5)
    }

    @Test fun allFiveStagesRequireBothAgeAndXp() {
        val pet = egg()
        assertEquals(GrowthStage.EGG, PetGrowth.stage(pet.copy(xp = 999)))
        assertEquals(GrowthStage.EGG, PetGrowth.stage(pet.copy(activeMinutes = 20, xp = 14)))
        assertEquals(GrowthStage.BABY, PetGrowth.stage(pet.copy(activeMinutes = 15, xp = 15)))
        assertEquals(GrowthStage.YOUNG, PetGrowth.stage(pet.copy(activeMinutes = 1440, xp = 40)))
        assertEquals(GrowthStage.COCOON, PetGrowth.stage(pet.copy(activeMinutes = 4320, xp = 120)))
        assertEquals(GrowthStage.ADULT, PetGrowth.stage(pet.copy(activeMinutes = 5760, xp = 175)))
    }

    @Test fun levelThresholdsAndProgressAreBounded() {
        assertEquals(15, PetGrowth.xpForLevel(2))
        assertEquals(40, PetGrowth.xpForLevel(3))
        assertEquals(120, PetGrowth.xpForLevel(5))
        assertEquals(175, PetGrowth.xpForLevel(6))
        assertEquals(2, PetGrowth.level(16))
        assertEquals(0f, PetGrowth.progressInLevel(egg()), 0.001f)
        assertEquals(1f, PetGrowth.progressInLevel(egg().copy(xp=Int.MAX_VALUE)), 0.001f)
    }

    @Test fun eatingGrantsXpAndCanHatchWithEnoughTime() {
        val first = egg()
        val result = GameEngine.act(first.copy(activeMinutes = 15), Action.FEED, start + 1000)
        assertEquals(3, result.xp)
        assertEquals(GrowthStage.EGG, PetGrowth.stage(result))
        val leveled = result.copy(xp = 14)
        val baby = GameEngine.act(leveled, Action.WATER, start + 2000)
        assertEquals(GrowthStage.BABY, PetGrowth.stage(baby))
        assertEquals(16, baby.xp)
    }

    @Test fun dailyCareXpCannotBeFarmedIndefinitely() {
        var pet = egg()
        repeat(25) { pet = PetGrowth.recordCare(pet, Action.PLAY_WIN, start, false) }
        assertEquals(PetGrowth.DAILY_CARE_XP_LIMIT, pet.xp)
        assertEquals(1, pet.careDays)
        assertEquals(1, pet.careStreak)
    }

    @Test fun activeDayGivesSurvivalRewardOnce() {
        assertEquals(0, PetGrowth.survivalBonus(1438, 1439))
        assertEquals(8, PetGrowth.survivalBonus(1439, 1440))
        assertEquals(0, PetGrowth.survivalBonus(1440, 1450))
        assertEquals(16, PetGrowth.survivalBonus(1430, 4320))
    }

    @Test fun hibernationFreezesAgeAndXpAcrossLongAbsence() {
        val base = egg()
        val frozen = GameEngine.act(base, Action.HIBERNATE, start + 1000)
        val checked = GameEngine.advance(frozen, start + 20 * 86_400_000L)
        assertEquals(0, checked.xp)
        assertEquals(0L, checked.activeMinutes)
        assertEquals(GrowthStage.EGG, PetGrowth.stage(checked))
        assertTrue(checked.alive)
        assertEquals(Mode.HIBERNATION, checked.mode)
    }

    @Test fun consecutiveCareDaysAndHibernationProtectStreak() {
        val day = 86_400_000L
        var pet = PetGrowth.recordCare(egg(), Action.FEED, start, false)
        pet = PetGrowth.recordCare(pet, Action.WATER, start, false)
        assertEquals(1, pet.careDays)
        pet = PetGrowth.recordCare(pet, Action.FEED, start + day, false)
        assertEquals(2, pet.careStreak)
        pet = PetGrowth.recordCare(pet, Action.WAKE, start + day * 6, true)
        assertEquals(3, pet.careStreak)
    }

    @Test fun adultLegacyPetsStayAdult() {
        val old = egg().copy(legacyAdult = true, xp = 400)
        assertEquals(GrowthStage.ADULT, PetGrowth.stage(old))
        assertEquals(null, PetGrowth.nextStage(old))
    }
}
