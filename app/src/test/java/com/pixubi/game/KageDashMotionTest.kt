package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class KageDashMotionTest {
    @Test fun kneelsBeforeMoving() {
        val p=KageDashMotion.pose(.35f)
        assertEquals(0f,p.travel,.001f)
        assertTrue(p.drop>=24f)
        assertTrue(p.farShin>80f)
        assertTrue(p.nearThigh< -60f)
    }
    @Test fun fastTravelAndStableFinish() {
        assertTrue(KageDashMotion.pose(.65f).travel>50f)
        assertTrue(KageDashMotion.pose(.65f).trail>0f)
        assertEquals(88f,KageDashMotion.pose(1f).travel,.001f)
        assertEquals(0f,KageDashMotion.pose(1f).drop,.001f)
        assertEquals(KageDashMotion.pose(1f),KageDashMotion.pose(2f))
        assertEquals(KageDashPose(),KageDashMotion.pose(-1f))
    }
    @Test fun neverMovesBackwards() {
        val poses=(0..100).map{KageDashMotion.pose(it/100f)}
        assertTrue(poses.zipWithNext().all{(a,b)->a.travel<=b.travel})
    }
}
