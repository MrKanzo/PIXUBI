package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class AdventureBalanceTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L)
    @Test fun emergencyRecoveryPreservesProgressAndPaysNothing(){
        val p=pet().copy(energy=0,integrity=5,coins=0,adventure=AdventureState(pos=GridPoint(5,2),cleared=setOf(17),artifacts=setOf(FrontierArtifact.CORE)))
        val evacuated=AdventureBalance.recover(p)
        assertEquals(AdventureRules.base,evacuated.adventure.pos)
        val repaired=AdventureBalance.recover(evacuated)
        assertEquals(25,repaired.energy)
        assertEquals(35,repaired.integrity)
        assertEquals(p.coins,repaired.coins)
        assertEquals(p.xp,repaired.xp)
        assertEquals(p.adventure.cleared,repaired.adventure.cleared)
        assertEquals(p.adventure.artifacts,repaired.adventure.artifacts)
        assertEquals(repaired,AdventureBalance.recover(repaired))
    }
    @Test fun scaleAndRewardsHaveLimits(){
        assertEquals(1f,AdventureBalance.enemyScale(1,0),.001f)
        assertTrue(AdventureBalance.enemyScale(1,5)<1.5f)
        assertEquals(AdventureBalance.enemyScale(25,20),AdventureBalance.enemyScale(100,100),.001f)
        assertEquals(AdventureBalance.battleCoins(25,2,true),AdventureBalance.battleCoins(100,2,true))
        assertTrue(AdventureBalance.repairPrice(5)<AdventureBalance.repairPrice(80))
    }
    @Test fun battleLocksExternalCareAndMissions(){
        val p=pet().copy(energy=50,integrity=50,adventure=AdventureState(pos=GridPoint(3,2)))
        val q=AdventureRules.move(p,GridPoint(3,1),1000)
        assertNotNull(q.adventure.battle)
        assertEquals(q,WorldRules.start(q,WorldTask.SEARCH,1000))
        assertEquals(q,WorldRules.care(q,0))
        assertEquals(q,WorldRules.mode(q,WorldMode.SLEEP,1000))
        assertEquals(q,AdventureBalance.recover(q))
    }
}
