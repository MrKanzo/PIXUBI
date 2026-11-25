package com.pixubi.game

import kotlin.math.*

/** Native nearest-neighbour joint motion. Each joint uses a continuous
 * displacement field: no transparent cut-out rectangles or pasted limbs.
 * Original full-body illustrated packs never enter this path. */
internal object NativePoseMotion {
    private const val N=192
    private fun smooth(v:Float):Float { val t=v.coerceIn(0f,1f);return t*t*(3f-2f*t) }
    private fun sample(src:IntArray,x:Float,y:Float):Int {
        val xx=x.roundToInt();val yy=y.roundToInt()
        return if(xx in 0 until N&&yy in 0 until N)src[yy*N+xx] else 0
    }
    private fun distance(x:Float,y:Float,ax:Float,ay:Float,bx:Float,by:Float):Float {
        val dx=bx-ax;val dy=by-ay
        val t=(( (x-ax)*dx+(y-ay)*dy )/(dx*dx+dy*dy).coerceAtLeast(.001f)).coerceIn(0f,1f)
        return hypot(x-ax-t*dx,y-ay-t*dy)
    }
    private fun weight(points:IntArray,x:Float,y:Float):Float {
        var inside=false;var d=Float.MAX_VALUE;var j=points.size-2
        for(i in points.indices step 2){
            val ax=points[j].toFloat();val ay=points[j+1].toFloat()
            val bx=points[i].toFloat();val by=points[i+1].toFloat()
            if((ay>y)!=(by>y)&&x<(bx-ax)*(y-ay)/(by-ay)+ax)inside=!inside
            d=minOf(d,distance(x,y,ax,ay,bx,by));j=i
        }
        return smooth((if(inside)d+6f else 6f-d)/12f)
    }
    private fun tip(src:IntArray,t:IllustratedMotionFrames.Tip,angle:Float):IntArray {
        if(angle==0f)return src
        val a=t.points
        val pad=28
        val l=(a.indices.filter{it%2==0}.minOf{a[it]}-pad).coerceAtLeast(0)
        val r=(a.indices.filter{it%2==0}.maxOf{a[it]}+pad).coerceAtMost(N-1)
        val top=(a.indices.filter{it%2==1}.minOf{a[it]}-pad).coerceAtLeast(0)
        val bottom=(a.indices.filter{it%2==1}.maxOf{a[it]}+pad).coerceAtMost(N-1)
        val out=src.copyOf()
        for(y in top..bottom)for(x in l..r){
            val dx=(x-t.px).toFloat();val dy=(y-t.py).toFloat()
            val a0=angle*weight(a,x.toFloat(),y.toFloat())*smooth(hypot(dx,dy)/30f)
            if(abs(a0)<.002f)continue
            val cs=cos(a0);val sn=sin(a0)
            out[y*N+x]=sample(src,t.px+cs*dx+sn*dy,t.py-sn*dx+cs*dy)
        }
        return out
    }
    private fun paw(src:IntArray,p:IllustratedMotionFrames.Paw,lift:Float,swing:Float):IntArray{
        val out=src.copyOf()
        val center=(p.l+p.r)*.5f
        val radius=(p.r-p.l)*.5f+6f
        for(y in (p.t-8).coerceAtLeast(0)..(p.b+10).coerceAtMost(N-1))
            for(x in (p.l-9).coerceAtLeast(0)..(p.r+9).coerceAtMost(N-1)){
                val side=smooth(1f-abs(x-center)/radius)
                val lower=smooth((y-p.t+6f)/(p.b-p.t+4f))
                val edge=smooth((p.b+10f-y)/10f)
                val w=side*lower*edge
                out[y*N+x]=sample(src,x-swing*w,y+lift*w)
            }
        return out
    }
    /** Lower the upper body while keeping the baseline and preserving the
     * head's proportions. A derived crouch, not an illustrated curled pose. */
    fun crouch(src:IntArray,drop:Int):IntArray {
        if(drop<=0)return src
        var bottom=-1
        for(i in src.indices)if(src[i] ushr 24>=128)bottom=maxOf(bottom,i/N)
        if(bottom<60)return src
        val hip=bottom-45
        val d=drop.coerceIn(0,30)
        val out=IntArray(src.size)
        for(y in 0..bottom){
            val sy=if(y<=hip+d)y-d.toFloat() else
                hip+(y-hip-d)*45f/(45-d)
            if(sy<0f)continue
            val yy=sy.roundToInt().coerceIn(0,N-1)
            src.copyInto(out,y*N,yy*N,(yy+1)*N)
        }
        return out
    }
    fun frame(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,index:Int):IntArray{
        require(src.size==N*N)
        val older=g==GrowthStage.ADULT||g==GrowthStage.ULTRA
        val parts=if(older)ReferenceOlderFrames.anatomy(s,g) else IllustratedMotionFrames.parts(s,g)
        val f=index.mod(4)
        if(clip==SpriteClip.SLEEP)return crouch(src,if(f%2==0)22 else 24)
        if(clip==SpriteClip.SAD||clip==SpriteClip.TIRED)return crouch(src,if(clip==SpriteClip.TIRED)7 else 5)
        var out=src
        val wave=floatArrayOf(0f,-1f,.35f,1f)[f]
        val strength=when(clip){
            SpriteClip.FLY->.28f
            SpriteClip.SWIM->.20f
            SpriteClip.HAPPY->.12f
            SpriteClip.WALK,SpriteClip.DIG,SpriteClip.HOP->.075f
            SpriteClip.IDLE->.035f
            else->0f
        }
        for((i,t) in parts.tips.withIndex()){
            val sign=if(s==Species.KHMARYK&&i%2==1)-1f else 1f
            // Axolotl gills move less than the main swimming tail.
            val factor=if(s==Species.KRAPLYK&&i>0).3f else 1f
            out=tip(out,t,wave*strength*sign*factor)
        }
        if(clip==SpriteClip.WALK||clip==SpriteClip.DIG||clip==SpriteClip.HOP){
            for((i,p) in parts.paws.withIndex()){
                val phase=(f+i*2)%4
                val lift=when(clip){
                    SpriteClip.HOP -> if(f in 1..2)8f else 0f
                    SpriteClip.DIG -> if(phase==1&&i>=1)12f else if(phase==1)4f else 0f
                    else -> if(phase==1)8f else 0f
                }
                val swing=if(phase==1)4f else if(phase==3)-4f else 0f
                if(lift!=0f||swing!=0f)out=paw(out,p,lift,swing)
            }
        }
        return when(clip){
            SpriteClip.DIG->crouch(out,intArrayOf(3,12,17,8)[f])
            SpriteClip.SWIM->crouch(out,intArrayOf(7,12,8,4)[f])
            SpriteClip.FLY->crouch(out,intArrayOf(6,12,8,10)[f])
            SpriteClip.EAT,SpriteClip.DRINK->crouch(out,if(f%2==0)2 else 5)
            else->out
        }
    }
}
