package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LisykYoungArtworkTest {
    @Test fun correctStageAndActions() {
        assertTrue(LisykYoungArtwork.matches(Species.LISYK,GrowthStage.YOUNG))
        assertFalse(LisykYoungArtwork.matches(Species.LISYK,GrowthStage.BABY))
        assertFalse(LisykYoungArtwork.matches(Species.LISYK,GrowthStage.ADULT))
        assertEquals(listOf(0,1,2,3),(0..3).map{LisykYoungArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,it,false)})
        assertEquals(6,LisykYoungArtwork.index(SpriteClip.HOP,PetEmotion.IDLE,1,false))
        assertEquals(4,LisykYoungArtwork.index(SpriteClip.DIG,PetEmotion.IDLE,0,false))
        assertEquals(5,LisykYoungArtwork.index(SpriteClip.DIG,PetEmotion.IDLE,1,false))
        assertEquals(7,LisykYoungArtwork.index(SpriteClip.SLEEP,PetEmotion.SLEEPING,0,false))
        assertEquals(9,LisykYoungArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
    }
    @Test fun atlasHasSixteenTransparentCells() {
        val file=listOf(File("src/main/assets/"+LisykYoungArtwork.ASSET),File("app/src/main/assets/"+LisykYoungArtwork.ASSET)).first{it.isFile}
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
