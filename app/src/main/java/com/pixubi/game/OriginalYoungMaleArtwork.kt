package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/** Recover original side-facing male juveniles, without borrowing female limbs.
 * All missing poses remain derived and require artist approval. */
internal object OriginalYoungMaleArtwork {
    private data class FaceKey(val s:Species,val e:PetEmotion)
    private val originals by lazy { mutableMapOf<Species,IntArray>() }
    private val faces by lazy { object:LruCache<FaceKey,List<IntArray>>(6*1024*1024){
        override fun sizeOf(key:FaceKey,value:List<IntArray>)=value.sumOf{it.size*4}
    } }
    fun asset(s:Species)="original_male/pixubi_"+s.name.lowercase()+"_male_teen.png"
    fun matches(s:Species,g:GrowthStage)=s!=Species.LISYK&&g==GrowthStage.YOUNG
    fun pose(src:IntArray,k:PixuFrameKey,faceFrames:List<IntArray>?=null):IntArray{
        val step=if(k.clip==SpriteClip.SLEEP)4 else k.closure.coerceIn(0,4)
        val face=if(k.emotion==PetEmotion.IDLE&&step==0)src else
            (faceFrames?:OriginalFaceGeometry.frames(src,k.species,k.stage,Sex.BOY,k.emotion))[
                OriginalFaceGeometry.frameIndex(k.emotion,step,k.frame)]
        return OriginalFemaleMotion.frame(face,k.species,k.stage,k.clip,k.frame,Sex.BOY)
    }
    @Synchronized fun bitmap(context:Context,k:PixuFrameKey):Bitmap {
        val px=originals.getOrPut(k.species){
            val bmp=context.assets.open(asset(k.species)).use{BitmapFactory.decodeStream(it)}?:error("Missing original juvenile")
            require(bmp.width==128&&bmp.height==128)
            IntArray(128*128).also{bmp.getPixels(it,0,128,0,0,128,128)}
        }
        val step=if(k.clip==SpriteClip.SLEEP)4 else k.closure.coerceIn(0,4)
        val frames=if(k.emotion!=PetEmotion.IDLE||step>0){
            val fk=FaceKey(k.species,k.emotion)
            faces.get(fk)?:OriginalFaceGeometry.frames(px,k.species,k.stage,Sex.BOY,k.emotion).also{faces.put(fk,it)}
        }else null
        return Bitmap.createBitmap(OriginalFemaleArtwork.normalize(pose(px,k,frames)),192,192,Bitmap.Config.ARGB_8888)
    }
}
