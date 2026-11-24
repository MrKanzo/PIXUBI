package com.pixubi.game

/** Local geometry measured on the new 192px artwork, NOT the legacy 48px crops.
 * All expressions operate on the actual eye/mouth pixels. Tail, fin and wing
 * tips have conservative polygons; the head and torso are not mesh-warped. */
internal object ReferenceOlderFrames {
    const val N=192
    internal data class Face(val eyes:List<EyeRegion>,val mouth:EyeRegion)
    private fun e(x:Int,y:Int,rx:Int,ry:Int)=EyeRegion(x.toFloat(),y.toFloat(),rx.toFloat(),ry.toFloat())
    fun geometry(s:Species,g:GrowthStage):Face=if(g==GrowthStage.ADULT)when(s){
        Species.LISYK->Face(listOf(e(142,75,12,10),e(171,73,6,8)),e(158,90,10,5))
        Species.ISKRYK->Face(listOf(e(144,65,13,11),e(175,63,5,7)),e(167,83,9,5))
        Species.KRAPLYK->Face(listOf(e(144,95,12,11),e(174,91,5,7)),e(161,109,10,5))
        Species.PISHCHANYK->Face(listOf(e(131,80,13,10),e(168,78,7,9)),e(156,94,9,5))
        Species.KHMARYK->Face(listOf(e(140,71,12,11),e(172,69,7,9)),e(163,80,8,9))
    }else when(s){
        Species.LISYK->Face(listOf(e(148,67,12,10),e(174,65,5,7)),e(164,81,10,5))
        Species.ISKRYK->Face(listOf(e(148,60,13,10),e(175,59,5,7)),e(165,76,10,5))
        Species.KRAPLYK->Face(listOf(e(150,80,12,11),e(177,76,4,6)),e(165,94,10,5))
        Species.PISHCHANYK->Face(listOf(e(147,70,12,10),e(175,68,5,8)),e(165,83,9,5))
        Species.KHMARYK->Face(listOf(e(134,71,11,11),e(162,71,6,9)),e(154,81,8,9))
    }
    private fun p(l:Int,t:Int,r:Int,b:Int)=IllustratedMotionFrames.Paw(l,t,r,b)
    private fun t(px:Int,py:Int,vararg pts:Int)=IllustratedMotionFrames.Tip(pts,px,py)
    private fun parts(paws:List<IllustratedMotionFrames.Paw>,vararg tips:IllustratedMotionFrames.Tip)=
        IllustratedMotionFrames.Parts(paws,tips.toList())

    fun anatomy(s:Species,g:GrowthStage):IllustratedMotionFrames.Parts=if(g==GrowthStage.ADULT)when(s){
        Species.LISYK->parts(listOf(p(46,165,72,184),p(101,165,128,184),p(136,165,166,184)),
            t(76,126,7,49,73,49,86,82,79,106,68,139,25,147,7,124))
        Species.ISKRYK->parts(listOf(p(21,161,53,183),p(92,163,122,184),p(134,163,166,184)),
            t(41,108,3,17,72,17,74,42,58,61,46,84,32,107,15,96,6,62))
        Species.KRAPLYK->parts(listOf(p(58,164,83,184),p(104,165,131,184),p(137,165,162,184)),
            t(77,134,3,94,65,95,77,116,67,130,48,143,56,171,45,181,9,174,3,147),
            t(118,89,81,60,114,68,119,84,109,92,87,81))
        Species.PISHCHANYK->parts(listOf(p(57,162,83,184),p(112,162,141,184),p(143,164,168,184)),
            t(65,133,7,92,65,92,71,109,60,151,30,168,7,156))
        Species.KHMARYK->parts(listOf(p(56,163,82,184),p(123,163,145,184),p(152,164,176,184)),
            t(101,109,4,14,73,14,99,46,99,79,105,108,68,117,28,100,7,71))
    }else when(s){
        Species.LISYK->parts(listOf(p(45,163,73,184),p(102,164,133,184),p(135,164,169,184)),
            t(77,129,8,30,70,31,88,59,81,91,69,143,30,156,7,140))
        Species.ISKRYK->parts(listOf(p(19,163,48,184),p(97,165,129,184),p(136,165,166,184)),
            t(40,112,3,13,83,13,78,45,61,69,44,92,33,112,12,103,4,69))
        Species.KRAPLYK->parts(listOf(p(59,163,86,184),p(109,164,137,184),p(143,166,170,184)),
            t(83,135,4,95,54,98,72,114,85,132,61,147,50,179,5,181,3,142),
            t(111,70,39,35,85,34,125,44,118,66,95,68,65,56),
            t(108,99,9,64,67,62,103,83,114,98,83,104,56,94,8,103))
        Species.PISHCHANYK->parts(listOf(p(58,164,85,184),p(119,165,147,184),p(151,165,168,184)),
            t(63,125,8,60,68,59,81,88,66,111,57,142,31,160,7,146))
        Species.KHMARYK->parts(listOf(p(65,164,91,184),p(118,164,143,184),p(149,165,170,184)),
            t(105,116,5,17,79,20,99,48,106,86,109,114,77,128,28,112,5,75))
    }

    fun frame(src:IntArray,s:Species,g:GrowthStage,clip:SpriteClip,index:Int,e:PetEmotion,closure:Int):IntArray{
        require(src.size==N*N)
        val face=geometry(s,g)
        val step=if(e==PetEmotion.SLEEPING||e==PetEmotion.HIBERNATING)4 else closure.coerceIn(0,4)
        val idx=step+if(FaceExpression.animatedMouth(e)&&index%2==1)5 else 0
        val pixels=if(step==0&&e==PetEmotion.IDLE)src else
            EyePixelAnimator.faceFrames(src,N,N,face.eyes,face.mouth,s==Species.KHMARYK,e)[idx]
        return IllustratedMotionFrames.frameWithParts(pixels,N,s,anatomy(s,g),clip,index,older=true)
    }
}
