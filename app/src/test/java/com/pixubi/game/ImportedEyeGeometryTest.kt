package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedEyeGeometryTest {
    @Test fun originalEyesBlinkWithoutChangingSilhouette(){
        val species=Species.entries
        val sheet=BufferedImage(5*128,40*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in species)for(sex in listOf(Sex.GIRL,Sex.BOY))for(g in GrowthStage.playable){
            val gender=if(sex==Sex.GIRL)"female" else "male"
            val file="pixubi_${s.name.lowercase()}_${gender}_${ImportedPixuArt.stageName(g)}.png"
            val f=listOf(File("src/main/assets/imported_v2/$file"),File("app/src/main/assets/imported_v2/$file")).first{it.isFile}
            val image=ImageIO.read(f)
            val px=image.getRGB(0,0,128,128,null,0,128)
            val eyes=ImportedEyeGeometry.regions(s,g,sex)
            assertTrue(eyes.isNotEmpty())
            for(e in eyes){
                assertTrue(e.centerX-e.radiusX>=0 && e.centerX+e.radiusX<128)
                assertTrue(e.centerY-e.radiusY>=0 && e.centerY+e.radiusY<128)
            }
            val frames=EyePixelAnimator.frames(px,128,128,eyes)
            assertEquals(5,frames.size)
            assertArrayEquals(px,frames[0])
            assertFalse("$file blink did not change pixels",px.contentEquals(frames[4]))
            frames.forEachIndexed{step,frame->
                for(i in px.indices){
                    assertEquals("alpha $file $i",px[i] ushr 24,frame[i] ushr 24)
                    if(px[i]!=frame[i]){
                        val x=i%128;val y=i/128
                        assertTrue("change outside eye $file $i",eyes.any{e->
                            kotlin.math.abs(x-e.centerX)<=e.radiusX && kotlin.math.abs(y-e.centerY)<=e.radiusY
                        })
                    }
                }
                sheet.setRGB(step*128,row*128,128,128,frame,0,128)
            }
            row++
        }
        assertEquals(40,row)
        val out=File("build/imported-preview/blink-40.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
}
