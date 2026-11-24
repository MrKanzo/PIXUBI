package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ImportedPixuArtTest {
    @Test fun allFortyFormsHaveUniquePaths(){
        val paths=Species.entries.flatMap{s->GrowthStage.playable.flatMap{g->Sex.entries.map{sex->ImportedPixuArt.prefix(s,g,sex)}}}
        assertEquals(40,paths.toSet().size)
        assertEquals("pixubi/lisyk/male/young",ImportedPixuArt.prefix(Species.LISYK,GrowthStage.BABY,Sex.BOY))
        assertEquals("pixubi/lisyk/female/teen",ImportedPixuArt.prefix(Species.LISYK,GrowthStage.YOUNG,Sex.GIRL))
        assertEquals("young",ImportedPixuArt.stageName(GrowthStage.EGG))
        assertEquals("teen",ImportedPixuArt.stageName(GrowthStage.COCOON))
    }
    @Test fun supportedPosesOnly(){
        val full=ImportedPixuArt.Form("test",setOf("neutral","walk_a","walk_b","happy","jump","sleep"),emptyList(),true)
        assertEquals("walk_a",ImportedPixuArt.select(full,SpriteClip.WALK,PetEmotion.IDLE,0))
        assertEquals("walk_b",ImportedPixuArt.select(full,SpriteClip.WALK,PetEmotion.IDLE,1))
        assertEquals("sleep",ImportedPixuArt.select(full,SpriteClip.SLEEP,PetEmotion.SLEEPING,0))
        assertEquals("happy",ImportedPixuArt.select(full,SpriteClip.HAPPY,PetEmotion.HAPPY,0))
        assertNull(ImportedPixuArt.select(full,SpriteClip.EAT,PetEmotion.EATING,0))
        assertFalse(ImportedPixuArt.movement(full,SpriteClip.SWIM))
        assertFalse(ImportedPixuArt.movement(full.copy(frames=setOf("neutral","walk_a")),SpriteClip.WALK))
    }
    @Test fun ageScaleDoesNotAlterSaveStages(){
        assertEquals(listOf(128,147,166,192),GrowthStage.playable.map{ImportedPixuArt.size(it)})
    }
}
