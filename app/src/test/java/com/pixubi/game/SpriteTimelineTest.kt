package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class SpriteTimelineTest {
    @Test fun sleepOverridesMovement() {
        assertEquals(SpriteClip.SLEEP,SpriteTimeline.clip(PetMotion.WALK,PetEmotion.SLEEPING))
        assertEquals(SpriteClip.SLEEP,SpriteTimeline.clip(PetMotion.WALK,PetEmotion.HIBERNATING))
    }
    @Test fun everyClipHasValidFrameIndices() {
        for(c in SpriteClip.entries) {
            val seen=(0..999).map{SpriteTimeline.index(c,it/1000f)}.toSet()
            assertEquals((0 until c.count).toSet(),seen)
            assertEquals(0,SpriteTimeline.index(c,1f))
        }
    }
    @Test fun differentPawsLiftAtDifferentTimes() {
        val a=SpriteTimeline.stride(1)
        val b=SpriteTimeline.stride(3)
        assertTrue(a[1]<0 && a[3]==0)
        assertTrue(b[3]<0 && b[1]==0)
        assertEquals(SpriteClip.WALK,SpriteTimeline.clip(PetMotion.WALK,PetEmotion.IDLE))
        assertEquals(SpriteClip.SWIM,SpriteTimeline.clip(PetMotion.SWIM,PetEmotion.IDLE))
        assertEquals(SpriteClip.FLY,SpriteTimeline.clip(PetMotion.FLOAT,PetEmotion.IDLE))
    }
}
