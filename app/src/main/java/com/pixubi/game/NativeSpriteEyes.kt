package com.pixubi.game

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Blink frames are built by gently resampling the eye pixels INSIDE the supplied
 * original sprite. No extra pair of pupils, lashes, eyelid lines, or face patches
 * are painted over the artwork. The untouched source is frame 0.
 */
internal object NativeSpriteEyes {
    private data class Eye(val x: Float, val y: Float, val rx: Float, val ry: Float)
    private data class Eyes(val left: Eye, val right: Eye)

    // Coordinates are in the original, unscaled atlas CELL, not screen pixels.
    // Each sprite has its own facial proportions; do not use one generic overlay.
    private fun locations(species: Species, stage: GrowthStage): Eyes {
        val (left, right, y, rx, ry) = when (stage) {
            GrowthStage.BABY -> when (species) {
                Species.LISYK -> listOf(74f, 126f, 101f, 18f, 18f)
                Species.ISKRYK -> listOf(65f, 126f, 102f, 18f, 19f)
                Species.KRAPLYK -> listOf(68f, 123f, 107f, 18f, 17f)
                Species.PISHCHANYK -> listOf(68f, 126f, 109f, 17f, 18f)
                Species.KHMARYK -> listOf(70f, 128f, 100f, 18f, 18f)
            }
            GrowthStage.YOUNG -> when (species) {
                Species.LISYK -> listOf(69f, 117f, 94f, 15f, 16f)
                Species.ISKRYK -> listOf(68f, 126f, 99f, 16f, 17f)
                Species.KRAPLYK -> listOf(78f, 116f, 100f, 14f, 15f)
                Species.PISHCHANYK -> listOf(70f, 122f, 103f, 15f, 17f)
                Species.KHMARYK -> listOf(72f, 121f, 99f, 15f, 16f)
            }
            GrowthStage.ADULT -> when (species) {
                Species.LISYK -> listOf(72f, 113f, 71f, 14f, 16f)
                Species.ISKRYK -> listOf(71f, 111f, 71f, 14f, 16f)
                Species.KRAPLYK -> listOf(69f, 109f, 71f, 15f, 15f)
                Species.PISHCHANYK -> listOf(70f, 111f, 73f, 14f, 15f)
                Species.KHMARYK -> listOf(69f, 111f, 71f, 14f, 16f)
            }
            GrowthStage.EGG, GrowthStage.COCOON ->
                error("Shells have no eyes to animate")
        }
        return Eyes(Eye(left, y, rx, ry), Eye(right, y, rx, ry))
    }

    fun frames(original: Bitmap, species: Species, stage: GrowthStage): List<Bitmap> {
        if (stage == GrowthStage.EGG || stage == GrowthStage.COCOON) return listOf(original)
        val eyes = locations(species, stage)
        val width = original.width
        val height = original.height
        val source = IntArray(width * height)
        original.getPixels(source, 0, width, 0, 0, width, height)

        // Four prepared intermediate eyelid positions. No bitmap allocation per frame.
        return (0..4).map { step ->
            if (step == 0) original
            else {
                val changed = source.copyOf()
                val amount = step / 4f
                warpEye(source, changed, width, height, eyes.left, amount)
                warpEye(source, changed, width, height, eyes.right, amount)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    setPixels(changed, 0, width, 0, 0, width, height)
                }
            }
        }
    }

    private fun warpEye(
        source: IntArray, target: IntArray,
        width: Int, height: Int, eye: Eye, closure: Float
    ) {
        val x0 = max(0, floor(eye.x - eye.rx).toInt())
        val x1 = min(width - 1, ceil(eye.x + eye.rx).toInt())
        val y0 = max(0, floor(eye.y - eye.ry).toInt())
        val y1 = min(height - 1, ceil(eye.y + eye.ry).toInt())
        val openness = 1f - closure * .94f

        for (y in y0..y1) {
            val dy = (y - eye.y) / eye.ry
            for (x in x0..x1) {
                val dx = (x - eye.x) / eye.rx
                val dist = dx * dx + dy * dy
                if (dist >= 1f) continue

                // Inverse vertical sampling compresses the EXISTING eye, while
                // borrowing its surrounding fur at the top/bottom of the oval.
                val sampleY = (eye.y + (y - eye.y) / openness)
                    .toInt().coerceIn(y0, y1)
                val oldColor = source[y * width + x]
                val newColor = source[sampleY * width + x]
                // Feather the region edge so no square/oval patch boundary appears.
                val feather = ((1f - dist) / .23f).coerceIn(0f, 1f)
                target[y * width + x] = mix(oldColor, newColor, feather)
            }
        }
    }

    private fun mix(a: Int, b: Int, amount: Float): Int {
        if (amount <= 0f) return a
        if (amount >= 1f) return b
        val ai = 1f - amount
        val alpha = (((a ushr 24) and 255) * ai + ((b ushr 24) and 255) * amount).toInt()
        val red = (((a ushr 16) and 255) * ai + ((b ushr 16) and 255) * amount).toInt()
        val green = (((a ushr 8) and 255) * ai + ((b ushr 8) and 255) * amount).toInt()
        val blue = ((a and 255) * ai + (b and 255) * amount).toInt()
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
