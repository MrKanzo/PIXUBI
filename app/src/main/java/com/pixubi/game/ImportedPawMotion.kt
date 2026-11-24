package com.pixubi.game

import kotlin.math.roundToInt

/** Local paw-lift candidate poses. These do not turn a sitting portrait into
 * an authored quadruped gait. No translation is enabled for the paw preview. */
internal object ImportedPawMotion {
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
    fun frame(src:IntArray,s:Species,g:GrowthStage,sex:Sex,phase:Int):IntArray{
        require(src.size==N*N)
        val p=profile(s,g,sex)
        val step=phase.mod(4)
        if(step%2==0)return src
        val lo=if(step==1)p.left else p.split
        val hi=if(step==1)p.split else p.right
        val out=src.copyOf()
        for(y in p.top until 120)for(x in lo until hi){
            val wx=minOf(1f,(x-lo)/3f,(hi-1-x)/3f)
            val wy=minOf(1f,(y-p.top)/8f)
            val lift=(4f*wx*wy).roundToInt()
            out[y*N+x]=src[(y+lift).coerceAtMost(N-1)*N+x]
        }
        return out
    }
    fun localPaws(s:Species,clip:SpriteClip,authored:String?)=
        authored==null&&(clip==SpriteClip.WALK || (clip==SpriteClip.DIG && s==Species.PISHCHANYK))
    fun appendageAction(s:Species,clip:SpriteClip)=
        (s==Species.KRAPLYK&&clip==SpriteClip.SWIM)||(s==Species.KHMARYK&&clip==SpriteClip.FLY)
    fun supportsPreviewTravel(s:Species,clip:SpriteClip)=appendageAction(s,clip)
    /** Stationary search must not disappear at a hiding place it never reached. */
    fun stationarySearch(s:Species,frame:SearchFrame):SearchFrame {
        val y=if(s==Species.PISHCHANYK)67f*frame.concealment else 0f
        val conceal=if(s==Species.PISHCHANYK)frame.concealment else 0f
        val phase=if(s==Species.PISHCHANYK)frame.phase else SearchPhase.WANDERING
        return frame.copy(phase=phase,pose=frame.pose.copy(xDp=0f,yDp=y),
            alpha=1f-conceal,concealment=conceal,entryXDp=0f,exitXDp=0f,coverXDp=0f)
    }
}
