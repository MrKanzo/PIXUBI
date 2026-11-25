package com.pixubi.game

import kotlin.math.*

/**
 * Discrete, full-sized frames assembled from the real artwork. Each native
 * paw is translated as a whole; a tail tip turns around its attachment point.
 * All original pixel colors are retained. The face and torso are never warped.
 */
internal object IllustratedMotionFrames {
    internal data class Paw(val l:Int,val t:Int,val r:Int,val b:Int)
    internal data class Tip(val points:IntArray,val px:Int,val py:Int)
    internal data class Parts(val paws:List<Paw>,val tips:List<Tip>)
    private fun p(l:Int,t:Int,r:Int,b:Int)=Paw(l,t,r,b)
    private fun t(px:Int,py:Int,vararg points:Int)=Tip(points,px,py)
    internal fun parts(s:Species,g:GrowthStage):Parts=when(g) {
        GrowthStage.BABY -> when(s) {
            Species.LISYK -> Parts(listOf(p(80,157,105,185),p(108,157,132,185)),
                listOf(t(70,162,0,111,48,111,67,142,77,162,69,182,0,182)))
            Species.ISKRYK -> Parts(listOf(p(63,160,84,185),p(90,160,114,185)),
                listOf(t(124,164,126,145,141,113,176,110,180,175,142,180,124,171)))
            Species.KRAPLYK -> Parts(listOf(p(65,145,88,170),p(102,145,124,170)),
                listOf(t(124,151,125,131,178,119,188,155,156,169,124,164)))
            Species.PISHCHANYK -> Parts(listOf(p(65,159,86,185),p(89,159,111,185)),
                listOf(t(120,161,123,138,184,124,189,170,154,185,122,175)))
            Species.KHMARYK -> Parts(listOf(p(52,152,76,176),p(89,152,110,176)),
                listOf(t(45,137,5,122,45,123,45,157,4,165),
                    t(119,140,119,123,154,124,166,163,119,158),
                    t(149,158,153,139,191,137,191,179,151,179)))
        }
        GrowthStage.YOUNG -> when(s) {
            Species.LISYK -> Parts(listOf(p(33,162,56,191),p(65,162,89,191),p(100,160,125,190)),
                listOf(t(126,148,126,104,152,60,182,62,188,136,160,162,126,160)))
            Species.ISKRYK -> Parts(listOf(p(37,162,61,191),p(70,161,94,191),p(103,161,128,191)),
                listOf(t(134,146,135,99,151,49,178,46,190,117,171,157,135,161)))
            Species.KRAPLYK -> Parts(listOf(p(39,161,64,190),p(68,161,92,190),p(100,161,124,190)),
                listOf(t(137,148,136,105,155,74,190,75,191,145,166,165,138,162)))
            Species.PISHCHANYK -> Parts(listOf(p(46,162,70,191),p(80,163,104,191),p(112,161,137,191)),
                listOf(t(136,147,136,98,157,76,187,82,191,148,166,166,136,161)))
            Species.KHMARYK -> Parts(listOf(p(57,162,79,191),p(87,163,111,191)),
                listOf(t(40,106,0,85,38,85,45,119,38,149,0,159),
                    t(137,106,136,84,191,82,191,156,159,144,137,122)))
        }
        GrowthStage.ADULT -> when(s) {
            Species.LISYK -> Parts(listOf(p(68,124,91,152),p(94,124,116,152)),
                listOf(t(57,126,0,86,32,80,55,97,62,124,55,148,0,148)))
            Species.ISKRYK -> Parts(listOf(p(70,123,91,150),p(98,123,121,150)),
                listOf(t(60,125,5,83,37,78,61,106,65,143,5,147)))
            Species.KRAPLYK -> Parts(listOf(p(60,122,83,146),p(91,122,113,146)),
                listOf(t(52,120,0,89,37,86,56,105,57,139,0,142)))
            Species.PISHCHANYK -> Parts(listOf(p(55,123,77,148),p(82,123,103,148)),
                listOf(t(50,126,0,98,31,94,53,109,55,143,0,145)))
            Species.KHMARYK -> Parts(listOf(p(56,129,77,152),p(82,129,105,152)),
                listOf(t(43,111,0,92,40,91,47,118,42,139,0,142),
                    t(113,111,113,91,159,92,159,142,115,139)))
        }
        else -> Parts(emptyList(),emptyList())
    }

    private fun inside(a:IntArray,x:Float,y:Float):Boolean {
        var hit=false
        var j=a.size-2
        for(i in a.indices step 2) {
            val yi=a[i+1];val yj=a[j+1]
            if((yi>y)!=(yj>y) && x<(a[j]-a[i])*(y-yi)/(yj-yi)+a[i]) hit=!hit
            j=i
        }
        return hit
    }

