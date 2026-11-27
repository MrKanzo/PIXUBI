package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class EquipmentVisualIdentityTest{
    @Test fun everyGearKeepsItsIdentityInRenderer(){
        RobotGear.entries.forEach{g->
            assertEquals(g,WorldGearCatalog.visual(g))
            assertEquals(g,WorldGearCatalog.visual(RobotEquipment().equip(g)).equipped(g.slot))
        }
    }
    @Test fun allGearIsRepresentedBySlotsAndShop(){
        assertEquals(RobotGear.entries.toSet(),WorldRules.shop.map{it.gear}.toSet())
        RobotSlot.entries.forEach{assertTrue(RobotGear.forSlot(it).isNotEmpty())}
    }
}
