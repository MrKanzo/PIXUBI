package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class SquadResearchTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L).copy(coins=10000)
    @Test fun newGameOnlyHasTwoClasses(){
        val p=pet()
        assertEquals(setOf(SquadKind.ASSAULT,SquadKind.RANGER),SquadKind.entries.filter{SquadResearch.unlocked(p.adventure,it)}.toSet())
        assertEquals(p,SquadRules.recruit(p,SquadKind.GUARD))
        val advanced=SquadRules.expand(SquadRules.expand(p))
        assertEquals(advanced,SquadRules.recruit(advanced,SquadKind.AERIAL))
    }
    @Test fun onlyDefeatedEncountersCount(){
        val a=AdventureState()
        val layout=AdventureRules.layout(1)
        val loot=layout.resources.keys.map{AdventureRules.id(it)}.toSet()
        assertEquals(0,SquadResearch.victories(a.copy(cleared=loot)))
        val two=loot+layout.encounters.take(2).map{AdventureRules.id(it)}
        val p=pet().copy(adventure=a.copy(cleared=two))
        assertTrue(SquadResearch.unlocked(p.adventure,SquadKind.GUARD))
        assertFalse(SquadResearch.unlocked(p.adventure,SquadKind.MEDIC))
        assertTrue(SquadKind.GUARD in SquadRules.recruit(p,SquadKind.GUARD).adventure.squad)
    }
    @Test fun advancedClassesNeedSectorAndWins(){
        assertFalse(SquadResearch.unlocked(AdventureState(stage=2,pastCampaignWins=9),SquadKind.SIEGE))
        assertFalse(SquadResearch.unlocked(AdventureState(stage=1,pastCampaignWins=20),SquadKind.SIEGE))
        assertTrue(SquadResearch.unlocked(AdventureState(stage=2,pastCampaignWins=10),SquadKind.SIEGE))
        assertTrue(SquadResearch.unlocked(AdventureState(stage=3,pastCampaignWins=20),SquadKind.AERIAL))
    }
    @Test fun advancePreservesVictoriesAndOldRosterLicense(){
        val a=AdventureState(cleared=AdventureRules.encounters(AdventureState()).map{AdventureRules.id(it)}.toSet())
        val next=AdventureRules.advanceMap(pet().copy(adventure=a)).adventure
        assertEquals(2,next.stage);assertEquals(10,SquadResearch.victories(next))
        val migrated=AdventureCodec.decode("1|0,2|||0||16|0|SIEGE,125|2|")
        assertTrue(SquadKind.SIEGE in migrated.licensed)
        val p=pet().copy(adventure=migrated)
        val dismissed=SquadRules.dismiss(p,SquadKind.SIEGE)
        assertTrue(SquadResearch.unlocked(dismissed.adventure,SquadKind.SIEGE))
        assertEquals(dismissed.adventure,AdventureCodec.decode(AdventureCodec.encode(dismissed.adventure)))
    }
}
