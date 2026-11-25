package com.pixubi.game

import kotlin.math.*

internal data class KageStep(val thigh:Float,val shin:Float,val footX:Float,val footY:Float,val planted:Boolean)
internal data class KageGaitPose(val near:KageStep,val far:KageStep,val drop:Float,val lean:Float,val nearArm:Float,val farArm:Float)
internal object KageGait {
    fun isLocomotion(action:RobotAction)=action==RobotAction.WALK||action==RobotAction.RUN
    fun view(view:RobotView,action:RobotAction)=if(isLocomotion(action)&&view==RobotView.FRONT)RobotView.RIGHT else view

    fun pose(form:KageForm,action:RobotAction,phase:Float):KageGaitPose {
        val running=action==RobotAction.RUN
        val length=form.thigh+form.shin
        val height=length*(if(running).80f else .90f)
        val stride=length*(if(running).47f else .32f)
        val stance=if(running).50f else .62f
        fun step(p:Float):KageStep {
            val t=((p%1f)+1f)%1f
            val planted=t<stance
            val u=if(planted)t/stance else (t-stance)/(1f-stance)
            val smooth=u*u*(3f-2f*u)
            val x=if(planted)stride*(1f-2f*u) else stride*(-1f+2f*smooth)
            val y=height-if(planted)0f else length*(if(running).32f else .19f)*sin(u*PI.toFloat())
            val reach=sqrt(x*x+y*y).coerceAtMost(length-.05f)
            val scale=reach/sqrt(x*x+y*y).coerceAtLeast(.001f)
            val tx=x*scale;val ty=y*scale
            val knee=acos(((reach*reach-form.thigh*form.thigh-form.shin*form.shin)/(2f*form.thigh*form.shin)).coerceIn(-1f,1f))
            val thigh=atan2(-tx,ty)-atan2(form.shin*sin(knee),form.thigh+form.shin*cos(knee))
            val degrees=180f/PI.toFloat()
            return KageStep(thigh*degrees,(thigh+knee)*degrees,tx,ty,planted)
        }
        val n=step(phase);val f=step(phase+.5f)
        return KageGaitPose(n,f,length-height,if(running)15f else 4f,
            n.footX/stride*(if(running)38f else 21f),f.footX/stride*(if(running)38f else 21f))
    }
}
