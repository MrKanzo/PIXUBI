package com.pixubi.game

/** Kraplyk's baby land poses use the original illustration, not stretched body
 * sprites. Face pixels are supplied by the native expression atlas. */
internal object KraplykLandFrames {
    const val SIZE = KraplykSwimFrames.SIZE
    fun matches(s: Species, g: GrowthStage) =
        s == Species.KRAPLYK && g == GrowthStage.BABY

    fun locomotion(c: SpriteClip) =
        c in listOf(SpriteClip.WALK, SpriteClip.HOP, SpriteClip.DIG)

    fun faceEmotion(clip: SpriteClip, emotion: PetEmotion): PetEmotion =
        if (clip == SpriteClip.SLEEP) PetEmotion.SLEEPING else emotion

    fun faceIndex(clip: SpriteClip, emotion: PetEmotion, frame: Int,
                  closure: Float, blink: Boolean): Int {
        val e = faceEmotion(clip, emotion)
        return FaceExpression.frameIndex(e, NativeEyeMotion.closure(e, closure, blink),
            frame.mod(2) == 1)
    }

    fun pose(source: IntArray, clip: SpriteClip, frame: Int): IntArray {
        require(source.size == SIZE * SIZE)
        val i = frame.mod(clip.count)
        if (clip == SpriteClip.SWIM) return KraplykSwimFrames.pose(source, i)
        if (locomotion(clip) || clip == SpriteClip.HAPPY) {
            return IllustratedMotionFrames.frame(source, SIZE, Species.KRAPLYK,
                GrowthStage.BABY, clip, i)
        }
        val out = source.copyOf()
        // During rest both paws tuck under the body. Tiredness uses a less
        // pronounced crouch. The head, eyes, mouth and torso are untouched.
        if (clip == SpriteClip.SLEEP || clip == SpriteClip.TIRED || clip == SpriteClip.SAD) {
            val lift = when (clip) {
                SpriteClip.SLEEP -> 5 + i
                SpriteClip.TIRED -> 2 + i % 2
                else -> 2
            }
            tuck(out, source, 65, 88, lift)
            tuck(out, source, 102, 124, lift)
        }
        // A small independent gill pulse, visible even while sitting still.
        val dy = when (clip) {
            SpriteClip.SLEEP -> if (i == 0) 0 else 2
            SpriteClip.SURPRISE -> if (i == 0) -3 else 0
            else -> intArrayOf(0, -2, 0, 2)[i.mod(4)]
        }
        if (dy != 0) {
            for (x in 0 until SIZE) {
                val d = when {
                    x < 43 -> dy * (43 - x) / 43
                    x > 149 -> -dy * (x - 149) / (SIZE - 150)
                    else -> 0
                }
                if (d == 0) continue
                for (y in 0 until 116) out[y * SIZE + x] = 0
                for (y in 0 until 116) {
                    val dest = y + d
                    val pixel = source[y * SIZE + x]
                    if (dest in 0 until 116 && pixel ushr 24 != 0)
                        out[dest * SIZE + x] = pixel
                }
            }
        }
        return out
    }

    private fun tuck(out: IntArray, source: IntArray, left: Int, right: Int, lift: Int) {
        for (y in 145 until 170) for (x in left until right) out[y * SIZE + x] = 0
        for (y in 145 until 170) for (x in left until right) {
            val c = source[y * SIZE + x]
            if (c ushr 24 != 0) out[(y - lift) * SIZE + x] = c
        }
    }
}
