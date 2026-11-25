package com.pixubi.game

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class LisykMatureArtworkTest {
    @Test fun poseIndicesAreDistinctByAction(){
        assertEquals(listOf(0,1,2,3),(0..3).map{LisykMatureArtwork.index(SpriteClip.WALK,it,0)})
        assertEquals(listOf(4,5,6,7),(0..3).map{LisykMatureArtwork.index(SpriteClip.DIG,it,0)})
        assertEquals(listOf(8,9,10,11),(0..3).map{LisykMatureArtwork.index(SpriteClip.HOP,it,0)})
        assertEquals(listOf(12,13,13,14,14),(0..4).map{LisykMatureArtwork.index(SpriteClip.IDLE,0,it)})
        assertEquals(21,LisykMatureArtwork.index(SpriteClip.SLEEP,0,4))
        assertEquals(listOf(19,22),(0..1).map{LisykMatureArtwork.index(SpriteClip.EAT,it,0)})
        assertEquals(listOf(20,23),(0..1).map{LisykMatureArtwork.index(SpriteClip.DRINK,it,0)})
        assertTrue(PixuAnimationPolicy.hopOffset(Species.LISYK,GrowthStage.ADULT,.5f)<0f)
    }

    @Test fun allFourAtlasesAreCompleteAndUnclipped(){
        val n=LisykMatureArtwork.CELL
        val review=BufferedImage(n*6,n*4*4,BufferedImage.TYPE_INT_ARGB)
        val g=review.createGraphics()
        val neutralHashes=mutableSetOf<Int>()
        var row=0
        for(stage in listOf(GrowthStage.ADULT,GrowthStage.ULTRA))for(sex in Sex.entries){
            val asset=File("src/main/assets/"+LisykMatureArtwork.asset(stage,sex))
            val atlas=ImageIO.read(asset)
            assertEquals(n*6,atlas.width);assertEquals(n*4,atlas.height)
            val frames=(0 until 24).map{i->atlas.getRGB(i%6*n,i/6*n,n,n,null,0,n)}
            for((i,px) in frames.withIndex()){
                assertTrue("$stage $sex frame $i empty",px.count{it ushr 24>0}>1000)
                assertTrue("$stage $sex frame $i clipped",(0 until n).all{x->
                    px[x] ushr 24==0&&px[(n-1)*n+x] ushr 24==0&&
                        px[x*n] ushr 24==0&&px[x*n+n-1] ushr 24==0})
            }
            assertTrue(frames.map{it.contentHashCode()}.distinct().size>=23)
            assertTrue(neutralHashes.add(frames[12].contentHashCode()))
            assertFalse(frames[12].contentEquals(frames[14]))
            assertFalse(frames[12].contentEquals(frames[21]))
            val sleepY=frames[21].indices.first{frames[21][it] ushr 24>0}/n
            val idleY=frames[12].indices.first{frames[12][it] ushr 24>0}/n
            assertTrue("Sleep must be lower than sitting",sleepY>idleY+12)
            g.drawImage(atlas,0,row*n*4,null);row++
        }
        g.dispose()
        val dest=File("build/eye-preview/lisyk-mature-poses.png")
        dest.parentFile.mkdirs();assertTrue(ImageIO.write(review,"png",dest))
    }
}
