package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
class KageGaitTest {
    @Test fun feetReachTargetWithBentKnees() {
        for(tier in RobotTier.entries)for(action in listOf(RobotAction.WALK,RobotAction.RUN)){
            val form=KageForms.of(tier)
            for(i in 0..100){
                val p=KageGait.pose(form,action,i/100f)
                for(s in listOf(p.near,p.far)){
                    val t=s.thigh*PI.toFloat()/180f
                    val k=s.shin*PI.toFloat()/180f
                    assertEquals(s.footX,-form.thigh*sin(t)-form.shin*sin(k),.01f)
                    assertEquals(s.footY,form.thigh*cos(t)+form.shin*cos(k),.01f)
                    assertTrue(s.shin>=s.thigh)
                }
                if(action==RobotAction.WALK)assertTrue(p.near.planted||p.far.planted)
            }
        }
    }
    @Test fun frontWalkingTurnsToProfile() {
        assertEquals(RobotView.RIGHT,KageGait.view(RobotView.FRONT,RobotAction.WALK))
        assertEquals(RobotView.LEFT,KageGait.view(RobotView.LEFT,RobotAction.RUN))
        assertEquals(RobotView.FRONT,KageGait.view(RobotView.FRONT,RobotAction.IDLE))
    }
}
