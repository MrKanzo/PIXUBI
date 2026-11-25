package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SymbiontCatalogTest {
    @Test fun predatorProportions(){
        val forms=RobotTier.entries.map(SymbiontCatalog::of)
        assertTrue(forms.all{it.frontBulk>it.rearBulk&&it.shoulderRise>it.rumpDrop})
        assertTrue(forms.zipWithNext().all{(a,b)->b.frontBulk/b.rearBulk>a.frontBulk/a.rearBulk&&b.clawReach>a.clawReach})
    }
    @Test fun groundedPawPushesBackAndLiftedPawReachesForward(){
        for(run in listOf(false,true)){
            val stance=if(run).52f else .72f
            val plantedA=SymbiontCatalog.foot(stance*.2f,0,run)
            val plantedB=SymbiontCatalog.foot(stance*.8f,0,run)
            assertEquals(0f,plantedA.y,.001f)
            assertEquals(0f,plantedB.y,.001f)
            assertTrue(plantedA.x>plantedB.x)
            val liftedA=SymbiontCatalog.foot(stance+(1f-stance)*.2f,0,run)
            val liftedB=SymbiontCatalog.foot(stance+(1f-stance)*.8f,0,run)
            assertTrue(liftedA.y<0f&&liftedB.y<0f)
            assertTrue(liftedA.x<liftedB.x)
        }
    }
    @Test fun gaitKeepsDiagonalPairsAndWalkingSupport(){
        for(frame in 0 until 100){
            val p=frame/100f
            assertEquals(SymbiontCatalog.foot(p,0,true),SymbiontCatalog.foot(p,3,true))
            assertEquals(SymbiontCatalog.foot(p,1,true),SymbiontCatalog.foot(p,2,true))
            assertTrue((0..3).count{SymbiontCatalog.foot(p,it,false).y==0f}>=2)
        }
    }
    @Test fun omegaWingGirdleAndFlightCycle(){
        val f=SymbiontCatalog.of(RobotTier.OMEGA)
        val left=SymbiontCatalog.wingRoot(f,false,true)
        val right=SymbiontCatalog.wingRoot(f,false,false)
        assertEquals(-left.x,right.x,.001f)
        assertEquals(left.y,right.y,.001f)
        for(far in listOf(false,true)){
            val root=SymbiontCatalog.wingRoot(f,true,far)
            assertTrue(root.x>0f&&root.x<f.length*.67f)
            assertTrue(root.y<0f&&root.y> -f.depth-f.shoulderRise)
        }
        assertTrue(SymbiontCatalog.wingOpening(RobotAction.BIO_FLIGHT)>SymbiontCatalog.wingOpening(RobotAction.IDLE))
        assertEquals(SymbiontCatalog.wingAngle(0f,RobotAction.BIO_FLIGHT),SymbiontCatalog.wingAngle(1f,RobotAction.BIO_FLIGHT),.001f)
        assertTrue(SymbiontCatalog.wingAngle(.25f,RobotAction.BIO_FLIGHT)>SymbiontCatalog.wingAngle(.75f,RobotAction.BIO_FLIGHT))
    }
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
