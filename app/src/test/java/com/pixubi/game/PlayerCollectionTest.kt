package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlayerCollectionTest {
    private fun pet(species: Species = Species.LISYK) =
        GameEngine.hatch(species, Sex.GIRL, species.native, "Малюк", 100_000L, Random(7))

    @Test fun eggUnlocksOnlyEggAndBabyIsPreviewNotAchievement() {
        val p = pet()
        val c = CollectionRules.observe(PlayerCollection(startedAt=1L), p)
        assertEquals(1,c.unlockedCount)
        assertFalse(CollectionRules.hasForm(c,p.species,p.sex,p.colorIndex,GrowthStage.EGG))
        assertTrue(CollectionRules.hasForm(c,p.species,p.sex,p.colorIndex,GrowthStage.BABY))
        assertTrue(CollectionRules.canPreviewCurrent(c,p,GrowthStage.BABY))
        assertFalse(CollectionRules.canPreviewCurrent(c,p,GrowthStage.YOUNG))
        assertFalse(CollectionRules.canPreviewCurrent(c,p,GrowthStage.ADULT))
    }

    @Test fun elderAndAdultStaySecretUntilEarned() {
        val p = pet()
        val c = CollectionRules.observe(PlayerCollection(), p.copy(xp=40,activeMinutes=1_440L))
        assertTrue(CollectionRules.hasForm(c,p.species,p.sex,p.colorIndex,GrowthStage.YOUNG))
        assertFalse(CollectionRules.hasForm(c,p.species,p.sex,p.colorIndex,GrowthStage.COCOON))
        assertFalse(CollectionRules.hasForm(c,p.species,p.sex,p.colorIndex,GrowthStage.ADULT))
    }

    @Test fun reachingAdultUnlocksAllPreviousForms() {
        val p = pet()
        val c = CollectionRules.observe(PlayerCollection(),p.copy(xp=175,activeMinutes=5_760L))
        assertEquals(3,c.unlockedCount)
        assertEquals(1,c.grownAdults)
        assertEquals(1,c.totalRaised)
    }

    @Test fun archiveSurvivesNewPetsAndDuplicateObservations() {
        val a = pet()
        val b = GameEngine.hatch(Species.ISKRYK,Sex.BOY,Region.MOUNTAINS,"Новий",200_000L,Random(4))
        var c = CollectionRules.observe(PlayerCollection(), a)
        c = CollectionRules.observe(c,a)
        assertEquals(1,c.totalRaised)
        c = CollectionRules.observe(c,b)
        assertEquals(2,c.totalRaised)
        assertEquals(2,c.knownSpecies)
        val oldKeys = c.discovered
        c = CollectionRules.observe(c,b.copy(activeMinutes=1_440L,xp=40))
        assertTrue(c.discovered.containsAll(oldKeys))
        assertEquals(2,c.totalRaised)
    }

    @Test fun visibleStagesDoNotLeakAcrossSpeciesOrColors() {
        val a = pet()
        val c = CollectionRules.observe(PlayerCollection(),a.copy(activeMinutes=5_760L,xp=175))
        assertFalse(CollectionRules.hasForm(c,Species.ISKRYK,a.sex,a.colorIndex,GrowthStage.ADULT))
        assertFalse(CollectionRules.hasForm(c,a.species,a.sex,(a.colorIndex+1)%6,GrowthStage.ADULT))
    }

    @Test fun bestStreakAndStatsUsePermanentRecords() {
        val p = pet().copy(xp=200,careStreak=9,activeMinutes=5_760L,miniGameWins=3)
        val a=CollectionRules.observe(PlayerCollection(),p)
        val b=CollectionRules.observe(a,p.copy(careStreak=1,miniGameWins=4,alive=false))
        assertEquals(9,b.bestStreak)
        assertEquals(4,b.totalMiniGameWins)
        assertEquals(1,b.grownAdults)
        assertEquals(1,b.totalRaised)
        assertTrue(b.lifeDays >= 4L)
    }
}
