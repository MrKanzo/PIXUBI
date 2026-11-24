package com.pixubi.game

/** Only collapse keys when the imported renderer produces the same pixels.
 * Legacy sprites retain their original cache policy. */
internal object ImportedFrameCacheKey {
    fun normalize(k:PixuFrameKey,f:ImportedPixuArt.Form):PixuFrameKey{
        val authored=ImportedPixuArt.select(f,k.clip,k.emotion,k.frame)
        val expression=ImportedFaceGeometry.fallback(k.clip,k.emotion,authored)
        val neutral=authored==null||authored=="neutral"
        val closure=when{
            k.clip==SpriteClip.SLEEP -> 4
            expression!=null||neutral&&k.emotion==PetEmotion.IDLE -> k.closure.coerceIn(0,4)
            else -> 0
        }
        val frame=when{
            k.clip==SpriteClip.SLEEP -> if(k.emotion==PetEmotion.SLEEPING)k.frame.coerceIn(0,2) else 0
            k.clip==SpriteClip.HOP||k.clip==SpriteClip.WALK -> k.frame.mod(4)
            ImportedCareMotion.applies(k)||ImportedSpeciesSearch.sniffApplies(k,authored?:"neutral")||
                ImportedAppendageMotion.applies(k)||ImportedPawMotion.appendageAction(k.species,k.clip)||
                ImportedPawMotion.localPaws(k.species,k.clip,authored) -> k.frame.mod(4)
            expression!=null&&FaceExpression.animatedMouth(expression) -> k.frame.mod(2)
            else -> 0
        }
        return k.copy(frame=frame,closure=closure)
    }
}
