package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
class GenesisRulesTest{
    @Test fun careIsRequiredAndEnergyIsBounded(){
        var s=GenesisRules.create(RobotSeries.KAGE,Random(1))
        repeat(20){s=s.care(GenesisCare.CHARGE)}
        assertEquals(100,s.energy);assertFalse(s.ready);assertFalse(s.hatch().born)
        s=s.care(GenesisCare.SYNC).care(GenesisCare.TOUCH)
        assertTrue(s.ready);assertTrue(s.hatch().born)
        assertEquals(s.hatch(),s.hatch().care(GenesisCare.CHARGE))
    }
    @Test fun manualAndRandomAreDifferentModes(){
        assertEquals(RobotSeries.GLITCH,GenesisRules.create(RobotSeries.GLITCH,Random(2)).series)
        assertFalse(GenesisRules.create(RobotSeries.GLITCH,Random(2)).mystery)
        assertTrue(GenesisRules.create(null,Random(2)).mystery)
        assertEquals(5,RobotSeries.entries.map(GenesisRules::vessel).distinct().size)
    }
    @Test fun nameOnlyAfterBirth(){
        val s=GenesisRules.create(null,Random(3))
        assertEquals("",s.rename("Pix").name)
        assertEquals("Pix",s.copy(born=true).rename(" Pix ").name)
    }
}
