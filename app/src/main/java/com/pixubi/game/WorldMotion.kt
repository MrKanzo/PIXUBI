package com.pixubi.game
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

internal enum class WorldFx { NONE, CHARGE, REPAIR, SYNC, CONTACT, EVOLVE }
internal data class WorldMotionStyle(val idle:Int,val walk:Int,val run:Int,val lean:Float)
internal object WorldMotion{
    fun style(s:RobotSeries)=when(s){
        RobotSeries.CLASSIC->WorldMotionStyle(3500,1050,590,1.2f)
        RobotSeries.COMPANION->WorldMotionStyle(2900,870,530,2.0f)
        RobotSeries.KAGE->WorldMotionStyle(4200,740,410,.65f)
        RobotSeries.GLITCH->WorldMotionStyle(2600,820,450,1.1f)
        RobotSeries.VALKYRIE->WorldMotionStyle(3900,980,580,1.9f)
        RobotSeries.NEXUS->WorldMotionStyle(4400,1020,640,.55f)
        RobotSeries.SYMBIONT->WorldMotionStyle(3300,1200,540,1.6f)
        RobotSeries.MYCELIA->WorldMotionStyle(3700,1100,630,2.2f)
    }
    fun period(s:RobotSeries,action:RobotAction):Int=when(action){
        RobotAction.IDLE->style(s).idle
        RobotAction.WALK->style(s).walk
        RobotAction.RUN->style(s).run
        RobotAction.SLEEP->5200
        RobotAction.ATTACK->1250
        RobotAction.CHARGE->1800
        RobotAction.WAVE->1550
        else->2000
    }
    fun idleLean(s:RobotSeries,action:RobotAction,phase:Float,side:Boolean):Float{
        val wave=sin(phase*2f*PI.toFloat())
        return when(action){
            RobotAction.IDLE->wave*style(s).lean*(if(side)1f else .55f)
            RobotAction.ATTACK->if(side)sin(phase*PI.toFloat())*8f else wave*2f
            RobotAction.SLEEP->if(side)4f else 1f
            else->0f
        }
    }
    fun quadruped(phase:Float,index:Int,run:Boolean,stride:Float,lift:Float):Offset{
        val offset=if(run){if(index==0||index==3)0f else .5f}else when(index){0->0f;1->.75f;2->.5f;else->.25f}
        val raw=phase+offset;val t=raw-floor(raw);val stance=if(run).52f else .72f
        if(t<stance)return Offset(stride*(1f-2f*t/stance),0f)
        val u=(t-stance)/(1f-stance)
        return Offset(stride*(2f*u*u*(3f-2f*u)-1f),-lift*sin(PI.toFloat()*u).coerceAtLeast(0f))
    }
    fun companionGait(action:RobotAction,phase:Float)=KageGait.pose(KageForm(1f,1f,30f,1f,18f,22f,18f,20f,0f,0),action,phase)
}

/** Presentation-only patrol planner. Does not alter needs, rewards or saved progress. */
internal object HomeRoam {
    const val MIN_X=.32f
    const val MAX_X=.68f
    const val SCALE=.63f
    private val stops=listOf(MIN_X,.50f,MAX_X)
    fun destination(current:Float,random:kotlin.random.Random):Float {
        val candidates=stops.filter{abs(it-current)>=.14f}
        return candidates[random.nextInt(candidates.size)]
    }
    fun direction(from:Float,to:Float)=if(to<from)RobotView.LEFT else RobotView.RIGHT
    fun duration(series:RobotSeries,from:Float,to:Float,running:Boolean=false):Int {
        val period=WorldMotion.period(series,if(running)RobotAction.RUN else RobotAction.WALK)
        val distance=abs(to-from)
        return (distance/.095f*period).roundToInt().coerceIn(300,5200)
    }
    fun pause(random:kotlin.random.Random,mission:Boolean):Long=
        if(mission)random.nextLong(650L,1401L) else random.nextLong(1800L,4201L)
}
