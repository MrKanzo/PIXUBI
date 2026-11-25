package com.pixubi.game

import kotlin.math.roundToInt

/** One transform per original model, never recenter each animated frame.
 * A constant ground line and uniform scale preserve gait and aspect ratio. */
internal object NativeSpriteLayout {
    const val CELL=192
    const val GROUND=178
    internal data class Fit(val scale:Float,val centerX:Float,val bottom:Int)
    fun fit(src:IntArray,stage:GrowthStage):Fit {
        require(src.size==CELL*CELL)
        var l=CELL;var r=-1;var t=CELL;var b=-1
        for(i in src.indices)if(src[i] ushr 24>=128){
            val x=i%CELL;val y=i/CELL
            l=minOf(l,x);r=maxOf(r,x);t=minOf(t,y);b=maxOf(b,y)
        }
        require(r>=l&&b>=t){"Empty creature artwork"}
        val extent=maxOf(r-l+1,b-t+1)
        val target=when(stage){
            GrowthStage.YOUNG -> 150
            GrowthStage.ADULT -> 160
            GrowthStage.ULTRA -> 170
            else -> 145
        }
        return Fit(target.toFloat()/extent,(l+r)*.5f,b)
    }
    fun render(src:IntArray,fit:Fit,mirror:Boolean=false):IntArray {
        require(src.size==CELL*CELL)
        val out=IntArray(src.size)
        for(y in 0 until CELL)for(x in 0 until CELL){
            val dx=if(mirror)CELL-1-x else x
            val sx=((dx-(CELL-1)*.5f)/fit.scale+fit.centerX).roundToInt()
            val sy=((y-GROUND)/fit.scale+fit.bottom).roundToInt()
            if(sx in 0 until CELL&&sy in 0 until CELL)out[y*CELL+x]=src[sy*CELL+sx]
        }
        return PixuRaster.pixelate(out,CELL)
    }
    fun nativeMotion(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,frame:Int):IntArray {
        return NativePoseMotion.frame(src,s,g,clip,frame)
    }
}
