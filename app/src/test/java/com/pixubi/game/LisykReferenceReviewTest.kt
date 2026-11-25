package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LisykReferenceReviewTest {
    @Test fun candidatesKeepOriginalNeutralAndDistinctGroundedCells(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        val neutral=ImageIO.read(File(root,"imported_v2/pixubi_lisyk_male_young.png")).getRGB(0,0,128,128,null,0,128)
        for((path,cols,count) in listOf(Triple(LisykReferenceReview.WALK,4,12),Triple(LisykReferenceReview.SLEEP,3,6))){
            val atlas=ImageIO.read(File(root,path))
            assertEquals(cols*128,atlas.width)
            assertEquals(count/cols*128,atlas.height)
            val frames=(0 until count).map{atlas.getRGB(it%cols*128,it/cols*128,128,128,null,0,128)}
            assertArrayEquals(neutral,frames.first())
            for((i,f) in frames.withIndex()){
                assertEquals(115,f.indexOfLast{it ushr 24!=0}/128)
                assertTrue(f.all{it ushr 24 in listOf(0,255)})
                for(j in 0 until i)assertFalse(f.contentEquals(frames[j]))
            }
        }
    }
    @Test fun onlyOneFormExposesReferenceComparison(){
        assertEquals(1,Species.entries.sumOf{s->GrowthStage.playable.sumOf{g->Sex.entries.count{LisykReferenceReview.matches(s,g,it)}}})
    }
}
