package com.pixubi.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Four real creature stages, shared by home, onboarding, collection and developer preview. */
@Composable
fun GrowthSprite(
    pet: Pet,
    modifier: Modifier = Modifier,
    previewStage: GrowthStage? = null,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    chewOpen: Boolean = false,
    motion: PetMotion = PetMotion.IDLE,
    motionPhase: Float = 0f,
    eyeClosure: Float = 0f,
    facingLeft: Boolean = false
) {
    val stage = GrowthStage.migrate(previewStage ?: PetGrowth.stage(pet))
    Box(modifier, contentAlignment = Alignment.Center) {
        FramePetSprite(pet.species,stage,pet.sex,pet.colorIndex,emotion,motion,
            motionPhase,blink,eyeClosure,Modifier.fillMaxSize(),facingLeft)
    }
}
