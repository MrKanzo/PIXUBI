package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SquadRulesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L).copy(coins=1000,adventure=AdventureState(pastCampaignWins=2))
    private fun battle():WorldPet{
        val p=SquadRules.recruit(SquadRules.recruit(pet(),SquadKind.RANGER),SquadKind.GUARD)
        return AdventureRules.move(p.copy(adventure=p.adventure.copy(pos=GridPoint(3,2))),GridPoint(3,1),1000L)
    }
    @Test fun hireLimitAndPersistence(){
        var p=pet()
        for(k in SquadKind.entries)p=SquadRules.recruit(p,k)
        assertEquals(3,p.adventure.squad.size)
        assertEquals(1000-90-75-100,p.coins)
        assertEquals(p.adventure.squad,AdventureCodec.decode(AdventureCodec.encode(p.adventure)).squad)
    }
    @Test fun everyFriendlyActionRequiresOneEnemyResponse(){
        val p=battle()
        val q=AdventureRules.guard(p,1000L)
        assertTrue(q.adventure.battle!!.enemyPending)
        assertEquals(setOf(-1),q.adventure.battle!!.acted)
        assertEquals(q,AdventureRules.allyGuard(q,0,1000L))
        val r=AdventureRules.enemyTurn(q,1000L)
        assertFalse(r.adventure.battle!!.enemyPending)
        assertEquals(1,r.adventure.battle!!.turn)
        val s=AdventureRules.enemyTurn(AdventureRules.allyGuard(r,0,1000L),1000L)
        val u=AdventureRules.enemyTurn(AdventureRules.allyGuard(s,1,1000L),1000L)
        assertEquals(2,u.adventure.battle!!.turn);assertTrue(u.adventure.battle!!.acted.isEmpty())
    }
    @Test fun allyPositionsBlockPathsAndSavedTurnsRoundTrip(){
        val p=AdventureRules.guard(battle(),1000L);val b=p.adventure.battle!!
        assertFalse(SquadRules.reachable(p,b,0,b.pos))
        assertFalse(SquadRules.reachable(p,b,0,b.allies[1].pos))
        val a=AdventureCodec.decode(AdventureCodec.encode(p.adventure))
        assertEquals(b.allies,a.battle!!.allies);assertEquals(b.acted,a.battle!!.acted);assertEquals(b.guards,a.battle!!.guards)
    }
    @Test fun rangedUnitConsumesOwnCharges(){
        val p=battle();val b=p.adventure.battle!!
        val q=p.copy(adventure=p.adventure.copy(battle=b.copy(allies=b.allies.mapIndexed{i,u->if(i==0)u.copy(pos=GridPoint(5,2))else u})))
        val shot=AdventureRules.allyStrike(q,0,0,1000L)
        assertEquals(7,shot.adventure.battle!!.allies[0].charges)
        assertEquals(p.supplies,shot.supplies)
    }
}
