package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedFaceGeometryTest {
    @Test fun proceduralExpressionsAreRestrictedToOriginalFace(){
        val emotions=listOf(PetEmotion.HAPPY,PetEmotion.SAD,PetEmotion.SURPRISED,PetEmotion.TIRED,
            PetEmotion.EATING,PetEmotion.DRINKING,PetEmotion.SLEEPING)
        val sheet=BufferedImage((1+emotions.size*2)*128,40*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(sex in listOf(Sex.GIRL,Sex.BOY))for(g in GrowthStage.playable){
            val gender=if(sex==Sex.GIRL)"female" else "male"
            val name="pixubi_${s.name.lowercase()}_${gender}_${ImportedPixuArt.stageName(g)}.png"
            val file=listOf(File("src/main/assets/imported_v2/$name"),File("app/src/main/assets/imported_v2/$name")).first{it.isFile}
            val px=ImageIO.read(file).getRGB(0,0,128,128,null,0,128)
            val mouth=ImportedFaceGeometry.mouth(s,g,sex)
            val regions=ImportedEyeGeometry.regions(s,g,sex)+mouth
            assertTrue(mouth.centerX-mouth.radiusX>=0 && mouth.centerX+mouth.radiusX<128 &&
                mouth.centerY-mouth.radiusY>=0 && mouth.centerY+mouth.radiusY<128)
            sheet.setRGB(0,row*128,128,128,px,0,128)
            for((j,e) in emotions.withIndex()){
                val frames=ImportedFaceGeometry.frames(px,s,g,sex,e)
                assertEquals(if(FaceExpression.animatedMouth(e))10 else 5,frames.size)
                for(frame in frames)for(i in px.indices){
                    assertEquals("$name $e alpha",px[i] ushr 24,frame[i] ushr 24)
                    if(px[i]!=frame[i]){
                        val x=i%128;val y=i/128
                        assertTrue("$name $e changed outside face",regions.any{r->
                            kotlin.math.abs(x-r.centerX)<=r.radiusX && kotlin.math.abs(y-r.centerY)<=r.radiusY})
                    }
                }
                val closure=if(e==PetEmotion.SLEEPING)4 else 0
                sheet.setRGB((1+j*2)*128,row*128,128,128,
                    frames[ImportedFaceGeometry.frameIndex(e,closure,0)],0,128)
                sheet.setRGB((2+j*2)*128,row*128,128,128,
                    frames[ImportedFaceGeometry.frameIndex(e,closure,1)],0,128)
            }
            row++
        }
        assertEquals(40,row)
        val out=File("build/imported-preview/expressions-40.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
    @Test fun authoredFramesWinAndUnsupportedLocomotionStaysUnsupported(){
        assertNull(ImportedFaceGeometry.fallback(SpriteClip.HAPPY,PetEmotion.HAPPY,"happy"))
        assertEquals(PetEmotion.HAPPY,ImportedFaceGeometry.fallback(SpriteClip.HAPPY,PetEmotion.HAPPY,null))
        assertEquals(PetEmotion.EATING,ImportedFaceGeometry.fallback(SpriteClip.EAT,PetEmotion.EATING,null))
        assertNull(ImportedFaceGeometry.fallback(SpriteClip.SWIM,PetEmotion.IDLE,null))
        assertEquals(5,ImportedFaceGeometry.frameIndex(PetEmotion.EATING,0,1))
        assertEquals(0,ImportedFaceGeometry.frameIndex(PetEmotion.EATING,0,0))
    }
}
