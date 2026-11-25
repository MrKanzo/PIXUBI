package com.pixubi.game

import kotlin.math.*

/** Original 128px female art: action-specific local inverse sampling only
 * for missing illustrated frames. Existing authored action cells are preserved. */
internal object OriginalFemaleMotion {
    private const val N=128
    data class Profile(val left:Int,val split:Int,val right:Int,val top:Int)
    private val profiles=mapOf(
        Species.LISYK to arrayOf(
            Profile(46,66,89,91),
            Profile(75,91,113,92),
            Profile(77,92,111,90),
            Profile(76,92,113,90),
            Profile(70,88,105,94),
            Profile(74,92,113,91),
            Profile(77,94,115,91),
            Profile(76,92,113,91)
        ),
        Species.ISKRYK to arrayOf(
            Profile(43,65,88,90),
            Profile(77,91,112,92),
            Profile(76,92,112,90),
            Profile(78,94,113,90),
            Profile(72,87,105,92),
            Profile(78,94,113,92),
            Profile(77,94,115,91),
            Profile(76,94,114,91)
        ),
        Species.KRAPLYK to arrayOf(
            Profile(45,64,86,91),
            Profile(74,92,111,91),
            Profile(75,92,111,91),
            Profile(78,95,116,92),
            Profile(72,88,108,91),
            Profile(77,94,116,92),
            Profile(76,94,114,92),
            Profile(76,92,113,91)
        ),
        Species.PISHCHANYK to arrayOf(
            Profile(44,65,89,92),
            Profile(77,92,112,92),
            Profile(76,92,112,92),
            Profile(79,95,115,92),
            Profile(68,87,107,92),
            Profile(78,94,115,92),
            Profile(77,94,114,92),
            Profile(78,94,115,92)
        ),
        Species.KHMARYK to arrayOf(
            Profile(49,68,92,91),
            Profile(75,91,112,91),
            Profile(76,92,111,91),
            Profile(78,94,114,92),
            Profile(70,86,105,92),
            Profile(78,94,114,92),
            Profile(77,94,115,91),
            Profile(76,93,114,91)
        )
    )
    fun profile(s:Species,g:GrowthStage,sex:Sex):Profile{
        val stage=GrowthStage.playable.indexOf(GrowthStage.migrate(g))
        require(stage>=0)
        return profiles.getValue(s)[stage+if(sex==Sex.GIRL)0 else 4]
    }

    private fun smooth(x:Float):Float {val t=x.coerceIn(0f,1f);return t*t*(3-2*t)}
    private fun sample(src:IntArray,x:Float,y:Float):Int{
        val xx=x.roundToInt();val yy=y.roundToInt()
        return if(xx in 0 until N&&yy in 0 until N)src[yy*N+xx] else 0
    }
    private fun wave(frame:Int)=floatArrayOf(0f,-1f,.35f,1f)[frame.mod(4)]
    private fun appendage(src:IntArray,s:Species,g:GrowthStage,f:Int,strength:Float):IntArray{
        var current=src
        for(r in OriginalAppendageMotion.regions(s,g,Sex.GIRL)){
            val out=current.copyOf()
            for(y in (r.top-10).coerceAtLeast(0)..(r.bottom+10).coerceAtMost(N-1))for(x in 0 until N){
                val d=(if(r.right)x-r.root else r.root-x).toFloat()
                if(d<=0)continue
                val wy=smooth((y-r.top+8f)/16f)*smooth((r.bottom+8f-y)/16f)
                val dy=wave(f)*strength*r.amplitude*smooth(d/26f)*wy
                out[y*N+x]=sample(current,x.toFloat(),y-dy)
            }
            current=out
        }
        return current
    }
    private fun legs(src:IntArray,s:Species,g:GrowthStage,f:Int,action:SpriteClip):IntArray {
        val p=profile(s,g,Sex.GIRL)
        val out=src.copyOf()
        for(y in (p.top-6).coerceAtLeast(0) until N)for(x in (p.left-4).coerceAtLeast(0)..(p.right+4).coerceAtMost(N-1)){
            val left=x<p.split
            val lo=if(left)p.left-4 else p.split-3
            val hi=if(left)p.split+3 else p.right+4
            val phase=(f+if(left)0 else 2).mod(4)
            val wx=smooth((x-lo)/5f)*smooth((hi-x)/5f)
            val wy=smooth((y-p.top+5f)/18f)*smooth((127f-y)/8f)
            val lift=when(action){
                SpriteClip.SWIM,SpriteClip.FLY->4f+2f*abs(wave(f))
                SpriteClip.DIG->if(phase%2==1)7f else 0f
                SpriteClip.HOP->if(f in 1..2)5f else 0f
                else->if(phase%2==1)5f else 0f
            }
            val swing=if(action==SpriteClip.WALK||action==SpriteClip.DIG)wave(phase)*3f else 0f
            out[y*N+x]=sample(src,x-swing*wx*wy,y+lift*wx*wy)
        }
        return out
    }
    /** A low rest pose for unillustrated stages, not a claimed curled original. */
    internal fun lower(src:IntArray,amount:Int):IntArray{
        var bottom=-1
        for(i in src.indices)if(src[i] ushr 24>=128)bottom=maxOf(bottom,i/N)
        if(bottom<50)return src
        val hip=bottom-34
        val d=amount.coerceIn(0,19)
        if(d==0)return src
        val out=IntArray(src.size)
        for(y in 0..bottom){
            val sy=if(y<=hip+d)y-d.toFloat() else hip+(y-hip-d)*34f/(34-d)
            if(sy<0)continue
            val yy=sy.roundToInt().coerceIn(0,N-1)
            src.copyInto(out,y*N,yy*N,(yy+1)*N)
        }
        return out
    }
    private fun nod(src:IntArray,s:Species,g:GrowthStage,amount:Float):IntArray{
        if(amount==0f)return src
        val mouth=OriginalFaceGeometry.mouth(s,g,Sex.GIRL)
        val out=src.copyOf()
        val left=(mouth.centerX-31f).coerceAtLeast(0f)
        for(y in 0..100)for(x in left.toInt() until N){
            val w=smooth((x-left)/14f)*smooth(y/8f)*smooth((100f-y)/17f)
            out[y*N+x]=sample(src,x.toFloat(),y-amount*w)
        }
        return out
    }
    fun frame(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,index:Int):IntArray{
        require(src.size==N*N)
        val f=index.mod(4)
        return when(clip){
            SpriteClip.IDLE -> appendage(src,s,g,f,.5f)
            SpriteClip.WALK -> legs(appendage(src,s,g,f,.8f),s,g,f,clip)
            SpriteClip.HOP -> legs(src,s,g,f,clip)
            SpriteClip.DIG -> if(s==Species.LISYK)nod(appendage(src,s,g,f,.6f),s,g,intArrayOf(0,7,12,6)[f].toFloat())
                else legs(lower(src,intArrayOf(2,7,10,5)[f]),s,g,f,clip)
            SpriteClip.SWIM -> legs(appendage(src,s,g,f,2.4f),s,g,f,clip)
            SpriteClip.FLY -> legs(appendage(src,s,g,f,3f),s,g,f,clip)
            SpriteClip.EAT,SpriteClip.DRINK -> nod(src,s,g,intArrayOf(0,3,5,2)[f].toFloat())
            SpriteClip.SLEEP -> lower(src,17)
            else -> src
        }
    }
}
