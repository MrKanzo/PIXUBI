package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class PishchanykBabyArtworkTest {
    @Test fun distinctWalkingAndDiggingFrames() {
        assertEquals(listOf(0,1,2,3),(0..3).map{PishchanykBabyArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,it,false)})
        assertEquals(listOf(4,5,6,7),(0..3).map{PishchanykBabyArtwork.index(SpriteClip.DIG,PetEmotion.IDLE,it,false)})
        assertTrue(PishchanykBabyArtwork.locomotion(SpriteClip.WALK))
        assertTrue(PishchanykBabyArtwork.locomotion(SpriteClip.DIG))
        assertFalse(PishchanykBabyArtwork.locomotion(SpriteClip.IDLE))
    }
    @Test fun searchApproachesBeforeDigging() {
        assertEquals(PetMotion.WALK,PishchanykBabyArtwork.searchMotion(SearchPhase.WANDERING))
        assertEquals(PetMotion.DIG,PishchanykBabyArtwork.searchMotion(SearchPhase.HIDING))
        assertEquals(PetMotion.DIG,PishchanykBabyArtwork.searchMotion(SearchPhase.EMERGING))
    }
    @Test fun faceAndSleepStayInSameAtlas() {
        assertEquals(8,PishchanykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,false))
        assertEquals(9,PishchanykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
        assertEquals(9,PishchanykBabyArtwork.index(SpriteClip.SLEEP,PetEmotion.SLEEPING,0,false))
        assertEquals(14,PishchanykBabyArtwork.index(SpriteClip.EAT,PetEmotion.EATING,1,false))
        assertEquals(15,PishchanykBabyArtwork.index(SpriteClip.DRINK,PetEmotion.DRINKING,1,false))
        assertTrue(PishchanykBabyArtwork.matches(Species.PISHCHANYK,GrowthStage.BABY))
        assertFalse(PishchanykBabyArtwork.matches(Species.PISHCHANYK,GrowthStage.YOUNG))
    }
    @Test fun transparentAtlasHasSixteenNonemptyCells() {
        val file=listOf(File("src/main/assets/"+PishchanykBabyArtwork.ASSET),File("app/src/main/assets/"+PishchanykBabyArtwork.ASSET)).first{it.isFile}
        val image=ImageIO.read(file)
        assertEquals(768,image.width)
        assertEquals(768,image.height)
        assertTrue(image.colorModel.hasAlpha())
        for(i in 0..15) {
            val cell=image.getSubimage(i%4*192,i/4*192,192,192)
            assertEquals(0,cell.getRGB(0,0) ushr 24)
            assertTrue((0 until 192).any{y->(0 until 192).any{x->cell.getRGB(x,y) ushr 24 > 0}})
        }
    }
}
