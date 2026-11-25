package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Complete native pixel poses for the four mature Lisyk variants.
 * No cut-out limbs, face overlay, or deformation of a neutral portrait. */
internal object LisykMatureArtwork {
    const val CELL=96
    const val COLUMNS=6
    const val ROWS=4
    const val COUNT=24
    private val atlases=mutableMapOf<Pair<GrowthStage,Sex>,Bitmap>()
    fun matches(species:Species,stage:GrowthStage)=species==Species.LISYK &&
        (stage==GrowthStage.ADULT||stage==GrowthStage.ULTRA)
    fun asset(stage:GrowthStage,sex:Sex):String{
        require(matches(Species.LISYK,stage))
        return "lisyk_mature_v1/lisyk_"+stage.name.lowercase()+"_"+sex.name.lowercase()+".png"
    }
    fun index(clip:SpriteClip,frame:Int,closure:Int):Int=when(clip){
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.DIG -> 4+frame.mod(4)
        SpriteClip.HOP -> 8+frame.mod(4)
        SpriteClip.HAPPY -> 15
        SpriteClip.SAD -> 16
        SpriteClip.TIRED -> 17
        SpriteClip.SURPRISE -> 18
        SpriteClip.EAT -> if(frame.mod(2)==0)19 else 22
        SpriteClip.DRINK -> if(frame.mod(2)==0)20 else 23
        SpriteClip.SLEEP -> 21
        else -> when(closure.coerceIn(0,4)){0->12;1,2->13;else->14}
    }

    @Synchronized fun bitmap(context:Context,stage:GrowthStage,sex:Sex,clip:SpriteClip,frame:Int,closure:Int):Bitmap {
        val atlas=atlases.getOrPut(stage to sex){
            context.assets.open(asset(stage,sex)).use{BitmapFactory.decodeStream(it)?:error("Invalid Lisyk pose atlas")}
                .also{require(it.width==COLUMNS*CELL&&it.height==ROWS*CELL)}
        }
        val i=index(clip,frame,closure)
        return Bitmap.createBitmap(atlas,i%COLUMNS*CELL,i/COLUMNS*CELL,CELL,CELL)
    }
}
