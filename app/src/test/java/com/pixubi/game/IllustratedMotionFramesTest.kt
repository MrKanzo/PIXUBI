package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class IllustratedMotionFramesTest {
    @Test fun originalArtIsUnchangedAtRestAndSleep() {
        val n=192
        val a=IntArray(n*n){0xffaabbcc.toInt()}
        for(s in Species.entries) for(g in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT)) {
            assertArrayEquals(a,IllustratedMotionFrames.frame(a,n,s,g,SpriteClip.IDLE,0))
            assertArrayEquals(a,IllustratedMotionFrames.frame(a,n,s,g,SpriteClip.SLEEP,1))
        }
    }
    @Test fun actualAtlasFramePreview() {
        val a=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val b=ImageIO.read(File("src/main/assets/pixubi_avatars.png"))
        val sheet=BufferedImage(192*4,192*15,BufferedImage.TYPE_INT_ARGB)
        val graphics=sheet.createGraphics()
        for((row,stage) in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT).withIndex()) {
            val n=if(stage==GrowthStage.ADULT)160 else 192
            val atlas=if(stage==GrowthStage.ADULT)b else a
            for(s in Species.entries) {
                val y=if(stage==GrowthStage.YOUNG)192 else 0
                val src=atlas.getRGB(s.ordinal*n,y,n,n,null,0,n)
                for(i in 0..3) {
                    val frame=IllustratedMotionFrames.frame(src,n,s,stage,SpriteClip.WALK,i)
                    if(i==0)assertArrayEquals(src,frame)
                    // The head/eyes remain untouched by walking.
                    for(k in 0 until n*45)assertEquals(src[k],frame[k])
                    if(i==1||i==3)assertFalse(src.contentEquals(frame))
                    val img=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                    img.setRGB(0,0,n,n,frame,0,n)
                    graphics.drawImage(img,i*192+(192-n)/2,(row*5+s.ordinal)*192,null)
                }
            }
        }
        graphics.dispose()
        val dest=File("build/eye-preview/illustrated-motion-frames.png")
        dest.parentFile.mkdirs()
        assertTrue(ImageIO.write(sheet,"png",dest))
    }
}
