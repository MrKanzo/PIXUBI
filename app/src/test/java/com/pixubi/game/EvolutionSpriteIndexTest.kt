package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvolutionSpriteIndexTest {
    @Test fun uniquePositionsForFiveBabiesAndFiveTeenagers() {
        val all = Species.entries.flatMap { species ->
            listOf(
                EvolutionSpriteIndex.slot(species, GrowthStage.BABY),
                EvolutionSpriteIndex.slot(species, GrowthStage.YOUNG)
            )
        }
        assertEquals(10, all.size)
        assertEquals((0..9).toSet(), all.toSet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun adultCannotLoadJuvenileAsset() {
        EvolutionSpriteIndex.slot(Species.LISYK, GrowthStage.ADULT)
    }

    @Test fun allSecretsRemainConcealedForFreshEgg() {
        val pet = GameEngine.hatch(Species.ISKRYK,Sex.GIRL,Region.MOUNTAINS,"Яєчко",0L)
        val library = CollectionRules.observe(PlayerCollection(), pet)
        for (stage in listOf(GrowthStage.YOUNG, GrowthStage.COCOON, GrowthStage.ADULT)) {
            assertTrue(!CollectionRules.canPreviewCurrent(library, pet, stage))
        }
    }
}
