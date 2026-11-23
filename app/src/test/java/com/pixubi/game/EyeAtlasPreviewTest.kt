package com.pixubi.game

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pixel-for-pixel visual regression preview using the REAL atlas illustrations.
 * Host JVM can decode PNGs directly without Android emulation. The same pure
 * EyePixelAnimator used by the game builds the sample animation.
 */
class EyeAtlasPreviewTest {
    @Test fun previewLisykBlinkFromRealBabyTeenAndFemaleAdultSprites() {
        val evolution = ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val adults = ImageIO.read(File("src/main/assets/pixubi_avatars.png"))
        val cases = listOf(
            Triple(evolution.getSubimage(0, 0, 192, 192), 192,
                listOf(EyeRegion(73f, 102f, 19f, 19f), EyeRegion(125f, 102f, 19f, 19f))),
            Triple(evolution.getSubimage(0, 192, 192, 192), 192,
                listOf(EyeRegion(70f, 94f, 16f, 17f), EyeRegion(118f, 94f, 16f, 17f))),
            Triple(adults.getSubimage(0, 160 * 6, 160, 160), 160,
                listOf(EyeRegion(73f, 72f, 14f, 15f), EyeRegion(111f, 72f, 14f, 15f)))
        )
        val preview = BufferedImage(192 * 5, 192 + 192 + 160, BufferedImage.TYPE_INT_ARGB)
        val graphic = preview.createGraphics()
        var y = 0
        for ((base, cell, eyes) in cases) {
            val original = IntArray(cell * cell)
            base.getRGB(0, 0, cell, cell, original, 0, cell)
            for (index in 0..4) {
                val pixels = EyePixelAnimator.frame(original, cell, cell, eyes, index)
                val output = BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB)
                output.setRGB(0, 0, cell, cell, pixels, 0, cell)
                // Keep the stage's own original resolution, align in 192px cells.
                graphic.drawImage(output, index * 192 + (192 - cell) / 2, y, null)
                if (index > 0)
                    assertTrue("Blink stage $index must modify the original sprite",
                        !pixels.contentEquals(original))
            }
            y += cell
        }
        graphic.dispose()
        val target = File("build/eye-preview/lisyk-eye-frames.png")
        target.parentFile.mkdirs()
        assertTrue(ImageIO.write(preview, "png", target))
        assertTrue(target.length() > 0L)
    }
}
