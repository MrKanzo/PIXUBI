package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PixubiSearchTest {
    @Test fun eachSpeciesHasDistinctSearchStyle() {
        val sample = Species.entries.map { PixubiSearch.pose(it, .42f) }
        assertEquals(Species.entries.size, sample.distinct().size)
        assertEquals(Species.entries.size,
            Species.entries.map { PixubiSearch.activity(it) }.distinct().size)
        assertTrue(PixubiSearch.pose(Species.PISHCHANYK, .42f).submerged > .9f)
        assertTrue(PixubiSearch.pose(Species.KHMARYK, .42f).yDp < 0f)
        assertTrue(PixubiSearch.pose(Species.ISKRYK, .42f).yDp < 0f)
    }

    @Test fun searchCycleLoopsWithoutJumpsAndReturnsToSurface() {
        for (species in Species.entries) {
            assertEquals(PixubiSearch.pose(species, 0f),
                PixubiSearch.pose(species, 1f))
        }
        assertEquals(0f, PixubiSearch.pose(Species.PISHCHANYK, .05f).submerged)
        assertEquals(0f, PixubiSearch.pose(Species.PISHCHANYK, .95f).submerged)
    }

    @Test fun treasureRollDoesNotDependOnFrameOrWallClock() {
        val p = GameEngine.hatch(
            Species.PISHCHANYK, Sex.GIRL, Region.DESERT, "Піска", 12345L, Random(9)
        ).copy(activeMinutes = 15)
        assertEquals(PixubiSearch.foundRareTreasure(p), PixubiSearch.foundRareTreasure(p))
        assertTrue(!PixubiSearch.foundRareTreasure(p))
    }
}
