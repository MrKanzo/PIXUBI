package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Two independently illustrated stages for each species:
 * row 0 = baby, row 1 = serious armored juvenile.
 * The mature stage continues to use its original male/female 60-sprite atlas.
 */
object EvolutionSpriteIndex {
    const val CELL = 192
    const val COLUMNS = 5
    const val ROWS = 2

    fun slot(species: Species, stage: GrowthStage): Int {
        require(stage == GrowthStage.BABY || stage == GrowthStage.YOUNG)
        return species.ordinal + if (stage == GrowthStage.YOUNG) 5 else 0
    }
}

private object EvolutionSprites {
    private var atlas: Bitmap? = null
    private val cells = mutableMapOf<Int, Bitmap>()

    @Synchronized
    fun get(context: Context, species: Species, stage: GrowthStage): Bitmap {
        val id = EvolutionSpriteIndex.slot(species, stage)
        cells[id]?.let { return it }
        val source = atlas ?: context.assets.open("pixubi_evolution_10.png").use { stream ->
            BitmapFactory.decodeStream(stream) ?: error("PIXUBI evolution atlas not decoded")
        }.also {
            require(it.width == EvolutionSpriteIndex.COLUMNS * EvolutionSpriteIndex.CELL &&
                    it.height == EvolutionSpriteIndex.ROWS * EvolutionSpriteIndex.CELL) {
                "Unexpected evolution atlas dimensions"
            }
            atlas = it
        }
        val x = species.ordinal * EvolutionSpriteIndex.CELL
        val y = (if (stage == GrowthStage.BABY) 0 else 1) * EvolutionSpriteIndex.CELL
        return Bitmap.createBitmap(
            source, x, y, EvolutionSpriteIndex.CELL, EvolutionSpriteIndex.CELL
        ).also { cells[id] = it }
    }
}

@Composable
fun IllustratedYoungPixu(
    species: Species,
    sex: Sex,
    colorIndex: Int,
    stage: GrowthStage,
    modifier: Modifier = Modifier,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    eyeClosure: Float = 0f,
    chewOpen: Boolean = false,
    motion: PetMotion = PetMotion.IDLE,
    motionPhase: Float = 0f
) {
    val geneAccent = listOf(
        Color(0xFF75DDCB), Color(0xFFFFB37A), Color(0xFFC5A8FF),
        Color(0xFFFFA1CE), Color(0xFF92CCFF), Color(0xFFFAD17B)
    )[colorIndex.coerceIn(0,5)]
    Box(modifier, contentAlignment = Alignment.Center) {
        FramePetSprite(species,stage,sex,colorIndex,emotion,motion,
            motionPhase,blink,eyeClosure,Modifier.fillMaxSize())
        // The inherited gene color is a distinct glowing badge until the fully
        // individualized adult appearance is revealed.
        Text("◆", color = geneAccent, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start=7.dp,bottom=5.dp))
        if (sex == Sex.GIRL) {
            Text("🎀", fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top=7.dp,end=7.dp))
        }
        if (emotion == PetEmotion.HAPPY) {
            Text("💖",fontSize=18.sp,
                modifier=Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        if (emotion == PetEmotion.SLEEPING || emotion == PetEmotion.HIBERNATING) {
            Text("💤",fontSize=21.sp,
                modifier=Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}
