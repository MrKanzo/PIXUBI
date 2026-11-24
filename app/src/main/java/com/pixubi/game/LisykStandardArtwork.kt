package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Full-body pixel pose cells for the fourteen forms without original pose packs.
 * 64px native raster, 16 discrete poses, aligned ground; no face overlays.
 * This is the first pass of the Lisyk-standard redesign, not final art approval.
 */
internal object LisykStandardArtwork {
    const val CELL=64
    private val atlases=mutableMapOf<Pair<Species,GrowthStage>,Bitmap>()
    fun matches(s:Species,g:GrowthStage)=g==GrowthStage.ADULT||g==GrowthStage.ULTRA||
        (g==GrowthStage.YOUNG&&s!=Species.LISYK)
    fun asset(s:Species,g:GrowthStage):String {
        require(matches(s,g))
        return "lisyk_standard_"+s.name.lowercase()+"_"+g.name.lowercase()+".png"
    }
    fun index(clip:SpriteClip,e:PetEmotion,frame:Int,closed:Boolean):Int=when(clip) {
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.HOP -> when(frame.mod(4)){0->0;1,2->6;else->3}
        SpriteClip.DIG,SpriteClip.FLY,SpriteClip.SWIM -> 4+frame.mod(2)
        SpriteClip.SLEEP -> 7
        else -> 8+LisykFrontArtwork.index(e,frame,closed)
    }
    @Synchronized fun bitmap(context:Context,s:Species,g:GrowthStage,clip:SpriteClip,
        e:PetEmotion,frame:Int,closed:Boolean):Bitmap {
        val source=atlases.getOrPut(s to g){
            context.assets.open(asset(s,g)).use{BitmapFactory.decodeStream(it)?:error("Invalid pose sheet")}
                .also{require(it.width==4*CELL&&it.height==4*CELL)}
        }
        val i=index(clip,e,frame,closed)
        return Bitmap.createBitmap(source,i%4*CELL,i/4*CELL,CELL,CELL)
    }
}
