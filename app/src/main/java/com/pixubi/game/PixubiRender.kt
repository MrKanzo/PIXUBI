package com.pixubi.game

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal val LocalPixubiForeground=staticCompositionLocalOf{true}

/** Frame cadence is independent of simulation time and battle speed. */
internal object RenderBudget{
    const val ACTOR_FPS=30
    const val SCENERY_FPS=12
    const val MAX_PIXELS=2_000_000
    fun rasterSize(width:Int,height:Int):IntSize{
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        val scale=minOf(1.0,2048.0/maxOf(w,h),sqrt(MAX_PIXELS.toDouble()/(w.toDouble()*h)))
        return IntSize((w*scale).toInt().coerceAtLeast(1),(h*scale).toInt().coerceAtLeast(1))
    }
    fun phase(previous:Float,elapsedNanos:Long,periodMs:Int)=
        (previous+elapsedNanos.coerceAtLeast(0L)/1_000_000f/periodMs.coerceAtLeast(1))%1f
}
@Composable
internal fun rememberGamePhase(periodMs:Int,fps:Int=RenderBudget.ACTOR_FPS,enabled:Boolean=true):State<Float>{
    val foreground=LocalPixubiForeground.current
    val phase=remember{mutableFloatStateOf(0f)}
    LaunchedEffect(periodMs,fps,enabled,foreground){
        if(!enabled||!foreground)return@LaunchedEffect
        val interval=1_000_000_000L/fps.coerceIn(1,60)
        var previous=0L
        while(true)withFrameNanos{now->
            if(previous==0L)previous=now
            else if(now-previous>=interval){
                phase.floatValue=RenderBudget.phase(phase.floatValue,(now-previous).coerceAtMost(250_000_000L),periodMs)
                previous=now
            }
        }
    }
    return phase
}

/** Rasterize expensive, unchanged artwork once per size/content change.
 * Cache is scoped to the visible composable, bounded to roughly 8 MB per layer.
 */
internal fun Modifier.cachedScene(render:DrawScope.()->Unit)=drawWithCache{
    val target=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1))
    val raster=RenderBudget.rasterSize(target.width,target.height)
    val image=ImageBitmap(raster.width,raster.height)
    CanvasDrawScope().draw(this,layoutDirection,Canvas(image),Size(raster.width.toFloat(),raster.height.toFloat()),block=render)
    onDrawBehind{drawImage(image,dstSize=target)}
}
