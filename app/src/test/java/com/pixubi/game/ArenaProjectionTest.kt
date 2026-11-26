package com.pixubi.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

class ArenaProjectionTest{
    @Test fun centersAndFeetRoundTripInPortraitAndLandscape(){
        for((w,h) in listOf(360f to 370f,820f to 270f,1000f to 440f,320f to 240f)){
            val plane=ArenaProjection(w,h)
            assertTrue(plane.row<=plane.cell*.86f+.001f)
            for(q in AdventureRules.arenaCells){
                assertEquals(q,plane.hit(plane.center(q)))
                assertEquals(q,plane.hit(plane.foot(q)))
                for(v in plane.vertices(q)){
                    assertTrue(v.x in 0f..w)
                    assertTrue(v.y in 0f..h)
                }
            }
            assertTrue(plane.scale(GridPoint(0,2))>plane.scale(GridPoint(0,0)))
            assertTrue(plane.perspective(2f)>plane.perspective(0f))
            assertNull(plane.hit(Offset.Zero))
        }
    }
    @Test fun neighboringHexesShareTwoProjectedVertices(){
        val plane=ArenaProjection(800f,300f)
        for(q in AdventureRules.arenaCells){
            for(n in AdventureRules.arenaNeighbors(q).filter(AdventureRules::inArena)){
                val common=plane.vertices(q).count{a->plane.vertices(n).any{b->(a-b).getDistance()<.01f}}
                assertEquals(2,common)
            }
        }
    }
}
