package com.pixubi.game

/** Mouth regions specific to the original 128px neutral portraits.
 * These procedural expressions are preview candidates, not authored art. */
internal object OriginalFaceGeometry {
    private val coordinates=mapOf(
        Species.LISYK to arrayOf(
            intArrayOf(64,70,8,5),
            intArrayOf(82,58,6,3),
            intArrayOf(95,51,6,3),
            intArrayOf(94,60,5,3),
            intArrayOf(95,71,6,4),
            intArrayOf(101,56,5,3),
            intArrayOf(101,53,5,3),
            intArrayOf(99,52,5,2)
        ),
        Species.ISKRYK to arrayOf(
            intArrayOf(65,70,8,4),
            intArrayOf(88,55,6,3),
            intArrayOf(96,48,6,3),
            intArrayOf(100,58,5,3),
            intArrayOf(98,73,6,3),
            intArrayOf(103,61,5,3),
            intArrayOf(102,57,5,3),
            intArrayOf(103,58,5,3)
        ),
        Species.KRAPLYK to arrayOf(
            intArrayOf(63,71,8,4),
            intArrayOf(94,60,6,3),
            intArrayOf(88,57,6,3),
            intArrayOf(101,55,6,3),
            intArrayOf(97,71,6,4),
            intArrayOf(103,57,6,3),
            intArrayOf(103,52,5,3),
            intArrayOf(98,53,5,3)
        ),
        Species.PISHCHANYK to arrayOf(
            intArrayOf(66,74,7,4),
            intArrayOf(93,59,6,3),
            intArrayOf(91,53,5,3),
            intArrayOf(97,57,5,3),
            intArrayOf(98,72,6,4),
            intArrayOf(103,60,5,3),
            intArrayOf(102,53,5,3),
            intArrayOf(101,52,4,3)
        ),
        Species.KHMARYK to arrayOf(
            intArrayOf(66,61,5,6),
            intArrayOf(94,60,6,3),
            intArrayOf(87,47,5,3),
            intArrayOf(93,55,5,3),
            intArrayOf(99,72,5,3),
            intArrayOf(107,57,5,3),
            intArrayOf(108,49,5,3),
            intArrayOf(102,52,5,3)
        )
    )
    fun mouth(s:Species,g:GrowthStage,sex:Sex):EyeRegion {
        val stage=GrowthStage.playable.indexOf(GrowthStage.migrate(g))
        require(stage>=0)
        val v=coordinates.getValue(s)[stage+if(sex==Sex.GIRL)0 else 4]
        return EyeRegion(v[0].toFloat(),v[1].toFloat(),v[2].toFloat(),v[3].toFloat())
    }
    fun fallback(clip:SpriteClip,emotion:PetEmotion,authored:String?):PetEmotion? {
        if(authored!=null)return null
        return when(clip){
            SpriteClip.HAPPY,SpriteClip.SAD,SpriteClip.SURPRISE,SpriteClip.TIRED,
            SpriteClip.EAT,SpriteClip.DRINK,SpriteClip.SLEEP ->
                emotion.takeUnless{it==PetEmotion.IDLE}
            else -> null
        }
    }
    fun frameIndex(emotion:PetEmotion,closure:Int,phase:Int):Int =
        closure.coerceIn(0,4)+if(FaceExpression.animatedMouth(emotion)&&phase.mod(2)==1)5 else 0
    fun frames(px:IntArray,s:Species,g:GrowthStage,sex:Sex,emotion:PetEmotion):List<IntArray> =
        EyePixelAnimator.faceFrames(px,128,128,OriginalEyeGeometry.regions(s,g,sex),
            mouth(s,g,sex),s==Species.KHMARYK&&g==GrowthStage.BABY&&sex==Sex.GIRL,emotion)
}
