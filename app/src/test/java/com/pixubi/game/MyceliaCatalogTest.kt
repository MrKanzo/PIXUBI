package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class MyceliaCatalogTest {
    @Test fun evolutionAndActions(){
        val f=RobotTier.entries.map(MyceliaCatalog::of)
        assertEquals(listOf("PIXIE","FLORA","NYMPH","MYCELIA","AETHER"),f.map{it.title})
        assertEquals(listOf(1,1,2,2,3),f.map{it.tails})
        assertTrue(f.zipWithNext().all{(a,b)->a.leg<b.leg&&a.length<b.length&&a.blooms<b.blooms})
        assertFalse(robotActionAvailable(RobotSeries.MYCELIA,RobotTier.MK1,RobotAction.MYCO_BLOOM))
        assertTrue(robotActionAvailable(RobotSeries.MYCELIA,RobotTier.MK2,RobotAction.MYCO_BLOOM))
        assertFalse(robotActionAvailable(RobotSeries.SYMBIONT,RobotTier.OMEGA,RobotAction.MYCO_BLOOM))
        assertTrue(robotActionAvailable(RobotSeries.MYCELIA,RobotTier.OMEGA,RobotAction.BIO_FLIGHT))
        for(i in 0..3){
            assertEquals(MyceliaCatalog.foot(0f,i,true).x,MyceliaCatalog.foot(1f,i,true).x,.001f)
            assertEquals(MyceliaCatalog.foot(0f,i,true).y,MyceliaCatalog.foot(1f,i,true).y,.001f)
        }
    }
}
