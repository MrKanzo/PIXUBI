package com.pixubi.game

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.Locale
import java.util.zip.ZipInputStream

/** Test-only import. Stable artwork remains available until the new animation set is complete. */
internal object ImportedPixuArt {
    const val ZIP = "PIXUBI_GameAssets_v2.zip"
    const val NATIVE = 128
    data class Form(val prefix:String,val frames:Set<String>,val missing:List<String>,val mirrorWalk:Boolean)
    private data class Pack(val images:Map<String,ByteArray>,val forms:Map<String,Form>)
    private var attempted=false
    private var pack:Pack?=null
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
            val entries=mutableMapOf<String,ByteArray>()
            context.assets.open(ZIP).use{source->
                ZipInputStream(source).use{zip->
                    while(true){
                        val entry=zip.nextEntry?:break
                        val path=entry.name
                        if(!entry.isDirectory && (path.endsWith("/sprite.json") ||
                            path.contains("/frames/") && path.endsWith(".png")) && path.startsWith("pixubi/")){
                            val out=java.io.ByteArrayOutputStream()
                            val buffer=ByteArray(8192)
                            while(true){
                                val n=zip.read(buffer)
                                if(n<0)break
                                require(out.size()+n<=1_048_576){"Oversized art entry"}
                                out.write(buffer,0,n)
                            }
                            require(entries.size<500 && entries.put(path,out.toByteArray())==null){"Invalid archive"}
                        }
                        zip.closeEntry()
                    }
                }
            }
            val forms=mutableMapOf<String,Form>()
            for(s in Species.entries)for(g in GrowthStage.playable)for(sex in Sex.entries){
                val p=prefix(s,g,sex)
                val manifest=JSONObject(entries.getValue("$p/sprite.json").toString(Charsets.UTF_8))
                require(manifest.getInt("schemaVersion")==2)
                require(manifest.getString("id")==s.name.lowercase(Locale.ROOT))
                require(manifest.getString("stage")==stageName(g))
                require(manifest.getString("sex")==(if(sex==Sex.GIRL)"female" else "male"))
                val names=manifest.getJSONObject("frames").keys().asSequence().toSet()
                require("neutral" in names)
                for(name in names){
                    require(name.matches(Regex("[a-z_0-9]+")))
                    val bytes=entries.getValue("$p/frames/$name.png")
                    val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    BitmapFactory.decodeByteArray(bytes,0,bytes.size,opts)
                    require(opts.outWidth==NATIVE && opts.outHeight==NATIVE){"Invalid frame: $p/$name"}
                }
                val missing=manifest.getJSONArray("missing").let{a->List(a.length()){a.getString(it)}}
                forms[p]=Form(p,names,missing,manifest.optBoolean("mirrorWalkLeft"))
            }
            require(forms.size==40)
            pack=Pack(entries,forms)
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
            SpriteClip.HOP -> if(movement(form,clip))"jump" else null
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
    fun bitmap(context:Context,key:PixuFrameKey,form:Form):Bitmap{
        // Unsupported action: a static portrait, not a claimed animation.
        val name=select(form,key.clip,key.emotion,key.frame)?:"neutral"
        val bytes=checkNotNull(load(context)).images.getValue(form.prefix+"/frames/"+name+".png")
        val raw=BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:error("Invalid imported frame")
        val colored=PixuPalette.apply(raw,key.species,Sex.BOY,key.gene)
        val result=Bitmap.createBitmap(PixuFrameLibrary.CELL,PixuFrameLibrary.CELL,Bitmap.Config.ARGB_8888)
        val size=size(key.stage)
        val x=(PixuFrameLibrary.CELL-size)/2
        val y=174-116*size/NATIVE
        Canvas(result).drawBitmap(colored,null,Rect(x,y,x+size,y+size),Paint().apply{isFilterBitmap=false})
        return result
    }
}
