package com.pixubi.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Soft joints on the ORIGINAL sprite: all positions are atlas pixels. */
internal data class LimbJoint(
    val x: Float,val y: Float,val rx: Float,val ry: Float,
    val pivotX: Float,val pivotY: Float,val kind: LimbKind,val side: Float
)
internal enum class LimbKind { PAW, TAIL, WING, FIN }
internal data class LimbOffset(val dx: Float,val dy: Float)

internal object NativeLimbRig {
    const val GRID = 48

    fun joints(species: Species,stage: GrowthStage): List<LimbJoint> {
        fun paw(x:Int,y:Int,rx:Int,ry:Int,side:Int) =
            LimbJoint(x.toFloat(),y.toFloat(),rx.toFloat(),ry.toFloat(),
                x.toFloat(),(y-ry).toFloat(),LimbKind.PAW,side.toFloat())
        fun part(x:Int,y:Int,rx:Int,ry:Int,px:Int,py:Int,kind:LimbKind,side:Int=1) =
            LimbJoint(x.toFloat(),y.toFloat(),rx.toFloat(),ry.toFloat(),
                px.toFloat(),py.toFloat(),kind,side.toFloat())
        val tail=LimbKind.TAIL; val wing=LimbKind.WING; val fin=LimbKind.FIN
        return when(stage) {
            GrowthStage.BABY -> when(species) {
                Species.LISYK -> listOf(paw(93,168,20,22,1),paw(124,168,19,22,-1),
                    part(34,147,49,44,79,165,tail,-1))
                Species.ISKRYK -> listOf(paw(75,169,17,18,1),paw(102,169,17,18,-1),
                    part(151,147,35,39,119,166,tail))
                Species.KRAPLYK -> listOf(paw(77,151,17,15,1),paw(109,151,17,15,-1),
                    part(146,141,38,30,114,151,fin))
                Species.PISHCHANYK -> listOf(paw(79,168,16,19,1),paw(106,168,17,19,-1),
                    part(148,150,37,37,120,168,tail))
                Species.KHMARYK -> listOf(paw(65,165,16,17,1),paw(98,165,16,17,-1),
                    part(24,142,31,35,52,129,wing,-1),part(139,142,32,35,111,129,wing),
                    part(157,162,30,20,126,156,tail))
            }
            GrowthStage.YOUNG -> when(species) {
                Species.LISYK -> listOf(paw(48,166,17,26,1),paw(82,168,17,25,-1),
                    paw(116,169,15,21,1),part(140,115,41,51,112,149,tail))
                Species.ISKRYK -> listOf(paw(52,169,17,22,1),paw(84,168,16,23,-1),
                    paw(119,169,16,21,1),part(155,108,32,47,122,151,tail))
                Species.KRAPLYK -> listOf(paw(53,165,18,23,1),paw(81,167,16,23,-1),
                    paw(114,168,16,20,1),part(146,113,42,49,114,151,fin))
                Species.PISHCHANYK -> listOf(paw(61,168,17,24,1),paw(92,169,17,23,-1),
                    paw(126,169,17,21,1),part(143,125,39,44,119,151,tail))
                Species.KHMARYK -> listOf(paw(70,168,15,20,1),paw(100,168,15,20,-1),
                    part(31,119,39,52,60,99,wing,-1),part(145,119,44,54,111,98,wing),
                    part(145,160,39,28,110,151,tail))
            }
            GrowthStage.ADULT -> when(species) {
                Species.LISYK -> listOf(paw(76,132,14,19,1),paw(103,132,15,19,-1),
                    part(34,113,40,34,64,130,tail,-1))
                Species.ISKRYK -> listOf(paw(80,131,15,19,1),paw(109,131,15,19,-1),
                    part(39,111,40,35,67,130,tail,-1))
                Species.KRAPLYK -> listOf(paw(72,129,14,17,1),paw(99,129,15,17,-1),
                    part(33,107,36,35,58,128,fin,-1))
                Species.PISHCHANYK -> listOf(paw(65,130,14,18,1),paw(91,130,14,18,-1),
                    part(38,117,27,28,57,130,tail,-1))
                Species.KHMARYK -> listOf(paw(66,134,14,15,1),paw(91,134,14,15,-1),
                    part(29,113,36,29,53,100,wing,-1),part(130,113,33,29,103,100,wing))
            }
            else -> emptyList()
        }
    }

