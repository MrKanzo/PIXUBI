package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SquadDisclosureTest{
    @Test fun lockedAdvancedClassesDoNotExposeTheirNames(){
        val a=AdventureState()
        for(k in listOf(SquadKind.SIEGE,SquadKind.AERIAL)){
            val info=SquadDisclosure.locked(a,k)!!
            assertTrue(info.secret)
            assertFalse(info.title.contains(k.title))
        }
    }
    @Test fun onlyUnlockedClassesCanRevealRealModel(){
        val a=AdventureState()
        assertNull(SquadDisclosure.locked(a,SquadKind.RANGER))
        assertNotNull(SquadDisclosure.locked(a,SquadKind.GUARD))
        assertNull(SquadDisclosure.locked(a.copy(pastCampaignWins=2),SquadKind.GUARD))
        assertNull(SquadDisclosure.locked(a.copy(licensed=setOf(SquadKind.AERIAL)),SquadKind.AERIAL))
    }
    @Test fun enteringSecondSectorRevealsBlueprint(){
        val a=AdventureState(cleared=AdventureRules.encounters(AdventureState()).map{AdventureRules.id(it)}.toSet())
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L).copy(adventure=a)
        val next=AdventureRules.advanceMap(p)
        assertTrue(SquadKind.SIEGE in next.adventure.licensed)
        assertTrue(next.message.contains(SquadKind.SIEGE.title))
    }
}
