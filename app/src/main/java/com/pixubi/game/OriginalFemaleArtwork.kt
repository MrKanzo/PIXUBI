package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/** Twenty original, independently illustrated girls, restored from the project
 * archive. Native authored faces and full-body poses always take priority. */
internal object OriginalFemaleArtwork {
    const val NATIVE=128
    const val CELL=192
    enum class Quality { AUTHORED, DERIVED }
    internal data class Choice(val index:Int,val quality:Quality)
    private data class FaceKey(val s:Species,val g:GrowthStage,val e:PetEmotion)
    private val atlasCache by lazy { object:LruCache<String,Bitmap>(12*1024*1024){
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    } }
    private val faceCache by lazy { object:LruCache<FaceKey,List<IntArray>>(8*1024*1024){
        override fun sizeOf(key:FaceKey,value:List<IntArray>)=value.sumOf{it.size*4}
    } }
    fun stageName(g:GrowthStage)=when(GrowthStage.migrate(g)){
        GrowthStage.BABY->"young";GrowthStage.YOUNG->"teen"
        GrowthStage.ADULT->"adult";GrowthStage.ULTRA->"ultra"
        else->error("Invalid stage")
    }
    fun asset(s:Species,g:GrowthStage)="original_female/pixubi_"+s.name.lowercase()+"_female_"+stageName(g)+".png"
    fun hasBodyPoses(g:GrowthStage)=GrowthStage.migrate(g) in listOf(GrowthStage.BABY,GrowthStage.ADULT)
    fun choice(g:GrowthStage,clip:SpriteClip,f:Int,closure:Int=0):Choice {
        val body=hasBodyPoses(g)
        val authored=when(clip){
            SpriteClip.HAPPY->1;SpriteClip.SAD->2;SpriteClip.SURPRISE->3;SpriteClip.TIRED->4
            SpriteClip.WALK->if(body)intArrayOf(5,5,6,6)[f.mod(4)] else null
            SpriteClip.HOP->if(body&&f.mod(4) in 1..2)7 else null
            SpriteClip.SLEEP->if(body)8 else null
            SpriteClip.IDLE->if(closure==0&&f.mod(4)==0)0 else null
            else->null
        }
        return if(authored!=null)Choice(authored,Quality.AUTHORED) else Choice(0,Quality.DERIVED)
    }
    /** Same origin for every frame, including the much lower sleeping pose. */
    fun normalize(px:IntArray):IntArray{
        require(px.size==NATIVE*NATIVE)
        val out=IntArray(CELL*CELL)
        for(y in 4 until CELL)for(x in 0 until CELL){
            val sy=(y-4)*NATIVE/CELL;val sx=x*NATIVE/CELL
            if(sy in 0 until NATIVE)out[y*CELL+x]=px[sy*NATIVE+sx]
        }
        return out
    }
    fun derive(px:IntArray,k:PixuFrameKey):IntArray{
        val e=k.emotion
        val step=if(k.clip==SpriteClip.SLEEP)4 else k.closure.coerceIn(0,4)
        val facial=k.clip in listOf(SpriteClip.IDLE,SpriteClip.EAT,SpriteClip.DRINK,SpriteClip.SLEEP)
        val faced=if(facial&&(e!=PetEmotion.IDLE||step>0)){
            val fk=FaceKey(k.species,k.stage,e)
            val frames=faceCache.get(fk)?:OriginalFaceGeometry.frames(px,k.species,k.stage,Sex.GIRL,e)
                .also{faceCache.put(fk,it)}
            frames[OriginalFaceGeometry.frameIndex(e,step,k.frame)]
        }else px
        return OriginalFemaleMotion.frame(faced,k.species,k.stage,k.clip,k.frame)
    }
    @Synchronized fun bitmap(context:Context,k:PixuFrameKey):Bitmap{
        val path=asset(k.species,k.stage)
        val atlas=atlasCache.get(path)?: (context.assets.open(path).use{BitmapFactory.decodeStream(it)}
            ?:error("Missing original female art")).also{
                require(it.width==NATIVE*4&&it.height==NATIVE*(if(hasBodyPoses(k.stage))3 else 2))
                atlasCache.put(path,it)
            }
        val c=choice(k.stage,k.clip,k.frame,k.closure)
        val px=IntArray(NATIVE*NATIVE)
        atlas.getPixels(px,0,NATIVE,c.index%4*NATIVE,c.index/4*NATIVE,NATIVE,NATIVE)
        val raw=if(c.quality==Quality.AUTHORED)px else derive(px,k)
        return Bitmap.createBitmap(normalize(raw),CELL,CELL,Bitmap.Config.ARGB_8888)
    }
}
