package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ExpandedShopTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L).copy(xp=RobotProgress.requiredXp(20),coins=2000)
    @Test fun everyGearHasCategoryAndPrice(){
        assertEquals(31,RobotGear.entries.size)
        assertEquals(RobotGear.entries.size,WorldRules.shop.size)
        RobotGear.entries.forEach{g->assertTrue(WorldRules.shop.first{it.gear==g}.price>0);assertTrue(WorldGearCatalog.category(g).title.isNotBlank())}
    }
    @Test fun ammunitionIsChargedOnceAtMissionStart(){
        val p=pet().copy(equipment=RobotEquipment().equip(RobotGear.RIFLE_L).equip(RobotGear.BLASTER_R))
        val q=WorldRules.start(p,WorldTask.TRAIN,1000L)
        assertNotNull(q.mission);assertEquals(p.stock(WorldSupply.PULSE)-6,q.stock(WorldSupply.PULSE))
        assertEquals(q.stock(WorldSupply.PULSE),WorldRules.tick(q,90000L).stock(WorldSupply.PULSE))
        val dry=p.copy(supplies=emptyMap())
        assertNull(WorldRules.start(dry,WorldTask.TRAIN,1000L).mission)
        assertEquals(dry.energy,WorldRules.start(dry,WorldTask.TRAIN,1000L).energy)
    }
    @Test fun purchaseAndUseRepairAndPower(){
        val p=pet().copy(energy=10,integrity=10,equipment=RobotEquipment().equip(RobotGear.POWERBANK_MAX).equip(RobotGear.REPAIR_L))
        val q=WorldRules.buySupply(p,WorldSupply.NANOKIT)
        assertEquals(2,q.stock(WorldSupply.NANOKIT));assertEquals(p.coins-45,q.coins)
        val fixed=WorldRules.useSupply(q,WorldSupply.NANOKIT)
        assertEquals(100,fixed.integrity);assertEquals(1,fixed.stock(WorldSupply.NANOKIT))
        val charged=WorldRules.useSupply(WorldRules.buySupply(p,WorldSupply.CELL_SMALL),WorldSupply.CELL_SMALL)
        assertEquals(60,charged.energy)
    }
    @Test fun visualAliasesKeepSlots(){
        RobotGear.entries.forEach{assertEquals(it.slot,WorldGearCatalog.visual(it).slot)}
    }
}
