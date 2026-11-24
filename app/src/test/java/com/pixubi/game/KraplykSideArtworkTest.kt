package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class KraplykSideArtworkTest {
    @Test fun dedicatedSideWalkOnly() {
        assertTrue(KraplykSideArtwork.matches(Species.KRAPLYK,GrowthStage.BABY))
        assertFalse(KraplykSideArtwork.matches(Species.KRAPLYK,GrowthStage.YOUNG))
        for (c in SpriteClip.entries) assertEquals(c==SpriteClip.WALK,KraplykSideArtwork.usesSide(c))
        assertEquals(0,KraplykSideArtwork.index(4))
        assertEquals(3,KraplykSideArtwork.index(-1))
    }
    @Test fun fourDistinctGaitsAndGroundContact() {
        val feet=(0..3).map{KraplykSideArtwork.feet(it)}
        for (i in feet.indices) {
            assertEquals(8,feet[i].size)
            assertTrue((1..7 step 2).count{feet[i][it]==0}>=2)
            for(j in 0 until i)assertFalse(feet[i].contentEquals(feet[j]))
        }
        assertTrue(feet[0][0]>0 && feet[2][0]<0)
        assertTrue(feet[0][6]>0 && feet[2][6]<0)
    }
}
