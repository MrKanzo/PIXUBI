package com.pixubi.game

import kotlin.math.abs
import kotlin.math.roundToInt

/** Discrete native-pixel poses for the five adult and five ultra silhouettes.
 * Motion zones are mapped to the actual reference crops (48 x 48).
 * No mesh stretches the face/torso; appendage roots remain stationary. */
internal object OlderPixuFrames {
    const val N=48
    internal enum class Part { TAIL, FIN, WING, CAPE, PAW }
    internal data class Zone(val part:Part,val x0:Int,val y0:Int,val x1:Int,val y1:Int,val rootX:Int,val rootY:Int)
    private fun z(p:Part,x0:Int,y0:Int,x1:Int,y1:Int,rx:Int,ry:Int)=Zone(p,x0,y0,x1,y1,rx,ry)
    private val T=Part.TAIL;private val F=Part.FIN;private val W=Part.WING;private val C=Part.CAPE;private val P=Part.PAW
    fun zones(s:Species,g:GrowthStage):List<Zone> {
        val ultra=g==GrowthStage.ULTRA
        val appendages=if(!ultra)when(s){
            Species.LISYK->listOf(z(T,29,16,46,40,29,36))
            Species.ISKRYK->listOf(z(T,37,22,47,38,37,35))
            Species.KRAPLYK->listOf(z(F,2,27,17,43,17,36),z(F,2,17,18,25,18,21))
            Species.PISHCHANYK->listOf(z(T,31,19,46,41,31,35),z(C,2,31,13,41,13,33))
            Species.KHMARYK->listOf(z(W,2,15,18,32,18,29),z(W,30,16,46,35,30,29))
        }else when(s){
            Species.LISYK->listOf(z(T,2,21,21,42,21,35))
            Species.ISKRYK->listOf(z(T,3,25,17,39,17,34))
            Species.KRAPLYK->listOf(z(F,3,25,20,43,20,36),z(F,2,12,23,22,23,19))
            Species.PISHCHANYK->listOf(z(T,2,27,18,43,18,36),z(C,2,19,24,29,24,27))
            Species.KHMARYK->listOf(z(W,2,12,19,36,19,29),z(W,30,12,46,35,30,29))
        }
        // Native legs are at the ground line. These ranges deliberately
        // exclude the tail on each species instead of lifting every column.
        val legs=if(!ultra)when(s){
            Species.LISYK->listOf(5..11,14..19,23..29,30..33)
            Species.ISKRYK->listOf(6..11,14..19,27..32,34..38)
            Species.KRAPLYK->listOf(19..24,27..32,34..39,40..44)
            Species.PISHCHANYK->listOf(12..17,19..23,27..31,32..36)
            Species.KHMARYK->listOf(19..24,26..30)
        }else when(s){
            Species.LISYK->listOf(16..20,23..27,32..36,39..43)
            Species.ISKRYK->listOf(14..19,21..25,31..35,38..42)
            Species.KRAPLYK->listOf(15..20,23..28,32..36,39..43)
            Species.PISHCHANYK->listOf(13..19,23..27,32..36,38..43)
            Species.KHMARYK->listOf(18..23,25..30)
        }
        return appendages+legs.map{z(P,it.first,39,it.last,45,(it.first+it.last)/2,39)}
    }

    private fun face(s:Species,g:GrowthStage):Pair<EyeRegion,EyeRegion>{
        val a=if(g==GrowthStage.ADULT)when(s){
            Species.LISYK->intArrayOf(16,22,22,26)
            Species.ISKRYK->intArrayOf(20,22,24,25)
            Species.KRAPLYK->intArrayOf(38,22,41,25)
            Species.PISHCHANYK->intArrayOf(23,23,27,27)
            Species.KHMARYK->intArrayOf(26,23,29,26)
        }else when(s){
            Species.LISYK->intArrayOf(37,22,42,26)
            Species.ISKRYK->intArrayOf(38,22,42,25)
            Species.KRAPLYK->intArrayOf(38,22,42,25)
            Species.PISHCHANYK->intArrayOf(37,22,42,25)
            Species.KHMARYK->intArrayOf(26,23,30,26)
        }
        return EyeRegion(a[0].toFloat(),a[1].toFloat(),1.9f,1.55f) to
            EyeRegion(a[2].toFloat(),a[3].toFloat(),1.65f,1.35f)
    }

