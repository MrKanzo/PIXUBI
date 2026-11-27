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
    @Test fun everyPairOfUnitsFitsWithoutIntersectingAtRest(){
        for((w,h) in listOf(360f to 274f,820f to 270f,1000f to 440f,320f to 240f)){
            val p=ArenaProjection(w,h)
            for(a in AdventureRules.arenaCells)for(b in AdventureRules.arenaCells.filter{it!=a}){
                val aa=p.envelope(a,listOf(a,b));val bb=p.envelope(b,listOf(a,b))
                val af=p.foot(a);val bf=p.foot(b)
                val separatedX=kotlin.math.abs(af.x-bf.x)>=(aa.width+bb.width)/2f-.01f
                val separatedY=af.y-aa.height*.89f>=bf.y+bb.height*.11f-.01f||
                    bf.y-bb.height*.89f>=af.y+aa.height*.11f-.01f
                assertTrue("Overlapping cells $a / $b at $w x $h",separatedX||separatedY)
                assertTrue(af.x-aa.width/2>=0f&&af.x+aa.width/2<=w)
                assertTrue(af.y-aa.height*.89f>=-.01f)
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
