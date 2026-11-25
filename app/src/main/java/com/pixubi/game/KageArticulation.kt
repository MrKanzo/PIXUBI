package com.pixubi.game

import kotlin.math.*

internal data class KageShell(val hoodFront:String,val hoodSide:String,val torsoFront:String,val torsoSide:String,
    val shoulder:String,val forearm:String,val thigh:String,val shin:String,val knee:String)
internal object KageShells {
    private val adult=KageShell(
        "M 0 -48 C 18 -47 31 -29 37 -12 L 40 2 Q 28 18 11 23 L 0 25 L -11 23 Q -28 18 -40 2 L -37 -12 C -31 -29 -18 -47 0 -48 Z",
        "M -36 -16 Q -33 -39 -8 -46 L 9 -46 Q 24 -39 34 -24 L 37 -13 L 25 17 L 5 24 Q -26 20 -36 -16 Z",
        "M -24 -10 L -13 -14 L 13 -14 L 24 -10 L 32 6 L 26 19 L 17 30 L 12 43 L 13 48 L 23 58 L 19 67 L 0 73 L -19 67 L -23 58 L -13 48 L -12 43 L -17 30 L -26 19 L -32 6 Z",
        "M -20 -11 L 8 -13 L 24 1 L 27 13 L 12 31 L 8 44 L 17 58 L 14 68 L -15 70 L -22 61 L -13 43 L -18 28 L -25 10 Z",
        "M -12 -7 L -4 -12 Q 6 -13 13 -5 L 17 5 L 11 12 L 0 15 L -13 11 L -17 4 Z",
        "M -6 0 L 7 0 L 13 10 L 15 17 L 10 29 L 5 34 L -7 33 L -14 17 L -12 7 Z",
        "M -12 0 L 11 0 L 15 12 L 12 24 L 7 38 L 3 42 L -6 42 L -12 28 L -15 14 Z",
        "M -12 10 L 0 17 L 12 10 L 16 21 L 10 32 L 7 47 L -7 47 L -10 32 L -16 21 Z",
        "M -9 -7 L 8 -7 L 12 2 L 9 14 L 1 19 L -9 13 L -13 2 Z")
    fun of(grade:Int):KageShell=when(grade) {
        0->KageShell(
            "M 0 -43 C 24 -43 37 -26 38 -6 Q 37 18 0 25 Q -37 18 -38 -6 C -37 -26 -24 -43 0 -43 Z",
            "M -33 -16 Q -30 -43 -3 -43 Q 28 -44 36 -20 L 36 -5 Q 30 20 5 25 Q -34 19 -33 -16 Z",
            "M -21 -10 Q 0 -20 21 -10 Q 29 0 26 19 L 21 40 L 24 56 Q 0 77 -24 56 L -21 40 L -26 19 Q -29 0 -21 -10 Z",
            "M -18 -11 Q 12 -17 23 3 L 22 32 L 17 57 Q 0 73 -18 59 L -23 19 Q -25 0 -18 -11 Z",
            "M -11 -4 Q 0 -13 11 -4 L 12 7 Q 0 15 -12 7 Z",
            "M -7 0 Q 0 -4 7 0 L 10 14 L 8 31 Q 0 35 -8 31 L -10 14 Z",
            "M -10 0 Q 0 -5 10 0 L 11 15 L 7 42 L -7 42 L -11 15 Z",
            "M -9 10 Q 0 6 9 10 L 12 23 L 8 47 L -8 47 L -12 23 Z",
            "M -8 -5 Q 0 -11 8 -5 L 9 8 Q 0 19 -9 8 Z")
        1->adult.copy(
            hoodFront="M 0 -46 Q 25 -43 35 -15 L 39 1 L 27 17 L 0 25 L -27 17 L -39 1 L -35 -15 Q -25 -43 0 -46 Z",
            hoodSide="M -34 -15 Q -31 -42 -5 -45 L 14 -39 L 36 -18 L 28 13 L 6 24 L -25 15 Z",
            torsoFront="M -23 -10 L 23 -10 L 28 10 L 20 30 L 16 45 L 21 59 L 0 71 L -21 59 L -16 45 L -20 30 L -28 10 Z",
            torsoSide="M -20 -10 L 12 -10 L 25 7 L 19 30 L 13 45 L 18 62 L -17 69 L -22 54 L -17 30 L -23 9 Z",
            shoulder="M -10 -8 L 10 -8 L 14 6 L 7 12 L -9 12 L -14 4 Z",
            forearm="M -6 0 L 7 0 L 10 11 L 9 29 L 5 33 L -7 32 L -10 12 Z",
            thigh="M -10 0 L 10 0 L 12 16 L 6 42 L -6 42 L -12 16 Z",
            shin="M -9 10 L 9 10 L 12 23 L 7 47 L -7 47 L -12 23 Z")
        2->adult.copy(
            hoodFront="M 0 -48 L 12 -45 Q 28 -37 39 -2 L 30 15 L 0 25 L -30 15 L -39 -2 Q -28 -37 -12 -45 Z",
            torsoFront="M -24 -12 L 24 -12 L 30 7 L 19 32 L 13 46 L 22 58 L 17 70 L -17 70 L -22 58 L -13 46 L -19 32 L -30 7 Z",
            shoulder="M -10 -9 L 9 -11 L 16 0 L 13 10 L 0 15 L -14 9 L -16 2 Z",
            forearm="M -6 0 L 7 0 L 13 14 L 8 32 L -7 33 L -13 15 Z",
            shin="M -11 10 L 0 16 L 11 10 L 14 22 L 7 47 L -7 47 L -14 22 Z")
        3->adult
        else->adult.copy(
            hoodFront="M 0 -51 L 13 -47 L 32 -28 L 43 -3 L 31 18 L 0 28 L -31 18 L -43 -3 L -32 -28 L -13 -47 Z",
            torsoFront="M -28 -14 L 28 -14 L 37 5 L 29 22 L 17 34 L 12 46 L 26 57 L 22 71 L 0 76 L -22 71 L -26 57 L -12 46 L -17 34 L -29 22 L -37 5 Z",
            shoulder="M -12 -7 L -3 -15 L 11 -14 L 23 -7 L 18 8 L 7 17 L -14 13 L -21 5 Z",
            forearm="M -7 -1 L 8 -1 L 17 12 L 18 19 L 10 33 L -9 35 L -18 17 Z",
            thigh="M -13 0 L 13 0 L 17 13 L 13 30 L 7 42 L -7 42 L -15 27 L -17 12 Z",
            shin="M -13 10 L 0 16 L 13 10 L 19 22 L 12 32 L 8 47 L -8 47 L -12 32 L -19 22 Z",
            knee="M -11 -9 L 10 -9 L 15 1 L 10 15 L 0 22 L -12 14 L -15 1 Z")
    }
}
internal data class KageBends(val nearElbow:Float,val farElbow:Float,val nearKnee:Float,val farKnee:Float,val ankle:Float,val wrist:Float)
internal object KageJointMotion {
    fun of(action:RobotAction,phase:Float):KageBends {
        val t=phase.coerceIn(0f,1f)
        val s=sin(t*2f*PI.toFloat())
        return when(action) {
            RobotAction.WALK->KageBends(-15f-20f*max(0f,-s),-15f-20f*max(0f,s),32f*max(0f,s),32f*max(0f,-s),8f,3f)
            RobotAction.RUN->KageBends(-65f-12f*s,-65f+12f*s,65f*max(0f,s),65f*max(0f,-s),12f,5f)
            RobotAction.JUMP->{val k=sin(t*PI.toFloat()).coerceAtLeast(0f);KageBends(-45f*k,-45f*k,50f*k,60f*k,10f*k,6f*k)}
            RobotAction.WAVE->KageBends(-65f+24f*s,-7f,0f,0f,0f,16f*s)
            RobotAction.ATTACK->KageBends(-35f-30f*s,-75f,8f,12f,0f,-15f*s)
            RobotAction.SLEEP->KageBends(-18f,-18f,0f,0f,0f,0f)
            RobotAction.CHARGE->KageBends(-12f,-12f,0f,0f,0f,0f)
            RobotAction.SCAN->KageBends(-25f,-10f,0f,0f,0f,0f)
            RobotAction.NINJA_DASH->{val d=KageDashMotion.pose(t);KageBends(-d.trail*55f-d.drop*.7f,-d.trail*40f-d.drop*.5f,d.nearShin-d.nearThigh,d.farShin-d.farThigh,0f,0f)}
            else->KageBends(-5f,-5f,0f,0f,0f,0f)
        }
    }
}
