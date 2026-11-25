package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ValkyrieCatalogTest{
    @Test fun allStagesGrow(){
        val f=RobotTier.entries.map(ValkyrieCatalog::of)
        assertEquals(listOf("LUMI","NOVA","AURA","VALKYRIE","SERAPH"),f.map{it.title})
        assertTrue(f.zipWithNext().all{(a,b)->a.thigh<b.thigh&&a.shoulder<b.shoulder})
        assertTrue(ValkyrieCatalog.hover(.25f)>ValkyrieCatalog.hover(.75f))
    }
}
