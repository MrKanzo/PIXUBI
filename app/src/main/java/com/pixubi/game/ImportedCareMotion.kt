package com.pixubi.game

import kotlin.math.roundToInt

/** Cosmetic care animation. Resource changes are owned by GameEngine. */
internal enum class CareVisual { FOOD, WATER, CHARGE }
internal object ImportedCareMotion {
    fun accepted(before:Pet,after:Pet,action:Action):CareVisual? {
        if(!before.alive||before.mode!=Mode.AWAKE||!after.alive)return null
        return when(action){
            Action.FEED -> if(after.foodStock==before.foodStock-1)CareVisual.FOOD else null
            Action.WATER -> if(after.waterStock==before.waterStock-1)CareVisual.WATER else null
            Action.CHARGE -> if(after.chargeStock==before.chargeStock-1)CareVisual.CHARGE else null
            else -> null
        }
    }
    fun allowed(pet:Pet,searching:Boolean)=pet.alive&&pet.mode==Mode.AWAKE&&!searching
    fun fromDeveloper(action:DevAction?)=when(action){
        DevAction.EAT->CareVisual.FOOD
        DevAction.DRINK->CareVisual.WATER
        DevAction.CHARGE->CareVisual.CHARGE
        else->null
    }
    fun applies(k:PixuFrameKey)=k.clip==SpriteClip.EAT||k.clip==SpriteClip.DRINK
    fun nod(src:IntArray,s:Species,g:GrowthStage,sex:Sex,clip:SpriteClip,phase:Int):IntArray{
        require(src.size==128*128)
        val amount=if(clip==SpriteClip.DRINK)intArrayOf(1,4,3,0)[phase.mod(4)]
            else intArrayOf(0,2,1,2)[phase.mod(4)]
        if(amount==0)return src
        val mouth=ImportedFaceGeometry.mouth(s,g,sex)
        val left=(mouth.centerX.toInt()-32).coerceAtLeast(0)
        val top=6
        val bottom=94
        val out=src.copyOf()
        for(y in top until bottom)for(x in left until 128){
            val w=minOf(1f,(x-left)/12f,(y-top)/8f,(bottom-1-y)/12f)
            val dy=(amount*w).roundToInt()
            out[y*128+x]=src[(y-dy)*128+x]
        }
        return out
    }
    fun phase(seconds:Float)=if(seconds.isFinite())((seconds.coerceAtLeast(0f)%1f)) else 0f
    // Convert a native mouth x coordinate to the same normalized 192px cell as the world sprite.
    fun propX(s:Species,g:GrowthStage,sex:Sex):Float {
        val n=ImportedPixuArt.size(g)
        val mouth=ImportedFaceGeometry.mouth(s,g,sex)
        return ((mouth.centerX-64f)*n/128f)*194f/192f
    }
}
