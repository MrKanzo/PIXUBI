package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Independent illustrated face poses. No generated eyelid or mouth patches. */
internal object LisykFrontArtwork {
    const val ASSET="lisyk_baby_front_expressions.png"
    const val CELL=192
    private var atlas:Bitmap?=null
    private val cache=mutableMapOf<Int,Bitmap>()

    fun usesFront(clip:SpriteClip)=clip!=SpriteClip.WALK&&clip!=SpriteClip.DIG&&clip!=SpriteClip.SLEEP

    /** 0 calm, 1 blink, 2 joy, 3 sad, 4 tired, 5 surprised, 6 chew, 7 sip. */
    fun index(emotion:PetEmotion,frame:Int,blink:Boolean):Int=when(emotion) {
        PetEmotion.IDLE -> if(blink)1 else 0
        PetEmotion.HAPPY -> if(frame%4==0)0 else 2
        PetEmotion.SAD -> 3
        PetEmotion.TIRED -> 4
        PetEmotion.SURPRISED -> 5
        PetEmotion.EATING -> if(frame%2==0)0 else 6
        PetEmotion.DRINKING -> if(frame%2==0)0 else 7
        PetEmotion.SLEEPING,PetEmotion.HIBERNATING -> 1
    }

    @Synchronized fun get(context:Context,index:Int):Bitmap {
        require(index in 0..7)
        cache[index]?.let{return it}
        val src=atlas?:context.assets.open(ASSET).use{
            BitmapFactory.decodeStream(it)?:error("Cannot decode Lisyk front art")
        }.also {
            require(it.width==CELL*4&&it.height==CELL*2)
            atlas=it
        }
        return Bitmap.createBitmap(src,index%4*CELL,index/4*CELL,CELL,CELL).also{cache[index]=it}
    }
}
