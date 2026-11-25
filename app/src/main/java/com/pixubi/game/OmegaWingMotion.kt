package com.pixubi.game

import kotlin.math.*

internal data class OmegaWingPose(val spread:Float,val pulse:Float)
internal object OmegaWingMotion {
    fun pose(action:RobotAction,phase:Float):OmegaWingPose {
        val t=phase.coerceIn(0f,1f)
        val wave=sin(t*2f*PI.toFloat())
        val spread=when(action){
            RobotAction.NINJA_DASH->.10f+.90f*KageDashMotion.pose(t).trail
            RobotAction.JUMP->.12f+.88f*sin(t*PI.toFloat()).coerceAtLeast(0f)
            RobotAction.ATTACK->.65f+.30f*wave
            RobotAction.RUN->.28f+.10f*wave
            RobotAction.WALK->.12f+.04f*wave
            RobotAction.SLEEP->.02f
            else->.10f+.025f*wave
        }.coerceIn(0f,1f)
        return OmegaWingPose(spread,if(action==RobotAction.SLEEP).25f else .80f+.12f*wave)
    }
    fun featherAngle(index:Int,spread:Float,side:Boolean):Float {
        val i=index.coerceIn(0,5)
        val folded=if(side) 12f+i*6f else 8f+i*8f
        val open=if(side) 24f+i*10f else 26f+i*18f
        return -(folded+(open-folded)*spread.coerceIn(0f,1f))
    }
}
