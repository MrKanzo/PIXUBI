package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 5 species x 2 sexes x 6 color genes = 60 full-body sprites.
 * The original atlas is pixel art and ships in the APK (no network).
 * Atlas order: columns = Species.entries; rows 0..5 boys,
 * rows 6..11 girls; within each block 6 genetic colors.
 */
object PixubiSpriteIndex {
    const val CELL = 160
    const val COLUMNS = 5
    const val ROWS = 12
    fun column(species: Species) = species.ordinal
    fun row(sex: Sex, colorIndex: Int) =
        (if (sex == Sex.GIRL) 6 else 0) + colorIndex.coerceIn(0, 5)
    fun position(species: Species, sex: Sex, colorIndex: Int) =
        row(sex, colorIndex) * COLUMNS + column(species)
}

private object PixubiSpriteCache {
    private var atlas: Bitmap? = null
    private val cells = mutableMapOf<Int, Bitmap>()

    @Synchronized
    fun get(context: Context, species: Species, sex: Sex, colorIndex: Int): Bitmap {
        val slot = PixubiSpriteIndex.position(species, sex, colorIndex)
        cells[slot]?.let { return it }
        val source = atlas ?: context.assets.open("pixubi_avatars.png").use { input ->
            BitmapFactory.decodeStream(input)
                ?: error("PIXUBI sprite atlas could not be decoded")
        }.also {
            require(it.width == PixubiSpriteIndex.CELL * PixubiSpriteIndex.COLUMNS &&
                it.height == PixubiSpriteIndex.CELL * PixubiSpriteIndex.ROWS) {
                "PIXUBI atlas has unexpected dimensions"
            }
            atlas = it
        }
        val x = PixubiSpriteIndex.column(species) * PixubiSpriteIndex.CELL
        val y = PixubiSpriteIndex.row(sex, colorIndex) * PixubiSpriteIndex.CELL
        return Bitmap.createBitmap(source, x, y, PixubiSpriteIndex.CELL, PixubiSpriteIndex.CELL)
            .also { cells[slot] = it }
    }
}

/** Render real pixel sprites with lightweight Compose emotion overlays. */
@Composable
fun PixelPetSprite(
    species: Species,
    sex: Sex,
    coatIndex: Int,
    modifier: Modifier = Modifier,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    chewOpen: Boolean = false,
    motionPhase: Float = 0f,
    eyeClosure: Float = 0f,
    motion: PetMotion = PetMotion.IDLE
) {
    val appContext = LocalContext.current.applicationContext
    // The image itself has eyes. Build a few blink frames FROM its own pixels,
    // instead of drawing a second pair of eyes on top.
    val nativeFrames = remember(species, sex, coatIndex, emotion) {
        NativeSpriteEyes.frames(
            PixubiSpriteCache.get(appContext, species, sex, coatIndex),
            species, GrowthStage.ADULT, emotion
        )
    }
    val closure = NativeEyeMotion.closure(emotion, eyeClosure, blink)
    val frame = nativeFrames[FaceExpression.frameIndex(emotion,closure,chewOpen)]
    Box(modifier, contentAlignment = Alignment.Center) {
        ArticulatedPixuImage(
            bitmap = frame, species = species, stage = GrowthStage.ADULT,
            motion = motion, phase = motionPhase, description = species.title + " — " + sex.title,
            modifier = Modifier.fillMaxSize()
        )

        when (emotion) {
            PetEmotion.EATING -> Text(
                if (chewOpen) "🍓" else "🍪",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 9.dp).offset(y = 22.dp),
                fontSize = 24.sp
            )
            PetEmotion.DRINKING -> Text(
                "💧", modifier = Modifier.align(Alignment.CenterEnd).padding(end=7.dp)
                    .offset(y=20.dp), fontSize = 25.sp
            )
            PetEmotion.HAPPY -> Text(
                "💖", modifier = Modifier.align(Alignment.TopEnd).padding(top=5.dp, end=6.dp),
                fontSize = 22.sp
            )
            PetEmotion.SAD -> Text(
                "💧", modifier = Modifier.align(Alignment.CenterEnd).padding(end=14.dp)
                    .offset(y=(-5).dp), fontSize = 19.sp
            )
            PetEmotion.SLEEPING -> Text(
                "ᶻ ᶻ", modifier = Modifier.align(Alignment.TopEnd).padding(top=9.dp),
                color = Color(0xFFE7F9FF), fontSize = 23.sp, fontWeight=FontWeight.Bold
            )
            PetEmotion.HIBERNATING -> Text(
                "❄️", modifier = Modifier.align(Alignment.TopEnd).padding(7.dp), fontSize = 22.sp
            )
            PetEmotion.SURPRISED -> Text(
                "❕", modifier = Modifier.align(Alignment.TopEnd).padding(5.dp), fontSize = 24.sp
            )
            PetEmotion.TIRED -> Text(
                "💤", modifier = Modifier.align(Alignment.TopEnd).padding(5.dp), fontSize = 22.sp
            )
            PetEmotion.IDLE -> Unit
        }
    }
}
