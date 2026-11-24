package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** One consistently illustrated baby Kraplyk atlas; full poses, no face overlay. */
internal object KraplykBabyArtwork {
    const val CELL=192
    const val ASSET="kraplyk_baby_art.png"
    private var atlas:Bitmap?=null
    private val cache=mutableMapOf<Int,Bitmap>()
    fun matches(s:Species,g:GrowthStage)=s==Species.KRAPLYK&&g==GrowthStage.BABY
    fun locomotion(c:SpriteClip)=c in listOf(SpriteClip.WALK,SpriteClip.SWIM,SpriteClip.HOP,SpriteClip.DIG)

    // 0..3 walking; 4..7 swimming; 8..15 facial expressions.
    fun index(c:SpriteClip,e:PetEmotion,frame:Int,blink:Boolean):Int=when(c) {
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.SWIM -> 4+frame.mod(4)
        SpriteClip.HOP -> if(frame.mod(4) in 1..2) 2 else 0
        SpriteClip.DIG -> 1+frame.mod(2)
        SpriteClip.SLEEP -> 9
        else -> 8+LisykFrontArtwork.index(e,frame,blink)
    }

    @Synchronized fun get(context:Context,index:Int):Bitmap {
        require(index in 0..15)
        cache[index]?.let{return it}
        val src=atlas?:context.assets.open(ASSET).use{
            BitmapFactory.decodeStream(it)?:error("Cannot decode Kraplyk artwork")
        }.also {
            require(it.width==CELL*4&&it.height==CELL*4)
            atlas=it
        }
        return Bitmap.createBitmap(src,index%4*CELL,index/4*CELL,CELL,CELL).also{cache[index]=it}
    }
}
