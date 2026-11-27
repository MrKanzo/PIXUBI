package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class MiningCraftTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L).copy(xp=RobotProgress.requiredXp(20),coins=2000)
    @Test fun nodesDoNotReplaceExistingContent(){
        for(s in 1..10){
            val l=AdventureRules.layout(s)
            val sites=MiningRules.sites(s)
            assertTrue(sites.size>=7)
            assertTrue(sites.keys.none{it in l.walls||it in l.resources||it in l.encounters||it in FrontierSites.sites(s)||it in FrontierNetwork.portals(s)||it in FrontierNetwork.gates(s)})
        }
    }
    @Test fun miningConsumesEnergyAndCannotRepeat(){
        val p=pet();val (at,node)=MiningRules.sites(1).entries.first()
        val q=p.copy(adventure=p.adventure.copy(pos=at))
        val mined=MiningRules.harvest(q)
        assertEquals(node.amount,mined.material(node.material))
        assertEquals(q.energy-node.energy,mined.energy)
        assertNull(MiningRules.node(mined.adventure,at))
        assertEquals(mined,MiningRules.harvest(mined))
        assertNull(MiningRules.node(AdventureCodec.decode(AdventureCodec.encode(mined.adventure)),at))
    }
    @Test fun craftConsumesExactMaterialsAndAddsRealGearOrUnit(){
        val p=pet().copy(buildings=setOf(BaseBuilding.WORKSHOP,BaseBuilding.BARRACKS),materials=CraftMaterial.entries.associateWith{10})
        val q=CraftRules.craft(p,CraftRecipe.BLADE)
        assertTrue(RobotGear.BLADE_L in q.owned)
        assertEquals(6,q.material(CraftMaterial.IRON))
        assertEquals(q,CraftRules.craft(q,CraftRecipe.BLADE))
        val u=CraftRules.craft(q,CraftRecipe.ASSAULT)
        assertEquals(1,SquadRules.stackCount(u.adventure,SquadKind.ASSAULT))
        assertEquals(2,u.material(CraftMaterial.IRON))
        assertEquals(u,CraftRules.craft(u,CraftRecipe.ASSAULT))
        assertEquals(p.copy(buildings=emptySet()),CraftRules.craft(p.copy(buildings=emptySet()),CraftRecipe.BLADE))
    }
}
