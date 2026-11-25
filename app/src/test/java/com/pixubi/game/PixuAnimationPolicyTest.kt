package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class PixuAnimationPolicyTest {
    @Test fun everyFormUsesSameMovementContract() {
        for(s in Species.entries)for(g in GrowthStage.playable) {
            assertTrue(PixuAnimationPolicy.supports(s,g,SpriteClip.WALK))
            assertTrue(PixuAnimationPolicy.supports(s,g,SpriteClip.HOP))
            assertEquals(s==Species.KRAPLYK,PixuAnimationPolicy.supports(s,g,SpriteClip.SWIM))
            assertEquals(s==Species.KHMARYK,PixuAnimationPolicy.supports(s,g,SpriteClip.FLY))
            assertEquals(if(s==Species.KRAPLYK)SpriteClip.SWIM else SpriteClip.IDLE,
                PixuAnimationPolicy.clip(s,g,PetMotion.SWIM,PetEmotion.IDLE))
            assertEquals(SpriteClip.SLEEP,PixuAnimationPolicy.clip(s,g,PetMotion.WALK,PetEmotion.SLEEPING))
        }
    }
    @Test fun growthScaleIncreasesAndLisykUsesSideHop() {
        val sizes=GrowthStage.playable.map(PixuAnimationPolicy::scale)
        assertTrue(sizes.zipWithNext().all{(a,b)->a<b})
        assertFalse(LisykFrontArtwork.usesFront(SpriteClip.HOP))
        assertTrue(LisykBabyArtwork.locomotion(SpriteClip.HOP))
        assertEquals(listOf(0,5,5,0),(0..3).map{LisykBabyArtwork.cell(SpriteClip.HOP,it)})
    }
}
