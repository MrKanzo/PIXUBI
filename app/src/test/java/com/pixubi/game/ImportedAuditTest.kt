package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class ImportedAuditTest {
    @Test fun neutralOnlyBlinkDoesNotUseWrongPoseGeometry(){
        val k=PixuFrameKey(Species.LISYK,GrowthStage.BABY,Sex.BOY,0,SpriteClip.IDLE,0,PetEmotion.IDLE,4)
        for(clip in listOf(SpriteClip.IDLE,SpriteClip.WALK,SpriteClip.SWIM,SpriteClip.FLY,SpriteClip.DIG)){
            assertTrue(ImportedEyeGeometry.applies(k.copy(clip=clip),"neutral"))
            assertFalse(ImportedEyeGeometry.applies(k.copy(clip=clip),"walk_a"))
        }
        assertFalse(ImportedEyeGeometry.applies(k.copy(closure=0),"neutral"))
        assertFalse(ImportedEyeGeometry.applies(k.copy(emotion=PetEmotion.HAPPY),"neutral"))
    }
    @Test fun frameAdvanceNeverSkipsGaitTicks(){
        var s=0f
        for(i in 1..480){
            s=GaitReviewRoute.nextFrame(s)
            assertEquals(i%480,(s*10f).toInt())
        }
    }
    @Test fun auditDoesNotCallExperimentalGaitAuthored(){
        val f=ImportedPixuArt.Form("test",setOf("neutral"),emptyList(),true)
        val s=Species.LISYK
        assertTrue(ImportedActionAudit.describe(f,s,GrowthStage.BABY,Sex.BOY,DevAction.WALK,true).contains("Експериментальні"))
        assertTrue(ImportedActionAudit.describe(f,s,GrowthStage.BABY,Sex.BOY,DevAction.WALK,false).contains("без прогулянки"))
        assertTrue(ImportedActionAudit.describe(f,s,GrowthStage.BABY,Sex.BOY,DevAction.SLEEP,false).contains("немає пози"))
        assertTrue(ImportedActionAudit.describe(f.copy(frames=setOf("neutral","walk_a","walk_b")),s,GrowthStage.BABY,Sex.GIRL,DevAction.WALK,false).contains("оригінальні"))
    }
}
