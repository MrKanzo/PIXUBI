package com.pixubi.game

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Eye regions are confined to existing original-art iris/eyelid pixels. */
internal data class EyeRegion(
    val centerX: Float, val centerY: Float, val radiusX: Float, val radiusY: Float
)

/**
 * In-place texture editing, NOT a new pair of vector eyes.
 *
 * For each eye, interpolate non-eye fur pixels from just above and below the
 * ORIGINAL painted eye. The upper eyelid advances down the eye to cover its
 * already-painted iris/pupil. The last frame draws a small closed-eyelid crease
 * inside that replaced eye region. All pixels outside the masks remain identical.
 */
internal object EyePixelAnimator {
    fun frame(
        original: IntArray, width: Int, height: Int,
        eyes: List<EyeRegion>, step: Int
    ): IntArray {
        require(width > 0 && height > 0 && original.size == width * height)
        require(step in 0..4)
        val output = original.copyOf()
        if (step == 0) return output

        for (eye in eyes) {
            val x0 = max(0, floor(eye.centerX - eye.radiusX).toInt())
            val x1 = min(width - 1, ceil(eye.centerX + eye.radiusX).toInt())
            val y0 = max(0, floor(eye.centerY - eye.radiusY).toInt())
            val y1 = min(height - 1, ceil(eye.centerY + eye.radiusY).toInt())
            val topSampleY = (eye.centerY - eye.radiusY - 5f).toInt().coerceIn(0, height - 1)
            val bottomSampleY = (eye.centerY + eye.radiusY + 5f).toInt().coerceIn(0, height - 1)
            val progress = step / 4f

            for (x in x0..x1) {
                val dx = (x - eye.centerX) / eye.radiusX
                if (dx * dx >= 1f) continue
                val halfHeight = sqrt(1f - dx * dx) * eye.radiusY
                val top = eye.centerY - halfHeight
                val bottom = eye.centerY + halfHeight
                val lid = top + (bottom - top) * progress

                for (y in max(y0, floor(top).toInt())..min(y1, ceil(bottom).toInt())) {
                    val dy = (y - eye.centerY) / eye.radiusY
                    val distance = dx * dx + dy * dy
                    if (distance >= 1f) continue
                    val index = y * width + x
                    val originalColor = original[index]
                    // Do not fill transparency outside the pet silhouette.
                    if (((originalColor ushr 24) and 255) < 180) continue

                    val lidOpacity = ((lid - y + 1f) / 2f).coerceIn(0f, 1f)
                    if (lidOpacity <= 0f) continue
                    val feather = ((1f - distance) * 14f).coerceIn(0f, 1f)
                    val opacity = lidOpacity * feather
                    if (opacity <= 0f) continue

                    val topColor = original[topSampleY * width + x]
                    val bottomColor = original[bottomSampleY * width + x]
                    val color = when {
                        ((topColor ushr 24) and 255) < 180 -> bottomColor
                        ((bottomColor ushr 24) and 255) < 180 -> topColor
                        else -> {
                            val position = ((y - (eye.centerY - eye.radiusY)) /
                                (2f * eye.radiusY)).coerceIn(0f, 1f)
                            blend(topColor, bottomColor, position)
                        }
                    }
                    if (((color ushr 24) and 255) >= 180)
                        output[index] = blend(originalColor, color, opacity)
                }
            }

            // Draw the eyelid crease INSIDE the overwritten eye region, only when
            // fully shut. It replaces the open pupil rather than sitting above it.
            if (step == 4) {
                val ink = selectNativeOutline(original, width, height, eye)
                val minX = ceil(eye.centerX - eye.radiusX * .72f).toInt().coerceIn(0, width - 1)
                val maxX = floor(eye.centerX + eye.radiusX * .72f).toInt().coerceIn(0, width - 1)
                for (x in minX..maxX) {
                    val nx = (x - eye.centerX) / eye.radiusX
                    val arch = (1f - nx * nx).coerceAtLeast(0f) * eye.radiusY * .11f
                    val centerLine = (eye.centerY + arch).toInt().coerceIn(0, height - 1)
                    for (y in centerLine..min(height - 1, centerLine + 1)) {
                        val i = y * width + x
                        if (((original[i] ushr 24) and 255) >= 180)
                            output[i] = ink
                    }
                }
            }
        }
        return output
    }

    private fun selectNativeOutline(
        input: IntArray, width: Int, height: Int, eye: EyeRegion
    ): Int {
        val centerX = eye.centerX.toInt().coerceIn(0, width - 1)
        val y0 = (eye.centerY - eye.radiusY * .5f).toInt().coerceIn(0, height - 1)
        val y1 = (eye.centerY + eye.radiusY * .5f).toInt().coerceIn(0, height - 1)
        var best = 0xFF273047.toInt()
        var dark = Int.MAX_VALUE
        for (y in y0..y1) for (x in
            max(0, centerX - eye.radiusX.toInt())..min(width - 1, centerX + eye.radiusX.toInt())) {
            val c = input[y * width + x]
            if (((c ushr 24) and 255) < 240) continue
            val lum = ((c ushr 16) and 255) * 3 + ((c ushr 8) and 255) * 5 + (c and 255) * 2
            if (lum < dark) { dark = lum; best = c }
        }
        return best
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val g = 1f - f
        val alpha = ((((a ushr 24) and 255) * g) + (((b ushr 24) and 255) * f)).toInt()
        val red = ((((a ushr 16) and 255) * g) + (((b ushr 16) and 255) * f)).toInt()
        val green = ((((a ushr 8) and 255) * g) + (((b ushr 8) and 255) * f)).toInt()
        val blue = (((a and 255) * g) + ((b and 255) * f)).toInt()
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
