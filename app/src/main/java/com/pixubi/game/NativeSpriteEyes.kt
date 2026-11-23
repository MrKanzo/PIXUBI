package com.pixubi.game

import android.graphics.Bitmap

/** Build the clean face and all native eye frames once per displayed sprite. */
internal object NativeSpriteEyes {
    fun frames(original: Bitmap, species: Species, stage: GrowthStage): List<Bitmap> {
        if (stage == GrowthStage.EGG || stage == GrowthStage.COCOON) return List(5) { original }
        val width = original.width
        val height = original.height
        val source = IntArray(width * height)
        original.getPixels(source, 0, width, 0, 0, width, height)
        return EyePixelAnimator.frames(source, width, height,
            NativeEyeGeometry.regions(species, stage)).mapIndexed { index, pixels ->
            if (index == 0) original
            else Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }
    }
}
