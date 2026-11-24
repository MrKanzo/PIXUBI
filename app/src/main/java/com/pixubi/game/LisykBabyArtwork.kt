package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Eight individually illustrated, aligned, transparent poses; never warp an original. */
internal object LisykBabyArtwork {
    const val CELL=192
    const val ASSET="lisyk_baby_pixel_poses.png"
    private var atlas:Bitmap?=null
    private val cells=mutableMapOf<Int,Bitmap>()
    fun matches(s:Species,g:GrowthStage)=s==Species.LISYK&&g==GrowthStage.BABY
    fun locomotion(c:SpriteClip)=c==SpriteClip.WALK||c==SpriteClip.DIG
    fun cell(c:SpriteClip,frame:Int)=when(c) {
        SpriteClip.WALK -> frame.mod(4)
        SpriteClip.DIG -> 6
        SpriteClip.SLEEP -> 7
        SpriteClip.SURPRISE,SpriteClip.HOP -> 5
        else -> 4
    }
    @Synchronized fun get(context:Context,index:Int):Bitmap {
        require(index in 0..7)
        cells[index]?.let{return it}
        val src=atlas?:context.assets.open(ASSET).use {
            BitmapFactory.decodeStream(it)?:error("Lisyk artwork failed to decode")
        }.also {
            require(it.width==CELL*4&&it.height==CELL*2)
            atlas=it
        }
        return Bitmap.createBitmap(src,index%4*CELL,index/4*CELL,CELL,CELL).also{cells[index]=it}
    }
    // Per-pose socket locations in this sheet, not the old frontal avatar.
    // These are initial attachment guides; no gear is baked onto new body poses.
    private val guides=arrayOf(
        intArrayOf(111,62,134,115,90,133,72,141),
        intArrayOf(115,61,139,113,94,132,67,143),
        intArrayOf(122,62,143,114,96,135,64,140),
        intArrayOf(126,63,148,115,101,135,61,141),
        intArrayOf(116,65,132,124,111,144,76,153),
        intArrayOf(119,61,143,114,94,134,66,142),
        intArrayOf(129,114,128,151,91,137,56,121),
        intArrayOf(70,114,81,151,121,146,142,156)
    )
    fun sockets(index:Int):List<RigSocket> {
        val a=guides[index]
        fun at(s:EquipmentSlot,l:PixuLayer,i:Int)=RigSocket(s,l,RigPoint(a[i].toFloat(),a[i+1].toFloat()))
        return listOf(at(EquipmentSlot.HEAD,PixuLayer.HEAD,0),
            at(EquipmentSlot.NECK,PixuLayer.BODY,2),
            at(EquipmentSlot.BACK,PixuLayer.BODY,4),
            at(EquipmentSlot.TAIL,PixuLayer.TAIL,6))
    }

    /** Only the sitting face uses local facial frames. The walking and sleeping
     * silhouettes are complete, authored PNG cells, so legs cannot split. */
    fun sittingFaces(context:Context,emotion:PetEmotion):List<Bitmap> {
        val src=get(context,4)
        val px=IntArray(CELL*CELL)
        src.getPixels(px,0,CELL,0,0,CELL,CELL)
        val eyes=listOf(EyeRegion(114f,95f,15f,13f,.22f),EyeRegion(146f,91f,9f,12f,.20f))
        val mouth=EyeRegion(134f,110f,10f,9f)
        return EyePixelAnimator.faceFrames(px,CELL,CELL,eyes,mouth,false,emotion).mapIndexed { i,colors->
            if(i==0&&emotion==PetEmotion.IDLE)src
            else Bitmap.createBitmap(colors,CELL,CELL,Bitmap.Config.ARGB_8888)
        }
    }
}