    private fun smooth(t:Float):Float {
        val x=t.coerceIn(0f,1f)
        return x*x*(3f-2f*x)
    }

    fun offset(joints:List<LimbJoint>, x:Float,y:Float, motion:PetMotion,
               phase:Float):LimbOffset {
        if(motion==PetMotion.SLEEP || !phase.isFinite()) return LimbOffset(0f,0f)
        val a=phase*2.0*PI
        val stride=sin(a*3).toFloat()
        val wag=sin(a*2).toFloat()
        var dx=0f;var dy=0f
        for(j in joints) {
            val nx=(x-j.x)/j.rx
            val ny=(y-j.y)/j.ry
            val weight=smooth(1f-sqrt(nx*nx+ny*ny))
            if(weight<=0f) continue
            when(j.kind) {
                LimbKind.PAW -> {
                    val wave=stride*j.side
                    val (sx,sy)=when(motion) {
                        PetMotion.WALK -> (wave*7f) to (-max(0f,wave)*11f)
                        PetMotion.DIG -> (wave*5f) to (-max(0f,wave)*12f)
                        PetMotion.HOP,PetMotion.BOUNCE ->
                            (j.side*abs(wag)*2f) to (-abs(sin(a).toFloat())*9f)
                        PetMotion.SWIM -> (wave*5f) to (-wave*5f)
                        PetMotion.FLOAT -> (wave*2f) to (-abs(wave)*3f)
                        PetMotion.IDLE -> (wave*.9f) to (-max(0f,wave)*1.2f)
                        else -> 0f to 0f
                    }
                    dx+=sx*weight;dy+=sy*weight
                }
                LimbKind.TAIL,LimbKind.FIN,LimbKind.WING -> {
                    val amplitude=when(motion) {
                        PetMotion.WALK -> .24f
                        PetMotion.HOP,PetMotion.BOUNCE -> .35f
                        PetMotion.DIG -> .29f
                        PetMotion.SWIM -> .37f
                        PetMotion.FLOAT -> .34f
                        PetMotion.IDLE -> .16f
                        PetMotion.SLOUCH,PetMotion.TIRED -> .045f
                        else -> .10f
                    }
                    val oscillation=if(j.kind==LimbKind.WING)
                        wag*j.side else wag
                    val theta=amplitude*oscillation
                    // Rotation about the root: the attached end stays attached.
                    val bx=x-j.pivotX;val by=y-j.pivotY
                    val s=sin(theta.toDouble()).toFloat()
                    val c=kotlin.math.cos(theta.toDouble()).toFloat()
                    dx+=((c-1f)*bx-s*by)*weight
                    dy+=(s*bx+(c-1f)*by)*weight
                }
            }
        }
        return LimbOffset(dx,dy)
    }

    /** One continuous mesh, no duplicate paws/tail and no holes from cutouts. */
    fun vertices(width:Int,height:Int,joints:List<LimbJoint>,motion:PetMotion,
                 phase:Float,grid:Int=GRID):FloatArray {
        val result=FloatArray((grid+1)*(grid+1)*2)
        var i=0
        for(row in 0..grid) for(col in 0..grid) {
            val x=width*col/grid.toFloat();val y=height*row/grid.toFloat()
            val d=offset(joints,x,y,motion,phase)
            // Keep the mesh inside its cell and maintain fixed outer boundaries.
            val edge=smooth(minOf(x,y,width-x,height-y)/5f)
            result[i++]=x+d.dx*edge
            result[i++]=y+d.dy*edge
        }
        return result
    }
}
