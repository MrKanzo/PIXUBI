package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
class WorldGenesisTest{
    @Test fun allVesselsCanProduceTheirSeries(){
        RobotSeries.entries.forEach{series->
            val seed=GenesisRules.create(series,Random(7))
            assertEquals(series,seed.series)
            assertFalse(seed.mystery)
            assertTrue(GenesisRules.vessel(series).isNotBlank())
            assertNull(WorldRules.adoptGenesis(seed,1000L))
            val born=seed.copy(energy=100,careMask=7).hatch().rename("PIX")
            val p=WorldRules.adoptGenesis(born,1000L)!!
            assertEquals(seed.series,p.series);assertEquals(seed.skin,p.skin);assertEquals(seed.trait,p.trait)
            assertEquals("PIX",p.name);assertEquals(RobotTier.MK1,p.tier);assertEquals(0,p.xp)
        }
    }
    @Test fun randomBirthKeepsItsOriginalRareColorAndSeries(){
        val seeds=(0..1000).map{GenesisRules.create(null,Random(it))}
        assertEquals(RobotSeries.entries.toSet(),seeds.map{it.series}.toSet())
        assertTrue(seeds.all{it.mystery})
        val rare=seeds.first{it.rare}
        val p=WorldRules.adoptGenesis(rare.copy(energy=100,careMask=7).hatch().rename("Gold"),1000L)!!
        assertEquals(RobotSkin.GOLD,p.skin);assertEquals(rare.series,p.series)
    }
    @Test fun adoptionRequiresCompleteNamedBirth(){
        val s=GenesisRules.create(RobotSeries.CLASSIC).copy(energy=100,careMask=7)
        assertNull(WorldRules.adoptGenesis(s,1))
        assertNull(WorldRules.adoptGenesis(s.hatch(),1))
        assertNull(WorldRules.adoptGenesis(s.copy(energy=50,born=true,name="X"),1))
    }
}
