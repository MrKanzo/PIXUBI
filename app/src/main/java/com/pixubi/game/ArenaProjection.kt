package com.pixubi.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.min

/** Shared ground projection for the grid, taps, actors and effects.
 * The plane narrows toward the horizon; models stay upright in screen space.
 */
internal class ArenaProjection(val width:Float,val height:Float){
    val cell=width/8.2f
    val row=min(cell*.86f,height*.57f/5f)
    val groundBottom=height-row*.88f-height*.015f
    private fun depth(y:Float)=((y+.5f)/5f).coerceIn(0f,1f)
    fun perspective(y:Float)=.87f+.13f*depth(y)
    fun depth(q:GridPoint)=q.y+if(q.x%2==1).5f else 0f
    fun project(x:Float,y:Float):Offset{
        val d=perspective(y)
        return Offset(width*.5f+(x-3f)*cell*d,groundBottom-(4f-y)*row)
    }
    fun center(q:GridPoint)=project(q.x*.75f,depth(q))
    fun vertices(q:GridPoint):List<Offset>{
        val x=q.x*.75f;val y=depth(q)
        return listOf(project(x-.5f,y),project(x-.25f,y-.5f),project(x+.25f,y-.5f),
            project(x+.5f,y),project(x+.25f,y+.5f),project(x-.25f,y+.5f))
    }
    fun foot(q:GridPoint)=project(q.x*.75f,depth(q)+.12f)
    fun scale(q:GridPoint)=1f
    // Arena-specific fixed screen-space size, independent of neighbors, depth and class.
    val actorHeight=min(width*.166f,height*.28f)
    fun envelope(q:GridPoint,occupied:List<GridPoint>):Size=Size(actorHeight*1.9f,actorHeight/.70f)
    fun hit(p:Offset):GridPoint?=AdventureRules.arenaCells.firstOrNull{q->
        val v=vertices(q)
        var positive=false;var negative=false
        for(i in v.indices){
            val a=v[i];val b=v[(i+1)%v.size]
            val cross=(b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x)
            if(cross>.01f)positive=true
            if(cross<-.01f)negative=true
        }
        !(positive&&negative)
    }
}
