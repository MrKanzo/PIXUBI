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
                NativeEyeGeometry.regions(Species.LISYK, GrowthStage.BABY)),
            Triple(evolution.getSubimage(0, 192, 192, 192), 192,
                NativeEyeGeometry.regions(Species.LISYK, GrowthStage.YOUNG)),
            Triple(adults.getSubimage(0, 160 * 6, 160, 160), 160,
                NativeEyeGeometry.regions(Species.LISYK, GrowthStage.ADULT))
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
    @Test fun mapYoungLisykRealEyeCoordinates() {
        val atlas = ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val zoom = BufferedImage(760, 550, BufferedImage.TYPE_INT_RGB)
        val g = zoom.createGraphics()
        val scale = 6
        val baseX = 45
        val baseY = 50
        val regionWidth = 114
        val regionHeight = 79
        val margin = 45
        g.color = java.awt.Color(15, 24, 43)
        g.fillRect(0, 0, zoom.width, zoom.height)
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        g.drawImage(
            atlas, margin, margin,
            margin + regionWidth * scale, margin + regionHeight * scale,
            baseX, 192 + baseY, baseX + regionWidth, 192 + baseY + regionHeight,
            null
        )
        g.font = java.awt.Font("Monospaced", java.awt.Font.BOLD, 12)
        for (x in 50..150 step 10) {
            val xx = margin + (x - baseX) * scale
            g.color = java.awt.Color(255, 255, 255, 85)
            g.drawLine(xx, margin, xx, margin + regionHeight * scale)
            g.color = java.awt.Color.WHITE
            g.drawString(x.toString(), xx - 9, 30)
        }
        for (y in 50..125 step 10) {
            val yy = margin + (y - baseY) * scale
            g.color = java.awt.Color(255, 255, 255, 85)
            g.drawLine(margin, yy, margin + regionWidth * scale, yy)
            g.color = java.awt.Color.WHITE
            g.drawString(y.toString(), 7, yy + 4)
        }
        g.color = java.awt.Color.RED
        g.stroke = java.awt.BasicStroke(2f)
        for ((x, y) in listOf(61 to 75, 90 to 75)) {
            val cx = margin + (x - baseX) * scale
            val cy = margin + (y - baseY) * scale
            g.drawOval(cx - 7, cy - 7, 14, 14)
            g.drawLine(cx - 12, cy, cx + 12, cy)
            g.drawLine(cx, cy - 12, cx, cy + 12)
        }
        g.dispose()
        val dest = File("build/eye-preview/lisyk-young-grid.png")
        dest.parentFile.mkdirs()
        assertTrue(ImageIO.write(zoom, "png", dest))
    }

    @Test fun gridForEveryIllustratedYoungSpecies() {
        val atlas = ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        for (species in Species.entries) {
            val zoom = BufferedImage(760, 550, BufferedImage.TYPE_INT_RGB)
            val g = zoom.createGraphics()
            val scale = 6
            val baseX = 45
            val baseY = 50
            val regionWidth = 114
            val regionHeight = 79
            val margin = 45
            g.color = java.awt.Color(15, 24, 43)
            g.fillRect(0, 0, zoom.width, zoom.height)
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            )
            g.drawImage(
                atlas, margin, margin,
                margin + regionWidth * scale, margin + regionHeight * scale,
                species.ordinal * 192 + baseX, 192 + baseY,
                species.ordinal * 192 + baseX + regionWidth,
                192 + baseY + regionHeight, null
            )
            g.font = java.awt.Font("Monospaced", java.awt.Font.BOLD, 12)
            for (x in 50..150 step 10) {
                val xx = margin + (x - baseX) * scale
                g.color = java.awt.Color(255, 255, 255, 85)
                g.drawLine(xx, margin, xx, margin + regionHeight * scale)
                g.color = java.awt.Color.WHITE
                g.drawString(x.toString(), xx - 9, 30)
            }
            for (y in 50..125 step 10) {
                val yy = margin + (y - baseY) * scale
                g.color = java.awt.Color(255, 255, 255, 85)
                g.drawLine(margin, yy, margin + regionWidth * scale, yy)
                g.color = java.awt.Color.WHITE
                g.drawString(y.toString(), 7, yy + 4)
            }
            g.dispose()
            val path = File("build/eye-preview/" + species.name.lowercase() + "-young-grid.png")
            path.parentFile.mkdirs()
            assertTrue(ImageIO.write(zoom, "png", path))
        }
    }

    @Test fun previewAllFiveYoungSpeciesActualEyes() {
        val atlas = ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val preview = BufferedImage(192 * 5, 192 * 5, BufferedImage.TYPE_INT_ARGB)
        val graphic = preview.createGraphics()
        for (species in Species.entries) {
            val cell = 192
            val source = atlas.getSubimage(species.ordinal * cell, cell, cell, cell)
            val original = IntArray(cell * cell)
            source.getRGB(0, 0, cell, cell, original, 0, cell)
            for (frameIndex in 0..4) {
                val pixels = EyePixelAnimator.frame(original, cell, cell,
                    NativeEyeGeometry.regions(species, GrowthStage.YOUNG), frameIndex)
                val frame = BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB)
                frame.setRGB(0, 0, cell, cell, pixels, 0, cell)
                graphic.drawImage(frame, frameIndex * cell, species.ordinal * cell, null)
                if (frameIndex > 0)
                    assertTrue("Species " + species.name + " has no blink frames",
                        !pixels.contentEquals(original))
            }
        }
        graphic.dispose()
        val output = File("build/eye-preview/all-young-eye-frames.png")
        output.parentFile.mkdirs()
        assertTrue(ImageIO.write(preview, "png", output))
    }

}
