package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedPawMotionTest {
    @Test fun alternatingPawsStayWithinLocalRegions(){
        val sheet=BufferedImage(4*128,40*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(sex in listOf(Sex.GIRL,Sex.BOY))for(g in GrowthStage.playable){
            val gender=if(sex==Sex.GIRL)"female" else "male"
            val name="pixubi_${s.name.lowercase()}_${gender}_${ImportedPixuArt.stageName(g)}.png"
            val f=listOf(File("src/main/assets/imported_v2/$name"),File("app/src/main/assets/imported_v2/$name")).first{it.isFile}
            val px=ImageIO.read(f).getRGB(0,0,128,128,null,0,128)
            val p=ImportedPawMotion.profile(s,g,sex)
            for(step in 0..3){
                val frame=ImportedPawMotion.frame(px,s,g,sex,step)
                if(step%2==0)assertArrayEquals(px,frame) else {
                    assertFalse("$name $step no motion",px.contentEquals(frame))
                    val lo=if(step==1)p.left else p.split
                    val hi=if(step==1)p.split else p.right
                    for(i in px.indices)if(px[i]!=frame[i])
                        assertTrue("$name changed outside selected paw",i%128 in lo until hi && i/128 in p.top until 120)
                }
                sheet.setRGB(step*128,row*128,128,128,frame,0,128)
            }
            row++
        }
        assertEquals(40,row)
        val out=File("build/imported-preview/paws-40.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
    @Test fun previewsAreSpeciesSpecificAndDoNotAdvertiseWalking(){
        assertTrue(ImportedPawMotion.localPaws(Species.LISYK,SpriteClip.WALK,null))
        assertFalse(ImportedPawMotion.localPaws(Species.LISYK,SpriteClip.WALK,"walk_a"))
        assertTrue(ImportedPawMotion.localPaws(Species.PISHCHANYK,SpriteClip.DIG,null))
        assertFalse(ImportedPawMotion.supportsPreviewTravel(Species.LISYK,SpriteClip.WALK))
        assertTrue(ImportedPawMotion.supportsPreviewTravel(Species.KRAPLYK,SpriteClip.SWIM))
        assertTrue(ImportedPawMotion.supportsPreviewTravel(Species.KHMARYK,SpriteClip.FLY))
        assertFalse(ImportedPawMotion.supportsPreviewTravel(Species.LISYK,SpriteClip.FLY))
        val original=PixubiSearch.frame(Species.PISHCHANYK,.5f)
        val centered=ImportedPawMotion.stationarySearch(Species.PISHCHANYK,original)
        assertEquals(0f,centered.coverXDp,0f)
        assertEquals(0f,centered.pose.xDp,0f)
        assertEquals(original.phase,centered.phase)
        val land=ImportedPawMotion.stationarySearch(Species.LISYK,original)
        assertEquals(SearchPhase.WANDERING,land.phase)
        assertEquals(0f,land.concealment,0f)
    }
}
