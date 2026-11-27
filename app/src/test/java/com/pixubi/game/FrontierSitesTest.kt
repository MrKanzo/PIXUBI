package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class FrontierSitesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L)
    @Test fun sitesPreserveOriginalLayoutAcrossAllSectors(){
        for(stage in 1..100){
            val l=AdventureRules.layout(stage)
            val sites=FrontierSites.sites(stage)
            assertEquals(setOf(AdventureTile.LAB,AdventureTile.CACHE,AdventureTile.VAULT),sites.values.toSet())
            assertTrue(sites.keys.none{it in l.walls||it in l.encounters||it in l.resources})
            val vault=sites.entries.first{it.value==AdventureTile.VAULT}.key
            val blocked=l.walls+l.encounters+vault
            val reached=mutableSetOf(AdventureRules.base);val queue=ArrayDeque<GridPoint>();queue.add(AdventureRules.base)
            while(queue.isNotEmpty())queue.removeFirst().neighbors().filter{AdventureRules.inMap(it)&&it !in blocked&&reached.add(it)}.forEach{queue.add(it)}
            assertTrue(l.resources.keys.all{it in reached})
            assertTrue(l.encounters.all{e->e.neighbors().any{it in reached}})
            assertTrue(vault.neighbors().any{it in reached})
        }
    }
    @Test fun labLootIsCollectedOnceAndArtifactPersists(){
        val lab=FrontierSites.sites(1).entries.first{it.value==AdventureTile.LAB}.key
        val from=lab.neighbors().first{AdventureRules.inMap(it)&&AdventureRules.tile(AdventureState(),it)==AdventureTile.EMPTY}
        val p=pet().copy(adventure=AdventureState(pos=from))
        val q=AdventureRules.move(p,lab,1000L)
        assertTrue(FrontierArtifact.SCANNER in q.adventure.artifacts)
        assertEquals(AdventureTile.EMPTY,AdventureRules.tile(q.adventure,lab))
        assertEquals(q.adventure,AdventureCodec.decode(AdventureCodec.encode(q.adventure)))
        val again=AdventureRules.move(q.copy(adventure=q.adventure.copy(pos=from)),lab,1000L)
        assertEquals(q.cells,again.cells)
    }
    @Test fun optionalVaultStartsSavedBattleWithoutChangingCampaignCount(){
        val a=AdventureState()
        val vault=FrontierSites.sites(1).entries.first{it.value==AdventureTile.VAULT}.key
        val from=vault.neighbors().first{AdventureRules.inMap(it)&&!FrontierSites.hostile(AdventureRules.tile(a,it))&&AdventureRules.tile(a,it)!=AdventureTile.WALL}
        val p=pet().copy(adventure=a.copy(pos=from))
        val q=AdventureRules.move(p,vault,1000L)
        assertEquals(2,q.adventure.battle!!.enemies.size)
        assertEquals(q.adventure.battle!!.encounter,AdventureCodec.decode(AdventureCodec.encode(q.adventure)).battle!!.encounter)
        assertEquals(10,AdventureRules.encounters(q.adventure).size)
    }
}
