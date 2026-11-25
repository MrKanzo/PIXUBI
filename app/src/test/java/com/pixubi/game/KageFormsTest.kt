package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class KageFormsTest {
    @Test fun fiveDistinctSilhouettesAndIncreasingArmor() {
        val forms=RobotTier.entries.map(KageForms::of)
        assertEquals(5,forms.distinct().size)
        assertTrue(forms.zipWithNext().all{(a,b)->a.height<b.height && a.thigh<b.thigh && a.grade<b.grade})
        assertEquals(4,forms.last().grade)
    }
}
