package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ImportedSpeciesSearchTest {
    @Test fun trajectoriesMatchCoverAndKeepUnsupportedWalkStationary(){
        for(s in Species.entries)for(walk in listOf(true,false))for(i in 0..4000){
            val f=ImportedSpeciesSearch.frame(s,i/1000f,0,walk)
            assertTrue(f.pose.xDp.isFinite()&&f.pose.yDp.isFinite())
            assertEquals(f.pose.xDp,f.coverXDp,.001f)
            if(s==Species.PISHCHANYK||s==Species.LISYK&&!walk)assertEquals(0f,f.pose.xDp,0f)
            if(s==Species.LISYK&&!walk)assertEquals(SearchPhase.WANDERING,f.phase)
        }
        val sparks=(0..1000).map{ImportedSpeciesSearch.frame(Species.ISKRYK,it/1000f,0,false)}
        assertTrue(sparks.any{kotlin.math.abs(it.pose.xDp)>20f})
        assertTrue(sparks.any{it.pose.yDp< -10f})
        assertEquals(0f,ImportedSpeciesSearch.flightEnvelope(0f),0f)
        assertEquals(1f,ImportedSpeciesSearch.flightEnvelope(.5f),0f)
        assertEquals(0f,ImportedSpeciesSearch.flightEnvelope(1f),0f)
    }
    @Test fun allEightLisykSniffPreviewsKeepGroundAndOriginalPalette(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        for(g in GrowthStage.playable)for(sex in Sex.entries){
            val gender=if(sex==Sex.BOY)"male" else "female"
            val src=ImageIO.read(File(root,"imported_v2/pixubi_lisyk_${gender}_${ImportedPixuArt.stageName(g)}.png"))
                .getRGB(0,0,128,128,null,0,128)
            val f=ImportedSpeciesSearch.sniff(src,g,sex,2)
            assertFalse(src.contentEquals(f))
            assertArrayEquals(src,ImportedSpeciesSearch.sniff(src,g,sex,0))
            assertArrayEquals(src.copyOfRange(94*128,src.size),f.copyOfRange(94*128,f.size))
            val palette=src.toSet()
            assertTrue(f.all{it in palette})
        }
    }
}
