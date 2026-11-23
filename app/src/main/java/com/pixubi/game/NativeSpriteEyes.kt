package com.pixubi.game

import android.graphics.Bitmap

/**
 * Generate eyelid frames from the actual image pixels once per species.
 * The original eyes are NOT overlaid with a new pair: the eye region of the
 * original illustration is replaced during the blink, and fully intact at rest.
 */
internal object NativeSpriteEyes {
    fun frames(original: Bitmap, species: Species, stage: GrowthStage): List<Bitmap> {
        if (stage == GrowthStage.EGG || stage == GrowthStage.COCOON) return listOf(original)
        val width = original.width
        val height = original.height
        val source = IntArray(width * height)
        original.getPixels(source, 0, width, 0, 0, width, height)
        val eyeRegions = NativeEyeGeometry.regions(species, stage)
        return (0..4).map { step ->
            if (step == 0) original
            else {
                val pixels = EyePixelAnimator.frame(source, width, height, eyeRegions, step)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    setPixels(pixels, 0, width, 0, 0, width, height)
                }
            }
        }
    }
}
