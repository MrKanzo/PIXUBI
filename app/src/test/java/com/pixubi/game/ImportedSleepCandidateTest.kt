package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ImportedSleepCandidateTest {
    @Test fun fiveBabyMaleSleepAtlasesPreserveNeutralAndGround(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        for(s in Species.entries){
            val original=ImageIO.read(File(root,"imported_v2/pixubi_${s.name.lowercase(java.util.Locale.ROOT)}_male_young.png"))
            val native=original.getRGB(0,0,128,128,null,0,128)
            val atlas=ImageIO.read(File(root,ImportedSleepCandidate.asset(s)))
            assertEquals(384,atlas.width);assertEquals(256,atlas.height)
            val frames=(0..5).map{atlas.getRGB((it%3)*128,(it/3)*128,128,128,null,0,128)}
            assertArrayEquals(native,frames[0])
            val ground=native.indices.last{native[it] ushr 24!=0}/128
            for(i in frames.indices){
                val f=frames[i]
                assertTrue(f.all{(it ushr 24) in listOf(0,255)})
                assertEquals(ground,f.indices.last{f[it] ushr 24!=0}/128)
                for(j in 0 until i)assertFalse(f.contentEquals(frames[j]))
            }
            assertTrue(ImportedSleepCandidate.available(s,GrowthStage.BABY,Sex.BOY))
            assertFalse(ImportedSleepCandidate.available(s,GrowthStage.BABY,Sex.GIRL))
            assertFalse(ImportedSleepCandidate.available(s,GrowthStage.YOUNG,Sex.BOY))
        }
    }
    @Test fun breathingAndHibernationSelectDifferentFrames(){
        assertEquals(listOf(3,4,5),(0..2).map{ImportedSleepCandidate.restFrame(PetEmotion.SLEEPING,it)})
        assertEquals(listOf(3,3,3),(0..2).map{ImportedSleepCandidate.restFrame(PetEmotion.HIBERNATING,it)})
        assertEquals((0..5).toSet(),(0..139).map{ImportedSleepCandidate.reviewFrame(it/50f)}.toSet())
    }
}
