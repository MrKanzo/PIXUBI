package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class OriginalYoungMaleArtworkTest {
    @Test fun sideFacingJuvenilesHaveOwnRealAssetsAndActionFrames(){
        val n=128
        val sheet=BufferedImage(n*12,n*4,BufferedImage.TYPE_INT_ARGB)
        val graphics=sheet.createGraphics()
        var row=0
        for(s in Species.entries.filter{it!=Species.LISYK}){
            val atlas=ImageIO.read(File("src/main/assets/"+OriginalYoungMaleArtwork.asset(s)))
            assertEquals(n,atlas.width);assertEquals(n,atlas.height)
            val px=atlas.getRGB(0,0,n,n,null,0,n)
            val action=when(s){Species.KRAPLYK->SpriteClip.SWIM;Species.KHMARYK->SpriteClip.FLY;else->SpriteClip.DIG}
            val keys=(0..3).map{PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,SpriteClip.WALK,it,PetEmotion.IDLE,0)}+
                (0..3).map{PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,action,it,PetEmotion.IDLE,0)}+
                listOf(PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,SpriteClip.IDLE,0,PetEmotion.IDLE,4),
                    PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,SpriteClip.HAPPY,1,PetEmotion.HAPPY,0),
                    PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,SpriteClip.SAD,0,PetEmotion.SAD,0),
                    PixuFrameKey(s,GrowthStage.YOUNG,Sex.BOY,0,SpriteClip.SLEEP,0,PetEmotion.SLEEPING,4))
            val rendered=keys.map{OriginalYoungMaleArtwork.pose(px,it)}
            assertFalse(rendered[1].contentEquals(rendered[3]))
            assertFalse(rendered[8].contentEquals(px))
            assertFalse(rendered[11].contentEquals(px))
            for((col,pixels) in rendered.withIndex()){
                assertTrue("$s frame $col empty",pixels.count{it ushr 24>0}>1000)
                val img=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                img.setRGB(0,0,n,n,pixels,0,n);graphics.drawImage(img,col*n,row*n,null)
            }
            row++
        }
        graphics.dispose()
        val f=File("build/eye-preview/original-young-male-poses.png")
        f.parentFile.mkdirs();assertTrue(ImageIO.write(sheet,"png",f))
    }
}
