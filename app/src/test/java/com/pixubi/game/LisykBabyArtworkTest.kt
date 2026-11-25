package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LisykBabyArtworkTest {
    @Test fun actualApprovedSheetContainsEightDistinctTransparentPoses() {
        val img=ImageIO.read(File("src/main/assets/lisyk_baby_pixel_poses.png"))
        assertEquals(768,img.width);assertEquals(384,img.height)
        val frames=(0..7).map{i->img.getRGB(i%4*192,i/4*192,192,192,null,0,192)}
        for(i in frames.indices) {
            val f=frames[i]
            assertTrue(f.count{it ushr 24>0}>2000)
            assertEquals(0,f[0] ushr 24)
            assertTrue(f.sliceArray(182*192 until 192*192).all{it ushr 24==0})
            for(j in 0 until i)assertFalse(f.contentEquals(frames[j]))
        }
        for(i in 0..3)assertEquals(i,LisykBabyArtwork.cell(SpriteClip.WALK,i))
        assertEquals(listOf(6,6,6,0),(0..3).map{LisykBabyArtwork.cell(SpriteClip.DIG,it)})
        assertEquals(7,LisykBabyArtwork.cell(SpriteClip.SLEEP,0))
        assertEquals(4,LisykBabyArtwork.cell(SpriteClip.IDLE,0))
        assertTrue(LisykBabyArtwork.matches(Species.LISYK,GrowthStage.BABY))
        assertFalse(LisykBabyArtwork.matches(Species.LISYK,GrowthStage.ADULT))
        for(i in 0..7)assertTrue(LisykBabyArtwork.sockets(i).all{it.point.x in 0f..191f&&it.point.y in 0f..191f})
    }
}
