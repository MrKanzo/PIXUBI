package com.pixubi.game

import kotlin.math.roundToInt

/** A small rear-body breathing movement on the ten original lying poses.
 * Front paws, head and ground stay fixed. Hibernation holds the resting frame.
 * Does not create a lying pose from an upright portrait or modify game time. */
internal object ImportedSleepMotion {
    const val N=128
    fun frameIndex(cycle:Float,emotion:PetEmotion):Int {
        if(emotion!=PetEmotion.SLEEPING||!cycle.isFinite())return 0
        val t=((cycle%1f)+1f)%1f
        return intArrayOf(0,1,2,1)[(t*4f).toInt().coerceIn(0,3)]
    }
    fun applies(pose:String,clip:SpriteClip,emotion:PetEmotion)=
        pose=="sleep"&&clip==SpriteClip.SLEEP&&emotion==PetEmotion.SLEEPING
    fun frame(src:IntArray,amplitude:Int):IntArray {
        require(src.size==N*N)
        val amp=amplitude.coerceIn(0,2)
        if(amp==0)return src
        val out=src.copyOf()
        for(y in 37 until 104)for(x in 4 until 52){
            val w=minOf(1f,(x-4)/6f,(51-x)/9f,(y-37)/6f,(103-y)/12f)
            val dy=(amp*w).roundToInt()
            out[y*N+x]=src[(y+dy)*N+x]
        }
        return out
    }
}
