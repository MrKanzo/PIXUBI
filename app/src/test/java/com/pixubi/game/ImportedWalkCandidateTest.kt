package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ImportedWalkCandidateTest {
    @Test fun candidatePreservesIdentityAndHasFourDifferentPoses(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        val original=ImageIO.read(File(root,"imported_v2/pixubi_lisyk_male_young.png"))
        val candidate=ImageIO.read(File(root,ImportedWalkCandidate.ASSET))
        assertEquals(512,candidate.width)
        assertEquals(128,candidate.height)
        val frames=(0..3).map{candidate.getRGB(it*128,0,128,128,null,0,128)}
        val native=original.getRGB(0,0,128,128,null,0,128)
        for(i in frames.indices){
            assertFalse(frames[i].contentEquals(native))
            for(j in 0 until i)assertFalse(frames[i].contentEquals(frames[j]))
            assertArrayEquals(native.copyOfRange(0,94*128),frames[i].copyOfRange(0,94*128))
            assertTrue(frames[i].all{(it ushr 24) in listOf(0,255)})
        }
        assertTrue(ImportedWalkCandidate.available(Species.LISYK,GrowthStage.BABY,Sex.BOY))
        assertFalse(ImportedWalkCandidate.available(Species.LISYK,GrowthStage.BABY,Sex.GIRL))
        assertFalse(ImportedWalkCandidate.available(Species.ISKRYK,GrowthStage.BABY,Sex.BOY))
    }
}
