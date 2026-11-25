package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotMotionTest {
    @Test fun walkingAlternatesLimbs() {
        val p=RobotMotion.pose(RobotAction.WALK,.25f)
        assertEquals(-p.nearLeg,p.farLeg,.001f)
        assertTrue(p.nearLeg>20)
        assertTrue(p.nearArm<0)
    }
    @Test fun frontShouldersSwingOutward() {
        for (motion in listOf(-36f, -20f, 0f, 20f, 36f)) {
            assertTrue(RobotMotion.frontArmAngle(motion,false)>0f)
            assertTrue(RobotMotion.frontArmAngle(motion,true)<0f)
        }
        assertEquals(-145f,RobotMotion.frontArmAngle(145f,true),.001f)
    }
    @Test fun neutralPoseAndJump() {
        assertEquals(0f,RobotMotion.pose(RobotAction.IDLE,0f).nearLeg,0f)
        assertTrue(RobotMotion.pose(RobotAction.JUMP,.5f).lift<0f)
    }
    @Test fun sideSocketsMirrorAndAllSlotsExist() {
        val r=RobotMotion.sockets(RobotView.RIGHT)
        val l=RobotMotion.sockets(RobotView.LEFT)
        assertEquals(RobotSlot.entries.size,r.size)
        r.forEach { (slot,p) ->
            val opposite=when(slot) {
                RobotSlot.LEFT_HAND -> RobotSlot.RIGHT_HAND
                RobotSlot.RIGHT_HAND -> RobotSlot.LEFT_HAND
                else -> slot
            }
            assertEquals(200f-p.x,l.getValue(opposite).x,.001f)
        }
        assertNotEquals(RobotMotion.sockets(RobotView.FRONT),r)
    }
}