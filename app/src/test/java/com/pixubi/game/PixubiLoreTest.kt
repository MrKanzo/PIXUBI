package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class PixubiLoreTest {
    @Test fun everySpeciesHasAnIndependentLegend() {
        val stories=Species.entries.map(PixubiLore::story)
        assertEquals(5,stories.size)
        assertEquals(5,stories.map{it.legend}.distinct().size)
        assertTrue(stories.all{it.legend.length>100 && it.talent.isNotBlank()})
        assertTrue(PixubiLore.assistant.contains("ChatGPT"))
        assertTrue(PixubiLore.creator.contains("MrKanzo"))
    }
}
