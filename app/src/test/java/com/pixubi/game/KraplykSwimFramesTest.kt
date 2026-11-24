package com.pixubi.game

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class KraplykSwimFramesTest {
    @Test fun originalFacePreservedAndFourFinPosesAreDistinct() {
        val atlas=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val n=KraplykSwimFrames.SIZE
        val source=atlas.getRGB(2*n,0,n,n,null,0,n)
        val frames=(0..3).map{KraplykSwimFrames.pose(source,it)}
        assertArrayEquals(source,frames[0])
        for (i in 1..3) {
            for (j in 0 until i) assertFalse(frames[i].contentEquals(frames[j]))
            for(y in 0 until n) for(x in 43..145)
                assertEquals(source[y*n+x],frames[i][y*n+x])
            assertTrue(frames[i].count{it ushr 24!=0}>2000)
        }
        assertTrue(KraplykSwimFrames.matches(Species.KRAPLYK,GrowthStage.BABY,SpriteClip.SWIM))
        assertFalse(KraplykSwimFrames.matches(Species.KRAPLYK,GrowthStage.YOUNG,SpriteClip.SWIM))
        assertFalse(KraplykSwimFrames.matches(Species.KRAPLYK,GrowthStage.BABY,SpriteClip.WALK))
    }
}
