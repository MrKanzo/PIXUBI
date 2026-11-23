package com.pixubi.game

internal enum class SpriteClip(val count:Int,val ticksPerCycle:Int) {
    IDLE(4,1), WALK(4,5), HOP(4,2), SWIM(4,3), DIG(4,5), FLY(4,3),
    HAPPY(4,2), EAT(4,4), DRINK(4,3), SLEEP(2,1), TIRED(4,1), SAD(2,1), SURPRISE(2,1)
}

internal object SpriteTimeline {
    fun clip(motion:PetMotion,emotion:PetEmotion):SpriteClip = when {
        emotion==PetEmotion.SLEEPING || emotion==PetEmotion.HIBERNATING || motion==PetMotion.SLEEP -> SpriteClip.SLEEP
        motion==PetMotion.WALK -> SpriteClip.WALK
        motion==PetMotion.DIG -> SpriteClip.DIG
        motion==PetMotion.SWIM -> SpriteClip.SWIM
        motion==PetMotion.FLOAT -> SpriteClip.FLY
        emotion==PetEmotion.EATING -> SpriteClip.EAT
        emotion==PetEmotion.DRINKING -> SpriteClip.DRINK
        emotion==PetEmotion.SURPRISED -> SpriteClip.SURPRISE
        emotion==PetEmotion.HAPPY -> SpriteClip.HAPPY
        emotion==PetEmotion.SAD -> SpriteClip.SAD
        emotion==PetEmotion.TIRED -> SpriteClip.TIRED
        motion==PetMotion.HOP -> SpriteClip.HOP
        else -> SpriteClip.IDLE
    }
    fun index(clip:SpriteClip,phase:Float):Int {
        if(!phase.isFinite()) return 0
        val t=((phase%1f)+1f)%1f
        return (t*clip.count*clip.ticksPerCycle).toInt()%clip.count
    }
    // Positions are authored keyframes, not scaled or stretched bitmap regions.
    fun stride(frame:Int):IntArray = when(frame%4) {
        0 -> intArrayOf(-5,0,5,0)
        1 -> intArrayOf(0,-7,0,0)
        2 -> intArrayOf(5,0,-5,0)
        else -> intArrayOf(0,0,0,-7)
    }
}
