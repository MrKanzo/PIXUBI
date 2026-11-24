package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class ImportedTimingAndEffectsTest {
    @Test fun jumpFramesAndHeightAgree(){
        for(i in 0..999){
            val p=i/1000f
            val frame=ImportedMotionTiming.frame(SpriteClip.HOP,p)
            val y=ImportedMotionTiming.jumpY(p)
            if(frame==0||frame==3)assertEquals(0f,y,0f) else assertTrue(y<=0f)
        }
        assertTrue(ImportedMotionTiming.jumpY(.25f)<-20f)
        assertEquals(0,ImportedMotionTiming.frame(SpriteClip.HOP,1f))
    }
    @Test fun staticPortraitsDoNotWander(){
        val partial=ImportedPixuArt.Form("test",setOf("neutral"),emptyList(),false)
        assertFalse(ImportedMotionTiming.canWander(partial))
        assertTrue(ImportedMotionTiming.canWander(partial.copy(frames=setOf("neutral","walk_a","walk_b"))))
        assertTrue(ImportedMotionTiming.canWander(null))
    }
    @Test fun effectsHaveSeparateRowsAndSixFrames(){
        val rows=Species.entries.flatMap{s->Sex.entries.map{sex->ImportedUltraEffects.row(s,sex)}}
        assertEquals((0..9).toSet(),rows.toSet())
        assertEquals((0..5).toSet(),(0..199).mapNotNull{ImportedUltraEffects.frame(it/1000f)}.toSet())
        assertNull(ImportedUltraEffects.frame(.3f))
        assertNull(ImportedUltraEffects.frame(Float.NaN))
    }
}
