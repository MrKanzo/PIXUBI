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
            assertEquals(plane.scale(GridPoint(0,4)),plane.scale(GridPoint(0,0)),.001f)
            assertTrue(plane.perspective(2f)>plane.perspective(0f))
            assertNull(plane.hit(Offset.Zero))
        }
    }
    @Test fun spritesKeepSameSizeForEveryCellAndOccupancy(){
        for((w,h) in listOf(360f to 274f,820f to 270f,1000f to 440f,320f to 240f)){
            val p=ArenaProjection(w,h)
            val expected=p.envelope(GridPoint(0,0),emptyList())
            for(q in AdventureRules.arenaCells){
                assertEquals(expected,p.envelope(q,AdventureRules.arenaCells))
                assertEquals(expected,p.envelope(q,emptyList()))
                assertTrue(p.foot(q).y-expected.height*.89f>=0f)
            }
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
