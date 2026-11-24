package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class DeveloperModeTest {
    @Test fun syntheticPreviewSupportsEveryLockedFormWithoutUnlockingIt() {
        val real=DevSpec().pet(DevAction.IDLE)
        val originalCollection=CollectionRules.observe(PlayerCollection(),real)
        for(s in Species.entries)for(g in GrowthStage.entries)for(sex in Sex.entries)for(color in 0..5){
            val test=DevSpec.from(real).copy(species=s,stage=g,sex=sex,color=color).pet(DevAction.EXPLORE)
            assertEquals(g,PetGrowth.stage(test))
            assertEquals(color,test.colorIndex)
            assertEquals(1L,test.expeditionStartedAt)
            assertEquals(0L,real.expeditionStartedAt)
        }
        assertEquals(2,originalCollection.unlockedCount)
        assertFalse(CollectionRules.hasForm(originalCollection,Species.KHMARYK,Sex.BOY,5,GrowthStage.ADULT))
    }
    @Test fun forcedActionsAndClockAreIndependentOfRealPet() {
        val real=DevSpec().pet(DevAction.IDLE)
        val test=DevSpec.from(real).copy(hunger=2,home=true).pet(DevAction.SLEEP)
        assertEquals(Mode.SLEEP,test.mode)
        assertEquals(Mode.AWAKE,real.mode)
        assertEquals(85.0,real.hunger,0.0)
        assertFalse(real.hasHome)
        assertEquals(PetEmotion.SAD,EmotionResolver.resolve(DevSpec(hunger=2).pet(DevAction.AUTO)))
        assertEquals(.5f,DevScene(DevAction.WALK,1.25f,false).movementCycle,.0001f)
        assertEquals(.5f,DevScene(DevAction.EXPLORE,6f,false).scoutCycle,.0001f)
    }
}
