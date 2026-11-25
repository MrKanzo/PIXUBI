package com.pixubi.game

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class OriginalFemaleArtworkTest {
    @Test fun allTwentyOriginalsAndPoses(){
        val n=128
        val sheet=BufferedImage(9*n,20*n,BufferedImage.TYPE_INT_ARGB)
        val graph=sheet.createGraphics()
        val identities=mutableSetOf<Int>()
        var row=0
        for(s in Species.entries)for(g in GrowthStage.playable){
            val atlas=ImageIO.read(File("src/main/assets/"+OriginalFemaleArtwork.asset(s,g)))
            assertEquals(4*n,atlas.width)
            assertEquals((if(OriginalFemaleArtwork.hasBodyPoses(g))3 else 2)*n,atlas.height)
            val count=if(OriginalFemaleArtwork.hasBodyPoses(g))9 else 5
            val frames=(0 until count).map{i->atlas.getRGB(i%4*n,i/4*n,n,n,null,0,n)}
            assertTrue("Duplicate identity $s $g",identities.add(frames[0].contentHashCode()))
            assertEquals(count,frames.map{it.contentHashCode()}.distinct().size)
            for(i in frames.indices){
                assertTrue(frames[i].count{it ushr 24>0}>1000)
                val image=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                image.setRGB(0,0,n,n,frames[i],0,n);graph.drawImage(image,i*n,row*n,null)
            }
            val body=OriginalFemaleArtwork.hasBodyPoses(g)
            assertEquals(body,OriginalFemaleArtwork.choice(g,SpriteClip.SLEEP,0).quality==OriginalFemaleArtwork.Quality.AUTHORED)
            assertEquals(body,OriginalFemaleArtwork.choice(g,SpriteClip.WALK,0).quality==OriginalFemaleArtwork.Quality.AUTHORED)
            assertEquals(1,OriginalFemaleArtwork.choice(g,SpriteClip.HAPPY,0).index)
            assertEquals(2,OriginalFemaleArtwork.choice(g,SpriteClip.SAD,0).index)
            assertEquals(3,OriginalFemaleArtwork.choice(g,SpriteClip.SURPRISE,0).index)
            val blink=EyePixelAnimator.frame(frames[0],n,n,OriginalEyeGeometry.regions(s,g,Sex.GIRL),4)
            assertFalse("No blink $s $g",blink.contentEquals(frames[0]))
            val low=OriginalFemaleMotion.lower(blink,17)
            assertFalse(blink.contentEquals(low))
            for(clip in listOf(SpriteClip.WALK,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY)){
                val move=OriginalFemaleMotion.frame(frames[0],s,g,clip,1)
                assertTrue(move.count{it ushr 24>0}>1000)
            }
            assertEquals(192*192,OriginalFemaleArtwork.normalize(frames[0]).size)
            row++
        }
        graph.dispose()
        assertEquals(20,row)
        val dest=File("build/eye-preview/original-female-poses.png")
        dest.parentFile.mkdirs()
        assertTrue(ImageIO.write(sheet,"png",dest))
    }
}
