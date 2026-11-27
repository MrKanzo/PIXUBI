package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test

class ArenaSkillFxTest{
    private fun battle(kind:SquadKind)=ArenaBattle(19,GridPoint(0,1),50,100,
        listOf(
            ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,GridPoint(6,1),80,80,10,10,30),
            ArenaBot(RobotSeries.CLASSIC,RobotTier.MK1,GridPoint(6,2),80,80,10,10,30)
        ),allies=listOf(ArenaAlly(kind,GridPoint(1,0)),ArenaAlly(SquadKind.ASSAULT,GridPoint(0,0),hp=30)))
    @Test fun everyClassHasDistinctSkillVisual(){
        val effects=SquadKind.entries.map{ArenaSkillFx.forActor(it)}+ArenaSkillFx.forActor(null)
        assertEquals(7,effects.distinct().size)
        assertTrue(effects.all{it.millis in 600..1000})
    }
    @Test fun shieldAndRepairAffectCorrectFriends(){
        val shield=battle(SquadKind.GUARD)
        assertEquals(setOf(shield.pos,shield.allies[0].pos,shield.allies[1].pos),skillFxCells(shield,0,-1).toSet())
        val repair=battle(SquadKind.MEDIC)
        assertEquals(setOf(repair.pos,repair.allies[1].pos),skillFxCells(repair,0,-1).toSet())
    }
    @Test fun areaEffectsIncludeNearbyLivingEnemiesOnly(){
        val siege=battle(SquadKind.SIEGE)
        assertEquals(siege.enemies.map{it.pos}.toSet(),skillFxCells(siege,0,0).toSet())
        val dead=siege.copy(enemies=listOf(siege.enemies[0],siege.enemies[1].copy(hp=0)))
        assertEquals(listOf(dead.enemies[0].pos),skillFxCells(dead,0,0))
        val ranger=battle(SquadKind.RANGER)
        assertEquals(listOf(ranger.enemies[0].pos),skillFxCells(ranger,0,0))
        assertTrue(skillFxCells(ranger,0,-1).isEmpty())
    }
}
