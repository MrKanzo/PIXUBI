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

    @Test fun closedEyeRestoresContinuousShadingRatherThanFlatPatch() {
        val w = 61
        val eye = EyeRegion(30f,30f,12f,11f)
        val original = IntArray(w*w) { i ->
            val x = i%w
            val y = i/w
            0xFF000000.toInt() or ((90+x*2) shl 16) or ((70+y*2) shl 8) or 50
        }
        val clean = original.copyOf()
        for (y in 22..38) for (x in 21..39) {
            if ((x-30)*(x-30)+(y-30)*(y-30)<88) original[y*w+x]=darkEye
        }
        val frames = EyePixelAnimator.frames(original,w,w,listOf(eye))
        assertArrayEquals(original,frames[0])
        val closed = frames[4]
        for (y in 24..29) for (x in 24..36) {
            val a = closed[y*w+x]
            val b = clean[y*w+x]
            assertTrue(kotlin.math.abs(((a ushr 16) and 255)-((b ushr 16) and 255)) < 5)
            assertTrue(kotlin.math.abs(((a ushr 8) and 255)-((b ushr 8) and 255)) < 5)
        }
        assertNotEquals(closed[27*w+26],closed[27*w+34])
        assertEquals(clean[15*w+15],closed[15*w+15])
    }
}
