package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class KhmarykBabyArtworkTest {
    @Test fun distinctWalkingAndFlyingFrames() {
        assertEquals(listOf(0,1,2,3),(0..3).map{KhmarykBabyArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,it,false)})
        assertEquals(listOf(4,5,6,7),(0..3).map{KhmarykBabyArtwork.index(SpriteClip.FLY,PetEmotion.IDLE,it,false)})
        assertTrue(KhmarykBabyArtwork.locomotion(SpriteClip.WALK))
        assertTrue(KhmarykBabyArtwork.locomotion(SpriteClip.FLY))
        assertFalse(KhmarykBabyArtwork.locomotion(SpriteClip.IDLE))
    }
    @Test fun faceAndSleepStayInSameAtlas() {
        assertEquals(8,KhmarykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,false))
        assertEquals(9,KhmarykBabyArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
        assertEquals(9,KhmarykBabyArtwork.index(SpriteClip.SLEEP,PetEmotion.SLEEPING,0,false))
        assertEquals(14,KhmarykBabyArtwork.index(SpriteClip.EAT,PetEmotion.EATING,1,false))
        assertEquals(15,KhmarykBabyArtwork.index(SpriteClip.DRINK,PetEmotion.DRINKING,1,false))
        assertTrue(KhmarykBabyArtwork.matches(Species.KHMARYK,GrowthStage.BABY))
        assertFalse(KhmarykBabyArtwork.matches(Species.KHMARYK,GrowthStage.YOUNG))
    }
    @Test fun transparentAtlasHasSixteenNonemptyCells() {
        val file=listOf(File("src/main/assets/"+KhmarykBabyArtwork.ASSET),File("app/src/main/assets/"+KhmarykBabyArtwork.ASSET)).first{it.isFile}
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
