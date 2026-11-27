package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class CompactMarketTest{
    @Test fun filtersNeverDuplicateOrLoseOffers(){
        val p=WorldRules.hatch(RobotSeries.KAGE,"Test",1000L)
        val all=MarketCatalog.groups(p,false,MarketKind.ALL,false,false,"").flatten()
        assertEquals(WorldRules.shop.toSet(),all.toSet())
        assertEquals(all.size,all.toSet().size)
        assertTrue(MarketCatalog.groups(p,false,MarketKind.WEAPON,true,false,"").flatten().all{it.gear==RobotGear.BLADE_L||it.gear==RobotGear.BLADE_R})
        assertTrue(MarketCatalog.groups(p,true,MarketKind.ALL,false,false,"").isEmpty())
        val owned=p.copy(owned=setOf(RobotGear.HELMET_PRISM))
        assertEquals(listOf(RobotGear.HELMET_PRISM),MarketCatalog.groups(owned,true,MarketKind.HEAD,false,false,"PRISM").flatten().map{it.gear})
    }
}
