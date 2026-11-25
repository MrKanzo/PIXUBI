package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

internal data class PixuFrameKey(
    val species:Species,val stage:GrowthStage,val sex:Sex,val gene:Int,
    val clip:SpriteClip,val frame:Int,val emotion:PetEmotion,val closure:Int
)

/** Six authored pose packs plus fourteen distinct original species rasters. */
internal object PixuFrameLibrary {
    const val CELL=192
    private val cache=object:LruCache<PixuFrameKey,Bitmap>(16*1024*1024){
        override fun sizeOf(key:PixuFrameKey,value:Bitmap)=value.allocationByteCount
    }
    private data class Model(val species:Species,val stage:GrowthStage,val sex:Sex)
    private data class FaceKey(val model:Model,val emotion:PetEmotion)
    private val originals=mutableMapOf<Model,IntArray>()
    private val layouts=mutableMapOf<Model,NativeSpriteLayout.Fit>()
    private val faces=object:LruCache<FaceKey,List<IntArray>>(8*1024*1024){
        override fun sizeOf(key:FaceKey,value:List<IntArray>)=value.sumOf{it.size*4}
    }
    fun directional(clip:SpriteClip)=PixuAnimationPolicy.directional(clip)
    fun moves(s:Species,g:GrowthStage,clip:SpriteClip)=
        directional(clip)&&PixuAnimationPolicy.supports(s,g,clip)
    fun mirror(k:PixuFrameKey,left:Boolean)=left&&moves(k.species,k.stage,k.clip)

    private fun sourcePixels(context:Context,model:Model,source:PixuArtSource):IntArray =
        originals.getOrPut(model){
            val src=if(source==PixuArtSource.OLDER_ATLAS)
                ReferenceOlderArtwork.pixels(context,model.species,model.stage)
            else {
                val bmp=EvolutionSprites.get(context,model.species,GrowthStage.YOUNG)
                require(bmp.width==CELL&&bmp.height==CELL)
                IntArray(CELL*CELL).also{bmp.getPixels(it,0,CELL,0,0,CELL,CELL)}
            }
            // A consistent silhouette per model, rather than per animation frame.
            if(model.sex==Sex.GIRL)PixuSilhouette.female(src,CELL,CELL,model.species) else src
        }

    private fun nativeFrame(context:Context,k:PixuFrameKey,source:PixuArtSource):Bitmap {
        val model=Model(k.species,k.stage,k.sex)
        val src=sourcePixels(context,model,source)
        val older=source==PixuArtSource.OLDER_ATLAS
        val geometry=if(older)ReferenceOlderFrames.geometry(k.species,k.stage) else null
        val step=if(k.clip==SpriteClip.SLEEP)4 else k.closure.coerceIn(0,4)
        val idx=step+if(FaceExpression.animatedMouth(k.emotion)&&k.frame%2==1)5 else 0
        val pixels=if(k.emotion==PetEmotion.IDLE&&step==0)src else {
            val fk=FaceKey(model,k.emotion)
            val frames=faces.get(fk)?:EyePixelAnimator.faceFrames(src,CELL,CELL,
                geometry?.eyes?:NativeEyeGeometry.regions(k.species,k.stage),
                geometry?.mouth?:NativeMouthGeometry.region(k.species,k.stage),
                k.species==Species.KHMARYK,k.emotion).also{faces.put(fk,it)}
            frames[idx]
        }
        val moved=NativeSpriteLayout.nativeMotion(pixels,k.species,k.stage,k.clip,k.frame)
        val fit=layouts.getOrPut(model){NativeSpriteLayout.fit(src,k.stage)}
        // Keep one native orientation for rest and travel: no idle-to-walk flip.
        val oriented=NativeSpriteLayout.render(moved,fit,mirror=!older)
        return Bitmap.createBitmap(oriented,CELL,CELL,Bitmap.Config.ARGB_8888)
    }

    @Synchronized fun get(context:Context,k:PixuFrameKey):Bitmap {
        cache.get(k)?.let{return it}
        val source=PixuArtRouting.source(k.species,k.stage)
        val raw=if(source==PixuArtSource.POSE_PACK){
            val pack=requireNotNull(PetPosePack.forPet(k.species,k.stage))
            val original=pack.bitmap(context,k.clip,k.emotion,k.frame,k.closure>=3)
            if(k.sex==Sex.GIRL){
                val w=original.width;val h=original.height
                val px=IntArray(w*h);original.getPixels(px,0,w,0,0,w,h)
                Bitmap.createBitmap(PixuSilhouette.female(px,w,h,k.species),w,h,Bitmap.Config.ARGB_8888)
            }else original
        }else nativeFrame(context,k,source)
        val colored=PixuPalette.apply(raw,k.species,k.sex,k.gene)
        val final=if(colored.width==CELL&&colored.height==CELL)colored
            else Bitmap.createScaledBitmap(colored,CELL,CELL,false)
        cache.put(k,final)
        return final
    }
}
