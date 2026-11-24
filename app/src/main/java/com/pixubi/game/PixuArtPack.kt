package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import org.json.JSONObject

/** Complete authored poses only; no partial pack can override the original. */
internal object PixuArtPack {
    private fun key(s:Species,g:GrowthStage,sex:Sex,gene:Int,c:SpriteClip,i:Int)=
        "rig/${s.name.lowercase()}/${g.name.lowercase()}/${sex.name.lowercase()}/${gene.coerceIn(0,5)}/${c.name.lowercase()}/${i.toString().padStart(2,'0')}"
    private fun load(context:Context,s:Species,g:GrowthStage,sex:Sex,gene:Int,c:SpriteClip,i:Int):RigFrame? {
        val root=key(s,g,sex,gene,c,i)
        return try {
            val j=context.assets.open("$root/pose.json").bufferedReader().use{JSONObject(it.readText())}
            val n=j.getInt("size");require(n in 64..512)
            val declared=j.getJSONArray("layers")
            val layers=(0 until declared.length()).map { k ->
                val name=declared.getString(k);val id=PixuLayer.valueOf(name)
                val bmp=context.assets.open("$root/$name.png").use{BitmapFactory.decodeStream(it)}?:error("Missing layer")
                require(bmp.width==n&&bmp.height==n)
                val px=IntArray(n*n);bmp.getPixels(px,0,n,0,0,n,n);RigLayer(id,px)
            }
            require(layers.any{it.id==PixuLayer.BODY}&&layers.any{it.id==PixuLayer.HEAD})
            require(layers.map{it.id}.distinct().size==layers.size)
            val sockets=j.getJSONArray("sockets")
            val anchors=(0 until sockets.length()).map{k->
                val a=sockets.getJSONObject(k)
                RigSocket(EquipmentSlot.valueOf(a.getString("slot")),PixuLayer.valueOf(a.getString("parent")),
                    RigPoint(a.getDouble("x").toFloat(),a.getDouble("y").toFloat(),a.optDouble("angle",0.0).toFloat()))
            }
            require(anchors.map{it.slot}.distinct().size==anchors.size)
            require(anchors.all{it.point.x in 0f..n.toFloat()&&it.point.y in 0f..n.toFloat()&&it.point.angle.isFinite()})
            require(anchors.all{a->layers.any{it.id==a.parent}})
            RigFrame(n,layers,anchors,true)
        }catch(_:Exception){null}
    }
    fun loadClip(context:Context,s:Species,g:GrowthStage,sex:Sex,gene:Int,c:SpriteClip):List<RigFrame>? {
        val frames=(0 until c.count).map{load(context,s,g,sex,gene,c,it)?:return null}
        return frames.takeIf{it.map{f->f.size}.distinct().size==1}
    }
    /** Render an equipped item at its per-pose socket, separately from the body. */
    data class Equipped(val slot:EquipmentSlot,val bitmap:Bitmap,val pivotX:Float,val pivotY:Float)
    fun composite(frame:RigFrame,equipment:List<Equipped> = emptyList()):Bitmap {
        val n=frame.size
        val bitmap=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888)
        bitmap.setPixels(frame.composite(),0,n,0,0,n,n)
        if(equipment.isEmpty())return bitmap
        val c=Canvas(bitmap);val paint=Paint().apply{isFilterBitmap=false}
        for(item in equipment){
            val socket=frame.sockets.firstOrNull{it.slot==item.slot}?:continue
            val save=c.save();c.translate(socket.point.x,socket.point.y);c.rotate(socket.point.angle)
            c.drawBitmap(item.bitmap,-item.pivotX,-item.pivotY,paint);c.restoreToCount(save)
        }
        return bitmap
    }
}
