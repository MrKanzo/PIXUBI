package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Independent armored juvenile fox poses, never scaled baby art. */
internal object LisykYoungArtwork {
    const val CELL=192
    const val ASSET="lisyk_young_art.png"
    private var atlas:Bitmap?=null
    private val cache=mutableMapOf<Int,Bitmap>()
    fun matches(s:Species,g:GrowthStage)=s==Species.LISYK&&g==GrowthStage.YOUNG
    fun locomotion(c:SpriteClip)=c in listOf(SpriteClip.WALK,SpriteClip.HOP,SpriteClip.DIG)

    fun index(c:SpriteClip,e:PetEmotion,frame:Int,blink:Boolean):Int=when(c) {
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.HOP -> if(frame.mod(4) in 1..2) 6 else 0
        SpriteClip.DIG -> 4+frame.mod(2)
        SpriteClip.SLEEP -> 7
        else -> 8+LisykFrontArtwork.index(e,frame,blink)
    }

    @Synchronized fun get(context:Context,index:Int):Bitmap {
        require(index in 0..15)
        cache[index]?.let{return it}
        val src=atlas?:context.assets.open(ASSET).use{
            BitmapFactory.decodeStream(it)?:error("Cannot decode juvenile Lisyk artwork")
        }.also {
            require(it.width==CELL*4&&it.height==CELL*4)
            atlas=it
        }
        return Bitmap.createBitmap(src,index%4*CELL,index/4*CELL,CELL,CELL).also{cache[index]=it}
    }
}
