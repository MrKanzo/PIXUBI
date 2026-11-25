package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class KageArticulationTest {
    @Test fun stagesUseDistinctArmorSilhouettes() {
        val shells=(0..4).map(KageShells::of)
        assertEquals(5,shells.map{it.hoodFront}.distinct().size)
        assertEquals(5,shells.map{it.torsoFront}.distinct().size)
        assertEquals(5,shells.map{it.shoulder}.distinct().size)
        assertEquals(5,shells.map{it.forearm}.distinct().size)
    }
    @Test fun runningBendsOppositeKnees() {
        val a=KageJointMotion.of(RobotAction.RUN,.25f)
        val b=KageJointMotion.of(RobotAction.RUN,.75f)
        assertTrue(a.nearKnee>50f && a.farKnee<1f)
        assertTrue(b.farKnee>50f && b.nearKnee<1f)
        assertNotEquals(a.nearElbow,b.nearElbow)
    }
    @Test fun waveBendsElbowAndWrist() {
        val a=KageJointMotion.of(RobotAction.WAVE,.25f)
        assertTrue(a.nearElbow< -30f)
        assertTrue(a.wrist>10f)
    }
    @Test fun dashKeepsOriginalKneePose() {
        val p=KageDashMotion.pose(.35f)
        val b=KageJointMotion.of(RobotAction.NINJA_DASH,.35f)
        assertEquals(p.farShin,p.farThigh+b.farKnee,.001f)
    }
}
