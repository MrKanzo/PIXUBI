package com.pixubi.game

import kotlin.math.max

/**
 * Pure math for blinking the ORIGINAL sprite eyes, not overlaying painted eyes.
 * The 3.3-second loop spends ~200 ms closing and opening.
 */
object NativeEyeMotion {
    fun blinkAt(cycle: Float): Float {
        if (!cycle.isFinite()) return 0f
        val t = ((cycle % 1f) + 1f) % 1f
        val start = 0.925f
        val duration = 0.068f
        if (t < start || t > start + duration) return 0f
        val blink = (t - start) / duration
        return if (blink < .5f) (blink * 2f).coerceIn(0f, 1f)
        else ((1f - blink) * 2f).coerceIn(0f, 1f)
    }

    fun closure(emotion: PetEmotion, blinkValue: Float, legacyBlink: Boolean = false): Float {
        val expression = when (emotion) {
            PetEmotion.SLEEPING, PetEmotion.HIBERNATING -> 1f
            PetEmotion.TIRED -> .60f
            // A happy or sad character still keeps its exact ORIGINAL
            // illustrated open eyes between blinks; only the body animates.
            else -> 0f
        }
        return max(expression, max(blinkValue.coerceIn(0f, 1f), if (legacyBlink) 1f else 0f))
    }

    fun frameIndex(closure: Float): Int =
        (closure.coerceIn(0f, 1f) * 4f + .5f).toInt().coerceIn(0, 4)
}
