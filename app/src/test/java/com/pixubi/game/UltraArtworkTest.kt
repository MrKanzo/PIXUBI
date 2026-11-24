package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test

class UltraArtworkTest {
    @Test fun allFiveUltraSpritesAreDistinctAndTransparent() {
        val hashes=mutableSetOf<Int>()
        for(stage in listOf(GrowthStage.ADULT,GrowthStage.ULTRA))for(s in Species.entries){
            val pixels=UltraPixuArtwork.pixels(s,stage)
            assertEquals(48*48,pixels.size)
            assertTrue(pixels.count{it ushr 24>0}>300)
            assertTrue(pixels.any{it==0})
            assertTrue(hashes.add(pixels.contentHashCode()))
        }
        assertEquals(10,hashes.size)
    }
    @Test fun stageAndLegacyKeysAreCompatible(){
        assertEquals(listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT,GrowthStage.ULTRA),GrowthStage.playable)
        assertEquals("LISYK:BOY:0:BABY",CollectionRules.migrateKey("LISYK:BOY:0:EGG"))
        assertEquals("LISYK:BOY:0:YOUNG",CollectionRules.migrateKey("LISYK:BOY:0:COCOON"))
        assertEquals(240,CollectionRules.ALL_FORMS)
    }
}
