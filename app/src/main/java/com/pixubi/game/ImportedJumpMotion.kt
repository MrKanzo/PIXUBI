package com.pixubi.game

import kotlin.math.roundToInt

/** Four constructed jump poses, not independently illustrated art.
 * Use each form's own lower limb regions and keep original jump art preferred.
 * The world trajectory is supplied by ImportedMotionTiming, not this raster. */
internal object ImportedJumpMotion {
    private const val N=128
    fun applies(form:ImportedPixuArt.Form,clip:SpriteClip)=clip==SpriteClip.HOP&&"jump" !in form.frames
    fun frame(src:IntArray,s:Species,g:GrowthStage,sex:Sex,phase:Int):IntArray{
        require(src.size==N*N)
        val out=src.copyOf()
        val step=phase.mod(4)
        val ground=src.indexOfLast{it ushr 24!=0}/N
        if(step==0||step==3){
            // Grounded preparation/landing: lower the upper body and taper to fixed feet.
            val amplitude=if(step==0)3 else 2
            for(y in 0 until ground){
                val dy=(amplitude*minOf(1f,(ground-y)/30f)).roundToInt()
                val sy=(y-dy).coerceAtLeast(0)
                src.copyInto(out,y*N,sy*N,(sy+1)*N)
            }
        }else{
            // Airborne frames tuck both visible paw regions without altering the head.
            val p=ImportedPawMotion.profile(s,g,sex)
            val amplitude=if(step==1)5 else 3
            for(y in p.top until 120)for(x in p.left until p.right){
                val wx=minOf(1f,(x-p.left)/3f,(p.right-1-x)/3f)
                val wy=minOf(1f,(y-p.top)/8f)
                val lift=(amplitude*wx*wy).roundToInt()
                out[y*N+x]=src[(y+lift).coerceAtMost(N-1)*N+x]
            }
        }
        return out
    }
}
