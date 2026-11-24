package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class IskrykBabyArtworkTest {
    @Test fun sixteenDistinctAlignedTransparentCells() {
        val atlas=ImageIO.read(File("src/main/assets/iskryk_baby_art.png"))
        assertEquals(768,atlas.width);assertEquals(768,atlas.height)
        val frames=(0..15).map{i->atlas.getRGB(i%4*192,i/4*192,192,192,null,0,192)}
        for(i in frames.indices){
            val f=frames[i]
            assertTrue(f.count{it ushr 24>0}>2000)
            assertTrue(f.sliceArray(182*192 until 192*192).all{it ushr 24==0})
            for(j in 0 until i)assertFalse(f.contentEquals(frames[j]))
        }
        // The airborne image must lift off the shared planted baseline.
        fun bottom(f:IntArray)=f.indexOfLast{it ushr 24!=0}/192
        assertTrue(bottom(frames[5])<bottom(frames[0])-10)
    }
    @Test fun movementSleepAndExpressionsUseCorrectArtwork() {
        assertTrue(IskrykBabyArtwork.matches(Species.ISKRYK,GrowthStage.BABY))
        assertFalse(IskrykBabyArtwork.matches(Species.LISYK,GrowthStage.BABY))
        assertFalse(IskrykBabyArtwork.matches(Species.ISKRYK,GrowthStage.ADULT))
        for(i in 0..3)assertEquals(i,IskrykBabyArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,i,false))
        assertEquals(listOf(0,5,5,3),(0..3).map{IskrykBabyArtwork.index(SpriteClip.HOP,PetEmotion.IDLE,it,false)})
        assertEquals(6,IskrykBabyArtwork.index(SpriteClip.DIG,PetEmotion.IDLE,0,false))
        assertEquals(7,IskrykBabyArtwork.index(SpriteClip.SLEEP,PetEmotion.HIBERNATING,0,false))
        assertEquals(9,IskrykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
        assertEquals(11,IskrykBabyArtwork.index(SpriteClip.SAD,PetEmotion.SAD,0,false))
        assertEquals(14,IskrykBabyArtwork.index(SpriteClip.EAT,PetEmotion.EATING,1,false))
        assertEquals(15,IskrykBabyArtwork.index(SpriteClip.DRINK,PetEmotion.DRINKING,1,false))
    }
}
