package com.pixubi.game

import kotlin.math.PI
import kotlin.math.sin

/** Keep illustrated gait and world height on the same phase. */
internal object ImportedMotionTiming {
    private fun unit(phase:Float):Float =
        if(phase.isFinite())((phase%1f)+1f)%1f else 0f
    fun phase(phase:Float)=unit(phase*2f)
    fun frame(clip:SpriteClip,cycle:Float):Int = when(clip){
        SpriteClip.WALK,SpriteClip.HOP -> (phase(cycle)*4f).toInt().coerceIn(0,3)
        else -> SpriteTimeline.index(clip,cycle)
    }
    fun jumpY(cycle:Float):Float {
        val t=phase(cycle)
        return if(t<.25f||t>=.75f)0f else
            -23f*sin(PI*((t-.25f)/.5f)).toFloat()
    }
    fun canWander(form:ImportedPixuArt.Form?) =
        form==null || ImportedPixuArt.movement(form,SpriteClip.WALK)
}
