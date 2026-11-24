package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class ImportedFrameCacheKeyTest {
    private val f=ImportedPixuArt.Form("test",setOf("neutral","happy","sad","jump","walk_a","walk_b","sleep"),emptyList(),true)
    private val k=PixuFrameKey(Species.LISYK,GrowthStage.BABY,Sex.GIRL,0,SpriteClip.IDLE,0,PetEmotion.IDLE,0)
    @Test fun staticAuthoredEmotionCollapsesUnusedInputs(){
        val happy=k.copy(clip=SpriteClip.HAPPY,emotion=PetEmotion.HAPPY)
        val keys=(0..3).flatMap{i->(0..4).map{c->ImportedFrameCacheKey.normalize(happy.copy(frame=i,closure=c),f)}}.toSet()
        assertEquals(1,keys.size)
    }
    @Test fun dynamicNeutralAndAuthoredActionsKeepRelevantPhases(){
        val neutral=(0..3).flatMap{i->(0..4).map{c->ImportedFrameCacheKey.normalize(k.copy(frame=i,closure=c),f)}}.toSet()
        assertEquals(20,neutral.size)
        val walk=k.copy(clip=SpriteClip.WALK)
        assertNotEquals(ImportedFrameCacheKey.normalize(walk,f),ImportedFrameCacheKey.normalize(walk.copy(frame=1),f))
        val eat=k.copy(clip=SpriteClip.EAT,emotion=PetEmotion.EATING)
        assertNotEquals(ImportedFrameCacheKey.normalize(eat,f),ImportedFrameCacheKey.normalize(eat.copy(frame=1),f))
        val sleep=k.copy(clip=SpriteClip.SLEEP,emotion=PetEmotion.SLEEPING)
        assertNotEquals(ImportedFrameCacheKey.normalize(sleep,f),ImportedFrameCacheKey.normalize(sleep.copy(frame=2),f))
        val h=sleep.copy(emotion=PetEmotion.HIBERNATING)
        assertEquals(ImportedFrameCacheKey.normalize(h,f),ImportedFrameCacheKey.normalize(h.copy(frame=2,closure=3),f))
    }
}
