package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class NativeCombatTest {
    @Test fun ninjaDashReturnsAndDoesNotUseAmmo(){
        val p=WorldRules.hatch(RobotSeries.KAGE,"Test",1000L)
        val from=GridPoint(3,2);val target=GridPoint(5,2)
        val e=ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,target,999,999,10,10,30)
        val b=ArenaBattle(19,from,100,100,listOf(e))
        val q=p.copy(adventure=p.adventure.copy(battle=b),equipment=RobotEquipment().equip(RobotGear.BLASTER_L))
        assertEquals(ArenaAttackStyle.KATANA,ArenaAttackStyle.of(q.series,RobotGear.BLASTER_L))
        assertTrue(AdventureRules.weapons(q).isEmpty())
        assertFalse(AdventureRules.canHit(q,b,e,RobotGear.BLASTER_L))
        assertTrue(AdventureRules.canHit(q,b,e,null))
        val next=AdventureRules.strike(q,0,null,2000)
        assertEquals(from,next.adventure.battle!!.pos)
        assertTrue(next.adventure.battle!!.enemies[0].hp<999)
        assertEquals(q.supplies,next.supplies)
        assertTrue(next.adventure.battle!!.enemyPending)
        assertEquals(0f,ArenaAttackStyle.travel(0f),.001f)
        assertEquals(1f,ArenaAttackStyle.travel(.5f),.001f)
        assertEquals(0f,ArenaAttackStyle.travel(1f),.001f)
    }
    @Test fun exploredGroundStaysVisibleAfterMovingAway(){
        var p=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L)
        val q=GridPoint(0,0)
        p=AdventureRules.move(p,GridPoint(1,2),1000L)
        p=AdventureRules.move(p,GridPoint(2,2),1000L)
        p=AdventureRules.move(p,GridPoint(3,2),1000L)
        assertTrue(AdventureRules.id(q) in p.adventure.seen)
        assertTrue(AdventureRules.visible(p.adventure,q))
        assertFalse(AdventureRules.inSight(p.adventure,q))
        assertTrue(AdventureRules.visible(AdventureCodec.decode(AdventureCodec.encode(p.adventure)),q))
    }
}
