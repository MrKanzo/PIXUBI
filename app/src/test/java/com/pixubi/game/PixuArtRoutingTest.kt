package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test

class PixuArtRoutingTest {
    @Test fun allTwentyFormsHaveDistinctSpeciesSources(){
        val sources=Species.entries.flatMap{s->GrowthStage.playable.map{g->PixuArtRouting.source(s,g)}}
        assertEquals(20,sources.size)
        assertEquals(6,sources.count{it==PixuArtSource.POSE_PACK})
        assertEquals(4,sources.count{it==PixuArtSource.YOUNG_ATLAS})
        assertEquals(8,sources.count{it==PixuArtSource.OLDER_ATLAS})
        assertEquals(2,sources.count{it==PixuArtSource.LISYK_MATURE_PACK})
        for(s in Species.entries)for(g in GrowthStage.playable)
            assertEquals(PetPosePack.forPet(s,g)!=null,PixuArtRouting.source(s,g)==PixuArtSource.POSE_PACK)
    }
    @Test fun pixelGridAndMirroringAreStable(){
        val a=0xFFCC5511.toInt()
        val b=0xFF11CC55.toInt()
        val src=intArrayOf(a,a,b,b,a,a,b,b,0,0,b,b,0,0,b,b)
        assertArrayEquals(src,PixuRaster.pixelate(src,4))
        assertArrayEquals(src,PixuRaster.mirror(PixuRaster.mirror(src,4),4))
        assertEquals(b,PixuRaster.mirror(src,4)[0])
    }
}
