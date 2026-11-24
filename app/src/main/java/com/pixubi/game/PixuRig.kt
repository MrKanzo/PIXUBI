package com.pixubi.game

import kotlin.math.*

internal enum class PixuLayer { TAIL, WING_LEFT, WING_RIGHT, BODY, BACK_LEFT, BACK_RIGHT,
    FRONT_LEFT, FRONT_RIGHT, HEAD, EAR_LEFT, EAR_RIGHT, EYE_LEFT, EYE_RIGHT, MOUTH }
internal enum class EquipmentSlot { HEAD, EARS, EYES, NECK, BODY, BACK, FRONT_LEFT,
    FRONT_RIGHT, BACK_LEFT, BACK_RIGHT, TAIL, WING_LEFT, WING_RIGHT }
internal data class RigPoint(val x:Float,val y:Float,val angle:Float=0f)
internal data class RigSocket(val slot:EquipmentSlot,val parent:PixuLayer,val point:RigPoint)
internal data class RigLayer(val id:PixuLayer,val pixels:IntArray)
internal data class RigFrame(val size:Int,val layers:List<RigLayer>,val sockets:List<RigSocket>,
    val authored:Boolean=false) {
    fun composite():IntArray {
        val out=IntArray(size*size)
        for(layer in layers) for(i in out.indices) {
            val c=layer.pixels[i]
            if(c ushr 24 !=0) out[i]=over(c,out[i])
            else if(layer.id==PixuLayer.BODY && out[i] ushr 24==0)out[i]=c
        }
        return out
    }
    companion object {
        fun over(src:Int,dst:Int):Int {
            val a=src ushr 24
            if(a==0)return dst
            val b=(dst ushr 24)*(255-a)/255
            if(b==0)return src
            val sum=a+b
            fun c(shift:Int)=(((src ushr shift)and 255)*a+((dst ushr shift)and 255)*b)/sum
            return (sum shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
        }
    }
}

/** Lossless visible-pixel partition. Hidden surfaces require authored pose art. */
internal object PixuRig {
    private fun contains(poly:IntArray,x:Float,y:Float):Boolean {
        var inside=false;var j=poly.size-2
        for(i in poly.indices step 2) {
            val yi=poly[i+1];val yj=poly[j+1]
            if((yi>y)!=(yj>y) && x<(poly[j]-poly[i])*(y-yi)/(yj-yi)+poly[i])inside=!inside
            j=i
        }
        return inside
    }
    private fun eye(e:EyeRegion,x:Int,y:Int):Boolean =
        abs((x-e.centerX)/e.radiusX).pow(2.8f)+abs((y-e.centerY)/e.radiusY).pow(2.8f)<1f
    private val pawIds=listOf(PixuLayer.FRONT_LEFT,PixuLayer.FRONT_RIGHT,PixuLayer.BACK_RIGHT,PixuLayer.BACK_LEFT)
    private val footSlots=listOf(EquipmentSlot.FRONT_LEFT,EquipmentSlot.FRONT_RIGHT,EquipmentSlot.BACK_RIGHT,EquipmentSlot.BACK_LEFT)
    private fun tipIds(s:Species)=if(s==Species.KHMARYK)listOf(PixuLayer.WING_LEFT,PixuLayer.WING_RIGHT,PixuLayer.TAIL)else listOf(PixuLayer.TAIL)
    fun partition(pixels:IntArray,n:Int,s:Species,g:GrowthStage):RigFrame {
        require(pixels.size==n*n)
        val parts=IllustratedMotionFrames.parts(s,g)
        val eyes=NativeEyeGeometry.regions(s,g)
        val mouth=NativeMouthGeometry.region(s,g)
        val data=PixuLayer.entries.associateWith { IntArray(n*n) }
        val cx=eyes.map{it.centerX}.average().toFloat()
        val cy=eyes.map{it.centerY}.average().toFloat()
        val neck=mouth.centerY+n*.07f
        for(y in 0 until n)for(x in 0 until n) {
            val i=y*n+x;val c=pixels[i]
            if(c ushr 24==0){data.getValue(PixuLayer.BODY)[i]=c;continue}
            val paw=parts.paws.indexOfFirst { x in it.l until it.r && y in it.t until it.b }
            val tip=parts.tips.indexOfFirst { contains(it.points,x+.5f,y+.5f) }
            val id=when {
                eye(eyes[0],x,y)->PixuLayer.EYE_LEFT
                eye(eyes[1],x,y)->PixuLayer.EYE_RIGHT
                eye(mouth,x,y)->PixuLayer.MOUTH
                paw>=0->pawIds[paw.coerceAtMost(3)]
                tip>=0 && y>neck-15->tipIds(s)[tip.coerceAtMost(tipIds(s).lastIndex)]
                y<cy-n*.20f && x<cx-n*.10f->PixuLayer.EAR_LEFT
                y<cy-n*.20f && x>cx+n*.10f->PixuLayer.EAR_RIGHT
                y<neck->PixuLayer.HEAD
                else->PixuLayer.BODY
            }
            data.getValue(id)[i]=c
        }
        return RigFrame(n,PixuLayer.entries.map{RigLayer(it,data.getValue(it))},sockets(n,s,g))
    }
    fun sockets(n:Int,s:Species,g:GrowthStage):List<RigSocket> {
        val eyes=NativeEyeGeometry.regions(s,g)
        val mouth=NativeMouthGeometry.region(s,g)
        val parts=IllustratedMotionFrames.parts(s,g)
        val cx=eyes.map{it.centerX}.average().toFloat()
        val cy=eyes.map{it.centerY}.average().toFloat()
        val neck=(mouth.centerY+n*.085f).coerceAtMost(n*.8f)
        fun at(slot:EquipmentSlot,parent:PixuLayer,x:Float,y:Float)=
            RigSocket(slot,parent,RigPoint(x.coerceIn(0f,n-1f),y.coerceIn(0f,n-1f)))
        val out=mutableListOf(at(EquipmentSlot.HEAD,PixuLayer.HEAD,cx,cy-n*.29f),
            at(EquipmentSlot.EARS,PixuLayer.HEAD,cx,cy-n*.20f),at(EquipmentSlot.EYES,PixuLayer.HEAD,cx,cy),
            at(EquipmentSlot.NECK,PixuLayer.BODY,cx,neck),at(EquipmentSlot.BODY,PixuLayer.BODY,cx,neck+n*.10f),
            at(EquipmentSlot.BACK,PixuLayer.BODY,cx+n*.12f,neck+n*.11f))
        for((i,p) in parts.paws.withIndex())out.add(at(footSlots[i],pawIds[i],(p.l+p.r)/2f,p.b-4f))
        for((i,t) in parts.tips.withIndex()) {
            val layer=tipIds(s)[i.coerceAtMost(tipIds(s).lastIndex)]
            val slot=EquipmentSlot.valueOf(layer.name)
            if(out.none{it.slot==slot})out.add(at(slot,layer,t.px.toFloat(),t.py.toFloat()))
        }
        return out
    }
}
