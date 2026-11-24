package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

internal data class PixuFrameKey(
    val species:Species,val stage:GrowthStage,val sex:Sex,val gene:Int,
    val clip:SpriteClip,val frame:Int,val emotion:PetEmotion,val closure:Int
)

/** One frame contract: a transparent 192px cell, discrete frames and fixed
 * native direction. Original illustrated poses remain preferred. Generated
 * poses are derived only from the corresponding stage's own artwork. */
internal object PixuFrameLibrary {
    const val CELL=192
    private const val CACHE_BYTES=16*1024*1024
    private val cache=object:LruCache<PixuFrameKey,Bitmap>(CACHE_BYTES){
        override fun sizeOf(key:PixuFrameKey,value:Bitmap)=value.allocationByteCount
    }
    private val nativeOlder=mutableMapOf<Pair<Species,GrowthStage>,IntArray>()
    private val nativeYoung=mutableMapOf<Species,IntArray>()

    fun directional(clip:SpriteClip)=clip in setOf(SpriteClip.WALK,SpriteClip.HOP,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY)
    fun moves(context:Context,s:Species,g:GrowthStage,sex:Sex,clip:SpriteClip):Boolean =
        ImportedPixuArt.form(context,s,g,sex)?.let{ImportedPixuArt.movement(it,clip)} ?: moves(s,g,clip)
    fun moves(s:Species,g:GrowthStage,clip:SpriteClip):Boolean {
        if(!directional(clip))return false
        val pack=PetPosePack.forPet(s,g)
        return pack?.locomotion(clip)==true || clip==SpriteClip.HOP || pack==null
    }
    fun mirror(context:Context,k:PixuFrameKey,left:Boolean):Boolean =
        ImportedPixuArt.form(context,k.species,k.stage,k.sex)?.let{
            left && ((it.mirrorWalk && ImportedPixuArt.movement(it,k.clip)) ||
                ((ImportedPawMotion.supportsPreviewTravel(k.species,k.clip)||(k.species==Species.ISKRYK&&k.clip==SpriteClip.HOP)||(k.species==Species.LISYK&&k.clip==SpriteClip.DIG)) &&
                    !(k.stage==GrowthStage.BABY && k.sex==Sex.GIRL)))
        } ?: mirror(k,left)
    fun mirror(k:PixuFrameKey,left:Boolean):Boolean {
        if(!directional(k.clip))return false
        if(LisykStandardArtwork.matches(k.species,k.stage))return left
        val pack=PetPosePack.forPet(k.species,k.stage)
        if(pack!=null)return left&&pack.locomotion(k.clip)
        // The old juvenile illustration faces left; all ten new older sprites face right.
        return if(k.stage==GrowthStage.YOUNG)!left else left
    }

    @Synchronized fun get(context:Context,k:PixuFrameKey):Bitmap {
        ImportedPixuArt.form(context,k.species,k.stage,k.sex)?.let{form->
            val key=ImportedFrameCacheKey.normalize(k,form)
            cache.get(key)?.let{return it}
            return ImportedPixuArt.bitmap(context,key,form).also{cache.put(key,it)}
        }
        cache.get(k)?.let{return it}
        val pack=PetPosePack.forPet(k.species,k.stage)
        val raw=when {
            pack!=null -> pack.bitmap(context,k.clip,k.emotion,k.frame,k.closure>=3)
            LisykStandardArtwork.matches(k.species,k.stage) -> LisykStandardArtwork.bitmap(context,k.species,k.stage,k.clip,k.emotion,k.frame,k.closure>=3)
            k.stage==GrowthStage.ADULT||k.stage==GrowthStage.ULTRA -> {
                val src=nativeOlder.getOrPut(k.species to k.stage){ReferenceOlderArtwork.pixels(context,k.species,k.stage)}
                val pixels=ReferenceOlderFrames.frame(src,k.species,k.stage,k.clip,k.frame,k.emotion,k.closure)
                Bitmap.createBitmap(pixels,CELL,CELL,Bitmap.Config.ARGB_8888)
            }
            else -> {
                val src=nativeYoung.getOrPut(k.species){
                    val bmp=EvolutionSprites.get(context,k.species,GrowthStage.YOUNG)
                    IntArray(bmp.width*bmp.height).also{bmp.getPixels(it,0,bmp.width,0,0,bmp.width,bmp.height)}
                }
                val idx=k.closure+if(FaceExpression.animatedMouth(k.emotion)&&k.frame%2==1)5 else 0
                val face=if(k.emotion==PetEmotion.IDLE&&k.closure==0)src else
                    EyePixelAnimator.faceFrames(src,CELL,CELL,NativeEyeGeometry.regions(k.species,k.stage),
                        NativeMouthGeometry.region(k.species,k.stage),k.species==Species.KHMARYK,k.emotion)[idx]
                val moved=IllustratedMotionFrames.frame(face,CELL,k.species,k.stage,k.clip,k.frame)
                Bitmap.createBitmap(moved,CELL,CELL,Bitmap.Config.ARGB_8888)
            }
        }
        // Shape edits are done on the native raster, before nearest-neighbor
        // normalization. They are never extra eyes or a screen overlay.
        val appearance=if(k.sex==Sex.GIRL){
            val w=raw.width;val h=raw.height
            val px=IntArray(w*h);raw.getPixels(px,0,w,0,0,w,h)
            val variant=PixuSilhouette.female(px,w,h,k.species)
            Bitmap.createBitmap(variant,w,h,Bitmap.Config.ARGB_8888)
        }else raw
        val colored=PixuPalette.apply(appearance,k.species,k.sex,k.gene)
        val final=if(colored.width==CELL&&colored.height==CELL)colored
            else Bitmap.createScaledBitmap(colored,CELL,CELL,false)
        cache.put(k,final)
        return final
    }
}
