package com.pixubi.game

import android.graphics.Bitmap

/**
 * Real frames made from the pixels of the original animal illustration.
 * Closed eyelids REPLACE the open-eye pixels inside the bitmap. There is never
 * a second Compose Canvas of eyes above the character.
 */
internal object NativeSpriteEyes {
    private data class PairOfEyes(val first: EyeRegion, val second: EyeRegion)

    // Coordinates in the ORIGINAL 192x192 baby/teen and 160x160 adult atlas cells.
    // The eye positions genuinely differ between species and evolution stages.
    private fun eyes(species: Species, stage: GrowthStage): PairOfEyes {
        val values = when (stage) {
            GrowthStage.BABY -> when (species) {
                Species.LISYK -> floatArrayOf(73f, 125f, 102f, 19f, 19f)
                Species.ISKRYK -> floatArrayOf(66f, 127f, 103f, 19f, 19f)
                Species.KRAPLYK -> floatArrayOf(70f, 122f, 107f, 19f, 18f)
                Species.PISHCHANYK -> floatArrayOf(67f, 126f, 107f, 18f, 19f)
                Species.KHMARYK -> floatArrayOf(71f, 127f, 101f, 19f, 19f)
            }
            GrowthStage.YOUNG -> when (species) {
                Species.LISYK -> floatArrayOf(79f, 126f, 90f, 13f, 11f)
                Species.ISKRYK -> floatArrayOf(70f, 124f, 100f, 17f, 18f)
                Species.KRAPLYK -> floatArrayOf(80f, 119f, 102f, 16f, 17f)
                Species.PISHCHANYK -> floatArrayOf(72f, 122f, 102f, 16f, 18f)
                Species.KHMARYK -> floatArrayOf(73f, 122f, 99f, 16f, 17f)
            }
            GrowthStage.ADULT -> when (species) {
                Species.LISYK -> floatArrayOf(73f, 111f, 72f, 14f, 15f)
                Species.ISKRYK -> floatArrayOf(71f, 112f, 72f, 14f, 15f)
                Species.KRAPLYK -> floatArrayOf(72f, 111f, 71f, 15f, 15f)
                Species.PISHCHANYK -> floatArrayOf(72f, 112f, 72f, 14f, 16f)
                Species.KHMARYK -> floatArrayOf(70f, 111f, 72f, 15f, 16f)
            }
            else -> error("Eggs and cocoons have no painted eyes")
        }
        val left = EyeRegion(values[0], values[2], values[3], values[4])
        val right = EyeRegion(values[1], values[2], values[3], values[4])
        return PairOfEyes(left, right)
    }

    /** Precompute once per illustration; drawing a frame never runs a pixel loop. */
    fun frames(original: Bitmap, species: Species, stage: GrowthStage): List<Bitmap> {
        if (stage == GrowthStage.EGG || stage == GrowthStage.COCOON) return listOf(original)
        val width = original.width
        val height = original.height
        val input = IntArray(width * height)
        original.getPixels(input, 0, width, 0, 0, width, height)
        val e = eyes(species, stage)
        return (0..4).map { frame ->
            if (frame == 0) original // Open-eye image is 100% untouched.
            else {
                val pixels = EyePixelAnimator.frame(
                    input, width, height, listOf(e.first, e.second), frame
                )
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    setPixels(pixels, 0, width, 0, 0, width, height)
                }
            }
        }
    }
}
