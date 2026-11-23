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
    @Test fun allSpeciesActuallyHideAndReappear() {
        for (species in Species.entries) {
            for (seed in 0..5) {
                val hidden = PixubiSearch.frame(species, .50f, seed)
                assertEquals(SearchPhase.HIDDEN, hidden.phase)
                assertEquals(0f, hidden.alpha, 0f)
                assertEquals(1f, hidden.concealment, 0f)
                val visible = PixubiSearch.frame(species, .98f, seed)
                assertEquals(SearchPhase.WANDERING, visible.phase)
                assertEquals(1f, visible.alpha, 0f)
            }
        }
    }

    @Test fun concealmentFadesSmoothlyWithoutSuddenTeleport() {
        for (species in Species.entries) {
            val seed = 3
            val hiding = PixubiSearch.frame(species, .37f, seed)
            assertEquals(SearchPhase.HIDING, hiding.phase)
            assertTrue(hiding.alpha in 0f..1f)
            val emerging = PixubiSearch.frame(species, .70f, seed)
            assertEquals(SearchPhase.EMERGING, emerging.phase)
            assertTrue(emerging.alpha in 0f..1f)
            val start = PixubiSearch.frame(species, 0f, seed)
            val end = PixubiSearch.frame(species, 3.9999f, seed)
            assertTrue(kotlin.math.abs(start.pose.xDp - end.pose.xDp) < .01f)
            assertEquals(1f, start.alpha, 0f)
            assertEquals(1f, end.alpha, 0f)
        }
    }

    @Test fun hideLocationsAlternateBetweenSearchScenes() {
        for (species in Species.entries) {
            val first = PixubiSearch.frame(species, .50f, 4)
            val second = PixubiSearch.frame(species, 1.50f, 4)
            assertEquals(-first.entryXDp, second.entryXDp, 0f)
            assertEquals(-first.exitXDp, second.exitXDp, 0f)
            assertTrue(first.coverXDp != second.coverXDp)
        }
    }

}
