package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class FaceExpressionTest {
    @Test fun faceReactsWithDistinctMouthShapes() {
        assertEquals(MouthPose.SMILE,FaceExpression.mouth(PetEmotion.HAPPY,false))
        assertEquals(MouthPose.FROWN,FaceExpression.mouth(PetEmotion.SAD,false))
        assertEquals(MouthPose.ROUND,FaceExpression.mouth(PetEmotion.SURPRISED,false))
        assertEquals(MouthPose.YAWN,FaceExpression.mouth(PetEmotion.TIRED,true))
        assertNotEquals(FaceExpression.mouth(PetEmotion.EATING,false),
                        FaceExpression.mouth(PetEmotion.EATING,true))
        for(e in PetEmotion.entries) for(step in 0..4) for(open in listOf(false,true)) {
            val index=FaceExpression.frameIndex(e,step/4f,open)
            assertTrue(index in 0 until if(FaceExpression.animatedMouth(e)) 10 else 5)
        }
    }

    @Test fun previewAllSpeciesAllStagesAndExpressions() {
        val evolution=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val adult=ImageIO.read(File("src/main/assets/pixubi_avatars.png"))
        val cell=192
        val stages=listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT)
        val emotions=PetEmotion.entries
        val sheet=BufferedImage(cell*emotions.size,cell*15,BufferedImage.TYPE_INT_ARGB)
        val g=sheet.createGraphics()
        for((s,stage) in stages.withIndex()) for(species in Species.entries) {
            val n=if(stage==GrowthStage.ADULT)160 else 192
            val atlas=if(stage==GrowthStage.ADULT)adult else evolution
            val y=if(stage==GrowthStage.YOUNG)192 else 0
            val sprite=atlas.getSubimage(species.ordinal*n,y,n,n)
            val pixels=sprite.getRGB(0,0,n,n,null,0,n)
            for((col,e) in emotions.withIndex()) {
                val frames=EyePixelAnimator.faceFrames(pixels,n,n,
                    NativeEyeGeometry.regions(species,stage),
                    NativeMouthGeometry.region(species,stage),species==Species.KHMARYK,e)
                val i=FaceExpression.frameIndex(e,NativeEyeMotion.closure(e,0f),true)
                val frame=frames[i]
                if(e==PetEmotion.IDLE) assertArrayEquals(pixels,frame)
                else assertFalse("Unchanged face: $species $stage $e",frame.contentEquals(pixels))
                for(k in pixels.indices) if((pixels[k] ushr 24)<180) assertEquals(pixels[k],frame[k])
                if(FaceExpression.animatedMouth(e))
                    assertFalse("Unchanged animated mouth: $species $stage $e",
                        frames[0].contentEquals(frames[5]))
                val img=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                img.setRGB(0,0,n,n,frame,0,n)
                g.drawImage(img,col*cell+(cell-n)/2,(s*5+species.ordinal)*cell,null)
            }
        }
        g.dispose()
        val dest=File("build/eye-preview/all-face-expressions.png")
        dest.parentFile.mkdirs()
        assertTrue(ImageIO.write(sheet,"png",dest))
    }
}
