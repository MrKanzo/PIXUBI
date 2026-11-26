package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

// Each evolution has its own silhouette and armor package, sharing only a joint rig.
internal data class KageForm(val height:Float,val head:Float,val shoulders:Float,val torso:Float,
    val thigh:Float,val shin:Float,val upperArm:Float,val forearm:Float,val scarf:Float,val grade:Int)
internal object KageForms {
    fun of(tier:RobotTier)=when(tier) {
        RobotTier.MK1->KageForm(.76f,1.05f,25f,.92f,22f,26f,18f,19f,29f,0)
        RobotTier.MK2->KageForm(.84f,1.00f,29f,.92f,31f,34f,23f,26f,49f,1)
        RobotTier.MK3->KageForm(.93f,.96f,33f,1f,38f,43f,27f,30f,66f,2)
        RobotTier.MK4->KageForm(1f,.87f,38f,1.10f,47f,53f,32f,35f,87f,3)
        RobotTier.OMEGA->KageForm(1.05f,.88f,43f,1.16f,49f,55f,35f,38f,99f,4)
    }
}
private val outline=Color(0xFF171B25)
private val graphite=Color(0xFF292D39)
private val carbon=Color(0xFF424653)
private val silver=Color(0xFFACB2C0)
private val steel=Color(0xFF707988)
private val cloth=Color(0xFF47465B)
private val black=Color(0xFF10151D)
private fun DrawScope.shape(d:String,fill:Color,edge:Float=1.35f) {
    val p=PathParser().parsePathString(d).toPath()
    drawPath(p,fill)
    if(edge>0f)drawPath(p,outline,style=Stroke(edge,join=StrokeJoin.Round))
}
private fun DrawScope.metal(d:String,light:Color=silver,dark:Color=graphite) {
    val p=PathParser().parsePathString(d).toPath()
    val b=p.getBounds()
    drawPath(p,Brush.linearGradient(listOf(light,steel,dark),Offset(b.left,b.top),Offset(b.right,b.bottom)))
    drawPath(p,outline,style=Stroke(1.35f,join=StrokeJoin.Round))
}
private fun DrawScope.line(x:Float,y:Float,u:Float,v:Float,c:Color,w:Float=1.4f)=drawLine(c,Offset(x,y),Offset(u,v),w,StrokeCap.Round)
private fun DrawScope.joint(x:Float,y:Float,r:Float,a:Color) {
    drawCircle(outline,r+1.2f,Offset(x,y));drawCircle(graphite,r,Offset(x,y))
    drawCircle(steel,r*.64f,Offset(x,y));drawCircle(carbon,r*.45f,Offset(x,y))
    drawCircle(a,r*.19f,Offset(x,y))
}
private fun DrawScope.led(x:Float,y:Float,u:Float,v:Float,a:Color,w:Float=2.6f) {
    line(x,y,u,v,a.copy(alpha=.22f),w+3f);line(x,y,u,v,a,w);line(x,y,u,v,Color.White.copy(alpha=.65f),.7f)
}
private fun DrawScope.kageCore(a:Color,g:Int,boost:Boolean) {
    val r=if(g==4)12f else if(g==0)7f else 9f
    val path=Path().apply {
        for(i in 0..5) {
            val t=(i*60f-90f)*PI.toFloat()/180f
            val x=r*cos(t);val y=r*sin(t)
            if(i==0)moveTo(x,y) else lineTo(x,y)
        };close()
    }
    drawCircle(a.copy(alpha=.17f),r+5f,Offset.Zero)
    drawPath(path,graphite);drawPath(path,a,style=Stroke(2.8f))
    scale(.67f,.67f,Offset.Zero){drawPath(path,a);drawPath(path,Color.White,style=Stroke(if(boost)2.4f else 1f))}
}
private fun DrawScope.kageMask(side:Boolean,f:RobotFace,blink:Boolean,a:Color) {
    val eyes=if(side) listOf(18f) else listOf(-13f,13f)
    for(x in eyes) {
        val sign=if(side)1f else if(x<0)-1f else 1f
        when {
            blink||f==RobotFace.SLEEP->line(x-5f,-3f,x+5f,-3f,a,2f)
            f==RobotFace.HAPPY->{val p=Path().apply{moveTo(x-5f,-1f);quadraticTo(x,-9f,x+5f,-1f)};drawPath(p,a,style=Stroke(2.5f,cap=StrokeCap.Round))}
            f==RobotFace.SURPRISE->drawOval(a,Offset(x-3.5f,-8f),Size(7f,10f),style=Stroke(2f))
            else->{
                val inner=when(f){RobotFace.SAD->-8f;RobotFace.ANGRY->-2f;else->-5f}
                val outer=when(f){RobotFace.SAD->-2f;RobotFace.ANGRY->-10f;else->-6f}
                val p=Path().apply{moveTo(x-6f*sign,inner);lineTo(x+6f*sign,outer);quadraticTo(x+4f*sign,3f,x-6f*sign,inner);close()}
                drawPath(p,a.copy(alpha=.25f),style=Stroke(3f));drawPath(p,a)
                line(x-2f,-2f,x+2f,-2f,Color.White.copy(alpha=.7f),.8f)
            }
        }
    }
}
private fun DrawScope.omegaHood(side:Boolean,a:Color,f:RobotFace,blink:Boolean) {
    val silhouette=if(side)
        "M -36 -17 C -36 -36 -16 -46 5 -46 C 22 -45 34 -32 40 -18 L 34 -12 Q 31 9 18 21 L 4 27 L -18 21 Q -32 8 -36 -17 Z"
    else "M 0 -49 C 17 -48 29 -34 38 -10 L 41 2 Q 27 21 0 25 Q -27 21 -41 2 L -38 -10 C -29 -34 -17 -48 0 -49 Z"
    val hood=PathParser().parsePathString(silhouette).toPath()
    drawPath(hood,Brush.linearGradient(listOf(Color(0xFF656173),Color(0xFF454357),Color(0xFF242633)),Offset(-28f,-44f),Offset(29f,24f)))
    drawPath(hood,outline,style=Stroke(1.6f,join=StrokeJoin.Round))
    if(side){
        // Continuous cloth contour over an inset, single-eye profile.
        shape("M 13 -30 Q 28 -24 36 -15 L 32 -6 L 29 8 L 18 21 L 7 13 L 3 -6 Z",black)
        metal("M 13 -24 Q 25 -20 31 -14 L 28 -8 L 16 -10 L 9 -15 Z",carbon,black)
        shape("M 12 0 L 29 -5 L 29 6 L 21 17 L 17 20 L 9 10 Z",graphite)
        metal("M 24 2 L 30 -4 L 28 9 L 19 19 L 18 12 Z",steel,graphite)
        // The eye follows the sloping visor rather than extending beyond its edge.
        translate(3f,-1f){scale(.86f,1f,Offset(18f,-3f)){kageMask(true,f,blink,a)}}
        val rim=PathParser().parsePathString("M -5 -43 Q 22 -46 40 -18 L 34 -12 Q 23 -25 13 -30 L 6 -31 Z").toPath()
        drawPath(rim,Brush.linearGradient(listOf(Color(0xFF6A6579),Color(0xFF3D3B4D)),Offset(2f,-43f),Offset(34f,-12f)))
        drawPath(rim,outline,style=Stroke(1f))
        led(24f,-28f,31f,-21f,a,2.5f)
        // Soft drape around the cheek; no hard diagonal seam across the hood.
        val fold=PathParser().parsePathString("M -35 -17 Q -22 -10 -12 6 Q -4 19 17 22 L 4 27 L -18 21 Q -32 8 -35 -17 Z").toPath()
        drawPath(fold,Brush.linearGradient(listOf(Color(0xFF454558),Color(0xFF2B2D3C)),Offset(-30f,-14f),Offset(7f,25f)))
        drawPath(fold,outline,style=Stroke(.8f))
        line(-26f,-29f,-7f,-39f,Color(0xFF777284),.9f)
        line(-20f,-4f,-8f,14f,Color(0xFF57596B),.9f)
        shape("M -18 -23 L -14 -25 L -10 -22 L -6 -24 L -4 -20 L -8 -16 L -14 -15 L -18 -19 Z",steel,.45f)
        drawCircle(a,1.2f,Offset(-11f,-20f))
    }else{
        shape("M 0 -28 Q 23 -20 32 -1 Q 25 17 0 22 Q -25 17 -32 -1 Q -23 -20 0 -28 Z",black)
        metal("M -22 -8 L -12 -19 L 0 -25 L 12 -19 L 22 -8 L 0 3 Z",carbon,black)
        shape("M -23 1 L -12 5 L 0 10 L 12 5 L 23 1 L 15 15 L 0 22 L -15 15 Z",graphite)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            metal("M 23 1 L 13 6 L 8 16 L 15 14 Z",steel,graphite)
            led(21f,-22f,30f,-11f,a,3.8f)
            line(13f,-39f,28f,-17f,steel,.8f)
            led(30f,2f,26f,8f,a,1.3f)
        }
        kageMask(false,f,blink,a)
        line(0f,-47f,0f,-30f,steel,.9f)
    }
}
private fun DrawScope.hood(side:Boolean,g:Int,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?) {
    if(g==4) omegaHood(side,a,f,blink)
    else if(g==0) {
        if(side) {
            metal("M -36 -9 Q -38 -41 -10 -45 Q 20 -47 35 -25 Q 40 -7 29 16 Q -10 35 -31 16 Z",cloth,black)
            shape("M 8 -23 Q 34 -26 35 -7 Q 34 14 12 17 L 6 5 Z",black)
            drawCircle(steel,11f,Offset(-10f,-2f));drawCircle(graphite,8f,Offset(-10f,-2f))
            drawCircle(a,4f,Offset(-10f,-2f))
        } else {
            metal("M -39 -8 Q -40 -45 0 -45 Q 40 -45 39 -8 Q 38 26 0 26 Q -38 26 -39 -8 Z",cloth,black)
            shape("M -29 -9 Q -29 -29 0 -29 Q 29 -29 29 -9 L 26 12 Q 0 22 -26 12 Z",black)
            for(s in listOf(-1f,1f))led(s*28f,-31f,s*34f,-20f,a,2f)
        }
        for(x in if(side)listOf(18f)else listOf(-13f,13f)){
            if(blink||f==RobotFace.SLEEP)line(x-5f,-3f,x+5f,-3f,a,2f)
            else if(f==RobotFace.HAPPY||f==RobotFace.SAD||f==RobotFace.ANGRY) {
                val sign=if(side||x>0f)1f else -1f
                val eye=Path().apply{
                    if(f==RobotFace.HAPPY){moveTo(x-5f,0f);quadraticTo(x,-9f,x+5f,0f)}
                    else {moveTo(x-5f*sign,if(f==RobotFace.SAD)-7f else -1f);lineTo(x+5f*sign,if(f==RobotFace.SAD)0f else -8f)}
                }
                drawPath(eye,a,style=Stroke(3f,cap=StrokeCap.Round))
            }else{
                drawRoundRect(a,Offset(x-5f,-11f),Size(10f,if(f==RobotFace.SURPRISE)17f else 13f),CornerRadius(5f))
                drawCircle(Color.White,1.5f,Offset(x-2f,-7f))
            }
        }
        if(f!=RobotFace.SLEEP){val mx=if(side)18f else 0f;line(mx-3f,10f,mx+3f,10f,a,1.5f)}
    } else if(g==1) {
        if(side){
            metal("M -34 -11 L -29 -33 L -9 -44 L 13 -40 L 37 -19 L 29 11 L 10 23 L -23 19 Z",cloth,black)
            shape("M 5 -20 L 34 -17 L 28 2 L 8 6 Z",black)
            metal("M 6 7 L 28 0 L 21 15 L 9 22 Z",steel,black)
            metal("M -20 -15 L -7 -17 L -2 -5 L -8 5 L -21 2 Z",silver,graphite)
        }else{
            metal("M 0 -45 L 20 -38 L 40 -8 L 34 15 L 14 25 L -14 25 L -34 15 L -40 -8 L -20 -38 Z",cloth,black)
            shape("M -32 -16 L 32 -16 L 29 5 L 0 11 L -29 5 Z",black)
            metal("M -29 5 L 0 14 L 29 5 L 19 20 L 0 25 L -19 20 Z",carbon,black)
            led(-28f,-20f,-18f,-20f,a,2.7f);led(18f,-20f,28f,-20f,a,2.7f)
        }
        if(blink||f!=RobotFace.NEUTRAL)kageMask(side,f,blink,a)
        else for(x in if(side)listOf(18f)else listOf(-13f,13f))drawRoundRect(a,Offset(x-7f,-7f),Size(14f,6f),CornerRadius(2f))
    } else if(side) {
        metal(KageShells.of(g).hoodSide,cloth,black)
        shape("M 9 -27 L 34 -13 L 26 9 L 12 16 L 3 1 Z",black)
        metal("M 7 0 L 29 -2 L 25 11 L 12 18 L 4 9 Z",carbon,black)
        kageMask(true,f,blink,a)
        line(-23f,-26f,1f,-39f,steel,.8f);line(-27f,-3f,-10f,15f,carbon)
        led(10f,-25f,23f,-17f,a,3f)
        if(g>=2){shape("M -17 -20 l 4 -4 5 3 4 -3 4 5 -4 6 -7 2 -5 -5 Z",steel,.4f);drawRect(a,Offset(-11f,-19f),Size(2f,2f))}
    } else {
        metal(KageShells.of(g).hoodFront,cloth,black)
        shape("M 0 -28 Q 22 -19 31 -1 Q 24 17 0 21 Q -24 17 -31 -1 Q -22 -19 0 -28 Z",black)
        metal("M -21 -7 L -12 -19 L 0 -24 L 12 -19 L 21 -7 L 0 3 Z",carbon,black)
        shape("M -23 0 L -13 4 L 0 9 L 13 4 L 23 0 L 15 15 L 0 21 L -15 15 Z",graphite)
        metal("M -23 0 L -13 5 L -8 15 L -15 13 Z",steel,graphite)
        metal("M 23 0 L 13 5 L 8 15 L 15 13 Z",steel,graphite)
        kageMask(false,f,blink,a)
        line(0f,-43f,0f,-29f,steel,.9f)
        for(s in listOf(-1f,1f)){
            line(s*14f,-37f,s*31f,-10f,steel,.8f)
            led(s*19f,-20f,s*28f,-8f,a,3.7f)
            line(s*35f,2f,s*24f,15f,carbon,1f)
        }
    }
    when(gear) {
        RobotGear.SCANNER->{shape("M -9 -47 L 12 -47 L 17 -40 L -12 -40 Z",graphite);led(-5f,-44f,9f,-44f,a)}
        RobotGear.HELMET->{metal("M -24 -34 Q 0 -54 24 -34 L 17 -30 Q 0 -44 -17 -30 Z",steel,graphite)}
        else->Unit
    }
}
private fun DrawScope.scarfTail(form:KageForm,side:Boolean,a:Color,phase:Float,action:RobotAction,trail:Float) {
    val swing=sin(phase*2f*PI.toFloat())*(if(action==RobotAction.SLEEP)0f else 6f+trail*8f)
    val n=if(form.grade>=4)2 else 1
    for(i in 0 until n) {
        val length=form.scarf-i*13f
        val root=Offset(if(side)-12f else -22f,-9f+i*5f)
        translate(root.x,root.y) {
            rotate(-12f-trail*35f+swing+i*17f,Offset.Zero) {
                scale(length/80f,length/80f,Offset.Zero) {
                    if(form.grade<2){
                        metal(if(form.grade==0)"M 5 -8 Q -38 -17 -72 6 L -58 30 L -78 39 Q -31 28 5 6 Z" else "M 4 -5 Q -43 -16 -79 5 L -67 17 L -82 29 Q -40 12 4 6 Z",cloth,black)
                        led(-50f,4f,-69f,10f,a,2.5f)
                    }else{
                        metal("M 4 -6 C -20 -9 -47 -7 -65 15 C -83 35 -52 51 -71 77 Q -52 66 -54 49 C -55 30 -45 17 3 5 Z",cloth,black)
                        shape("M -59 14 L -74 31 L -61 26 L -45 12 Z",a,.4f)
                        line(-11f,-2f,-49f,12f,steel,1f)
                    }
                }
            }
        }
    }
}
private fun DrawScope.sword(a:Color,drawn:Boolean) {
    shape("M -3 -19 L 3 -19 L 3 0 L -3 0 Z",graphite)
    for(i in 0..3)line(-1.5f,-16f+i*4f,1.5f,-16f+i*4f,a,1.6f)
    metal("M -9 -1 L 9 -1 L 8 3 L -8 3 Z",steel,graphite)
    metal("M -3 3 L 5 3 Q 8 38 -4 66 L -6 57 Z",if(drawn)a else graphite,if(drawn)a else black)
    if(drawn)led(1f,6f,-3f,59f,a,2f)
}
private fun DrawScope.omegaProfileBody(a:Color,e:RobotEquipment) {
    // Profile armor is drawn from scratch, not compressed front-facing plates.
    joint(-2f,-12f,8f,a)
    shape("M -19 -10 Q -1 -15 15 -9 L 26 5 L 23 21 L 10 33 L 9 45 L 17 56 L 12 69 L -16 70 L -22 59 L -12 44 L -15 30 L -23 11 Z",graphite)
    metal("M -18 -9 L 5 -10 L 22 0 L 27 10 L 17 25 L 4 31 L -8 25 L -21 11 Z",carbon,black)
    metal("M 12 -5 L 24 3 L 26 11 L 19 20 L 14 15 Z",silver,graphite)
    led(23f,4f,24f,14f,a,2.7f)
    metal("M -21 3 L -13 1 L -10 24 L -16 28 L -22 16 Z",steel,graphite)
    for(y in listOf(31f,37f,43f)){
        shape("M -12 ${y-2} L 8 ${y-3} L 11 ${y+3} L -10 ${y+4} Z",carbon)
        line(-8f,y,5f,y-1f,steel,.7f)
    }
    metal("M -19 49 L 17 48 L 19 57 L -20 58 Z",steel,graphite)
    metal("M 10 48 L 19 48 L 21 57 L 11 57 Z",silver,graphite)
    metal("M -15 58 L 4 55 L 14 65 L 5 77 L -13 72 L -21 64 Z",carbon,black)
    metal("M -16 56 L -4 55 L 3 69 L -9 81 L -20 69 Z",steel,graphite)
    led(-14f,62f,-10f,70f,a,1.8f)
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR)
        metal("M 6 -8 L 24 -2 L 30 10 L 20 27 L 12 29 L 9 17 Z",silver,graphite)
    if(e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)led(25f,3f,26f,15f,a,4f)
    shape("M -23 -15 Q -3 -7 17 -13 L 24 -7 L 11 2 L -14 -1 L -24 -7 Z",cloth)
    line(-18f,-10f,10f,-3f,steel,1f)
}
private fun DrawScope.torso(form:KageForm,side:Boolean,a:Color,e:RobotEquipment) {
    val g=form.grade
    if(side&&g==4){omegaProfileBody(a,e);return}
    if(g<2){
        scale(if(side).78f else form.torso,1f,Offset.Zero){
            if(g==0){
                metal("M -24 -10 Q 0 -18 24 -10 Q 33 4 29 28 Q 25 43 0 46 Q -25 43 -29 28 Q -33 4 -24 -10 Z",silver,graphite)
                shape("M -21 5 Q 0 -5 21 5 L 20 27 Q 0 38 -20 27 Z",carbon)
                val cx=if(side)12f else 0f
                drawCircle(outline,12f,Offset(cx,16f));drawCircle(a,9f,Offset(cx,16f),style=Stroke(3f))
                drawCircle(a,5f,Offset(cx,16f));drawCircle(Color.White,if(e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)4f else 2.5f,Offset(cx,16f))
                metal("M -19 43 Q 0 39 19 43 L 18 55 L -18 55 Z",carbon,black)
                metal("M -22 54 Q 0 48 22 54 L 17 70 Q 0 76 -17 70 Z",silver,graphite)
            }else{
                shape(if(side)KageShells.of(g).torsoSide else KageShells.of(g).torsoFront,graphite)
                metal("M -24 -8 L 19 -8 L 27 5 L 17 22 L 0 30 L -23 19 L -29 5 Z",carbon,black)
                metal("M -24 -8 L -6 -10 L 18 20 L 8 29 L -22 6 Z",silver,graphite)
                translate(10f,8f){kageCore(a,1,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}
                led(-19f,7f,-8f,18f,a,2f)
                for(y in listOf(34f,43f))shape("M -13 ${y-3} L 13 ${y-3} L 10 ${y+5} L -10 ${y+5} Z",carbon)
                metal("M -23 49 L 23 49 L 23 58 L -23 58 Z",steel,graphite)
                metal("M -25 55 L -13 55 L -10 70 L -29 76 Z",carbon,black)
                metal("M 15 55 L 26 57 L 28 74 L 15 70 Z",steel,graphite)
            }
            if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR){
                metal("M -29 -5 L -23 -12 L -17 -4 L -17 29 L -28 25 Z",silver,graphite)
                if(!side)metal("M 29 -5 L 23 -12 L 17 -4 L 17 29 L 28 25 Z",silver,graphite)
            }
        }
        shape("M -25 -13 Q 0 -7 25 -13 L 24 -3 Q 0 8 -24 -3 Z",cloth)
        return
    }
    scale(if(side).70f else form.torso,1f,Offset.Zero) {
        shape(if(side)KageShells.of(g).torsoSide else KageShells.of(g).torsoFront,graphite)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            if(g==0)metal("M 4 -7 Q 22 -13 25 6 L 22 24 Q 13 34 4 26 Z",silver,graphite)
            else if(g==1)metal("M 4 -7 L 22 -8 L 27 9 L 20 24 L 6 28 Z",carbon,black)
            else metal("M 4 -7 L 23 -8 L 29 8 L 20 23 L 8 31 L 3 18 Z",carbon,black)
            if(g>=2)metal("M 14 19 L 23 12 L 21 24 L 11 31 Z",silver,graphite)
            if(g>=2){line(9f,-3f,22f,0f,silver,.8f);line(16f,25f,12f,30f,a,1.5f)}
            if(g>=1) {
                val tail=if(g>=3)28 else 19
                metal("M 11 53 L 22 50 L 32 ${50+tail} L 21 ${55+tail} L 12 69 Z",silver,graphite)
                shape("M 15 56 L 20 55 L 25 66 L 20 68 Z",graphite)
                if(g>=3){line(21f,58f,26f,73f,steel);drawCircle(outline,1.2f,Offset(21f,61f))}
            }
        }
        for(y in if(g==0)listOf(38f) else if(g==1)listOf(35f,44f) else listOf(31f,38f,45f)){
            shape("M -13 ${y-3} L 0 $y L 13 ${y-3} L 10 ${y+4} L 0 ${y+6} L -10 ${y+4} Z",carbon)
        }
        metal("M -22 48 L 22 48 L 22 57 L -22 57 Z",steel,graphite)
        metal("M -7 48 L 7 48 L 8 57 L -8 57 Z",silver,steel)
        if(g==0)shape("M -12 59 L 12 59 L 9 69 L -9 69 Z",carbon)
        else if(g==1)shape("M -8 59 L 8 59 L 5 75 L -5 75 Z",cloth)
        else shape("M -9 59 L 9 59 L 6 81 L 0 88 L -6 81 Z",cloth)
        if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR){
            metal("M -25 -7 L -12 -9 L -9 18 L -22 27 L -30 11 Z",silver,graphite)
            metal("M 25 -7 L 12 -9 L 9 18 L 22 27 L 30 11 Z",silver,graphite)
        }
        if(!side)translate(0f,9f){kageCore(a,g,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}
        else led(27f,1f,24f,15f,a)
        if(g>=4){
            for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){metal("M 18 -9 L 31 -18 L 36 -6 L 25 7 Z",steel,black);led(25f,-9f,31f,-12f,a)}
        }
    }
    shape("M -26 -14 Q 0 -6 26 -14 L 27 -6 L 0 7 L -27 -6 Z",cloth)
    shape("M -23 -10 L 0 1 L 23 -10 L 18 0 L 0 10 L -18 0 Z",carbon)
    line(-17f,-6f,0f,2f,steel,1f)
}
private fun DrawScope.pivotDot(enabled:Boolean) { if(enabled){drawCircle(Color(0xFFFFB64D),3.1f,Offset.Zero,style=Stroke(1.5f));drawCircle(Color.White,1.1f,Offset.Zero)} }
private fun DrawScope.arm(form:KageForm,x:Float,y:Float,angle:Float,far:Boolean,a:Color,gear:RobotGear?,drawSword:Boolean,elbow:Float,wrist:Float,debug:Boolean,profile:Boolean=false) {
    val g=form.grade
    val u=form.upperArm;val v=form.forearm
    translate(x,y){rotate(angle,Offset.Zero){
        joint(0f,0f,6.4f,a)
        scale(1f,u/30f,Offset.Zero){
            metal("M -7 2 L 7 2 L 8 14 L 4 29 L -4 29 L -8 13 Z",carbon,black)
            if(profile&&g==4){
                metal("M -10 -8 Q 1 -14 11 -6 L 13 4 L 8 13 L -8 11 L -13 2 Z",if(far)steel else silver,graphite)
                shape("M -8 6 L 8 7 L 5 12 L -7 11 Z",graphite)
                led(-6f,8f,6f,8f,a,2f)
                drawCircle(carbon,2.2f,Offset(2f,-3f))
            }else{
                metal(KageShells.of(g).shoulder,if(g==1&&x>0f)carbon else if(far)steel else silver,graphite)
                shape("M -10 7 L 10 7 L 8 12 L -8 12 Z",graphite)
                led(-8f,7f,7f,7f,a,1.7f)
                if(g>=3){shape("M -10 -5 L -5 -8 L 3 -7 L 1 -4 Z",steel);drawCircle(outline,1.4f,Offset(7f,0f))}
            }
        }
        pivotDot(debug)
        translate(0f,u){rotate(elbow,Offset.Zero){
            joint(0f,0f,5.2f,a)
            scale(1f,v/33f,Offset.Zero){
                if(profile&&g==4){
                    metal("M -7 0 L 5 0 L 11 9 L 10 22 L 5 33 L -7 33 L -12 19 L -11 8 Z",if(far)steel else silver,graphite)
                    shape("M -6 5 L 3 4 L 6 12 L 1 26 L -5 25 L -8 13 Z",carbon)
                    led(3f,8f,2f,21f,a,2f);led(-6f,27f,3f,27f,a,2.3f)
                }else{
                    metal(KageShells.of(g).forearm,if(far)steel else silver,graphite)
                    shape("M -4 4 L 4 3 L 7 13 L 4 26 L -4 26 L -7 13 Z",carbon)
                    led(-5f,26f,5f,26f,a,2.2f)
                    if(g>=2)led(3f,6f,5f,11f,a,1.7f)
                }
            }
            pivotDot(debug)
            translate(0f,v){rotate(wrist,Offset.Zero){
                joint(0f,0f,4.3f,a)
                shape("M -6 1 L 5 1 L 7 7 L 4 14 L -5 13 L -8 7 Z",graphite)
                for(i in 0..2)metal("M ${-4+i*4} 4 l 3 0 0 5 -3 0 Z",steel,carbon)
                when(gear){
                    RobotGear.SHIELD_L,RobotGear.SHIELD_R->{translate(0f,-14f){metal("M -15 -13 L 15 -13 L 17 8 L 0 20 L -17 8 Z",steel,graphite);kageCore(a,2,false)}}
                    RobotGear.TOOL_L,RobotGear.TOOL_R->{shape("M -5 8 L 5 8 L 5 23 L -5 23 Z",steel);shape("M -9 20 L 9 20 L 9 25 L -9 25 Z",a)}
                    RobotGear.BLASTER_L,RobotGear.BLASTER_R->{metal("M -9 -14 L 9 -14 L 9 19 L -9 19 Z",steel,graphite);led(-5f,16f,5f,16f,a)}
                    RobotGear.BLADE_L,RobotGear.BLADE_R,RobotGear.CANNON_L,RobotGear.CANNON_R,RobotGear.RIFLE_L,RobotGear.RIFLE_R,RobotGear.ARC_L,RobotGear.ARC_R,RobotGear.REPAIR_L,RobotGear.REPAIR_R->drawWorldWeapon(gear,a)
                    else->if(drawSword)rotate(-90f,Offset(0f,6f)){translate(0f,7f){sword(a,true)}}
                }
                pivotDot(debug)
            }}
        }}
    }}
}
private fun rotatePoint(x:Float,y:Float,angle:Float):Offset {
    val r=angle*PI.toFloat()/180f
    return Offset(x*cos(r)-y*sin(r),x*sin(r)+y*cos(r))
}
private fun DrawScope.leg(form:KageForm,x:Float,y:Float,thigh:Float,shin:Float,side:Boolean,far:Boolean,a:Color,boots:RobotGear?,plant:Boolean,ankle:Float,debug:Boolean) {
    val u=form.thigh;val v=form.shin;val g=form.grade
    val knee=rotatePoint(0f,u,thigh)+Offset(x,y)
    translate(x,y){rotate(thigh,Offset.Zero){scale(1f,u/42f,Offset.Zero){
        joint(0f,0f,7f,a)
        if(side&&g==4){
            metal("M -11 0 L 9 0 L 13 11 L 9 24 L 6 41 L -6 42 L -12 26 L -14 12 Z",carbon,black)
            shape("M -10 6 L -3 3 L 2 17 L -2 31 L -7 35 L -11 21 Z",graphite)
            metal("M 5 7 L 10 10 L 9 24 L 4 30 L 2 25 Z",steel,graphite)
        }else{
            metal(KageShells.of(g).thigh,carbon,black)
            shape("M -9 6 L -2 3 L 3 17 L 0 32 L -5 35 L -10 21 Z",graphite)
            if(g>=2)metal("M 5 8 L 10 11 L 8 25 L 4 31 L 1 26 Z",steel,graphite)
        }
    };pivotDot(debug)}}
    translate(knee.x,knee.y){rotate(shin,Offset.Zero){
        joint(0f,0f,6.7f,a)
        scale(1f,v/47f,Offset.Zero){
            if(side&&g==4){
                metal("M -11 8 L -2 8 L 9 14 L 12 24 L 6 36 L 5 47 L -7 47 L -9 34 L -13 20 Z",if(far)graphite else carbon,black)
                metal("M 0 -7 L 9 -6 L 15 1 L 10 13 L 3 17 L -2 10 Z",if(far)steel else silver,graphite)
                drawCircle(if(far)carbon else steel,4.7f,Offset(-3f,2f));drawCircle(graphite,3.1f,Offset(-3f,2f))
                val shinLight=if(far)a.copy(alpha=.45f) else a
                led(7f,16f,4f,27f,shinLight,2.7f);led(4f,27f,1f,38f,shinLight,2.5f)
            }else{
            metal(KageShells.of(g).knee,if(far)steel else silver,graphite)
            metal(KageShells.of(g).shin,carbon,black)
            if(g==0){
                drawRoundRect(silver,Offset(-7f,19f),Size(14f,22f),CornerRadius(5f));led(-5f,32f,5f,32f,a,3f)
            }else if(g==1){
                shape("M -6 18 L 6 18 L 5 38 L -5 38 Z",carbon);led(0f,20f,0f,35f,a,2.5f)
            }else{
                shape("M -8 19 L 0 25 L 8 19 L 4 34 L 0 43 L -4 34 Z",graphite)
                led(-7f,19f,0f,25f,a,2.5f);led(7f,19f,0f,25f,a,2.5f);led(0f,25f,0f,36f,a,2.5f)
            }
            }
        }
        pivotDot(debug)
        translate(0f,v){rotate(if(plant)-shin else ankle,Offset.Zero){
            joint(0f,0f,5.2f,a)
            val toe=if(side)20 else 12
            metal(if(side)"M -8 -4 L 8 -4 L $toe 8 L $toe 13 L -11 13 L -12 8 Z" else "M -8 -4 L 8 -4 L 12 8 L 10 13 L -10 13 L -12 8 Z",if(side&&g==4&&far)carbon else steel,graphite)
            metal("M -7 -5 L 5 -5 L 9 3 L 5 6 L -6 3 Z",if(side&&g==4&&far)steel else silver,graphite)
            led(-7f,10f,toe-2f,10f,if(side&&g==4&&far)a.copy(alpha=.45f) else a,2f)
            if(boots==RobotGear.BOOTS||boots==RobotGear.SPEED_BOOTS){
                metal(if(side)"M -12 -2 L 9 -2 L ${toe+3} 8 L ${toe+3} 14 L -14 14 Z" else "M -11 -2 L 11 -2 L 14 8 L 12 14 L -12 14 L -14 8 Z",steel,black)
                led(-9f,11f,toe.toFloat(),11f,a,if(boots==RobotGear.SPEED_BOOTS)3f else 1.8f)
            }
            pivotDot(debug)
        }}
    }}
}
private fun DrawScope.backPack(a:Color,gear:RobotGear?) {
    if(gear==RobotGear.BATTERY||gear==RobotGear.BOOSTER){
        metal("M -18 -7 L 18 -7 L 21 28 L -21 28 Z",steel,graphite)
        led(-11f,0f,-11f,19f,a);led(11f,0f,11f,19f,a)
        if(gear==RobotGear.BOOSTER){shape("M -18 26 L -5 26 L -8 33 L -15 33 Z",a);shape("M 5 26 L 18 26 L 15 33 L 8 33 Z",a)}
    }
}
private fun DrawScope.omegaWing(a:Color,pose:OmegaWingPose,side:Boolean,far:Boolean) {
    // Six independent articulated feather blades, with the longest feathers below.
    val lengths=listOf(116f,105f,94f,83f,72f,62f)
    val glow=a.copy(alpha=pose.pulse)
    for(i in 0..5){
        val len=lengths[i]
        translate(i*2.2f,i*(-1.6f)){
            rotate(OmegaWingMotion.featherAngle(i,pose.spread,side),Offset.Zero){
                scale(1f,len/100f,Offset.Zero){
                    shape("M -9 5 L 0 -6 L 13 9 L 15 34 L 10 62 L 1 100 L -8 72 L -12 27 Z",black)
                    metal("M -7 4 L 1 -5 L 12 9 L 12 33 L 4 67 L -6 50 L -9 22 Z",if(far)steel else silver,graphite)
                    shape("M 2 31 L 11 21 L 9 60 L 1 100 L -5 74 Z",glow,.8f)
                    shape("M -5 17 L 4 9 L 7 29 L 0 54 L -5 43 Z",carbon)
                    led(4f,40f,1f,83f,glow,1.8f)
                    line(-6f,5f,-4f,23f,steel,.8f)
                }
            }
        }
    }
    metal("M -16 -6 L -6 -17 L 12 -17 L 23 -3 L 16 16 L -3 18 L -16 9 Z",if(far)steel else silver,black)
    drawCircle(black,12f,Offset(0f,0f));drawCircle(steel,9.5f,Offset.Zero)
    drawCircle(glow,7f,Offset.Zero,style=Stroke(2.4f))
    drawCircle(graphite,4.6f,Offset.Zero);drawCircle(glow,2.5f,Offset.Zero)
}
private fun DrawScope.omegaWings(a:Color,side:Boolean,action:RobotAction,phase:Float) {
    val p=OmegaWingMotion.pose(action,phase)
    if(side){
        // Profile: far wing is mostly occluded; both anchors are behind the torso.
        translate(-24f,-5f){scale(-.57f,.84f,Offset.Zero){omegaWing(a.copy(alpha=.65f),p,side=true,far=true)}}
        translate(-34f,2f){
            metal("M 2 -9 L 24 -6 L 28 7 L 3 14 L -8 7 Z",steel,graphite)
            scale(-.82f,1f,Offset.Zero){omegaWing(a,p,side=true,far=false)}
        }
    }else{
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(31f,-4f){
                metal("M -20 -3 L -2 -12 L 13 -4 L 8 12 L -17 12 Z",steel,graphite)
                omegaWing(a,p,side=false,far=false)
            }
        }
    }
}
@Composable
internal fun KageSeriesPreview(requestedView:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,showSockets:Boolean,modifier:Modifier=Modifier) {
    val form=KageForms.of(loadout.tier)
    val view=KageGait.view(requestedView,action)
    val gait=if(KageGait.isLocomotion(action))KageGait.pose(form,action,phase) else null
    val pose=RobotMotion.pose(action,phase)
    val dash=if(action==RobotAction.NINJA_DASH)KageDashMotion.pose(phase) else null
    val bends=KageJointMotion.of(action,phase)
    val side=view!=RobotView.FRONT
    val a=loadout.accent;val e=loadout.equipment
    Canvas(modifier){
        val wide=if(form.grade==4) { if(dash!=null)420f else if(side)275f else 338f } else if(dash!=null)340f else 230f
        val fit=min(size.width/wide,size.height/325f)
        val dir=if(view==RobotView.LEFT)-1f else 1f
        val travel=if(side&&dash!=null)(dash.travel-44f)*.9f*dir else 0f
        val ground=size.height*.90f
        val fscale=fit*form.height*(if(!side&&dash!=null)1f+dash.travel/550f else 1f)
        val g=form.grade
        drawOval(outline.copy(alpha=.22f),Offset(size.width/2+travel*fit-36f*fit,ground-2f*fit),Size(72f*fit,7f*fit))
        translate(size.width/2+travel*fit,ground){
            scale(fscale*dir,fscale,Offset.Zero){
                val nearT=gait?.near?.thigh?:dash?.nearThigh?:pose.nearLeg
                val farT=gait?.far?.thigh?:dash?.farThigh?:pose.farLeg
                val nearS=gait?.near?.shin?:nearT+bends.nearKnee
                val farS=gait?.far?.shin?:farT+bends.farKnee
                val nt=if(side)nearT else nearT*.18f
                val ft=if(side)farT else farT*.18f
                val ns=if(side)nearS else nearS*.18f
                val fs=if(side)farS else farS*.18f
                val hips=64f
                val base=hips+form.thigh+form.shin+13f
                fun extent(t:Float,s:Float)=form.thigh*cos(t*PI.toFloat()/180f)+form.shin*cos(s*PI.toFloat()/180f)+13f
                val drop=gait?.drop?:if(dash!=null)(form.thigh+form.shin+13f-max(extent(nt,ns),extent(ft,fs))).coerceAtLeast(0f) else 0f
                val lean=if(side)gait?.lean?:dash?.lean?:WorldMotion.idleLean(loadout.series,action,phase,side) else if(dash!=null)-dash.lean*.2f else WorldMotion.idleLean(loadout.series,action,phase,side)
                translate(0f,-base+drop+if(gait==null)pose.lift else 0f){
                    if(dash!=null&&dash.trail>0f&&side)for(i in 0..4)led(-110f-i*4f,30f+i*20f,-65f-i*4f,30f+i*20f,a.copy(alpha=dash.trail*.6f),1.5f)
                    val boots=e.equipped(RobotSlot.FEET)
                    val nearX=if(side&&g==4)1f else if(side)5f else 16f
                    val farX=if(side&&g==4)1f else if(side)-5f else -16f
                    val nearA=gait?.nearArm?:dash?.nearArm?:pose.nearArm
                    val farA=gait?.farArm?:dash?.farArm?:pose.farArm
                    val swordReady=action==RobotAction.ATTACK&&g>=1&&e.equipped(RobotSlot.LEFT_HAND)==null
                    fun DrawScope.rear(){
                        if(g==4&&side)translate(-9f,7f){scale(.78f,.86f,Offset.Zero){scarfTail(form,side,a,phase,action,dash?.trail?:0f)}}
                        else scarfTail(form,side,a,phase,action,dash?.trail?:0f)
                        if(g>=1&&!swordReady&&e.equipped(RobotSlot.BACK)==null)translate(if(side)-19f else 22f,1f){rotate(if(side)-15f else 30f,Offset.Zero){sword(a,false)}}
                        translate(if(side)-19f else 0f,8f){backPack(a,e.equipped(RobotSlot.BACK))}
                        if(g==4)omegaWings(a,side,action,phase)
                    }
                    if(side){
                        leg(form,farX,hips,ft,fs,true,true,a,boots,dash!=null||action==RobotAction.WALK||action==RobotAction.RUN,bends.ankle,showSockets)
                        rotate(lean,Offset(0f,hips)){rear();arm(form,if(g==4)-3f else -4f,4f,farA,true,a,e.equipped(view.farHand()),swordReady&&view.farHand()==RobotSlot.LEFT_HAND,bends.farElbow,bends.wrist,showSockets,profile=true)}
                        leg(form,nearX,hips,nt,ns,true,false,a,boots,dash!=null||action==RobotAction.WALK||action==RobotAction.RUN,bends.ankle,showSockets)
                        rotate(lean,Offset(0f,hips)){
                            torso(form,true,a,e)
                            arm(form,if(g==4)-3f else 0f,4f,nearA,false,a,e.equipped(view.nearHand()),swordReady&&view.nearHand()==RobotSlot.LEFT_HAND,bends.nearElbow,bends.wrist,showSockets,profile=true)
                            translate(if(g==4)-5f else 0f,if(g==4)-34f else -32f){scale(if(g==4)form.head*.94f else form.head,if(g==4)form.head*.94f else form.head,Offset(0f,22f)){hood(true,g,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,phase>.95f&&action==RobotAction.IDLE,e.equipped(RobotSlot.HEAD))}}
                        }
                    }else{
                        leg(form,farX,hips,ft,fs,false,false,a,boots,dash!=null||action==RobotAction.WALK||action==RobotAction.RUN,bends.ankle,showSockets)
                        leg(form,nearX,hips,nt,ns,false,false,a,boots,dash!=null||action==RobotAction.WALK||action==RobotAction.RUN,bends.ankle,showSockets)
                        rotate(lean,Offset(0f,hips)){
                            rear();torso(form,false,a,e)
                            // Shoulder bridges remain visible between the chest and shoulder armor.
                            for(s in listOf(-1f,1f)){line(s*22f,3f,s*form.shoulders,3f,graphite,10f)}
                            arm(form,-form.shoulders,4f,RobotMotion.frontArmAngle(farA,false),false,a,e.equipped(RobotSlot.RIGHT_HAND),false,-bends.farElbow,bends.wrist,showSockets)
                            arm(form,form.shoulders,4f,RobotMotion.frontArmAngle(nearA,true),false,a,e.equipped(RobotSlot.LEFT_HAND),swordReady,bends.nearElbow,bends.wrist,showSockets)
                            translate(0f,-32f){scale(form.head,form.head,Offset(0f,22f)){hood(false,g,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,phase>.95f&&action==RobotAction.IDLE,e.equipped(RobotSlot.HEAD))}}
                        }
                    }
                    if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=(1f-phase)*.5f),15f+phase*15f,Offset(0f,9f),style=Stroke(2f))
                    if(action==RobotAction.SCAN)line(if(side)27f else 0f,-35f,if(side)70f else 0f,-55f+phase*35f,a.copy(alpha=.65f),1.5f)
                    if(showSockets) {
                        translate(0f,-32f){pivotDot(true)}
                        translate(0f,9f){pivotDot(true)}
                        translate(if(side)-19f else 0f,8f){pivotDot(true)}
                    }
                }
            }
        }
    }
}
