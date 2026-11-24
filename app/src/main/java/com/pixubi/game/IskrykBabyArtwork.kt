package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Individually illustrated full-body poses, with aligned feet and no mesh warping. */
internal object IskrykBabyArtwork {
    const val CELL=192
    const val ASSET="iskryk_baby_art.png"
    private var atlas:Bitmap?=null
    private val cache=mutableMapOf<Int,Bitmap>()
    fun matches(s:Species,g:GrowthStage)=s==Species.ISKRYK&&g==GrowthStage.BABY
    fun locomotion(c:SpriteClip)=c==SpriteClip.WALK||c==SpriteClip.HOP||c==SpriteClip.DIG

    // 0..3 walk, 4 seated, 5 airborne, 6 sniff, 7 sleep;
    // 8..15 calm, blink, joy, sad, tired, surprise, chew, sip.
    fun index(c:SpriteClip,e:PetEmotion,frame:Int,blink:Boolean):Int=when(c) {
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.HOP -> when(frame.mod(4)){0->0;1,2->5;else->3}
        SpriteClip.DIG -> 6
        SpriteClip.SLEEP -> 7
        else -> 8+LisykFrontArtwork.index(e,frame,blink)
    }

    @Synchronized fun get(context:Context,i:Int):Bitmap {
        require(i in 0..15)
        cache[i]?.let{return it}
        val src=atlas?:context.assets.open(ASSET).use{
            BitmapFactory.decodeStream(it)?:error("Iskryk sprite sheet cannot be decoded")
        }.also {
            require(it.width==CELL*4&&it.height==CELL*4)
            atlas=it
        }
        return Bitmap.createBitmap(src,i%4*CELL,i/4*CELL,CELL,CELL).also{cache[i]=it}
    }
}
