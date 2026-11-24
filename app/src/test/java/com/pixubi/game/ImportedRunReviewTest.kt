package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImportedRunReviewTest {
    @Test fun routeSuspendsOnlyWhileRunningAndTurnsAtRest(){
        var last=ImportedRunReview.pose(0f,false)
        var airborne=false
        for(i in 1..3000){
            val p=ImportedRunReview.pose(i/1000f,false)
            assertTrue(p.xDp in 0f..64f)
            assertTrue(p.yDp in -11.01f..0f)
            if(p.yDp<-.001f){assertTrue(p.running);airborne=true}
            if(p.left!=last.left){assertEquals(0,p.frame);assertEquals(0,last.frame)}
            if(kotlin.math.abs(p.xDp-last.xDp)>.0001f)assertTrue(p.running||last.running)
            last=p
        }
        assertTrue(airborne)
        assertEquals(64f,ImportedRunReview.pose(1.4f,false).xDp,.01f)
        assertEquals(0f,ImportedRunReview.pose(3f,false).xDp,.01f)
        assertEquals(-64f,ImportedRunReview.pose(1.4f,true).xDp,.01f)
        assertFalse(ImportedRunReview.enabled(null,true))
        assertFalse(ImportedRunReview.enabled(DevScene(DevAction.RUN,0f,false),false))
        assertTrue(ImportedRunReview.enabled(DevScene(DevAction.RUN,0f,false),true))
        assertFalse(ImportedRunReview.enabled(DevScene(DevAction.WALK,0f,false),true))
        var t=0f
        for(i in 1..768){t=ImportedRunReview.nextFrame(t);assertEquals(i%768,(t*16f).toInt())}
    }
    @Test fun allFortyRunSourcesAndPixelShearPreserveGroundAndOpaqueCount(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        val sheet=BufferedImage(12*128,40*128,BufferedImage.TYPE_INT_ARGB)
        var row=0
        for(s in Species.entries)for(sex in Sex.entries)for(g in GrowthStage.playable){
            val gender=if(sex==Sex.BOY)"male" else "female"
            val authored=sex==Sex.GIRL&&g in setOf(GrowthStage.BABY,GrowthStage.ADULT)
            val atlas=ImageIO.read(File(root,if(authored)
                "imported_v2/pixubi_${s.name.lowercase(java.util.Locale.ROOT)}_${gender}_${ImportedPixuArt.stageName(g)}.png"
                else ImportedWalkCandidate.asset(s,g,sex)))
            for(i in 0..11){
                // These ten original atlases share the verified catalog coordinates.
                val (x,y)=if(authored)when(ImportedRunReview.authoredPose(i)){
                    "walk_a" -> 128 to 128
                    "walk_b" -> 256 to 128
                    else -> 0 to 0
                } else i%4*128 to i/4*128
                val px=atlas.getRGB(x,y,128,128,null,0,128)
                val f=ImportedRunReview.lean(px,i)
                assertEquals(px.count{it ushr 24!=0},f.count{it ushr 24!=0})
                assertEquals(px.indexOfLast{it ushr 24!=0}/128,f.indexOfLast{it ushr 24!=0}/128)
                assertTrue(f.all{it ushr 24 in listOf(0,255)})
                if(i==0)assertArrayEquals(px,f)
                sheet.setRGB(i*128,row*128,128,128,f,0,128)
            }
            row++
        }
        assertEquals(40,row)
        val out=File("build/imported-preview/run-40.png")
        out.parentFile.mkdirs()
        ImageIO.write(sheet,"png",out)
    }
}
