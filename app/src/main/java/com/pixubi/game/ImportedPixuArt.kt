package com.pixubi.game

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.util.LruCache
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.Locale


/** Test-only import. Stable artwork remains available until the new animation set is complete. */
internal object ImportedPixuArt {
    const val CATALOG = "imported_v2/catalog.json"
    const val NATIVE = 128
    data class Form(val prefix:String,val frames:Set<String>,val missing:List<String>,val mirrorWalk:Boolean,val atlas:String="",val rects:Map<String,IntArray> = emptyMap())
    private data class Pack(val forms:Map<String,Form>)
    private var attempted=false
    private var pack:Pack?=null
    private val atlases by lazy { object:LruCache<String,Bitmap>(8*1024*1024){
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    } }
    private val blinks by lazy { object:LruCache<String,List<IntArray>>(8*1024*1024){
        override fun sizeOf(key:String,value:List<IntArray>)=value.sumOf{it.size*4}
    } }
    private val expressions by lazy { object:LruCache<String,List<IntArray>>(8*1024*1024){
        override fun sizeOf(key:String,value:List<IntArray>)=value.sumOf{it.size*4}
    } }
    fun stageName(stage:GrowthStage):String=when(GrowthStage.migrate(stage)){
        GrowthStage.BABY -> "young"
        GrowthStage.YOUNG -> "teen"
        GrowthStage.ADULT -> "adult"
        GrowthStage.ULTRA -> "ultra"
        else -> error("Unmapped stage")
    }
    fun prefix(s:Species,g:GrowthStage,sex:Sex)="pixubi/"+s.name.lowercase(Locale.ROOT)+"/"+
        (if(sex==Sex.GIRL)"female" else "male")+"/"+stageName(g)
    fun size(g:GrowthStage)=when(GrowthStage.migrate(g)){
        GrowthStage.BABY -> 128
        GrowthStage.YOUNG -> 147
        GrowthStage.ADULT -> 166
        GrowthStage.ULTRA -> 192
        else -> error("Unmapped stage")
    }
    @Synchronized private fun load(context:Context):Pack? {
        if((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)==0)return null
        if(attempted)return pack
        attempted=true
        try {
            val catalog=context.assets.open(CATALOG).bufferedReader().use{JSONObject(it.readText())}
            require(catalog.getInt("schemaVersion")==2 && catalog.getInt("nativeCell")==NATIVE)
            val list=catalog.getJSONArray("forms")
            require(list.length()==40)
            val forms=mutableMapOf<String,Form>()
            for(i in 0 until list.length()){
                val m=list.getJSONObject(i)
                val p=m.getString("prefix")
                val atlas=m.getString("atlas")
                require(atlas.matches(Regex("imported_v2/pixubi_[a-z_]+[.]png")))
                val dims=m.getJSONArray("size")
                val w=dims.getInt(0); val h=dims.getInt(1)
                val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                context.assets.open(atlas).use{BitmapFactory.decodeStream(it,null,opts)}
                require(opts.outWidth==w && opts.outHeight==h)
                val frameObject=m.getJSONObject("frames")
                val rects=frameObject.keys().asSequence().associateWith{name->
                    val rect=frameObject.getJSONArray(name)
                    IntArray(4){rect.getInt(it)}.also{r->
                        require(r[0]>=0 && r[1]>=0 && r[2]==NATIVE && r[3]==NATIVE &&
                            r[0]+r[2]<=w && r[1]+r[3]<=h)
                    }
                }
                require("neutral" in rects)
                val missing=m.getJSONArray("missing").let{ar->List(ar.length()){ar.getString(it)}}
                require(forms.put(p,Form(p,rects.keys,missing,m.optBoolean("mirrorWalkLeft"),atlas,rects))==null)
            }
            for(s in Species.entries)for(g in GrowthStage.playable)for(sex in Sex.entries)
                require(prefix(s,g,sex) in forms)
            pack=Pack(forms)
        }catch(e:FileNotFoundException){
            Log.i("PIXUBI","Imported art not installed; using original poses")
        }catch(e:Exception){
            Log.e("PIXUBI","Imported art rejected; using original poses",e)
        }
        return pack
    }
    fun form(context:Context,s:Species,g:GrowthStage,sex:Sex)=load(context)?.forms?.get(prefix(s,g,sex))
    fun movement(form:Form,clip:SpriteClip)=when(clip){
        SpriteClip.WALK -> "walk_a" in form.frames && "walk_b" in form.frames
        SpriteClip.HOP -> "jump" in form.frames
        else -> false
    }
    fun select(form:Form,clip:SpriteClip,emotion:PetEmotion,frame:Int):String?{
        val desired=when(clip){
            SpriteClip.WALK -> if(movement(form,clip))if(frame.mod(2)==0)"walk_a" else "walk_b" else null
            SpriteClip.HOP -> if(movement(form,clip))if(frame.mod(4) in 1..2)"jump" else "neutral" else null
            SpriteClip.SLEEP -> "sleep"
            SpriteClip.EAT,SpriteClip.DRINK,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY -> null
            else -> when(emotion){
                PetEmotion.HAPPY -> "happy"
                PetEmotion.SAD -> "sad"
                PetEmotion.SURPRISED -> "surprised"
                PetEmotion.TIRED -> "tired"
                PetEmotion.IDLE -> "neutral"
                else -> null
            }
        }
        return desired?.takeIf{it in form.frames}
    }
    @Synchronized fun bitmap(context:Context,key:PixuFrameKey,form:Form):Bitmap{
        // Authored poses take priority; missing expressions use the same portrait.
        val authored=select(form,key.clip,key.emotion,key.frame)
        val expression=ImportedFaceGeometry.fallback(key.clip,key.emotion,authored)
        val name=authored?:"neutral"
        val r=form.rects.getValue(name)
        val atlas=atlases.get(form.atlas) ?: (context.assets.open(form.atlas).use{BitmapFactory.decodeStream(it)}?:error("Invalid atlas")).also{atlases.put(form.atlas,it)}
        val original=Bitmap.createBitmap(atlas,r[0],r[1],r[2],r[3])
        val raw=if(expression!=null){
            val id=form.prefix+":"+expression.name
            val frames=expressions.get(id) ?: run {
                val px=IntArray(NATIVE*NATIVE)
                original.getPixels(px,0,NATIVE,0,0,NATIVE,NATIVE)
                ImportedFaceGeometry.frames(px,key.species,key.stage,key.sex,expression)
                    .also{expressions.put(id,it)}
            }
            val closure=if(key.clip==SpriteClip.SLEEP)4 else key.closure
            Bitmap.createBitmap(frames[ImportedFaceGeometry.frameIndex(expression,closure,key.frame)],
                NATIVE,NATIVE,Bitmap.Config.ARGB_8888)
        }else if(ImportedEyeGeometry.applies(key)){
            val frames=blinks.get(form.prefix) ?: run {
                val px=IntArray(NATIVE*NATIVE)
                original.getPixels(px,0,NATIVE,0,0,NATIVE,NATIVE)
                EyePixelAnimator.frames(px,NATIVE,NATIVE,ImportedEyeGeometry.regions(key.species,key.stage,key.sex))
                    .also{blinks.put(form.prefix,it)}
            }
            Bitmap.createBitmap(frames[key.closure.coerceIn(0,4)],NATIVE,NATIVE,Bitmap.Config.ARGB_8888)
        }else original
        val colored=PixuPalette.apply(raw,key.species,Sex.BOY,key.gene)
        val result=Bitmap.createBitmap(PixuFrameLibrary.CELL,PixuFrameLibrary.CELL,Bitmap.Config.ARGB_8888)
        val size=size(key.stage)
        val x=(PixuFrameLibrary.CELL-size)/2
        val y=174-116*size/NATIVE
        Canvas(result).drawBitmap(colored,null,Rect(x,y,x+size,y+size),Paint().apply{isFilterBitmap=false})
        return result
    }
}
