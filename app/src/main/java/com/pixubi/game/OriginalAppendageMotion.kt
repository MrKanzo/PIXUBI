package com.pixubi.game

import kotlin.math.roundToInt

/** Local pixel deformation of the rear appendages, not a translation of the pet.
 * Four discrete idle frames. Authored walking and sleep poses are left intact.
 * Seated models are never passed off as full walking/flight/swimming cycles.
 */
internal object OriginalAppendageMotion {
    private const val N=128
    internal data class Region(val root:Int,val top:Int,val bottom:Int,val amplitude:Int,val right:Boolean=false){
        fun contains(x:Int,y:Int)=y in top until bottom && if(right)x>root else x<root
    }
    private val profiles=mapOf(
        Species.LISYK to arrayOf(
            Region(47,66,112,3),
            Region(52,35,113,4),
            Region(58,36,113,4),
            Region(49,54,113,3),
            Region(53,58,113,3),
            Region(54,40,113,3),
            Region(54,28,113,3),
            Region(49,60,113,3)
        ),
        Species.ISKRYK to arrayOf(
            Region(47,65,112,3),
            Region(54,37,112,4),
            Region(55,30,113,4),
            Region(55,45,113,3),
            Region(56,58,113,4),
            Region(55,45,113,4),
            Region(54,57,113,3),
            Region(54,63,113,3)
        ),
        Species.KRAPLYK to arrayOf(
            Region(46,72,113,3),
            Region(54,44,113,4),
            Region(56,58,113,4),
            Region(57,28,113,4),
            Region(55,62,113,4),
            Region(57,45,113,4),
            Region(58,45,113,4),
            Region(57,43,113,4)
        ),
        Species.PISHCHANYK to arrayOf(
            Region(46,66,113,3),
            Region(56,44,113,4),
            Region(51,60,113,4),
            Region(48,67,113,3),
            Region(52,57,113,4),
            Region(56,44,113,4),
            Region(51,69,113,3),
            Region(49,68,113,3)
        ),
        Species.KHMARYK to arrayOf(
            Region(49,60,94,4),
            Region(66,39,86,5),
            Region(70,23,85,5),
            Region(73,12,83,5),
            Region(74,37,83,5),
            Region(77,18,83,5),
            Region(76,10,84,5),
            Region(76,10,84,5)
        )
    )
    fun regions(s:Species,g:GrowthStage,sex:Sex):List<Region>{
        val stage=GrowthStage.playable.indexOf(GrowthStage.migrate(g))
        require(stage>=0)
        val main=profiles.getValue(s)[stage+if(sex==Sex.GIRL)0 else 4]
        return if(s==Species.KHMARYK && g==GrowthStage.BABY && sex==Sex.GIRL)
            listOf(main,Region(92,65,94,4,true)) else listOf(main)
    }
    fun applies(k:PixuFrameKey)=k.clip==SpriteClip.IDLE && k.emotion==PetEmotion.IDLE
    fun frame(src:IntArray,s:Species,g:GrowthStage,sex:Sex,phase:Int):IntArray{
        require(src.size==N*N)
        val sign=when(phase.mod(4)){1->-1;3->1;else->0}
        if(sign==0)return src
        var current=src
        for(region in regions(s,g,sex)){
            val out=current.copyOf()
            for(y in region.top until region.bottom){
                val wy=((y-region.top)/7f).coerceIn(0f,1f)*((region.bottom-1-y)/7f).coerceIn(0f,1f)
                for(x in 0 until N){
                    if(!region.contains(x,y))continue
                    val d=if(region.right)x-region.root else region.root-x
                    val shift=(sign*region.amplitude*(d/24f).coerceIn(0f,1f)*wy).roundToInt()
                    val sy=(y-shift).coerceIn(0,N-1)
                    out[y*N+x]=current[sy*N+x]
                }
            }
            current=out
        }
        return current
    }
}
