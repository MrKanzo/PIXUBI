package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ArenaFormationTest{
    @Test fun fiveUnitsFitInDeploymentZone(){
        val kinds=listOf(SquadKind.RANGER,SquadKind.ASSAULT,SquadKind.GUARD,SquadKind.MEDIC)
        val units=SquadRules.deploy(kinds.associateWith{it.hp},limit=4)
        val cells=units.map{it.pos}+GridPoint(0,2)
        assertEquals(5,cells.distinct().size)
        assertEquals(setOf(0,1,2,3,4),cells.map{it.y}.toSet())
        assertTrue(cells.all{AdventureRules.deploymentCell(it)})
    }
}
