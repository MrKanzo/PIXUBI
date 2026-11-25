package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class GlitchCatalogTest {
    @Test fun fiveFormsHaveDistinctDimensions() {
        val forms=RobotTier.entries.map(GlitchCatalog::of)
        assertEquals(5,forms.map{it.title}.distinct().size)
        assertEquals(5,forms.map{it.hips}.distinct().size)
        assertTrue(forms.zipWithNext().all{(a,b)->a.thigh<b.thigh&&a.grade<b.grade})
        assertEquals(2,forms.last().drones)
    }
    @Test fun teleportReformsAtEnd(){
        assertEquals(0f,GlitchCatalog.teleport(0f),.001f)
        assertEquals(1f,GlitchCatalog.teleport(.5f),.001f)
        assertEquals(0f,GlitchCatalog.teleport(1f),.001f)
    }
}