    private fun opaque(c:Int)=c ushr 24
    private fun alphaOver(src:Int,dst:Int):Int {
        val a=opaque(src)
        if(a==255 || opaque(dst)==0) return src
        if(a==0) return dst
        val b=opaque(dst)*(255-a)/255
        val out=a+b
        fun c(shift:Int)=(((src ushr shift)and 255)*a+((dst ushr shift)and 255)*b)/out
        return (out shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
    }

    private fun turn(out:IntArray,src:IntArray,n:Int,tip:Tip,angle:Float) {
        if(angle==0f)return
        val a=tip.points
        val minX=(a.indices.filter{it%2==0}.minOf{a[it]}-8).coerceAtLeast(0)
        val maxX=(a.indices.filter{it%2==0}.maxOf{a[it]}+8).coerceAtMost(n-1)
        val minY=(a.indices.filter{it%2==1}.minOf{a[it]}-8).coerceAtLeast(0)
        val maxY=(a.indices.filter{it%2==1}.maxOf{a[it]}+8).coerceAtMost(n-1)
        val cs=cos(angle);val sn=sin(angle)
        // Leave a small stationary root to avoid disconnecting the appendage.
        for(y in minY..maxY)for(x in minX..maxX) {
            if(inside(a,x+.5f,y+.5f) && hypot((x-tip.px).toFloat(),(y-tip.py).toFloat())>9f)
                out[y*n+x]=0
        }
        // Inverse rasterization: every destination pixel has a source; no mesh,
        // no interpolation and no holes from forward pixel splatting.
        for(y in minY..maxY)for(x in minX..maxX) {
            val dx=x-tip.px;val dy=y-tip.py
            val sx=(tip.px+cs*dx+sn*dy).roundToInt()
            val sy=(tip.py-sn*dx+cs*dy).roundToInt()
            if(sx !in 0 until n||sy !in 0 until n||!inside(a,sx+.5f,sy+.5f))continue
            val c=src[sy*n+sx]
            if(opaque(c)>0)out[y*n+x]=alphaOver(c,out[y*n+x])
        }
    }

    private fun lift(out:IntArray,src:IntArray,n:Int,p:Paw,amount:Int) {
        if(amount<=0)return
        val x0=p.l.coerceAtLeast(0);val x1=p.r.coerceAtMost(n)
        val y0=p.t.coerceAtLeast(0);val y1=p.b.coerceAtMost(n)
        // Whole paw moves upward, into the existing leg, rather than stretching
        // the leg or drawing a replacement rectangle.
        for(y in y0 until y1)for(x in x0 until x1)out[y*n+x]=0
        for(y in y0 until y1)for(x in x0 until x1) {
            val c=src[y*n+x]
            if(opaque(c)>0 && y-amount>=0) {
                val i=(y-amount)*n+x
                out[i]=alphaOver(c,out[i])
            }
        }
    }

    fun frame(src:IntArray,n:Int,s:Species,g:GrowthStage,clip:SpriteClip,index:Int):IntArray =
        frameWithParts(src,n,s,parts(s,g),clip,index)

    /** The new atlas supplies its own measured per-character attachment map. */
    fun frameWithParts(src:IntArray,n:Int,s:Species,parts:Parts,clip:SpriteClip,index:Int,
                       older:Boolean=false,enhanced:Boolean=false):IntArray {
        require(n>0 && src.size==n*n)
        val out=src.copyOf()
        if(clip==SpriteClip.SLEEP || clip==SpriteClip.SAD || clip==SpriteClip.TIRED || index==0)return out
        val wag=when(index%4){1->-.075f;2->if(enhanced).04f else 0f;3->.075f;else->0f}
        val strong=clip in listOf(SpriteClip.HAPPY,SpriteClip.SWIM,SpriteClip.FLY)
        for((i,t) in parts.tips.withIndex()) {
            val angle=wag*(if(strong)1.5f else 1f)*(if(older).65f else 1f)*(if(s==Species.KHMARYK&&i%2==1)-1f else 1f)
            turn(out,src,n,t,angle)
        }
        if(clip in listOf(SpriteClip.WALK,SpriteClip.DIG,SpriteClip.HOP) || (!older && clip in listOf(SpriteClip.SWIM,SpriteClip.FLY))) {
            val i=when(index%4){1->0;2->if(enhanced&&parts.paws.size>2)2 else -1;3->1;else->-1}
            if(i>=0 && parts.paws.isNotEmpty()) {
                // Lift alternating intact paws by just a few source pixels.
                lift(out,src,n,parts.paws[i%parts.paws.size],if(clip==SpriteClip.DIG)5 else if(enhanced)5 else 3)
                if(parts.paws.size>2 && i==0)lift(out,src,n,parts.paws[2],2)
            }
        }
        return out
    }
}
