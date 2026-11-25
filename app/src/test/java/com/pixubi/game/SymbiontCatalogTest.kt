package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SymbiontCatalogTest {
    @Test fun anatomyAndGates(){
        val f=RobotTier.entries.map(SymbiontCatalog::of)
        assertEquals(listOf("MITE","CRAWLER","STALKER","SYMBIONT","LEVIATHAN"),f.map{it.title})
        assertTrue(f.zipWithNext().all{(a,b)->a.tail<b.tail&&a.leg<b.leg&&a.length<b.length})
        assertFalse(robotActionAvailable(RobotSeries.SYMBIONT,RobotTier.MK2,RobotAction.BIO_CAMOUFLAGE))
        assertTrue(robotActionAvailable(RobotSeries.SYMBIONT,RobotTier.MK3,RobotAction.BIO_CAMOUFLAGE))
        assertFalse(robotActionAvailable(RobotSeries.SYMBIONT,RobotTier.MK4,RobotAction.BIO_FLIGHT))
        assertTrue(robotActionAvailable(RobotSeries.SYMBIONT,RobotTier.OMEGA,RobotAction.BIO_FLIGHT))
        for(i in 0..3) {
            assertEquals(SymbiontCatalog.foot(0f,i,true).x,SymbiontCatalog.foot(1f,i,true).x,.001f)
            assertEquals(SymbiontCatalog.foot(0f,i,true).y,SymbiontCatalog.foot(1f,i,true).y,.001f)
        }
    }
}
