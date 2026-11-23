package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow

/**
 * Separate illustrated babies, armored teenagers, existing adults, and cyber eggs/cocoons.
 * Stage changes replace the actual design rather than merely scaling an image.
 */
@Composable
fun GrowthSprite(
    pet: Pet,
    modifier: Modifier = Modifier,
    previewStage: GrowthStage? = null,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    chewOpen: Boolean = false
) {
    val stage = previewStage ?: PetGrowth.stage(pet)
    Box(modifier, contentAlignment = Alignment.Center) {
        when (stage) {
            GrowthStage.EGG, GrowthStage.COCOON ->
                ShellSprite(
                    pet.species, pet.colorIndex, stage,
                    Modifier.fillMaxSize()
                )
            GrowthStage.BABY, GrowthStage.YOUNG ->
                IllustratedYoungPixu(
                    species = pet.species,
                    sex = pet.sex,
                    colorIndex = pet.colorIndex,
                    stage = stage,
                    emotion = emotion,
                    modifier = Modifier.fillMaxSize()
                )
            GrowthStage.ADULT ->
                PixelPetSprite(
                    species = pet.species,
                    sex = pet.sex,
                    coatIndex = pet.colorIndex,
                    emotion = emotion,
                    blink = blink,
                    chewOpen = chewOpen,
                    modifier = Modifier.fillMaxSize()
                )
        }
        if (stage == GrowthStage.EGG && emotion == PetEmotion.HAPPY) {
            Text("✧", modifier = Modifier.align(Alignment.TopEnd), fontSize = 24.sp,
                color = Color(0xFFFFE8A9))
        }
        if (stage == GrowthStage.COCOON && emotion == PetEmotion.HIBERNATING) {
            Text("❄", modifier = Modifier.align(Alignment.TopEnd), fontSize = 25.sp,
                color = Color(0xFFDBF7FF))
        }
    }
}

@Composable
private fun ShellSprite(
    species: Species,
    colorIndex: Int,
    stage: GrowthStage,
    modifier: Modifier
) {
    val colors = listOf(
        Color(0xFF79D5BD), Color(0xFFFFAB77), Color(0xFFB9A5FF),
        Color(0xFFFF98C4), Color(0xFF7FD0FB), Color(0xFFFFD475)
    )
    val body = colors[colorIndex.coerceIn(0, 5)]
    val edge = Color(0xFF23334F)
    val dark = Color(0xFF385A77)
    val gleam = Color(0xFFF1F8FF)
    val light = when(species) {
        Species.LISYK -> Color(0xFF42F0B5)
        Species.ISKRYK -> Color(0xFFFFE05B)
        Species.KRAPLYK -> Color(0xFF53DFFF)
        Species.PISHCHANYK -> Color(0xFFFFB46B)
        Species.KHMARYK -> Color(0xFFC9A7FF)
    }

    Canvas(modifier) {
        val grid = 30
        val unit = size.minDimension / grid
        val originX = (size.width - grid * unit) / 2f
        val originY = (size.height - grid * unit) / 2f
        fun pixel(x: Int, y: Int, color: Color) {
            drawRect(color, Offset(originX + unit * x, originY + unit * y),
                Size(unit * 1.02f, unit * 1.02f))
        }
        // Tiny circuit halo and shadow ground the pixel creature in the world.
        for (i in 0..6) {
            val x = 4 + i * 3
            pixel(x, 24 + (i % 2), light.copy(alpha = .45f))
        }
        for (y in 3..25) {
            for (x in 3..26) {
                val xx = (x - 14.5) / (if(stage == GrowthStage.EGG) 9.1 else 7.2)
                val yy = (y - 14.0) / (if(stage == GrowthStage.EGG) 11.5 else 11.8)
                val rounded = xx * xx + yy * yy
                val topNarrow = if (y < 11 && stage == GrowthStage.EGG) .10 else 0.0
                val inside = rounded + topNarrow < .99
                if (!inside) continue
                val boundary = rounded + topNarrow > .76
                val shade = when {
                    boundary -> edge
                    x > 19 -> dark
                    x < 10 && y < 12 -> gleam
                    else -> body
                }
                pixel(x, y, shade)
            }
        }

        if (stage == GrowthStage.EGG) {
            // Broken zigzag circuit, symbolizing a future hatch.
            for (x in 9..20) {
                val y = if (x % 4 <= 1) 16 else 15
                pixel(x, y, edge)
                pixel(x, y - 1, light)
            }
            for (i in 0..3) {
                pixel(11 + i, 9, light)
                pixel(10 + i, 10, light)
            }
            pixel(14, 18, light)
            pixel(15, 18, light)
            pixel(14, 19, light)
            pixel(15, 19, light)
        } else {
            // Cocoon mesh / digital chrysalis, brighter than an egg.
            for (y in 7..22 step 3) {
                for (x in 10..19) {
                    if ((x + y) % 3 == 0) pixel(x, y, light.copy(alpha = .90f))
                }
            }
            for (y in 10..19) {
                pixel(14, y, if (y % 2 == 0) gleam else light)
                pixel(15, y, light)
            }
            for (x in 12..17) {
                pixel(x, 4, if (x % 2 == 0) light else edge)
                pixel(x, 25, light)
            }
        }
    }
}
