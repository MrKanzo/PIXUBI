package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ComicPlacementTest{
    @Test fun bubbleStaysInsideViewport(){
        assertEquals(0f,ComicPlacement.left(8f,168f,360f),.001f)
        assertEquals(192f,ComicPlacement.left(355f,168f,360f),.001f)
        assertEquals(0f,ComicPlacement.top(12f,70f,220f,false),.001f)
        assertEquals(150f,ComicPlacement.top(219f,70f,220f,true),.001f)
        assertEquals(50f,ComicPlacement.top(110f,60f,220f,false),.001f)
    }
    @Test fun thoughtsAndSpeechAreDifferent(){
        assertTrue(SpeechEvents.isThought(SpeechEvent.IDLE))
        assertTrue(SpeechEvents.isThought(SpeechEvent.LOW_ENERGY))
        assertFalse(SpeechEvents.isThought(SpeechEvent.ATTACK))
        assertFalse(SpeechEvents.isThought(SpeechEvent.LOOT))
    }
}
