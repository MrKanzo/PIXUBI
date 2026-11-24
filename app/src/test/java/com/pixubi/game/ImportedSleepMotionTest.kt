package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedSleepMotionTest {
    @Test fun tenAuthoredSleepPosesBreatheWithoutMovingFaceOrPaws(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        val sheet=BufferedImage(384,1280,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(g in listOf(GrowthStage.BABY,GrowthStage.ADULT)){
            val name="pixubi_${s.name.lowercase(java.util.Locale.ROOT)}_female_${ImportedPixuArt.stageName(g)}.png"
            val original=ImageIO.read(File(root,"imported_v2/$name"))
            val px=original.getRGB(0,256,128,128,null,0,128)
            val frames=(0..2).map{ImportedSleepMotion.frame(px,it)}
            assertArrayEquals(px,frames[0])
            assertFalse(px.contentEquals(frames[1]))
            assertFalse(frames[1].contentEquals(frames[2]))
            for((j,f) in frames.withIndex()){
                assertTrue(f.all{(it ushr 24) in listOf(0,255)})
                assertEquals(115,f.indices.last{f[it] ushr 24!=0}/128)
                for(i in px.indices)if(px[i]!=f[i])assertTrue(i%128 in 4 until 52&&i/128 in 37 until 104)
                sheet.setRGB(j*128,row*128,128,128,f,0,128)
            }
            row++
        }
        assertEquals(10,row)
        val out=File("build/imported-preview/sleep-breathing.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
    @Test fun hibernationHoldsRestAndUprightArtIsNotDeformed(){
        assertEquals(listOf(0,1,2,1),listOf(.1f,.3f,.6f,.9f).map{ImportedSleepMotion.frameIndex(it,PetEmotion.SLEEPING)})
        for(i in 0..100)assertEquals(0,ImportedSleepMotion.frameIndex(i/100f,PetEmotion.HIBERNATING))
        assertTrue(ImportedSleepMotion.applies("sleep",SpriteClip.SLEEP,PetEmotion.SLEEPING))
        assertFalse(ImportedSleepMotion.applies("neutral",SpriteClip.SLEEP,PetEmotion.SLEEPING))
        assertFalse(ImportedSleepMotion.applies("sleep",SpriteClip.SLEEP,PetEmotion.HIBERNATING))
    }
}
