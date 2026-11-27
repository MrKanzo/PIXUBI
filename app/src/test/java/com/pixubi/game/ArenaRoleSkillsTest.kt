package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ArenaRoleSkillsTest {
    private fun pet():WorldPet {
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L)
        val b=ArenaBattle(19,GridPoint(0,1),50,100,
            listOf(ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,GridPoint(6,1),999,999,10,10,30)),
            allies=listOf(ArenaAlly(SquadKind.GUARD,GridPoint(0,0)),ArenaAlly(SquadKind.MEDIC,GridPoint(1,0))))
        return p.copy(adventure=p.adventure.copy(battle=b))
    }
    @Test fun groupShieldCostsOneTurnAndCannotBeRepeated(){
        val p=pet()
        val q=AdventureRules.useSupportSkill(p,0,1000L)
        val b=q.adventure.battle!!
        assertEquals(setOf(-1,0,1),b.guards)
        assertEquals(setOf(0),b.skillUsed)
        assertTrue(b.enemyPending)
        assertEquals(q,AdventureRules.useSupportSkill(q,0,1000L))
    }
    @Test fun massRepairConsumesChargeAndRestoresCommander(){
        val p=pet()
        val q=AdventureRules.useSupportSkill(p,1,1000L)
        val b=q.adventure.battle!!
        assertEquals(75,b.hp)
        assertEquals(2,b.allies[1].charges)
        assertTrue(b.enemyPending)
        assertEquals(setOf(1),b.skillUsed)
    }
}
