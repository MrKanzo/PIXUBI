package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class EncounterRulesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L).copy(coins=2000)
    private fun contact(p:WorldPet,q:GridPoint):WorldPet{
        val from=q.neighbors().first{AdventureRules.inMap(it)&&AdventureRules.tile(p.adventure,it)!=AdventureTile.WALL}
        return EncounterRules.approach(p.copy(adventure=p.adventure.copy(pos=from)),q,1000L)
    }
    @Test fun dialoguePersistsAndFightCostsEnergyOnlyOnce(){
        val p=pet();val q=contact(p,AdventureRules.legacyEncounters.first())
        assertNull(q.adventure.battle);assertNotNull(q.adventure.contact);assertEquals(p.energy,q.energy)
        assertEquals(q.adventure,AdventureCodec.decode(AdventureCodec.encode(q.adventure)))
        val b=EncounterRules.fight(q,2000L)
        assertNotNull(b.adventure.battle);assertNull(b.adventure.contact);assertEquals(p.energy-AdventureBalance.BATTLE_ENERGY,b.energy)
        assertEquals(b,EncounterRules.fight(b,3000L))
    }
    @Test fun dealsAreDeterministicAndCannotBeCollectedTwice(){
        val p=pet().copy(xp=RobotProgress.requiredXp(25),adventure=AdventureState(squad=mapOf(SquadKind.ASSAULT to 325)))
        val contacts=AdventureRules.encounters(p.adventure).map{contact(p,it)}
        val peaceful=contacts.first{EncounterRules.assess(it)?.offer==EncounterOffer.TOLL}
        val d=EncounterRules.assess(peaceful)!!
        assertEquals(d,EncounterRules.assess(EncounterRules.leave(peaceful).copy(adventure=peaceful.adventure)))
        val q=EncounterRules.accept(peaceful,2000L)
        assertEquals(peaceful.coins-d.price,q.coins);assertEquals(peaceful.wins,q.wins)
        assertEquals(0,SquadResearch.victories(q.adventure))
        assertEquals(AdventureTile.EMPTY,AdventureRules.tile(q.adventure,d.at))
        assertEquals(q,EncounterRules.accept(q,3000L))
        assertEquals(q.adventure,AdventureCodec.decode(AdventureCodec.encode(q.adventure)))
        val boss=contacts.last()
        assertEquals(EncounterOffer.FIGHT,EncounterRules.assess(boss)!!.offer)
        assertEquals(boss,EncounterRules.accept(boss,2000L))
    }
}
