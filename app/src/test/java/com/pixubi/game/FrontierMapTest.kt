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
