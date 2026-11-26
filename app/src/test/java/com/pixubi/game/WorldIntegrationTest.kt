package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class WorldIntegrationTest{
    @Test fun allSeriesHaveLoreAndHabitat(){
        RobotSeries.entries.forEach{s->assertTrue(WorldCatalog.info(s).description.isNotBlank());assertTrue(HabitatCatalog.of(s).title.isNotBlank())}
    }
    @Test fun archiveUsesActualEvolution(){
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L)
        assertTrue(WorldCatalog.unlocked(p,RobotTier.MK1))
        assertFalse(WorldCatalog.unlocked(p,RobotTier.MK2))
    }
    @Test fun alertsUseWorldAndRespectHibernate(){
        val p=WorldRules.hatch(RobotSeries.SYMBIONT,"X",1000L).copy(energy=20,integrity=40,mood=20)
        val settings=PixubiNotificationSettings(enabled=true)
        assertTrue(WorldNeedRules.find(p,p,settings).any{it.need==PixubiNeed.FOOD})
        assertTrue(WorldNeedRules.find(p,p,settings).any{it.need==PixubiNeed.WATER})
        assertTrue(WorldNeedRules.find(p,p.copy(mode=WorldMode.HIBERNATE),settings).isEmpty())
        assertTrue(WorldNeedRules.find(p,p,settings.copy(enabled=false)).isEmpty())
    }
    @Test fun completedMissionNotification(){
        val p=WorldRules.start(WorldRules.hatch(RobotSeries.NEXUS,"N",1000L),WorldTask.SEARCH,1000L)
        val q=WorldRules.tick(p,90000L)
        assertTrue(WorldNeedRules.find(p,q,PixubiNotificationSettings(enabled=true)).any{it.message.contains("результат")})
    }
}
