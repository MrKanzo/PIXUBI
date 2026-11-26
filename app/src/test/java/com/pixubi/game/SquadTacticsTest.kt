package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SquadTacticsTest{
    @Test fun combinationsRequireLivingUnits(){
        val a=ArenaAlly(SquadKind.RANGER,GridPoint(0,1))
        val g=ArenaAlly(SquadKind.GUARD,GridPoint(0,3))
        val m=ArenaAlly(SquadKind.MEDIC,GridPoint(1,2))
        val b=ArenaBattle(19,GridPoint(0,2),100,100,listOf(ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,GridPoint(6,2),100,100,10,10,50)),allies=listOf(a,g,m))
        assertEquals(6,SquadTactics.attackBonus(b,SquadKind.RANGER))
        assertEquals(8,SquadTactics.defenseBonus(b,1))
        assertEquals(15,SquadTactics.repairBonus(b))
        assertEquals(0,SquadTactics.attackBonus(b.copy(allies=listOf(a,g.copy(hp=0),m)),SquadKind.RANGER))
    }
    @Test fun pendingEnemyTurnSurvivesSaveAndOnlyOneEnemyMoves(){
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L).copy(adventure=AdventureState(pos=GridPoint(7,1)))
        val started=AdventureRules.move(p,GridPoint(7,0),1000L)
        val pending=AdventureRules.guard(started,1000L)
        val b=pending.adventure.battle!!
        assertTrue(b.enemyPending)
        assertTrue(SquadRules.available(b).isEmpty())
        val saved=AdventureCodec.decode(AdventureCodec.encode(pending.adventure))
        assertTrue(saved.battle!!.enemyPending)
        val next=AdventureRules.enemyTurn(pending.copy(adventure=saved),1000L)
        val after=next.adventure.battle!!
        assertFalse(after.enemyPending)
        assertEquals(1,after.enemyCursor)
        assertEquals(b.enemies[1],after.enemies[1])
        assertEquals(next,AdventureRules.enemyTurn(next,1000L))
    }
}
