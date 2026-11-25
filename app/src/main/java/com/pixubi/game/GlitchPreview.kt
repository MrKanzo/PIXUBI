package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class GlitchForm(val title:String,val grade:Int,val height:Float,val head:Float,val shoulder:Float,val hips:Float,val thigh:Float,val shin:Float,val upper:Float,val fore:Float,val drones:Int)
internal object GlitchCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->GlitchForm("BYTE",0,.78f,1.20f,22f,45f,19f,22f,16f,18f,0)
        RobotTier.MK2->GlitchForm("PROXY",1,.86f,1.06f,28f,53f,29f,32f,23f,25f,0)
        RobotTier.MK3->GlitchForm("GHOST",2,.93f,.98f,33f,60f,37f,42f,27f,30f,0)
        RobotTier.MK4->GlitchForm("GLITCH",3,1f,.93f,37f,65f,45f,50f,31f,34f,1)
        RobotTier.OMEGA->GlitchForm("OVERLORD",4,1.04f,.94f,42f,68f,48f,53f,33f,37f,2)
    }
    fun teleport(phase:Float):Float=sin(PI.toFloat()*phase.coerceIn(0f,1f)).coerceAtLeast(0f)
}
private val edge=Color(0xFF100C1B)
private val dark=Color(0xFF221C32)
private val slate=Color(0xFF514962)
private val pale=Color(0xFFC3BBD5)
private val visor=Color(0xFF100B1C)
private fun DrawScope.poly(s:String,c:Color,w:Float=1.3f){
    val p=PathParser().parsePathString(s).toPath()
    drawPath(p,c);if(w>0f)drawPath(p,edge,style=Stroke(w,join=StrokeJoin.Round))
}
private fun DrawScope.plate(s:String,light:Color=pale,shadow:Color=dark){
    val p=PathParser().parsePathString(s).toPath();val b=p.getBounds()
    drawPath(p,Brush.linearGradient(listOf(light,slate,shadow),Offset(b.left,b.top),Offset(b.right,b.bottom)))
    drawPath(p,edge,style=Stroke(1.4f,join=StrokeJoin.Round))
}
private fun DrawScope.glowLine(a:Color,x:Float,y:Float,u:Float,v:Float,w:Float=2f){
    drawLine(a.copy(alpha=.22f),Offset(x,y),Offset(u,v),w+3f,StrokeCap.Round)
    drawLine(a,Offset(x,y),Offset(u,v),w,StrokeCap.Round)
    drawLine(Color.White.copy(alpha=.55f),Offset(x,y),Offset(u,v),.65f)
}
private fun DrawScope.hinge(a:Color,r:Float,debug:Boolean=false){
    drawCircle(edge,r+1f,Offset.Zero);drawCircle(slate,r,Offset.Zero);drawCircle(dark,r*.65f,Offset.Zero)
    drawCircle(a,r*.26f,Offset.Zero)
    if(debug)drawCircle(Color(0xFFFFB64D),r+2f,Offset.Zero,style=Stroke(1.2f))
}
private fun DrawScope.core(a:Color,g:Int){
    if(g==0){drawCircle(edge,7f,Offset.Zero);drawCircle(a,5f,Offset.Zero,style=Stroke(2f));drawCircle(a,2f,Offset.Zero)}
    else{
        poly("M -11 -8 L 11 -8 L 0 12 Z",dark)
        poly("M -8 -5 L 8 -5 L 0 8 Z",a,.6f)
        poly("M -4 -3 L 4 -3 L 0 4 Z",Color.White,.0f)
    }
}
private fun DrawScope.facePixels(a:Color,g:Int,side:Boolean,f:RobotFace,blink:Boolean){
    for(x in if(side)listOf(15f) else listOf(-12f,12f)){
        if(blink||f==RobotFace.SLEEP)glowLine(a,x-5f,-2f,x+5f,-2f)
        else if(f==RobotFace.HAPPY||g==0&&f==RobotFace.NEUTRAL){
            glowLine(a,x-5f,1f,x,-5f);glowLine(a,x,-5f,x+5f,1f)
        }else if(f==RobotFace.SURPRISE){drawRect(a,Offset(x-3f,-7f),Size(6f,10f),style=Stroke(2f))}
        else if(g<=1){
            for(i in 0..2)drawRect(a,Offset(x-4f+i*3f,-8f+i*(if(f==RobotFace.SAD)1f else 0f)),Size(2f,12f-i))
        }else{
            val sign=if(side)1f else if(x<0)-1f else 1f
            val outer=x+7f*sign
            val inner=x-6f*sign
            val oy=if(f==RobotFace.SAD)1f else if(f==RobotFace.ANGRY)-9f else -5f
            val iy=if(f==RobotFace.SAD)-7f else if(f==RobotFace.ANGRY)-1f else -3f
            poly("M $outer $oy L $inner $iy L ${inner+sign*2f} ${iy+5f} L ${outer-sign*3f} ${oy+5f} Z",a,.55f)
            glowLine(a,outer,oy,inner,iy,1.8f)
            if(g>=3&&f==RobotFace.NEUTRAL)glowLine(a,inner+sign*2f,iy+5f,inner+sign*4f,8f,1.5f)
        }
    }
}
private fun DrawScope.glitchHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?){
    if(side){
        val hood=when(g){
            0->"M -30 -14 Q -32 -41 0 -42 Q 33 -41 35 -13 L 32 13 Q 12 27 -20 20 Z"
            1->"M -34 -17 L -27 -39 L -9 -45 L 18 -38 L 37 -16 L 30 14 L 13 24 L -25 17 Z"
            2->"M -38 -17 L -24 -35 L -40 -39 L -10 -46 L 18 -39 L 40 -15 L 29 15 L 9 25 L -25 15 Z"
            3->"M -36 -18 Q -37 -33 -23 -40 L -31 -45 L -8 -48 Q 22 -47 39 -17 L 31 -9 L 27 10 L 12 26 Q -20 25 -32 6 Z"
            else->"M -37 -18 Q -38 -36 -20 -43 L -27 -49 L -7 -48 Q 24 -46 42 -17 L 34 -10 L 28 11 L 14 27 Q -22 24 -33 6 Z"
        }
        plate(hood,if(g<2)pale else slate)
        if(g>=3){
            poly("M 11 -30 Q 28 -24 36 -15 L 31 -6 L 28 9 L 14 23 L 4 10 L 1 -8 Z",visor)
            plate("M 10 3 L 28 -2 L 26 10 L 14 23 L 6 11 Z",slate)
            plate("M -7 -45 Q 24 -46 42 -17 L 34 -10 Q 24 -24 11 -30 L 3 -33 Z",slate)
            poly("M -36 -18 Q -21 -10 -11 8 Q -1 20 14 27 Q -21 24 -33 6 Z",dark,.9f)
            glowLine(a,22f,-29f,33f,-18f,2.5f)
            drawLine(slate,Offset(-23f,-7f),Offset(-9f,15f),1.1f)
            translate(-15f,-10f){hinge(a,5f)}
        }else{
            poly("M 6 -29 L 33 -15 L 27 9 L 13 20 L 2 6 Z",visor)
            if(g>=2)plate("M 7 4 L 27 0 L 22 14 L 13 20 Z",slate)
            glowLine(a,13f,-30f,29f,-18f,2.4f)
            translate(-14f,-1f){hinge(a,if(g<2)10f else 7f)}
            if(g<2)plate("M -21 -31 L -25 -48 L -8 -37 Z",slate)
            else poly("M -26 -34 L -34 -48 L -13 -40 Z",dark)
        }
    }else{
        val hood=when(g){
            0->"M -34 -11 Q -35 -42 0 -42 Q 35 -42 34 -11 Q 35 24 0 25 Q -35 24 -34 -11 Z"
            1->"M 0 -45 Q 25 -43 36 -16 L 37 8 L 23 23 L -23 23 L -37 8 L -36 -16 Q -25 -43 0 -45 Z"
            2->"M 0 -47 L 19 -40 L 38 -12 L 35 13 L 0 27 L -35 13 L -38 -12 L -19 -40 Z"
            else->"M 0 -49 Q 23 -46 35 -24 L 40 -12 L 35 14 Q 17 25 0 28 Q -17 25 -35 14 L -40 -12 L -35 -24 Q -23 -46 0 -49 Z"
        }
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 19 -32 L 26 -48 L 36 -25 Z",slate)
            poly("M 25 -39 L 30 -29 L 22 -32 Z",a,.5f)
        }
        plate(hood,if(g<2)pale else slate)
        poly(if(g<2)"M -27 -10 Q -27 -31 0 -31 Q 27 -31 27 -10 L 24 12 Q 0 24 -24 12 Z" else "M 0 -31 L 29 -13 L 27 9 L 0 23 L -27 9 L -29 -13 Z",visor)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            if(g<=1)translate(34f,-1f){hinge(a,7f)}
            else{plate("M 27 4 L 35 -1 L 27 17 L 16 21 Z",slate);glowLine(a,28f,-20f,34f,-10f)}
        }
        if(g>=3){
            for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 0 -49 Q 23 -46 35 -24 L 40 -12 L 33 -9 L 27 -22 Q 13 -36 0 -38 Z",slate)}
            glowLine(a,0f,-24f,0f,-18f,1.3f)
        }
    }
    if(side&&g>=3)translate(3f,-1f){scale(.86f,1f,Offset(15f,0f)){facePixels(a,g,true,f,blink)}} else facePixels(a,g,side,f,blink)
    when(gear){
        RobotGear.SCANNER->{poly("M -11 -47 L 12 -47 L 15 -39 L -12 -39 Z",dark);glowLine(a,-6f,-43f,8f,-43f)}
        RobotGear.HELMET->plate("M -27 -30 Q 0 -53 27 -30 L 20 -27 Q 0 -44 -20 -27 Z")
        else->Unit
    }
}
private fun DrawScope.body(form:GlitchForm,side:Boolean,a:Color,e:RobotEquipment){
    val g=form.grade;val h=form.hips
    if(g==0){
        plate(if(side)"M -18 -8 Q 14 -15 21 6 L 17 35 Q 0 48 -19 37 Z" else "M -22 -8 Q 0 -18 22 -8 L 25 12 L 20 36 Q 0 48 -20 36 L -25 12 Z")
        poly("M -12 1 L 12 1 L 10 30 L -10 30 Z",dark)
        translate(if(side)8f else 0f,14f){core(a,g)}
    }else{
        val w=if(side)19 else if(g==1)26 else 31
        plate("M -$w -9 L $w -9 L ${w+3} 10 L 15 ${h-20} L 21 ${h-5} L 0 ${h+8} L -21 ${h-5} L -15 ${h-20} L -${w+3} 10 Z",slate)
        if(side){
            plate("M -12 -7 L 18 -7 L 23 8 L 12 24 L -10 20 Z",pale)
            glowLine(a,21f,2f,20f,14f)
        }else{
            for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
                plate("M 6 -7 L ${w-2} -6 L $w 10 L 17 24 L 8 18 Z",if(g==1)pale else slate)
                if(g>=2)plate("M 19 17 L 26 10 L 21 27 L 13 33 Z")
                if(g>=3)glowLine(a,13f,24f,9f,32f)
            }
            translate(0f,10f){core(a,g)}
        }
        for(y in 29 until (h.toInt()-8) step 7)poly("M -12 $y L 12 $y L 9 ${y+5} L -9 ${y+5} Z",dark)
        plate("M -20 ${h-12} L 20 ${h-12} L 19 ${h-4} L -19 ${h-4} Z",pale)
        if(g>=2)for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 12 ${h-5} L 24 ${h-8} L 29 ${h+13} L 18 ${h+17} Z",pale)
        }
    }
    poly("M -21 -12 L 0 -4 L 21 -12 L 17 0 L 0 7 L -17 0 Z",dark)
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR)plate("M -24 -5 L -14 -9 L -10 18 L -23 25 L -29 7 Z")
    if(e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)drawCircle(a,13f,Offset(if(side)9f else 0f,11f),style=Stroke(1.5f))
}
private fun DrawScope.terminal(a:Color,phase:Float){
    drawRoundRect(a.copy(alpha=.14f),Offset(-30f,-24f),Size(60f,43f),CornerRadius(3f))
    drawRoundRect(a,Offset(-30f,-24f),Size(60f,43f),CornerRadius(3f),style=Stroke(1.3f))
    drawCircle(a,11f,Offset(-12f,-3f),style=Stroke(1.2f))
    drawArc(a,phase*360f,100f,false,Offset(-21f,-12f),Size(18f,18f),style=Stroke(2f))
    for(i in 0..4)drawRect(a.copy(alpha=.8f),Offset(4f,-16f+i*6f),Size(7f+((i*7)%13),1.4f))
}
private fun DrawScope.arm(form:GlitchForm,x:Float,angle:Float,elbow:Float,wrist:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean,showPanel:Boolean,phase:Float){
    val g=form.grade
    translate(x,3f){rotate(angle,Offset.Zero){
        hinge(a,5.8f,debug)
        scale(1f,form.upper/30f,Offset.Zero){
            plate("M -6 0 L 6 0 L 8 14 L 4 30 L -4 30 L -8 14 Z",slate)
            val cap=when(g){0->"M -9 -6 Q 0 -11 9 -6 L 10 7 L -10 7 Z";1->"M -10 -7 L 11 -5 L 13 6 L 4 12 L -10 8 Z";else->"M -12 -8 L 7 -13 L 17 -2 L 13 9 L -10 13 L -15 3 Z"}
            plate(cap,if(far)slate else pale);glowLine(a,-7f,7f,7f,7f,1.5f)
        }
        translate(0f,form.upper){rotate(elbow,Offset.Zero){
            hinge(a,4.8f,debug)
            scale(1f,form.fore/32f,Offset.Zero){
                plate(if(g==0)"M -8 1 Q 0 -4 8 1 L 9 24 Q 0 35 -9 24 Z" else "M -6 0 L 7 0 L 12 10 L 9 28 L 3 32 L -9 28 L -12 13 Z",if(far)slate else pale)
                poly("M -5 7 L 5 7 L 5 24 L -5 24 Z",dark);glowLine(a,0f,10f,0f,21f)
            }
            translate(0f,form.fore){rotate(wrist,Offset.Zero){
                hinge(a,4f,debug);plate(if(g==0)"M -6 0 Q 0 -4 6 0 L 7 9 Q 0 14 -7 9 Z" else "M -6 0 L 6 0 L 8 7 L 3 13 L -5 12 L -8 6 Z",slate)
                when(gear){
                    RobotGear.SHIELD_L,RobotGear.SHIELD_R->{plate("M -15 -28 L 15 -28 L 19 -10 L 0 7 L -19 -10 Z");translate(0f,-12f){core(a,g)}}
                    RobotGear.TOOL_L,RobotGear.TOOL_R->{poly("M -4 7 L 4 7 L 4 25 L -4 25 Z",slate);glowLine(a,-7f,24f,7f,24f,4f)}
                    RobotGear.BLASTER_L,RobotGear.BLASTER_R->{plate("M -8 -10 L 8 -10 L 8 24 L -8 24 Z");glowLine(a,-5f,22f,5f,22f,3f)}
                    else->Unit
                }
                if(showPanel)translate(if(side)25f else 21f,-25f){rotate(-angle-elbow-wrist,Offset.Zero){terminal(a,phase)}}
            }}
        }}
    }}
}
private fun DrawScope.leg(form:GlitchForm,x:Float,t:Float,s:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
    val g=form.grade;val u=form.thigh;val v=form.shin
    val rad=t*PI.toFloat()/180f
    translate(x,form.hips){rotate(t,Offset.Zero){
        hinge(a,6f,debug)
        scale(1f,u/40f,Offset.Zero){
            plate(if(g==0)"M -8 0 Q 0 -5 8 0 L 10 18 L 6 40 L -6 40 L -10 18 Z" else "M -10 0 L 10 0 L 13 12 L 6 40 L -6 40 L -13 12 Z",slate)
            if(g>=2)plate("M -8 5 L -1 3 L 3 19 L -4 30 L -9 20 Z")
        }
    }}
    translate(x-u*sin(rad),form.hips+u*cos(rad)){rotate(s,Offset.Zero){
        hinge(a,5.6f,debug)
        scale(1f,v/45f,Offset.Zero){
            plate(if(g<2)"M -8 -3 L 8 -3 Q 13 13 8 45 L -8 45 Q -13 13 -8 -3 Z" else "M -10 -5 L 8 -5 L 14 12 L 10 25 L 5 45 L -6 45 L -13 24 L -15 10 Z",if(far)slate else pale)
            poly("M -6 10 L 6 10 L 4 35 L -4 35 Z",dark);glowLine(a,0f,16f,0f,32f,2.5f)
        }
        translate(0f,v){rotate(-s,Offset.Zero){
            hinge(a,4.7f,debug)
            val toe=if(side)17 else 11
            plate(if(side)"M -9 -4 L 7 -4 L $toe 7 L $toe 12 L -11 12 L -12 6 Z" else "M -8 -4 L 8 -4 L 12 6 L 10 12 L -10 12 L -12 6 Z",if(far)slate else pale)
            glowLine(a,if(side)-6f else -8f,10f,if(side)toe-2f else 8f,10f)
            if(gear==RobotGear.BOOTS||gear==RobotGear.SPEED_BOOTS)plate(if(side)"M -12 -2 L 10 -2 L ${toe+3} 8 L ${toe+3} 13 L -14 13 Z" else "M -10 -2 L 10 -2 L 14 7 L 12 13 L -12 13 L -14 7 Z",slate)
        }}
    }}
}
private fun DrawScope.cape(g:Int,side:Boolean,a:Color,phase:Float){
    if(g<3)return
    val swing=sin(phase*2f*PI.toFloat())*7f
    translate(if(side)-15f else 0f,0f){rotate(swing,Offset.Zero){
        plate(if(side)"M 0 -7 L -25 2 Q -36 39 -54 101 L -37 91 L -48 123 L -22 106 L -14 119 L 5 9 Z" else "M -23 -9 L 23 -9 L 38 55 L 54 122 L 26 108 L 14 131 L 0 112 L -18 131 L -29 109 L -52 122 Z",slate)
        for(i in 0..6){val x=if(side)-39f+i*4f else -35f+i*11f;val y=98f+(i%3)*9f
            drawRect(a.copy(alpha=.65f),Offset(x,y),Size(3f,6f));drawRect(a.copy(alpha=.30f),Offset(x-3f,y+12f),Size(2f,4f))}
    }}
}
private fun DrawScope.wings(a:Color,side:Boolean,action:RobotAction,phase:Float){
    val spread=when(action){RobotAction.JUMP,RobotAction.ATTACK,RobotAction.DRONE_SWARM->.85f;RobotAction.TELEPORT->GlitchCatalog.teleport(phase);else->.24f}
    for(sign in if(side)listOf(-1f)else listOf(-1f,1f))scale(sign,1f,Offset.Zero){
        translate(if(side)25f else 35f,-2f){
            for(i in 0..4)rotate(-(15f+i*12f+spread*(12f+i*10f)),Offset.Zero){
                scale(1f,(100f-i*9f)/100f,Offset.Zero){
                    plate("M -8 0 L 6 -6 L 17 15 L 12 43 L 2 75 L -6 97 L -10 62 Z",slate)
                    poly("M 5 28 L 12 20 L 10 54 L -6 97 L -2 57 Z",a,.7f)
                    plate("M -6 7 L 5 1 L 12 16 L 6 32 L -5 48 L -8 30 Z",slate)
                    poly("M -5 48 L 5 32 L 0 64 L -6 77 Z",dark,.65f)
                    glowLine(a,6f,38f,-3f,79f,2f)
                }
            }
            hinge(a,10f)
        }
    }
}
private fun DrawScope.drone(a:Color,x:Float,y:Float,phase:Float){
    translate(x,y+sin(phase*2f*PI.toFloat())*5f){
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 6 -7 L 19 -13 L 15 -2 L 22 5 L 7 8 Z",slate);glowLine(a,11f,-4f,17f,-7f,1.5f)}
        drawCircle(edge,12f,Offset.Zero);drawCircle(pale,10f,Offset.Zero);drawCircle(dark,7f,Offset.Zero)
        drawCircle(a,5f,Offset.Zero,style=Stroke(2f));drawCircle(a,2.8f,Offset.Zero)
    }
}
@Composable
internal fun GlitchPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=GlitchCatalog.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val view=KageGait.view(requested,action);val side=view!=RobotView.FRONT
    val motion=RobotMotion.pose(action,phase);val bends=KageJointMotion.of(action,phase)
    val gait=if(KageGait.isLocomotion(action))KageGait.pose(KageForm(f.height,f.head,f.shoulder,1f,f.thigh,f.shin,f.upper,f.fore,0f,f.grade),action,phase)else null
    val warp=if(action==RobotAction.TELEPORT)GlitchCatalog.teleport(phase)else 0f
    Canvas(modifier){
        val fit=min(size.width/(if(f.grade==4)350f else 260f),size.height/320f)*f.height
        val ground=size.height*.89f
        val shift=if(action==RobotAction.TELEPORT)(if(phase<.5f)-25f else 25f)*warp*fit else 0f
        drawOval(edge.copy(alpha=.35f),Offset(size.width/2+shift-27f*fit,ground-2f),Size(54f*fit,5f*fit))
        translate(size.width/2+shift,ground){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            val base=f.hips+f.thigh+f.shin+12f
            translate(0f,-base+(gait?.drop?:0f)+(if(gait==null)motion.lift else 0f)){
                val nt=gait?.near?.thigh?:motion.nearLeg;val ft=gait?.far?.thigh?:motion.farLeg
                val ns=gait?.near?.shin?:nt+bends.nearKnee;val fs=gait?.far?.shin?:ft+bends.farKnee
                val na=gait?.nearArm?:motion.nearArm;val fa=gait?.farArm?:motion.farArm
                val showTerminal=f.grade>=2&&(action==RobotAction.SCAN||action==RobotAction.HACK)
                val actual=if(action==RobotAction.SLEEP)RobotFace.SLEEP else face
                fun DrawScope.robot(){
                    cape(f.grade,side,a,phase)
                    if(f.grade==4)wings(a,side,action,phase)
                    if(e.equipped(RobotSlot.BACK)!=null)translate(if(side)-22f else 0f,4f){plate("M -15 -8 L 15 -8 L 18 25 L -18 25 Z");glowLine(a,-9f,0f,-9f,19f)}
                    leg(f,if(side)-4f else -14f,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,side,a,e.equipped(RobotSlot.FEET),debug)
                    if(side)rotate(gait?.lean?:0f,Offset(0f,f.hips)){arm(f,-3f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug,false,phase)}
                    leg(f,if(side)4f else 14f,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,e.equipped(RobotSlot.FEET),debug)
                    rotate(gait?.lean?:0f,Offset(0f,f.hips)){
                        body(f,side,a,e)
                        if(!side){
                            for(s in listOf(-1f,1f))drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                            arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug,false,phase)
                        }
                        arm(f,if(side)3f else f.shoulder,if(showTerminal)-35f else if(side)na else RobotMotion.frontArmAngle(na,true),if(showTerminal)-60f else bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand() else RobotSlot.LEFT_HAND),debug,showTerminal,phase)
                        translate(0f,-32f){scale(f.head,f.head,Offset(0f,23f)){glitchHead(f.grade,side,a,actual,action==RobotAction.IDLE&&phase>.95f,e.equipped(RobotSlot.HEAD))}}
                    }
                }
                if(warp<.15f)robot()
                else if(warp<.90f){
                    for(i in 0..11){val y=-95f+i*25f;clipRect(-180f,y,180f,y+25f*(1f-warp*.70f)){translate(sin(i*7f+phase*20f)*warp*22f,0f){robot()}}}
                }
                if(warp>.05f)for(i in 0..27){val x=sin(i*13.3f+phase*8f)*65f*warp;val y=-80f+(i*31%235);drawRect(a.copy(alpha=warp*.8f),Offset(x,y.toFloat()),Size(2f+(i%4),4f))}
                if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=.5f*(1f-phase)),12f+phase*25f,Offset(0f,12f),style=Stroke(2f))
                val count=if(action==RobotAction.DRONE_SWARM&&f.grade>=2)max(1,f.drones+1) else f.drones
                for(i in 0 until count){val t=phase*2f*PI.toFloat()+i*2.1f;drone(a,cos(t)*75f,-52f+sin(t)*18f,phase+i*.2f)}
            }
        }}
    }
}
