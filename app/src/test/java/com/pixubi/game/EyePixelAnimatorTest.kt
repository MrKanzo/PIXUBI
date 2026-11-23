package com.pixubi.game

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EyePixelAnimatorTest {
    private val fur = 0xFFFFA552.toInt()
    private val darkEye = 0xFF17162E.toInt()

    @Test fun idleFrameIsExactlyOriginal() {
        val pixels = IntArray(41 * 41) { fur }
        for (y in 15..25) for (x in 15..25) pixels[y * 41 + x] = darkEye
        val frame = EyePixelAnimator.frame(pixels, 41, 41,
            listOf(EyeRegion(20f, 20f, 9f, 9f)), 0)
        assertArrayEquals(pixels, frame)
    }

    @Test fun blinkRemovesActualIrisPixelsNotDrawsAnotherEye() {
        val pixels = IntArray(41 * 41) { fur }
        for (y in 15..25) for (x in 15..25) pixels[y * 41 + x] = darkEye
        val eye = listOf(EyeRegion(20f, 20f, 9f, 9f))
        val first = EyePixelAnimator.frame(pixels, 41, 41, eye, 1)
        val half = EyePixelAnimator.frame(pixels, 41, 41, eye, 2)
        val shut = EyePixelAnimator.frame(pixels, 41, 41, eye, 4)
        assertEquals(darkEye, pixels[17 * 41 + 20])
        assertNotEquals(darkEye, shut[17 * 41 + 20])
        assertNotEquals(darkEye, half[17 * 41 + 20])
        assertTrue(first.contentEquals(pixels).not())
        // Absolutely no changes outside the original eye crop.
        assertEquals(fur, shut[1 * 41 + 1])
        assertEquals(fur, shut[20 * 41 + 2])
        assertEquals(fur, shut[20 * 41 + 39])
    }

    @Test fun transparencyOutsideIllustrationIsPreserved() {
        val pixels = IntArray(41 * 41) { fur }
        pixels[20 * 41 + 20] = 0
        val result = EyePixelAnimator.frame(pixels, 41, 41,
            listOf(EyeRegion(20f, 20f, 9f, 9f)), 4)
        assertEquals(0, result[20 * 41 + 20])
    }
}
