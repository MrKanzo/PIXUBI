package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedAppendageMotionTest {
    @Test fun allFortyFormsMoveLocallyWithoutTranslatingTheBody(){
        val sheet=BufferedImage(4*128,40*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(sex in listOf(Sex.GIRL,Sex.BOY))for(g in GrowthStage.playable){
            val gender=if(sex==Sex.GIRL)"female" else "male"
            val file="pixubi_${s.name.lowercase()}_${gender}_${ImportedPixuArt.stageName(g)}.png"
            val f=listOf(File("src/main/assets/imported_v2/$file"),File("app/src/main/assets/imported_v2/$file")).first{it.isFile}
            val px=ImageIO.read(f).getRGB(0,0,128,128,null,0,128)
            val regions=ImportedAppendageMotion.regions(s,g,sex)
            val frames=(0..3).map{ImportedAppendageMotion.frame(px,s,g,sex,it)}
            assertArrayEquals(px,frames[0]);assertArrayEquals(px,frames[2])
            assertFalse("$file has no appendage motion",px.contentEquals(frames[1]))
            assertFalse("$file repeats both motion extrema",frames[1].contentEquals(frames[3]))
            for((j,frame) in frames.withIndex()){
                for(i in px.indices)if(px[i]!=frame[i])
                    assertTrue("$file changed outside appendage",regions.any{it.contains(i%128,i/128)})
                // Ground/paws at the common pivot line are untouched.
                for(i in 113*128 until px.size)assertEquals(px[i],frame[i])
                sheet.setRGB(j*128,row*128,128,128,frame,0,128)
            }
            row++
        }
        assertEquals(40,row)
        val out=File("build/imported-preview/appendage-40.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
}
