package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class BaseReturnTest{
    @Test fun safeRouteReturnsFromNearbyGround(){
        val a=AdventureState(pos=GridPoint(1,2))
        val route=AdventureRules.homeRoute(a)
        assertEquals(listOf(AdventureRules.base),route)
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L).copy(adventure=a)
        assertNotNull(BaseRules.locationReason(p))
        val q=AdventureRules.move(p,route.first(),2000L)
        assertNull(BaseRules.locationReason(q))
        assertNull(BaseRules.buyReason(q.copy(coins=2000),BaseBuilding.BARRACKS))
    }
    @Test fun noRouteThroughUnexploredGround(){
        val a=AdventureState(pos=GridPoint(15,13))
        assertTrue(AdventureRules.homeRoute(a).isEmpty())
    }
    @Test fun routeUsesOnlyDiscoveredSafeMovement(){
        val a=AdventureState(pos=GridPoint(19,10),seen=(0 until AdventureRules.MAP_W*AdventureRules.MAP_H).toSet())
        val route=AdventureRules.homeRoute(a)
        assertTrue(route.isNotEmpty())
        var pos=a.pos
        for(q in route){
            assertEquals(1,pos.distance(q));assertNotEquals(AdventureTile.WALL,AdventureRules.tile(a,q));assertFalse(FrontierSites.hostile(AdventureRules.tile(a,q)))
            pos=FrontierNetwork.destination(a.stage,q)?:q
        }
        assertEquals(AdventureRules.base,pos)
    }
}
