package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedJumpMotionTest {
    @Test fun missingThirtyJumpSetsUseIndividualNativePixels(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        val sheet=BufferedImage(5*128,30*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(sex in Sex.entries)for(g in GrowthStage.playable){
            val originalJump=sex==Sex.GIRL&&g in setOf(GrowthStage.BABY,GrowthStage.ADULT)
            val f=ImportedPixuArt.Form("test",if(originalJump)setOf("neutral","jump") else setOf("neutral"),emptyList(),true)
            assertEquals(!originalJump,ImportedJumpMotion.applies(f,SpriteClip.HOP))
            assertFalse(ImportedJumpMotion.applies(f,SpriteClip.IDLE))
            if(originalJump)continue
            val gender=if(sex==Sex.GIRL)"female" else "male"
            val name="pixubi_${s.name.lowercase(java.util.Locale.ROOT)}_${gender}_${ImportedPixuArt.stageName(g)}.png"
            val px=ImageIO.read(File(root,"imported_v2/$name")).getRGB(0,0,128,128,null,0,128)
            val frames=(0..3).map{ImportedJumpMotion.frame(px,s,g,sex,it)}
            val ground=px.indexOfLast{it ushr 24!=0}/128
            val p=ImportedPawMotion.profile(s,g,sex)
            sheet.setRGB(0,row*128,128,128,px,0,128)
            for(i in frames.indices){
                val a=frames[i]
                assertTrue(a.all{(it ushr 24) in listOf(0,255)})
                assertFalse(px.contentEquals(a))
                for(j in 0 until i)assertFalse(a.contentEquals(frames[j]))
                if(i==0||i==3)assertEquals(ground,a.indexOfLast{it ushr 24!=0}/128)
                else {
                    assertArrayEquals(px.copyOfRange(0,p.top*128),a.copyOfRange(0,p.top*128))
                    for(k in px.indices)if(px[k]!=a[k])assertTrue(k%128 in p.left until p.right&&k/128 in p.top until 120)
                }
                sheet.setRGB((i+1)*128,row*128,128,128,a,0,128)
            }
            row++
        }
        assertEquals(30,row)
        val file=File("build/imported-preview/jump-30.png")
        file.parentFile.mkdirs()
        ImageIO.write(sheet,"png",file)
    }
}
