package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ClassicFormsTest {
    @Test fun distinctGrowingChassis(){
        val f=RobotTier.entries.map(ClassicForms::of)
        assertEquals(5,f.map{it.grade}.distinct().size)
        assertTrue(f.zipWithNext().all{(a,b)->a.thigh<b.thigh&&a.shin<b.shin&&a.shoulder<b.shoulder})
        assertTrue(f.first().head>f.last().head)
    }
}
