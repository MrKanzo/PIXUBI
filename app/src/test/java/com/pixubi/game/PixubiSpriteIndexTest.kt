package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixubiSpriteIndexTest {
    @Test fun allSixtyAppearancesHaveUniqueFrames() {
        val slots = buildList {
            for (species in Species.entries) {
                for (sex in Sex.entries) {
                    for (color in 0..5) {
                        add(PixubiSpriteIndex.position(species, sex, color))
                    }
                }
            }
        }
        assertEquals(60, slots.size)
        assertEquals(60, slots.toSet().size)
        assertEquals((0..59).toSet(), slots.toSet())
    }
    @Test fun sexAndGeneSelectIndependentRows() {
        assertEquals(0, PixubiSpriteIndex.row(Sex.BOY, 0))
        assertEquals(5, PixubiSpriteIndex.row(Sex.BOY, 5))
        assertEquals(6, PixubiSpriteIndex.row(Sex.GIRL, 0))
        assertEquals(11, PixubiSpriteIndex.row(Sex.GIRL, 5))
        assertEquals(11, PixubiSpriteIndex.row(Sex.GIRL, 15))
        assertEquals(0, PixubiSpriteIndex.row(Sex.BOY, -3))
    }
    @Test fun everySpeciesHasASeparateColumn() {
        assertEquals(5, Species.entries.map { PixubiSpriteIndex.column(it) }.toSet().size)
    }
}
