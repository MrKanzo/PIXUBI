package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Test

class PetEmotionTest {
    private fun pet() = GameEngine.hatch(
        Species.LISYK, Sex.GIRL, Region.FOREST, "Тест", 123456789L
    )

    @Test fun idlePetIsIdle() {
        assertEquals(PetEmotion.IDLE, EmotionResolver.resolve(pet()))
    }
    @Test fun hungryPetShowsSadness() {
        assertEquals(PetEmotion.SAD, EmotionResolver.resolve(pet().copy(hunger = 12.0)))
    }
    @Test fun thirstGetsPriorityInBubble() {
        val sad = pet().copy(thirst = 10.0, hunger = 10.0)
        assertEquals("💧 Хочу пити", EmotionResolver.bubble(sad, EmotionResolver.resolve(sad)))
    }
    @Test fun tiredPetFeelsTired() {
        assertEquals(PetEmotion.TIRED, EmotionResolver.resolve(pet().copy(energy = 12.0)))
    }
    @Test fun temporaryEatingOverridesHunger() {
        assertEquals(PetEmotion.EATING, EmotionResolver.resolve(pet().copy(hunger = 3.0), PetEmotion.EATING))
    }
    @Test fun sleepOverridesTemporaryReaction() {
        assertEquals(PetEmotion.SLEEPING, EmotionResolver.resolve(pet().copy(mode = Mode.SLEEP), PetEmotion.HAPPY))
    }
    @Test fun hibernationOverridesTemporaryReaction() {
        assertEquals(PetEmotion.HIBERNATING, EmotionResolver.resolve(pet().copy(mode = Mode.HIBERNATION), PetEmotion.HAPPY))
    }
}
