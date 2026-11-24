package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class GaitReviewRouteTest {
    @Test fun travelOnlyDuringGaitAndDirectionChangesWhileSeated(){
        for(i in 1..4800){
            val a=GaitReviewRoute.pose((i-1)/1000f,false)
            val b=GaitReviewRoute.pose(i/1000f,false)
            if(kotlin.math.abs(a.xDp-b.xDp)>.0001f)assertTrue(a.walking||b.walking)
            if(a.left!=b.left){assertEquals(0,a.frame);assertEquals(0,b.frame)}
            assertTrue(b.xDp in 0f..48f)
            assertTrue(b.frame in 0..11)
        }
        assertEquals(0f,GaitReviewRoute.pose(0f,false).xDp,0f)
        assertEquals(48f,GaitReviewRoute.pose(2.2f,false).xDp,.01f)
        assertEquals(-48f,GaitReviewRoute.pose(2.2f,true).xDp,.01f)
        assertEquals(0f,GaitReviewRoute.pose(4.8f,false).xDp,.01f)
    }
    @Test fun reviewIsIsolatedToExplicitDeveloperWalk(){
        val d=DevScene(DevAction.WALK,0f,false,true)
        assertTrue(GaitReviewRoute.enabled(d,Species.LISYK,GrowthStage.BABY,Sex.BOY,true))
        for(s in Species.entries){
            assertTrue(GaitReviewRoute.enabled(d,s,GrowthStage.BABY,Sex.BOY,true))
            assertTrue(GaitReviewRoute.enabled(d,s,GrowthStage.YOUNG,Sex.BOY,true))
            assertTrue(GaitReviewRoute.enabled(d,s,GrowthStage.ADULT,Sex.BOY,true))
            assertTrue(GaitReviewRoute.enabled(d,s,GrowthStage.ULTRA,Sex.BOY,true))
        }
        assertFalse(GaitReviewRoute.enabled(null,Species.LISYK,GrowthStage.BABY,Sex.BOY,true))
        assertFalse(GaitReviewRoute.enabled(d.copy(gaitPreview=false),Species.LISYK,GrowthStage.BABY,Sex.BOY,true))
        assertFalse(GaitReviewRoute.enabled(d,Species.LISYK,GrowthStage.BABY,Sex.GIRL,true))
        assertFalse(GaitReviewRoute.enabled(d,Species.LISYK,GrowthStage.ULTRA,Sex.GIRL,true))
        assertFalse(GaitReviewRoute.enabled(d.copy(action=DevAction.EAT),Species.LISYK,GrowthStage.BABY,Sex.BOY,true))
    }
}
