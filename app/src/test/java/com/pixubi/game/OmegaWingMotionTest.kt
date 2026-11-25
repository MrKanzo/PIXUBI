package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class OmegaWingMotionTest {
    @Test fun foldsAtRestAndOpensForDash() {
        assertTrue(OmegaWingMotion.pose(RobotAction.IDLE,0f).spread<.2f)
        assertTrue(OmegaWingMotion.pose(RobotAction.NINJA_DASH,.58f).spread>.9f)
        assertTrue(OmegaWingMotion.pose(RobotAction.NINJA_DASH,1f).spread<.2f)
    }
    @Test fun eachFeatherHasItsOwnAngle() {
        assertEquals(6,(0..5).map{OmegaWingMotion.featherAngle(it,1f,false)}.distinct().size)
        assertTrue(OmegaWingMotion.featherAngle(5,1f,false)<OmegaWingMotion.featherAngle(5,0f,false))
    }
}
