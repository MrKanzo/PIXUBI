package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class OlderPixuFramesTest {
    @Test fun allOlderFormsHaveSpeciesSpecificMotionAndSeparateSexSilhouettes(){
        for(g in listOf(GrowthStage.ADULT,GrowthStage.ULTRA))for(s in Species.entries){
            val original=UltraPixuArtwork.pixels(s,g)
            val female=PixuSilhouette.female(original,48,48,s)
            assertFalse("$s $g female shape did not change",original.contentEquals(female))
            val main=when(s){
                Species.KRAPLYK->SpriteClip.SWIM
                Species.KHMARYK->SpriteClip.FLY
                Species.PISHCHANYK->SpriteClip.DIG
                else->SpriteClip.WALK
            }
            val frames=(0..3).map{OlderPixuFrames.motion(original,s,g,main,it)}
            assertTrue("$s $g has no actual motion",frames.any{!it.contentEquals(original)})
            frames.forEach{
                assertEquals(48*48,it.size)
                assertTrue(it.any{px->px==0})
                assertTrue(it.count{px->px ushr 24>=128}>300)
            }
            assertTrue(OlderPixuFrames.zones(s,g).any{
                it.part==when(s){Species.KRAPLYK->OlderPixuFrames.Part.FIN
                    Species.KHMARYK->OlderPixuFrames.Part.WING
                    else->OlderPixuFrames.Part.TAIL}
            })
            for(e in PetEmotion.entries)for(closure in 0..4){
                val face=OlderPixuFrames.frame(original,s,g,SpriteClip.IDLE,1,e,closure)
                assertEquals(original.size,face.size)
                assertTrue(face.any{it ushr 24>=128})
            }
        }
    }
    @Test fun fortyBaseKeysAndAllSixGenesHaveUniqueIdentity(){
        val keys=Species.entries.flatMap{s->GrowthStage.playable.flatMap{g->Sex.entries.flatMap{sex->
            (0..5).map{PixuFrameKey(s,g,sex,it,SpriteClip.IDLE,0,PetEmotion.IDLE,0)}
        }}}
        assertEquals(240,keys.toSet().size)
        assertEquals(40,keys.map{Triple(it.species,it.stage,it.sex)}.toSet().size)
    }
}
