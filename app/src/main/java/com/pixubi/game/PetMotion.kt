package com.pixubi.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Pure animation policy. The body motion never changes game time or saved stats. */
enum class PetMotion { IDLE, WALK, HOP, BOUNCE, SWIM, DIG, FLOAT, CHEW, DRINK, SLOUCH, TIRED, SLEEP }

data class BodyPose(
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val rotationDegrees: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val step: Float = 0f
)

object MotionResolver {
    fun resolve(pet: Pet, emotion: PetEmotion, searching: Boolean): PetMotion {
        if (!pet.alive || pet.mode == Mode.HIBERNATION || pet.mode == Mode.SLEEP) return PetMotion.SLEEP
        if (searching) return when (pet.species) {
            Species.LISYK -> PetMotion.WALK
            Species.ISKRYK -> PetMotion.HOP
            Species.KRAPLYK -> PetMotion.SWIM
            Species.PISHCHANYK -> PetMotion.DIG
            Species.KHMARYK -> PetMotion.FLOAT
        }
        return when (emotion) {
            PetEmotion.HAPPY -> PetMotion.BOUNCE
            PetEmotion.SURPRISED -> PetMotion.HOP
            PetEmotion.EATING -> PetMotion.CHEW
            PetEmotion.DRINKING -> PetMotion.DRINK
            PetEmotion.SAD -> PetMotion.SLOUCH
            PetEmotion.TIRED -> PetMotion.TIRED
            PetEmotion.SLEEPING, PetEmotion.HIBERNATING -> PetMotion.SLEEP
            PetEmotion.IDLE -> PetMotion.IDLE
        }
    }

    /** Position, tilt and squash/stretch for each frame. Input is normalized, no timers here. */
    fun pose(motion: PetMotion, cycle: Float): BodyPose {
        val t = ((cycle % 1f) + 1f) % 1f
        val wave = sin(2.0 * PI * t).toFloat()
        val double = sin(4.0 * PI * t).toFloat()
        // One deliberate hop per cycle; the previous abs(sin(2πt)) bounced twice.
        val jump = sin(PI * t).toFloat().coerceAtLeast(0f)
        return when (motion) {
            PetMotion.WALK -> BodyPose(
                xDp = 2.5f * wave, yDp = -3.5f * abs(double),
                rotationDegrees = 4.0f * wave,
                scaleX = 1f + .018f * wave, scaleY = 1f - .018f * abs(double),
                step = wave
            )
            PetMotion.HOP -> BodyPose(
                yDp = -23f * jump, rotationDegrees = 4f * wave,
                scaleX = 1f - .05f * jump, scaleY = 1f + .075f * jump,
                step = wave
            )
            PetMotion.BOUNCE -> BodyPose(
                yDp = -16f * jump, rotationDegrees = 2f * wave,
                scaleX = 1f - .048f * jump, scaleY = 1f + .058f * jump,
                step = wave
            )
            PetMotion.SWIM -> BodyPose(
                yDp = 4f * wave, rotationDegrees = 6f * wave,
                scaleX = 1f + .025f * wave, scaleY = 1f - .02f * wave,
                step = wave
            )
            PetMotion.DIG -> BodyPose(
                xDp = 2f * double, yDp = 4f * abs(double),
                rotationDegrees = 5f * double,
                scaleX = 1f + .035f * abs(double), scaleY = 1f - .04f * abs(double),
                step = double
            )
            PetMotion.FLOAT -> BodyPose(
                yDp = -5f * wave, xDp = 2f * wave,
                rotationDegrees = 5f * wave,
                scaleX = 1f + .02f * wave, scaleY = 1f - .018f * wave,
                step = wave
            )
            PetMotion.CHEW -> BodyPose(
                yDp = 1.8f * abs(double),
                rotationDegrees = 1.6f * double,
                scaleX = 1f + .026f * abs(double), scaleY = 1f - .028f * abs(double),
                step = double
            )
            PetMotion.DRINK -> BodyPose(
                yDp = 3.5f * (1f + wave) / 2f,
                rotationDegrees = 3.5f * wave, step = wave
            )
            PetMotion.SLOUCH -> BodyPose(
                yDp = 5f, rotationDegrees = 1.5f * wave,
                scaleX = 1.025f, scaleY = .94f, step = .2f * wave
            )
            PetMotion.TIRED -> BodyPose(
                yDp = 4f, rotationDegrees = 2f * wave,
                scaleX = 1.02f, scaleY = .95f, step = .2f * wave
            )
            PetMotion.SLEEP -> BodyPose(
                yDp = 2.5f * wave,
                scaleX = 1f + .015f * wave, scaleY = .94f - .013f * wave
            )
            PetMotion.IDLE -> BodyPose(
                yDp = -1.6f * wave, rotationDegrees = 1.3f * wave,
                scaleX = 1f + .008f * wave, scaleY = 1f - .008f * wave
            )
        }
    }
}
