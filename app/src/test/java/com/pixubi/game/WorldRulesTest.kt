package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class WorldRulesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.SYMBIONT,"Test",1000L)
    @Test fun allSeriesAndEquipmentHaveWorldRules(){
        assertEquals(8,RobotSeries.entries.size)
        assertEquals(RobotGear.entries.size,WorldRules.shop.size)
        RobotSeries.entries.forEach{assertEquals(it,WorldRules.hatch(it,"",1).series)}
    }
    @Test fun shopRequiresOwnershipAndLevel(){
        val p=pet()
        assertEquals(p.equipment,WorldRules.equip(p,RobotGear.BLADE_L).equipment)
        assertEquals(p.coins,WorldRules.buy(p,RobotGear.CANNON_L).coins)
        val q=WorldRules.buy(p,RobotGear.SCANNER)
        assertEquals(p.coins-75,q.coins)
        assertEquals(RobotGear.SCANNER,WorldRules.equip(q,RobotGear.SCANNER).equipment.equipped(RobotSlot.HEAD))
        assertEquals(q.coins,WorldRules.buy(q,RobotGear.SCANNER).coins)
    }
    @Test fun missionPaysOnlyOnce(){
        val p=WorldRules.start(pet(),WorldTask.SEARCH,1000)
        assertNotNull(p.mission)
        assertEquals(0,WorldRules.tick(p,2000).xp)
        val q=WorldRules.tick(p,60000)
        assertNull(q.mission);assertEquals(95,q.xp);assertEquals(1,q.expeditions)
        assertEquals(q.xp,WorldRules.tick(q,65000).xp)
    }
    @Test fun sleepAndHibernate(){
        val p=pet().copy(energy=40)
        assertTrue(WorldRules.tick(WorldRules.mode(p,WorldMode.SLEEP,1000),121000).energy>40)
        assertEquals(40,WorldRules.tick(WorldRules.mode(p,WorldMode.HIBERNATE,1000),9000000).energy)
    }
    @Test fun omegaNeedsAchievements(){
        val p=pet().copy(xp=RobotProgress.MAX_XP)
        assertEquals(RobotTier.MK4,p.tier)
        assertEquals(RobotTier.OMEGA,p.copy(wins=10,expeditions=10).tier)
    }
}