    private fun opaque(c:Int)=c ushr 24>=128
    /** Fixed root, discrete tip shift; only pixels in the hand-mapped appendage
     * zone move. Empty pixels never overwrite the stationary body. */
    private fun shiftPart(out:IntArray,src:IntArray,z:Zone,dx:Int,dy:Int){
        if(dx==0&&dy==0)return
        fun weight(x:Int,y:Int):Float {
            val maxDist=maxOf(abs(z.x0-z.rootX),abs(z.x1-z.rootX),abs(z.y0-z.rootY),abs(z.y1-z.rootY),1)
            val dist=maxOf(abs(x-z.rootX),abs(y-z.rootY))
            return ((dist-3).toFloat()/(maxDist-3).coerceAtLeast(1)).coerceIn(0f,1f)
        }
        // Remove only the displaced source pixels. Keep a stable seam.
        for(y in z.y0..z.y1)for(x in z.x0..z.x1){
            val w=weight(x,y)
            if((dx*w).roundToInt()!=0||(dy*w).roundToInt()!=0)out[y*N+x]=0
        }
        for(y in (z.y0-3).coerceAtLeast(0)..(z.y1+3).coerceAtMost(N-1))
            for(x in (z.x0-3).coerceAtLeast(0)..(z.x1+3).coerceAtMost(N-1)){
                // Inverse lookup avoids holes on the tip edge.
                var sx=x;var sy=y
                repeat(3){
                    val w=weight(sx,sy)
                    sx=x-(dx*w).roundToInt();sy=y-(dy*w).roundToInt()
                }
                if(sx !in z.x0..z.x1||sy !in z.y0..z.y1)continue
                val c=src[sy*N+sx]
                if(opaque(c)&&(!opaque(out[y*N+x])||(x in z.x0..z.x1&&y in z.y0..z.y1)))out[y*N+x]=c
            }
    }

    private fun lift(out:IntArray,src:IntArray,z:Zone,amount:Int){
        if(amount<=0)return
        for(x in z.x0..z.x1){
            val bottom=(z.y1 downTo z.y0).firstOrNull{opaque(src[it*N+x])}?:continue
            if(bottom<42||!opaque(src[(bottom-3)*N+x]))continue
            for(y in bottom-2..bottom)out[y*N+x]=0
            for(y in bottom-2..bottom)if(opaque(src[y*N+x]))out[(y-amount)*N+x]=src[y*N+x]
        }
    }
    fun motion(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,frame:Int):IntArray{
        require(src.size==N*N)
        val out=src.copyOf()
        if(clip in setOf(SpriteClip.SLEEP,SpriteClip.SAD,SpriteClip.TIRED))return out
        val wave=when(frame.mod(4)){1->1;3->-1;else->0}
        val pieces=zones(s,g)
        for(z in pieces.filter{it.part!=P}){
            when(z.part){
                Part.TAIL -> shiftPart(out,src,z,wave,if(clip==SpriteClip.HAPPY||clip==SpriteClip.HOP)2*wave else wave)
                Part.FIN -> shiftPart(out,src,z,if(clip==SpriteClip.SWIM)2*wave else wave,wave)
                Part.WING -> shiftPart(out,src,z,0,if(clip==SpriteClip.FLY)3*wave else wave)
                Part.CAPE -> shiftPart(out,src,z,0,if(clip==SpriteClip.WALK||clip==SpriteClip.HOP)2*wave else wave)
                Part.PAW -> Unit
            }
        }
        val paws=pieces.filter{it.part==P}
        if(clip==SpriteClip.WALK||clip==SpriteClip.DIG){
            if(wave!=0)paws.forEachIndexed{i,z->if(i%2==if(wave>0)0 else 1)lift(out,src,z,if(clip==SpriteClip.DIG&&i==0)2 else 1)}
        }else if(clip==SpriteClip.HOP&&frame.mod(4) in 1..2){
            paws.forEach{lift(out,src,it,1)}
        }
        // Swimming and flying move fins and wings, never simulate walking feet.
        return out
    }
    fun frame(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,index:Int,e:PetEmotion,closure:Int):IntArray {
        val (eye,mouth)=face(s,g)
        val step=if(e==PetEmotion.SLEEPING||e==PetEmotion.HIBERNATING)4 else closure.coerceIn(0,4)
        val face=if(step==0&&e==PetEmotion.IDLE)src else
            EyePixelAnimator.faceFrames(src,N,N,listOf(eye),mouth,s==Species.KHMARYK,e)[step+if(FaceExpression.animatedMouth(e)&&index%2==1)5 else 0]
        return motion(face,s,g,clip,index)
    }
}
