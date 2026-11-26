package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class RegionalAudioTest {
    @Test fun allFiveRegionsHaveSixteenStepOriginalMelodies() {
        assertEquals(5, Region.entries.size)
        for (region in Region.entries) {
            val theme = RegionMelodies.forRegion(region)
            assertEquals(16, theme.notes.size)
            assertEquals(4, theme.bass.size)
            assertTrue(theme.bpm in 60..130)
            assertTrue(theme.notes.any { it >= 0 })
        }
    }

    @Test fun regionMotifsAreDifferent() {
        val motifs = Region.entries.map { RegionMelodies.forRegion(it).notes.toList() }
        assertEquals(5, motifs.toSet().size)
    }

    @Test fun instrumentalScoreHasLongDistinctScenes() {
        val base=PixubiScore.render(Region.FOREST,MusicScene.BASE,8000)
        val battle=PixubiScore.render(Region.FOREST,MusicScene.BATTLE,8000)
        assertTrue(base.size>40*8000)
        assertTrue(battle.size>25*8000)
        assertTrue(base.any{it>1000})
        assertTrue(battle.any{it< -1000})
        assertNotEquals(base.size,battle.size)
        assertTrue(base.all{it.toInt() in -30000..30000})
        assertTrue(battle.all{it.toInt() in -30000..30000})
    }

    @Test fun soundSettingsDefaultToOnAndGentleVolume() {
        val settings = SoundSettings()
        assertTrue(settings.music)
        assertTrue(settings.effects)
        assertTrue(settings.musicVolume in 0.1f..0.5f)
        assertTrue(settings.effectsVolume in 0.1f..0.8f)
    }
}
