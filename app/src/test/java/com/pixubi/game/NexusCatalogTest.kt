package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class NexusCatalogTest{
    @Test fun formsAndActionGates(){
        val f=RobotTier.entries.map(NexusCatalog::of)
        assertEquals(listOf("BIT","LOGIC","NEURAL","NEXUS","SINGULARITY"),f.map{it.title})
        assertTrue(f.zipWithNext().all{(a,b)->a.thigh<b.thigh&&a.shoulder<b.shoulder})
        assertFalse(robotActionAvailable(RobotSeries.NEXUS,RobotTier.MK2,RobotAction.HOLO_CLONE))
        assertTrue(robotActionAvailable(RobotSeries.NEXUS,RobotTier.MK3,RobotAction.HOLO_CLONE))
        assertFalse(robotActionAvailable(RobotSeries.NEXUS,RobotTier.MK3,RobotAction.REASSEMBLE))
        assertTrue(robotActionAvailable(RobotSeries.NEXUS,RobotTier.MK4,RobotAction.REASSEMBLE))
        assertFalse(robotActionAvailable(RobotSeries.GLITCH,RobotTier.OMEGA,RobotAction.REASSEMBLE))
        assertEquals(1f,NexusCatalog.separation(.5f),.001f)
        assertEquals(0f,NexusCatalog.separation(1f),.001f)
    }
}
