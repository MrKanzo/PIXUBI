package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ReferenceOlderArtworkTest {
    private val atlas by lazy {
        val f=listOf(File("src/main/assets/"+ReferenceOlderArtwork.ASSET),
            File("app/src/main/assets/"+ReferenceOlderArtwork.ASSET)).first{it.isFile}
        ImageIO.read(f)
    }
    @Test fun allTenNewModelsAreNativeFullResolutionAndDistinct(){
        assertEquals(960,atlas.width);assertEquals(384,atlas.height)
        assertTrue(atlas.colorModel.hasAlpha())
        val hashes=mutableSetOf<Int>()
        for((r,g) in listOf(GrowthStage.ADULT,GrowthStage.ULTRA).withIndex())for(s in Species.entries){
            val px=atlas.getRGB(s.ordinal*192,r*192,192,192,null,0,192)
            val visible=px.filter{it ushr 24>=128}
            assertTrue("$s $g artwork missing",visible.size>10000)
            assertTrue("$s $g palette too small",visible.distinct().size>40)
            assertTrue(hashes.add(px.contentHashCode()))
            assertEquals(0,px[0] ushr 24)
            assertEquals(0,px[191] ushr 24)
            assertEquals(0,px[191*192] ushr 24)
            assertEquals(0,px.last() ushr 24)
            val points=ReferenceOlderFrames.geometry(s,g).eyes+ReferenceOlderFrames.geometry(s,g).mouth
            assertTrue(points.all{it.centerX in 0f..191f&&it.centerY in 0f..191f})
            val clip=when(s){Species.KRAPLYK->SpriteClip.SWIM;Species.KHMARYK->SpriteClip.FLY
                Species.PISHCHANYK->SpriteClip.DIG;else->SpriteClip.WALK}
            val a=ReferenceOlderFrames.frame(px,s,g,clip,1,PetEmotion.IDLE,0)
            assertFalse("$s $g movement identical",px.contentEquals(a))
            val blink=ReferenceOlderFrames.frame(px,s,g,SpriteClip.IDLE,0,PetEmotion.IDLE,4)
            assertFalse("$s $g blink identical",px.contentEquals(blink))
            assertTrue(a.count{it ushr 24>=128}>9000)
        }
        assertEquals(10,hashes.size)
    }
}
