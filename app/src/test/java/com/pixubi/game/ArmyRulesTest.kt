package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ArmyRulesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L).copy(coins=10000)
    @Test fun stacksGrowAndLoseStrength(){
        var p=pet()
        repeat(7){p=SquadRules.recruit(p,SquadKind.RANGER)}
        assertEquals(5,SquadRules.stackCount(p.adventure,SquadKind.RANGER))
        val u=SquadRules.deploy(p.adventure.squad).single()
        assertEquals(5,u.count);assertEquals(75,u.attack);assertEquals(40,u.charges)
        val damaged=u.copy(hp=u.hp-u.unitHp)
        assertEquals(4,damaged.count);assertEquals(60,damaged.attack)
    }
    @Test fun barracksUnlockHeavyAndAir(){
        val p=pet()
        assertEquals(p,SquadRules.recruit(p,SquadKind.AERIAL))
        val q=SquadRules.expand(p)
        assertEquals(4,SquadRules.slots(q.adventure))
        val heavy=SquadRules.recruit(q,SquadKind.SIEGE)
        assertTrue(SquadKind.SIEGE in heavy.adventure.squad)
        val air=SquadRules.recruit(SquadRules.expand(heavy),SquadKind.AERIAL)
        assertTrue(SquadKind.AERIAL in air.adventure.squad)
        val restored=AdventureCodec.decode(AdventureCodec.encode(air.adventure))
        assertEquals(air.adventure,restored)
    }
    @Test fun ranksAndRepairsDoNotResurrectLostUnits(){
        var p=SquadRules.expand(pet())
        repeat(2){p=SquadRules.recruit(p,SquadKind.GUARD)}
        val q=SquadRules.upgrade(p,SquadKind.GUARD)
        assertEquals(2,SquadRules.rank(q.adventure,SquadKind.GUARD))
        assertEquals(2,SquadRules.stackCount(q.adventure,SquadKind.GUARD))
        val unit=SquadRules.unitHp(SquadKind.GUARD,2)
        val damaged=q.copy(adventure=q.adventure.copy(squad=mapOf(SquadKind.GUARD to unit-5)))
        val repaired=SquadRules.restore(damaged,SquadKind.GUARD)
        assertEquals(unit,repaired.adventure.squad[SquadKind.GUARD])
        assertEquals(1,SquadRules.stackCount(repaired.adventure,SquadKind.GUARD))
    }
    @Test fun aerialCanCrossOccupiedIntermediateCell(){
        val p=pet()
        val u=ArenaAlly(SquadKind.AERIAL,GridPoint(0,2))
        val b=ArenaBattle(1,GridPoint(1,2),100,100,listOf(ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,GridPoint(6,2),40,40,5,5,5)),allies=listOf(u))
        assertTrue(SquadRules.reachable(p,b,0,GridPoint(2,2)))
        assertFalse(SquadRules.reachable(p,b,0,b.pos))
    }
}
