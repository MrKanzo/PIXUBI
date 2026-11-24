package com.pixubi.game

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class KraplykLandFramesTest {
    @Test fun babyOnlyAndFrameBounds() {
        assertTrue(KraplykLandFrames.matches(Species.KRAPLYK,GrowthStage.BABY))
        assertFalse(KraplykLandFrames.matches(Species.KRAPLYK,GrowthStage.YOUNG))
        assertFalse(KraplykLandFrames.matches(Species.LISYK,GrowthStage.BABY))
        for (clip in SpriteClip.entries) for (e in PetEmotion.entries) {
            for (i in 0 until clip.count) for (blink in listOf(false,true)) {
                val actual=KraplykLandFrames.faceEmotion(clip,e)
                val idx=KraplykLandFrames.faceIndex(clip,e,i,.3f,blink)
                assertTrue(idx in 0 until (if(FaceExpression.animatedMouth(actual)) 10 else 5))
            }
        }
        assertEquals(4,KraplykLandFrames.faceIndex(SpriteClip.SLEEP,PetEmotion.IDLE,0,0f,false))
    }

    @Test fun realArtKeepsFaceAndRestChangesFeet() {
        val n=KraplykLandFrames.SIZE
        val atlas=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val source=atlas.getRGB(2*n,0,n,n,null,0,n)
        assertArrayEquals(source,KraplykLandFrames.pose(source,SpriteClip.IDLE,0))
        for (clip in SpriteClip.entries) for(i in 0 until clip.count) {
            val out=KraplykLandFrames.pose(source,clip,i)
            assertEquals(n*n,out.size)
            for(y in 65..115) for(x in 50..140) assertEquals(source[y*n+x],out[y*n+x])
            assertTrue(out.count{it ushr 24!=0}>2000)
        }
        assertFalse(source.contentEquals(KraplykLandFrames.pose(source,SpriteClip.WALK,1)))
        assertFalse(source.contentEquals(KraplykLandFrames.pose(source,SpriteClip.SLEEP,0)))
    }
}
