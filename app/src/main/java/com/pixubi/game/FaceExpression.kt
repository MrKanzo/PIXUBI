package com.pixubi.game

/** Facial choreography independent of saves and screen coordinates. */
internal enum class MouthPose { REST, SMILE, FROWN, POUT, ROUND, OPEN, YAWN, SLEEP }

internal object FaceExpression {
    fun animatedMouth(emotion: PetEmotion) =
        emotion in setOf(PetEmotion.EATING,PetEmotion.DRINKING,PetEmotion.TIRED,PetEmotion.HAPPY)

    fun eyeScale(emotion: PetEmotion): Float = when(emotion) {
        PetEmotion.HAPPY -> .84f
        PetEmotion.SAD -> .87f
        PetEmotion.EATING -> .84f
        PetEmotion.DRINKING -> .92f
        PetEmotion.SURPRISED -> 1.12f
        else -> 1f
    }

    fun eyeTilt(emotion: PetEmotion): Float = when(emotion) {
        PetEmotion.SAD -> -.18f
        PetEmotion.HAPPY -> .06f
        PetEmotion.TIRED -> -.06f
        else -> 0f
    }

    fun mouth(emotion: PetEmotion, open: Boolean): MouthPose = when(emotion) {
        PetEmotion.IDLE -> MouthPose.REST
        PetEmotion.HAPPY -> if(open) MouthPose.OPEN else MouthPose.SMILE
        PetEmotion.SAD -> MouthPose.FROWN
        PetEmotion.EATING -> if(open) MouthPose.OPEN else MouthPose.REST
        PetEmotion.DRINKING -> if(open) MouthPose.ROUND else MouthPose.POUT
        PetEmotion.SURPRISED -> MouthPose.ROUND
        PetEmotion.TIRED -> if(open) MouthPose.YAWN else MouthPose.REST
        PetEmotion.SLEEPING,PetEmotion.HIBERNATING -> MouthPose.SLEEP
    }

    fun frameIndex(emotion: PetEmotion, closure: Float, mouthOpen: Boolean): Int =
        NativeEyeMotion.frameIndex(closure) + if(animatedMouth(emotion) && mouthOpen) 5 else 0
}

/** Mouth coordinates use the actual atlas cells: 192px young, 160px adult.
 * The nose and surrounding facial markings are outside the editable mouth area.
 */
internal object NativeMouthGeometry {
    fun region(species: Species,stage: GrowthStage): EyeRegion {
        val v = when(stage) {
            GrowthStage.BABY -> when(species) {
                Species.LISYK -> intArrayOf(105,121,13,10)
                Species.ISKRYK -> intArrayOf(86,123,12,9)
                Species.KRAPLYK -> intArrayOf(96,109,13,6)
                Species.PISHCHANYK -> intArrayOf(95,125,12,9)
                Species.KHMARYK -> intArrayOf(82,111,8,11)
            }
            GrowthStage.YOUNG -> when(species) {
                Species.LISYK -> intArrayOf(76,85,7,4)
                Species.ISKRYK -> intArrayOf(74,80,8,4)
                Species.KRAPLYK -> intArrayOf(58,82,8,5)
                Species.PISHCHANYK -> intArrayOf(85,85,8,5)
                Species.KHMARYK -> intArrayOf(73,75,7,7)
            }
            GrowthStage.ADULT -> when(species) {
                Species.LISYK -> intArrayOf(93,82,8,5)
                Species.ISKRYK -> intArrayOf(94,87,8,5)
                Species.KRAPLYK -> intArrayOf(83,78,9,5)
                Species.PISHCHANYK -> intArrayOf(77,84,8,5)
                Species.KHMARYK -> intArrayOf(76,74,7,9)
            }
            else -> error("Shells have no mouth")
        }
        return EyeRegion(v[0].toFloat(),v[1].toFloat(),v[2].toFloat(),v[3].toFloat())
    }
}
