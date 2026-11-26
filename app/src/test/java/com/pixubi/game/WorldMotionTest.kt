package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class WorldMotionTest{
    @Test fun eachSeriesHasDistinctMotionTiming(){
        val styles=RobotSeries.entries.map(WorldMotion::style)
        assertEquals(8,styles.distinct().size)
        styles.forEach{assertTrue(it.run<it.walk&&it.walk<it.idle)}
    }
    @Test fun myceliaGroundedPawsPushBack(){
        for(run in listOf(false,true)){
            val stance=if(run).52f else .72f
            val a=MyceliaCatalog.foot(stance*.2f,0,run)
            val b=MyceliaCatalog.foot(stance*.8f,0,run)
            assertEquals(0f,a.y,.001f);assertEquals(0f,b.y,.001f);assertTrue(a.x>b.x)
            val c=MyceliaCatalog.foot(stance+(1f-stance)*.2f,0,run)
            val d=MyceliaCatalog.foot(stance+(1f-stance)*.8f,0,run)
            assertTrue(c.y<0f&&d.y<0f&&c.x<d.x)
        }
    }
    @Test fun companionHasArticulatedKneeAndPlantedFoot(){
        for(i in 0 until 100){
            val g=WorldMotion.companionGait(RobotAction.WALK,i/100f)
            assertTrue(g.near.planted||g.far.planted)
            assertTrue(g.near.shin>g.near.thigh)
        }
    }
}
