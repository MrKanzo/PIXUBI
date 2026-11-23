package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeEyeMotionTest {
    @Test fun idleOutsideBlinkKeepsOriginalFace() {
        assertEquals(0f, NativeEyeMotion.blinkAt(.5f), 0.0001f)
        assertEquals(0, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.IDLE, 0f)))
    }

    @Test fun animationClosesThenOpensOriginalEyes() {
        assertEquals(0f, NativeEyeMotion.blinkAt(.925f), .0001f)
        assertTrue(NativeEyeMotion.blinkAt(.959f) > .99f)
        assertEquals(0f, NativeEyeMotion.blinkAt(.993f), .0001f)
        assertEquals(0, NativeEyeMotion.frameIndex(NativeEyeMotion.blinkAt(.5f)))
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.blinkAt(.959f)))
    }

    @Test fun sleepyAndAsleepUseNativeEyeFrame() {
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.SLEEPING, 0f)))
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.HIBERNATING, 0f)))
        assertTrue(NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.TIRED, 0f)) > 0)
    }

    @Test fun legacyBlinkRemainsSupported() {
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.IDLE, 0f, true)))
    }
}
