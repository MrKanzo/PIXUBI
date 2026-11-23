package com.pixubi.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Native pixel coordinates, with a separate eyelid shape for each eye. */
internal data class EyeRegion(
    val centerX: Float, val centerY: Float, val radiusX: Float, val radiusY: Float,
    val lidCurve: Float = .20f, val lidTilt: Float = 0f
)

/**
 * Build an eyeless face from the local texture around each actual eye, then
 * extract the original iris as a separate, compressible eye layer.
 * No flat fur-colored ovals or second eyes on top of the painted eyes.
 */
internal object EyePixelAnimator {
    fun frame(original: IntArray, width: Int, height: Int,
              eyes: List<EyeRegion>, step: Int): IntArray =
        frames(original, width, height, eyes)[step.also { require(it in 0..4) }]

    fun frames(original: IntArray, width: Int, height: Int,
               eyes: List<EyeRegion>): List<IntArray> {
        require(width > 0 && height > 0 && original.size == width * height)
        val output = List(5) { original.copyOf() }
        for (eye in eyes) {
            val patch = Patch(original, width, height, eye)
            for (step in 1..4) patch.paint(output[step], step)
        }
        return output
    }

    private class Patch(val original: IntArray, val width: Int, val height: Int,
                        val eye: EyeRegion) {
        val left = max(0, floor(eye.centerX - eye.radiusX - 2).toInt())
        val right = min(width - 1, ceil(eye.centerX + eye.radiusX + 2).toInt())
        val top = max(0, floor(eye.centerY - eye.radiusY - 2).toInt())
        val bottom = min(height - 1, ceil(eye.centerY + eye.radiusY + 2).toInt())
        val w = right - left + 1
        val h = bottom - top + 1
        val mask = BooleanArray(w * h)
        val native = IntArray(w * h)
        val clean = IntArray(w * h)
        val red = FloatArray(w * h)
        val green = FloatArray(w * h)
        val blue = FloatArray(w * h)
        val ink: Int

        init {
            for (j in 0 until h) for (i in 0 until w) {
                val k = j * w + i
                val c = original[(top + j) * width + left + i]
                native[k] = c
                red[k] = channel(c, 16).toFloat()
                green[k] = channel(c, 8).toFloat()
                blue[k] = channel(c, 0).toFloat()
                // Rounded rectangular mask encompasses the eyelashes as well
                // as the iris; its shape is never drawn onto the face.
                val nx = abs((left + i - eye.centerX) / eye.radiusX)
                val ny = abs((top + j - eye.centerY) / eye.radiusY)
                mask[k] = i in 1 until w - 1 && j in 1 until h - 1 &&
                    nx.pow(2.8f) + ny.pow(2.8f) < 1f && channel(c, 24) >= 180
            }

            // Seed from the real border of this exact eye (not a common palette
            // or a single forehead color). Then solve a smooth RGB field with
            // the surrounding pixels as fixed boundary conditions. This keeps
            // native cheek markings and shading flowing through the eye patch.
            var sr = 0f; var sg = 0f; var sb = 0f; var count = 0
            for (j in 1 until h - 1) for (i in 1 until w - 1) {
                val k = j * w + i
                if (!mask[k] && channel(native[k], 24) >= 180 &&
                    (mask[k-1] || mask[k+1] || mask[k-w] || mask[k+w])) {
                    sr += red[k]; sg += green[k]; sb += blue[k]; count++
                }
            }
            if (count > 0) for (k in mask.indices) if (mask[k]) {
                red[k] = sr / count; green[k] = sg / count; blue[k] = sb / count
            }
            repeat(180) {
                for (j in 1 until h - 1) for (i in 1 until w - 1) {
                    val k = j * w + i
                    if (!mask[k]) continue
                    // Preserve transparency: a transparent neighbor cannot
                    // contribute black to an otherwise opaque fur patch.
                    var r = 0f; var g = 0f; var b = 0f; var n = 0
                    fun add(a: Int) {
                        if (channel(native[a], 24) >= 180) {
                            r += red[a]; g += green[a]; b += blue[a]; n++
                        }
                    }
                    add(k-1); add(k+1); add(k-w); add(k+w)
                    if (n > 0) {
                        red[k] = r / n; green[k] = g / n; blue[k] = b / n
                    }
                }
            }
            var darkest = Int.MAX_VALUE
            var outline = 0xFF30283C.toInt()
            for (k in native.indices) {
                clean[k] = if (mask[k]) pack(channel(native[k],24), red[k],green[k],blue[k])
                    else native[k]
                if (mask[k]) {
                    val c = native[k]
                    val lum = 3*channel(c,16)+5*channel(c,8)+2*channel(c,0)
                    if (lum < darkest) { darkest = lum; outline = c }
                }
            }
            ink = outline
        }

        fun paint(output: IntArray, step: Int) {
            val anchor = eye.centerY + eye.radiusY * .24f
            val scale = 1f - step / 4f
            val halfWidth = eye.radiusX * .78f
            for (j in 1 until h - 1) for (i in 1 until w - 1) {
                val k = j*w+i
                if (!mask[k]) continue
                val x = left+i
                val y = top+j
                var c = clean[k]
                if (step < 4) {
                    // Transfer the eye's color difference relative to its clean
                    // face, rather than pasting a compressed block of fur.
                    val sy = anchor + (y - anchor) / scale - top
                    val lo = floor(sy).toInt()
                    val t = sy - lo
                    fun delta(row: Int, shift: Int): Float {
                        if (row !in 0 until h) return 0f
                        val q = row*w+i
                        if (!mask[q]) return 0f
                        return (channel(native[q],shift)-channel(clean[q],shift)).toFloat()
                    }
                    fun color(shift: Int): Float = channel(c,shift) +
                        delta(lo,shift)*(1f-t)+delta(lo+1,shift)*t
                    c = pack(channel(c,24),color(16),color(8),color(0))
                } else {
                    // Delicate curved sleeping eyelid; no filled oval, no iris
                    // outline, no visible boundary around the restored texture.
                    val nx = (x-eye.centerX)/halfWidth
                    if (abs(nx) <= 1.05f) {
                        val curveY = anchor + eye.radiusY * (
                            eye.lidCurve * (1f-nx*nx) + eye.lidTilt*nx)
                        val thickness = (eye.radiusY*.10f).coerceIn(.75f,1.65f)
                        val edge = ((1.05f-abs(nx))*5f).coerceIn(0f,1f)
                        val a = ((thickness+.55f-abs(y-curveY))/.9f)
                            .coerceIn(0f,1f)*edge*.93f
                        if (a>0f) c = pack(channel(c,24),
                            channel(c,16)*(1-a)+channel(ink,16)*a,
                            channel(c,8)*(1-a)+channel(ink,8)*a,
                            channel(c,0)*(1-a)+channel(ink,0)*a)
                    }
                }
                output[y*width+x] = c
            }
        }
    }

    private fun channel(c: Int, shift: Int) = (c ushr shift) and 255
    private fun pack(a: Int, r: Float, g: Float, b: Float): Int =
        (a shl 24) or (r.roundToInt().coerceIn(0,255) shl 16) or
        (g.roundToInt().coerceIn(0,255) shl 8) or b.roundToInt().coerceIn(0,255)
}
