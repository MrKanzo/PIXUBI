package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class AdventureRulesTest{
    private fun pet()=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L)
    @Test fun collectsResourceOnlyOnce(){
        val p=pet();val q=AdventureRules.move(p,GridPoint(1,2),1000);val r=AdventureRules.move(q,GridPoint(1,1),1000)
        assertTrue(r.coins>p.coins)
        val s=AdventureRules.move(AdventureRules.move(r,GridPoint(1,2),1000),GridPoint(1,1),1000)
        assertEquals(r.coins,s.coins);assertEquals(r.xp,s.xp)
    }
    @Test fun adjacentEnemyStartsPersistentBattle(){
        val p=pet().copy(adventure=AdventureState(pos=GridPoint(3,2)))
        val q=AdventureRules.move(p,GridPoint(3,1),1000)
        assertNotNull(q.adventure.battle);assertEquals(p.energy-5,q.energy)
        val restored=AdventureCodec.decode(AdventureCodec.encode(q.adventure))
        assertEquals(q.adventure.battle!!.enemies,restored.battle!!.enemies)
        assertEquals(q.adventure.battle!!.hp,restored.battle!!.hp)
        assertEquals(q,WorldRules.tick(q,90000000))
    }
    @Test fun shootingCostsOneAndVictoryCannotBeClaimedAgain(){
        val p=pet().copy(equipment=RobotEquipment().equip(RobotGear.RIFLE_L),adventure=AdventureState(pos=GridPoint(3,2)))
        val q=AdventureRules.move(p,GridPoint(3,1),1000)
        val b=q.adventure.battle!!
        val ready=q.copy(adventure=q.adventure.copy(battle=b.copy(pos=GridPoint(4,2),enemies=listOf(b.enemies.first().copy(hp=1)))))
        val win=AdventureRules.strike(ready,0,RobotGear.RIFLE_L,2000)
        assertNull(win.adventure.battle);assertEquals(1,win.wins);assertEquals(ready.stock(WorldSupply.PULSE)-1,win.stock(WorldSupply.PULSE))
        assertEquals(win,AdventureRules.strike(win,0,RobotGear.RIFLE_L,2000))
        assertTrue(AdventureRules.id(GridPoint(3,1)) in win.adventure.cleared)
    }
    @Test fun hexNeighborsAndRangeAgree(){
        for(y in 0 until AdventureRules.ARENA_H)for(x in 0 until AdventureRules.ARENA_W){
            val p=GridPoint(x,y)
            val neighbors=AdventureRules.arenaNeighbors(p)
            assertEquals(6,neighbors.toSet().size)
            for(q in neighbors.filter{AdventureRules.inArena(it)}){
                assertTrue(p in AdventureRules.arenaNeighbors(q))
                assertEquals(1,AdventureRules.arenaDistance(p,q))
                assertEquals(1,AdventureRules.path(p,q,emptySet()))
            }
            assertEquals(0,AdventureRules.arenaDistance(p,p))
        }
    }
    @Test fun pathAvoidsOccupiedCells(){
        assertEquals(3,AdventureRules.path(GridPoint(0,2),GridPoint(2,2),setOf(GridPoint(1,2))))
        assertNull(AdventureRules.path(GridPoint(0,2),GridPoint(1,2),setOf(GridPoint(1,2))))
    }
}
