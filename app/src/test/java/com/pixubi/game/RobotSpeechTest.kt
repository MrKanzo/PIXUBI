package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RobotSpeechTest{
    @Test fun everySeriesAndEventHasEnoughLines(){
        assertTrue(SpeechLibrary.uniqueCount>=400)
        for(s in RobotSeries.entries)for(e in SpeechEvent.entries){
            val lines=SpeechLibrary.choices(s,e)
            assertTrue(lines.size>=8)
            assertEquals(lines.size,lines.toSet().size)
        }
    }
    @Test fun deckAvoidsRepeatsUntilPoolExhausted(){
        val deck=SpeechDeck(random=kotlin.random.Random(42))
        val pool=SpeechLibrary.choices(RobotSeries.KAGE,SpeechEvent.IDLE)
        assertEquals(pool.size,List(pool.size){deck.pick(pool)}.toSet().size)
        assertTrue(deck.pick(pool) in pool)
    }
    @Test fun eventsMatchActualChanges(){
        val p=WorldRules.hatch(RobotSeries.KAGE,"Test",1000L).copy(coins=2000)
        assertEquals(SpeechEvent.BUILD,SpeechEvents.between(p,BaseRules.buy(p,BaseBuilding.BARRACKS))?.event)
        assertEquals(SpeechEvent.MINE,SpeechEvents.between(p,p.copy(materials=mapOf(CraftMaterial.IRON to 4)))?.event)
        assertEquals(SpeechEvent.LOW_ENERGY,SpeechEvents.between(p,p.copy(energy=10))?.event)
        assertEquals(SpeechEvent.CHARGE,SpeechEvents.between(p.copy(energy=10),p)?.event)
        assertNull(SpeechEvents.between(p,p))
    }
}
