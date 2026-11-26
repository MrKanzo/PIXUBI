package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class FrontierMapTest{
    @Test fun projectionRoundTripsEveryNode(){
        for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
            val q=GridPoint(x,y);val c=FrontierProjection.center(q)
            assertEquals(q,FrontierProjection.at(c.x,c.y))
        }
    }
    @Test fun unknownAndRememberedObjectsAreNotExposed(){
        val hiddenEnemy=GridPoint(7,5)
        val hiddenResource=GridPoint(6,2)
        val start=AdventureState()
        assertFalse(AdventureRules.visible(start,hiddenEnemy))
        assertNull(AdventureRules.knownTile(start,hiddenEnemy))
        assertNull(AdventureRules.knownTile(start,hiddenResource))
        val explored=start.copy(seen=setOf(AdventureRules.id(hiddenEnemy),AdventureRules.id(hiddenResource)))
        assertTrue(AdventureRules.visible(explored,hiddenEnemy))
        assertFalse(AdventureRules.inSight(explored,hiddenEnemy))
        assertNull(AdventureRules.knownTile(explored,hiddenEnemy))
        assertNull(AdventureRules.knownTile(explored,hiddenResource))
        val nearby=explored.copy(pos=GridPoint(7,4))
        assertEquals(AdventureTile.ENEMY,AdventureRules.knownTile(nearby,hiddenEnemy))
        assertEquals(AdventureTile.COINS,AdventureRules.knownTile(explored.copy(pos=GridPoint(5,2)),hiddenResource))
        assertNull(AdventureRules.knownTile(start,GridPoint(-1,2)))
    }
    @Test fun pathAvoidsWallsAndStopsAtHostileDestination(){
        val a=AdventureState(pos=GridPoint(1,2),seen=(0 until 48).toSet())
        val route=AdventureRules.mapRoute(a,GridPoint(5,3))
        assertTrue(route.isNotEmpty());assertEquals(GridPoint(5,3),route.last())
        assertTrue(route.dropLast(1).none{AdventureRules.tile(a,it)==AdventureTile.ENEMY})
        assertTrue(route.none{AdventureRules.tile(a,it)==AdventureTile.WALL})
        assertTrue((listOf(a.pos)+route).zipWithNext().all{(x,y)->x.distance(y)==1})
        assertTrue(AdventureRules.mapRoute(a,GridPoint(4,3)).isEmpty())
        assertTrue(AdventureRules.mapRoute(AdventureState(),GridPoint(7,5)).isEmpty())
    }
}
