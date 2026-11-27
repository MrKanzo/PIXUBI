package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class FrontierExpansionTest{
    @Test fun width16SavePreservesCoordinates(){
        val a=AdventureCodec.decode("1|5,3|53,87|17,53|12||16|0")
        assertEquals(GridPoint(5,3),a.pos)
        assertEquals(setOf(65,107),a.cleared)
        assertEquals(setOf(21,65),a.seen)
        assertEquals(a,AdventureCodec.decode(AdventureCodec.encode(a)))
    }
    @Test fun portalMovesAndRevealsExit(){
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000).copy(adventure=AdventureState(pos=GridPoint(15,0)))
        val q=AdventureRules.move(p,GridPoint(16,0),2000)
        assertEquals(GridPoint(19,10),q.adventure.pos)
        assertTrue(AdventureRules.id(q.adventure.pos) in q.adventure.seen)
    }
    @Test fun gateUnlocksAfterRaid(){
        val a=AdventureState()
        val gate=FrontierNetwork.gates(1).first()
        assertEquals(AdventureTile.WALL,AdventureRules.tile(a,gate))
        val q=a.copy(cleared=FrontierNetwork.raids(1).map{AdventureRules.id(it)}.toSet())
        assertEquals(AdventureTile.EMPTY,AdventureRules.tile(q,gate))
    }
    @Test fun gearVariantsAreSeparateOffers(){
        val gears=listOf(RobotGear.HELMET_SHADOW,RobotGear.HELMET_TITAN,RobotGear.HELMET_PRISM,RobotGear.ARMOR_SHADOW,RobotGear.ARMOR_PRISM)
        gears.forEach{g->assertTrue(WorldRules.shop.any{it.gear==g});assertEquals(g,WorldGearCatalog.visual(g));assertEquals(g,RobotEquipment.decode(RobotEquipment().equip(g).encode()).equipped(g.slot))}
    }
}
