package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class KraplykBabyArtworkTest {
    @Test fun distinctWalkingAndSwimmingFrames() {
        assertEquals(listOf(0,1,2,3),(0..3).map{KraplykBabyArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,it,false)})
        assertEquals(listOf(4,5,6,7),(0..3).map{KraplykBabyArtwork.index(SpriteClip.SWIM,PetEmotion.IDLE,it,false)})
        assertTrue(KraplykBabyArtwork.locomotion(SpriteClip.WALK))
        assertTrue(KraplykBabyArtwork.locomotion(SpriteClip.SWIM))
        assertFalse(KraplykBabyArtwork.locomotion(SpriteClip.IDLE))
    }
    @Test fun faceAndSleepStayInSameAtlas() {
        assertEquals(8,KraplykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,false))
        assertEquals(9,KraplykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
        assertEquals(9,KraplykBabyArtwork.index(SpriteClip.SLEEP,PetEmotion.SLEEPING,0,false))
        assertEquals(14,KraplykBabyArtwork.index(SpriteClip.EAT,PetEmotion.EATING,1,false))
        assertEquals(15,KraplykBabyArtwork.index(SpriteClip.DRINK,PetEmotion.DRINKING,1,false))
        assertTrue(KraplykBabyArtwork.matches(Species.KRAPLYK,GrowthStage.BABY))
        assertFalse(KraplykBabyArtwork.matches(Species.KRAPLYK,GrowthStage.YOUNG))
    }
    @Test fun transparentAtlasHasSixteenNonemptyCells() {
        val file=listOf(File("src/main/assets/"+KraplykBabyArtwork.ASSET),File("app/src/main/assets/"+KraplykBabyArtwork.ASSET)).first{it.isFile}
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
