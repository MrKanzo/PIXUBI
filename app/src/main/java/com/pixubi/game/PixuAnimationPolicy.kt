package com.pixubi.game

/** Shared capabilities for every playable form. No invented swimming/flying
 * or moving a seated portrait sideways. Stage scaling uses a fixed foot anchor. */
internal object PixuAnimationPolicy {
    fun directional(clip:SpriteClip)=when(clip) {
        SpriteClip.WALK,SpriteClip.HOP,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY -> true
        else -> false
    }
    fun supports(species:Species,stage:GrowthStage,clip:SpriteClip):Boolean {
        if(!directional(clip))return true
        val form=GrowthStage.migrate(stage)
        if(form !in GrowthStage.playable)return false
        return when(clip) {
            SpriteClip.WALK,SpriteClip.HOP -> true
            SpriteClip.DIG -> species==Species.LISYK || species==Species.ISKRYK || species==Species.PISHCHANYK
            SpriteClip.SWIM -> species==Species.KRAPLYK
            SpriteClip.FLY -> species==Species.KHMARYK
            else -> false
        }
    }
    fun clip(species:Species,stage:GrowthStage,motion:PetMotion,emotion:PetEmotion):SpriteClip {
        val wanted=SpriteTimeline.clip(motion,emotion)
        return if(supports(species,stage,wanted)) wanted else SpriteTimeline.clip(PetMotion.IDLE,emotion)
    }
    fun scale(stage:GrowthStage):Float=when(GrowthStage.migrate(stage)){
        GrowthStage.BABY -> .79f
        GrowthStage.YOUNG -> .88f
        GrowthStage.ADULT -> .95f
        GrowthStage.ULTRA -> 1f
        else -> .79f
    }
    /** The airborne Iskryk and standard atlas hop poses already include lift.
     * Keep a modest shared external arc, except for those authored poses. */
    fun hopOffset(species:Species,stage:GrowthStage,phase:Float):Float =
        if(species==Species.ISKRYK&&GrowthStage.migrate(stage)==GrowthStage.BABY)0f
        else MotionResolver.pose(PetMotion.HOP,phase).yDp*.65f
}
