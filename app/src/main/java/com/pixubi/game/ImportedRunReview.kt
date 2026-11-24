package com.pixubi.game

import kotlin.math.roundToInt
import kotlin.math.sin

/** Separate experimental sprint: 16 ticks/s, 1.5 s per leg.
 * Acceleration and braking keep the same clock as the raster and suspension. */
internal object ImportedRunReview {
    const val FPS=16
    const val DISTANCE=64f
    data class Pose(val frame:Int,val xDp:Float,val yDp:Float,val left:Boolean,val running:Boolean)
    fun enabled(scene:DevScene?,imported:Boolean)=imported&&scene?.action==DevAction.RUN
    fun pose(seconds:Float,left:Boolean):Pose{
        val tick=if(seconds.isFinite())(seconds.coerceAtLeast(0f)*FPS)%48f else 0f
        val inbound=tick>=24f
        val t=if(inbound)tick-24f else tick
        val i=t.toInt().coerceIn(0,23)
        val moving=i in 4..19
        val frame=when(i){0->0;1->1;2->2;3->3;in 4..19->4+(i-4)%8;20->3;21->2;22->1;else->0}
        val progress=((t-4f)/16f).coerceIn(0f,1f)
        val eased=progress*progress*(3f-2f*progress)
        val offset=DISTANCE*(if(inbound)1f-eased else eased)
        val stride=if(moving)((t-4f)%4f)/4f else 0f
        // Two suspension arcs per eight stride samples; feet touch at 0 and .75.
        val height=if(moving&&stride in .25f.. .75f)
            -11f*sin(Math.PI*(stride-.25f)/.5f).toFloat() else 0f
        return Pose(frame,offset*(if(left)-1f else 1f),height,if(inbound)!left else left,moving)
    }
    fun nextFrame(seconds:Float):Float{
        val i=if(seconds.isFinite())(seconds.coerceIn(0f,47.999f)*FPS).toInt() else 0
        return ((i+1)%(48*FPS)+.5f)/FPS
    }
    /** Original two-pose sets use the neutral for transitions. */
    fun authoredPose(frame:Int):String = if(frame in 4..11)
        if((frame-4)/2%2==0)"walk_a" else "walk_b" else "neutral"
    /** Integer shear, no interpolation, no crop at the native cell edge.
     * Preserve each row's exact pixel colors and alpha; ground is anchored. */
    fun lean(src:IntArray,frame:Int):IntArray{
        require(src.size==128*128)
        if(frame==0)return src
        val ground=src.indexOfLast{it ushr 24!=0}/128
        val amount=when(frame){1,2,22->1;3->2;in 4..11->intArrayOf(3,4,5,4,3,4,5,4)[frame-4];else->0}
        val out=IntArray(src.size)
        for(y in 0..ground){
            val start=y*128
            val last=(127 downTo 0).firstOrNull{src[start+it] ushr 24!=0}?:continue
            val shift=(amount*((ground-y)/65f).coerceIn(0f,1f)).roundToInt().coerceAtMost(127-last)
            for(x in 0..last)out[start+x+shift]=src[start+x]
        }
        return out
    }
}
