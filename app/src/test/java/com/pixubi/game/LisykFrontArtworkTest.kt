package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LisykFrontArtworkTest {
    @Test fun distinctIllustratedExpressionsWithTransparentMargins() {
        val img=ImageIO.read(File("src/main/assets/lisyk_baby_front_expressions.png"))
        assertEquals(768,img.width)
        assertEquals(384,img.height)
        val frames=(0..7).map{i->img.getRGB(i%4*192,i/4*192,192,192,null,0,192)}
        for(i in frames.indices) {
            assertTrue(frames[i].count{it ushr 24>0}>2000)
            assertTrue(frames[i].sliceArray(182*192 until 192*192).all{it ushr 24==0})
            for(j in 0 until i)assertFalse(frames[i].contentEquals(frames[j]))
        }
    }
    @Test fun faceSelectionMatchesEmotionAndMovement() {
        assertEquals(0,LisykFrontArtwork.index(PetEmotion.IDLE,0,false))
        assertEquals(1,LisykFrontArtwork.index(PetEmotion.IDLE,0,true))
        assertEquals(2,LisykFrontArtwork.index(PetEmotion.HAPPY,1,false))
        assertEquals(3,LisykFrontArtwork.index(PetEmotion.SAD,0,false))
        assertEquals(4,LisykFrontArtwork.index(PetEmotion.TIRED,0,false))
        assertEquals(5,LisykFrontArtwork.index(PetEmotion.SURPRISED,0,false))
        assertEquals(6,LisykFrontArtwork.index(PetEmotion.EATING,1,false))
        assertEquals(7,LisykFrontArtwork.index(PetEmotion.DRINKING,1,false))
        for(c in listOf(SpriteClip.WALK,SpriteClip.DIG,SpriteClip.SLEEP))assertFalse(LisykFrontArtwork.usesFront(c))
        assertTrue(LisykFrontArtwork.usesFront(SpriteClip.IDLE))
        assertTrue(LisykFrontArtwork.usesFront(SpriteClip.HAPPY))
    }
}
