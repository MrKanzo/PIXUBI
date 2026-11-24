package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class PixuRigTest {
    @Test fun allSeventySpritesReassembleExactly() {
        val young=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val adult=ImageIO.read(File("src/main/assets/pixubi_avatars.png"))
        val nPreview=192
        val sheet=BufferedImage(nPreview*3,nPreview*15,BufferedImage.TYPE_INT_ARGB)
        val graphics=sheet.createGraphics()
        for((stageIndex,stage) in listOf(GrowthStage.BABY,GrowthStage.YOUNG,GrowthStage.ADULT).withIndex()) {
            val n=if(stage==GrowthStage.ADULT)160 else 192
            val atlas=if(stage==GrowthStage.ADULT)adult else young
            val rows=if(stage==GrowthStage.ADULT)(0..11).toList()else listOf(if(stage==GrowthStage.YOUNG)1 else 0)
            for(row in rows)for(s in Species.entries) {
                val pixels=atlas.getRGB(s.ordinal*n,row*n,n,n,null,0,n)
                val frame=PixuRig.partition(pixels,n,s,stage)
                assertArrayEquals("$s $stage $row",pixels,frame.composite())
                assertFalse(frame.authored)
                assertTrue(frame.sockets.any{it.slot==EquipmentSlot.HEAD})
                assertTrue(frame.sockets.any{it.slot==EquipmentSlot.NECK})
                assertEquals(frame.sockets.size,frame.sockets.map{it.slot}.distinct().size)
                assertTrue(frame.sockets.all{it.point.x in 0f..(n-1f)&&it.point.y in 0f..(n-1f)})
                if(stage!=GrowthStage.ADULT||row==0) {
                    val y=(stageIndex*5+s.ordinal)*nPreview
                    fun draw(px:IntArray,x:Int){val img=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                        img.setRGB(0,0,n,n,px,0,n);graphics.drawImage(img,x,y,null)}
                    draw(pixels,0);draw(frame.composite(),nPreview)
                    val debug=IntArray(n*n)
                    for(layer in frame.layers)for(i in debug.indices)if(layer.pixels[i] ushr 24!=0)
                        debug[i]=(layer.pixels[i] and 0xff00ffff.toInt()) or (((layer.id.ordinal*47+43)%256) shl 16)
                    draw(debug,nPreview*2)
                }
            }
        }
        graphics.dispose()
        val dest=File("build/eye-preview/rig-layer-review.png");dest.parentFile.mkdirs()
        assertTrue(ImageIO.write(sheet,"png",dest))
    }
}
