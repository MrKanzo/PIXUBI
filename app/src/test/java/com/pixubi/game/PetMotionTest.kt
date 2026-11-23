package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetMotionTest {
    private fun pet(species: Species = Species.LISYK): Pet =
        GameEngine.hatch(species, Sex.GIRL, species.native, "Пікс", 1750000000000L, Random(9))
            .copy(activeMinutes = 15L, xp = 15)

    @Test fun allFiveSpeciesHaveDistinctSearchMotion() {
        val motions = Species.entries.map {
            MotionResolver.resolve(pet(it), PetEmotion.IDLE, searching = true)
        }
        assertEquals(5, motions.distinct().size)
        assertEquals(PetMotion.WALK, motions[Species.LISYK.ordinal])
        assertEquals(PetMotion.HOP, motions[Species.ISKRYK.ordinal])
        assertEquals(PetMotion.SWIM, motions[Species.KRAPLYK.ordinal])
        assertEquals(PetMotion.DIG, motions[Species.PISHCHANYK.ordinal])
        assertEquals(PetMotion.FLOAT, motions[Species.KHMARYK.ordinal])
    }

    @Test fun emotionDrivesBodyMovement() {
        assertEquals(PetMotion.BOUNCE, MotionResolver.resolve(pet(), PetEmotion.HAPPY, false))
        assertEquals(PetMotion.HOP, MotionResolver.resolve(pet(), PetEmotion.SURPRISED, false))
        assertEquals(PetMotion.CHEW, MotionResolver.resolve(pet(), PetEmotion.EATING, false))
        assertEquals(PetMotion.DRINK, MotionResolver.resolve(pet(), PetEmotion.DRINKING, false))
        assertEquals(PetMotion.SLOUCH, MotionResolver.resolve(pet(), PetEmotion.SAD, false))
        assertEquals(PetMotion.TIRED, MotionResolver.resolve(pet(), PetEmotion.TIRED, false))
        assertEquals(PetMotion.IDLE, MotionResolver.resolve(pet(), PetEmotion.IDLE, false))
    }

    @Test fun sleepAndHibernationOverrideAllAnimation() {
        assertEquals(PetMotion.SLEEP,
            MotionResolver.resolve(pet().copy(mode = Mode.SLEEP), PetEmotion.HAPPY, true))
        assertEquals(PetMotion.SLEEP,
            MotionResolver.resolve(pet().copy(mode = Mode.HIBERNATION), PetEmotion.HAPPY, true))
        assertEquals(PetMotion.SLEEP,
            MotionResolver.resolve(pet().copy(alive = false), PetEmotion.HAPPY, true))
    }

    @Test fun movementCyclesAreStableAndJumpReallyLeavesGround() {
        for (motion in PetMotion.entries) {
            assertEquals(MotionResolver.pose(motion, 0f), MotionResolver.pose(motion, 1f))
            for (i in 0..20) {
                val pos = MotionResolver.pose(motion, i / 20f)
                assertTrue(pos.scaleX in .75f..1.25f)
                assertTrue(pos.scaleY in .75f..1.25f)
                assertTrue(pos.rotationDegrees in -20f..20f)
            }
        }
        assertTrue(MotionResolver.pose(PetMotion.HOP, .25f).yDp < -15f)
        assertTrue(MotionResolver.pose(PetMotion.BOUNCE, .25f).yDp < -10f)
        assertNotEquals(MotionResolver.pose(PetMotion.WALK, .25f),
            MotionResolver.pose(PetMotion.FLOAT, .25f))
    }

    @Test fun blinkIsAnImageFrameChangeNotASecondPairOfEyes() {
        assertEquals(0, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.IDLE, 0f)))
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.SLEEPING, 0f)))
        assertEquals(4, NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.IDLE, 1f)))
        assertTrue(NativeEyeMotion.frameIndex(NativeEyeMotion.closure(PetEmotion.TIRED, 0f)) > 0)
    }
}
