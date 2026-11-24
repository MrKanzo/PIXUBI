package com.pixubi.game

import kotlin.math.roundToInt

/** Species-specific debug artwork choreography. Expedition rewards and saved
 * timing remain entirely in GameEngine. No new independently drawn poses. */
internal object ImportedSpeciesSearch {
    fun label(s:Species)=when(s){
        Species.LISYK -> "Нюхання і пошук слідів"
        Species.ISKRYK -> "Ривки та стрибки"
        Species.KRAPLYK -> "Плавання і пірнання"
        Species.PISHCHANYK -> "Копання та заривання"
        Species.KHMARYK -> "Зліт, політ і посадка"
    }
    private fun unit(t:Float)=if(t.isFinite())((t%1f)+1f)%1f else 0f
    private fun smooth(t:Float):Float {
        val a=t.coerceIn(0f,1f)
        return a*a*(3f-2f*a)
    }
    // One hop each second of the twelve-second search loop.
    fun dashClock(t:Float)=unit(t*6f)
    private fun dashTimeline(t:Float):Float{
        val a=if(t.isFinite())((t%4f)+4f)%4f else 0f
        val tick=a*12f
        val base=tick.toInt()
        val u=tick-base
        return (base+smooth((u-.25f)/.5f))/12f
    }
    fun flightEnvelope(t:Float):Float{
        val a=unit(t)
        return minOf(smooth(a/.15f),smooth((1f-a)/.15f))
    }
    fun frame(s:Species,t:Float,seed:Int,walk:Boolean):SearchFrame{
        val safe=if(t.isFinite())t else 0f
        val f=PixubiSearch.frame(s,if(s==Species.ISKRYK)dashTimeline(safe) else safe,seed)
        return when(s){
            Species.LISYK -> if(walk)f.copy(pose=f.pose.copy(yDp=7f*f.concealment))
                else ImportedPawMotion.stationarySearch(s,f)
            Species.ISKRYK -> f.copy(pose=f.pose.copy(
                yDp=ImportedMotionTiming.jumpY(dashClock(safe))*(1f-f.concealment)+7f*f.concealment))
            Species.KRAPLYK -> f
            Species.PISHCHANYK -> ImportedPawMotion.stationarySearch(s,f)
            Species.KHMARYK -> f.copy(pose=f.pose.copy(yDp=f.pose.yDp*flightEnvelope(safe)))
        }
    }
    fun direction(s:Species,t:Float,seed:Int,walk:Boolean):Boolean{
        val before=frame(s,t-.003f,seed,walk)
        val after=frame(s,t+.003f,seed,walk)
        val dx=after.pose.xDp-before.pose.xDp
        if(kotlin.math.abs(dx)>.001f)return dx<0f
        return after.entryXDp<0f
    }
    fun sniffApplies(k:PixuFrameKey,pose:String)=
        k.species==Species.LISYK&&k.clip==SpriteClip.DIG&&pose=="neutral"
    /** Small downward head nod, tapered toward the rear and fixed paws. */
    fun sniff(src:IntArray,g:GrowthStage,sex:Sex,phase:Int):IntArray{
        require(src.size==128*128)
        val amount=intArrayOf(0,2,4,2)[phase.mod(4)]
        if(amount==0)return src
        val eye=ImportedEyeGeometry.regions(Species.LISYK,g,sex).first()
        val left=(eye.centerX.toInt()-28).coerceAtLeast(0)
        val top=8
        val bottom=94
        val out=src.copyOf()
        for(y in top until bottom)for(x in left until 128){
            val weight=minOf(1f,(x-left)/12f,(y-top)/8f,(bottom-1-y)/14f)
            val dy=(amount*weight).roundToInt()
            out[y*128+x]=src[(y-dy)*128+x]
        }
        return out
    }
}
