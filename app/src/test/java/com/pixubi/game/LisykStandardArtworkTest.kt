package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class LisykStandardArtworkTest {
    @Test fun allTwentyFormsHaveFullPoseSources(){
        var originals=0
        var redesigned=0
        for(s in Species.entries)for(g in GrowthStage.playable){
            val original=PetPosePack.forPet(s,g)!=null
            val new=LisykStandardArtwork.matches(s,g)
            assertTrue("$s $g must use exactly one pose source",original xor new)
            if(original)originals++
            if(new){
                redesigned++
                val name=LisykStandardArtwork.asset(s,g)
                val file=listOf(File("src/main/assets/$name"),File("app/src/main/assets/$name")).first{it.isFile}
                val img=ImageIO.read(file)
                assertEquals(256,img.width)
                assertEquals(256,img.height)
                assertTrue(img.colorModel.hasAlpha())
                val hashes=mutableSetOf<Int>()
                for(i in 0..15){
                    val px=img.getRGB(i%4*64,i/4*64,64,64,null,0,64)
                    assertTrue("$name pose $i blank",px.count{it ushr 24>=128}>200)
                    if(i<4)assertTrue("$name walk pose $i repeats",hashes.add(px.contentHashCode()))
                }
            }
        }
        assertEquals(6,originals)
        assertEquals(14,redesigned)
    }
    @Test fun posesSelectConsistentActionsAndDirection(){
        for(i in 0..3)assertEquals(i,LisykStandardArtwork.index(SpriteClip.WALK,PetEmotion.IDLE,i,false))
        assertEquals(7,LisykStandardArtwork.index(SpriteClip.SLEEP,PetEmotion.SLEEPING,0,false))
        assertEquals(8,LisykStandardArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,false))
        assertEquals(9,LisykStandardArtwork.index(SpriteClip.IDLE,PetEmotion.IDLE,0,true))
        assertEquals(10,LisykStandardArtwork.index(SpriteClip.HAPPY,PetEmotion.HAPPY,1,false))
        assertEquals(14,LisykStandardArtwork.index(SpriteClip.EAT,PetEmotion.EATING,1,false))
        assertEquals(15,LisykStandardArtwork.index(SpriteClip.DRINK,PetEmotion.DRINKING,1,false))
        for(s in Species.entries)for(g in GrowthStage.playable.filter{LisykStandardArtwork.matches(s,it)}){
            val k=PixuFrameKey(s,g,Sex.BOY,0,SpriteClip.WALK,0,PetEmotion.IDLE,0)
            // A new side-facing sprite always faces right in the source.
            // Do not initialize the Android bitmap cache in this JVM test.
            assertTrue(LisykStandardArtwork.matches(k.species,k.stage))
        }
    }
}
