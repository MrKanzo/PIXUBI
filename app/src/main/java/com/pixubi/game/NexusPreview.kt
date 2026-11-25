package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class NexusForm(val title:String,val grade:Int,val height:Float,val head:Float,val shoulder:Float,val hips:Float,val thigh:Float,val shin:Float,val upper:Float,val fore:Float)
internal object NexusCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->NexusForm("BIT",0,.78f,1.15f,22f,43f,20f,23f,17f,19f)
        RobotTier.MK2->NexusForm("LOGIC",1,.87f,1.03f,28f,51f,30f,34f,23f,25f)
        RobotTier.MK3->NexusForm("NEURAL",2,.94f,.96f,32f,59f,38f,44f,27f,30f)
        RobotTier.MK4->NexusForm("NEXUS",3,1f,.90f,38f,65f,46f,51f,32f,35f)
        RobotTier.OMEGA->NexusForm("SINGULARITY",4,1.02f,.89f,41f,68f,49f,55f,34f,37f)
    }
    fun separation(phase:Float)=sin(phase.coerceIn(0f,1f)*PI.toFloat()).coerceIn(0f,1f)
}
private val gold=Color(0xFFE5B857)
private val edge=Color(0xFF122631)
private val dark=Color(0xFF173C49)
private val slate=Color(0xFF5E8993)
private val pale=Color(0xFFE4FBF8)
private val visor=Color(0xFF071B25)
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
private fun DrawScope.reactor(a:Color,r:Float,upgraded:Boolean){
    drawCircle(edge,r+4f,Offset.Zero);drawCircle(slate,r+1f,Offset.Zero,style=Stroke(2f))
    drawCircle(a.copy(alpha=.2f),r,Offset.Zero)
    poly("M 0 ${-r} L $r 0 L 0 $r L ${-r} 0 Z",if(upgraded)gold else a,.8f)
    poly("M 0 ${-r*.48f} L ${r*.48f} 0 L 0 ${r*.48f} L ${-r*.48f} 0 Z",Color.White,.5f)
}
private fun DrawScope.nexusFace(a:Color,side:Boolean,face:RobotFace,blink:Boolean,g:Int){
    for(x in if(side)listOf(18f)else listOf(-12f,12f)){
        when {
            blink||face==RobotFace.SLEEP->glowLine(a,x-5f,0f,x+5f,0f,2f)
            face==RobotFace.HAPPY->{glowLine(a,x-5f,1f,x,-5f);glowLine(a,x,-5f,x+5f,1f)}
            face==RobotFace.SURPRISE->drawCircle(a,5f,Offset(x,-3f),style=Stroke(2f))
            face==RobotFace.ANGRY||face==RobotFace.SAD->{
                val s=if(side||x>0f)1f else -1f
                val y=if(face==RobotFace.ANGRY)-8f else 2f
                glowLine(a,x+6f*s,y,x-5f*s,-4f-y*.5f,2.7f)
            }
            g<=1->{drawRect(a,Offset(x-4f,-7f),Size(8f,11f));drawRect(Color.White,Offset(x-2f,-5f),Size(2f,2f))}
            else->{glowLine(a,x-5f,-7f,x+5f,-7f);glowLine(a,x-5f,-7f,x-5f,3f);glowLine(a,x+5f,-7f,x+5f,3f);glowLine(a,x-5f,3f,x+5f,3f)}
        }
    }
    if(!side&&!blink&&face!=RobotFace.SLEEP)glowLine(a,-4f,10f,4f,10f,1.2f)
}
private fun DrawScope.nexusHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?,phase:Float){
    val shape=if(side)when(g){
        0->"M -28 -25 L -12 -39 L 18 -39 L 35 -23 L 35 6 L 19 22 L -14 22 L -28 8 Z"
        1->"M -33 -18 L -19 -40 L 12 -43 L 35 -27 L 39 -9 L 28 17 L 7 25 L -25 16 Z"
        else->"M -35 -17 L -23 -39 L 0 -47 L 23 -36 L 42 -17 L 36 6 L 20 25 L -16 25 L -32 10 Z"
    }else when(g){
        0->"M -23 -38 L 23 -38 L 35 -25 L 35 10 L 23 23 L -23 23 L -35 10 L -35 -25 Z"
        1->"M -24 -40 L 24 -40 L 38 -22 L 35 9 L 19 25 L -19 25 L -35 9 L -38 -22 Z"
        2->"M 0 -45 L 26 -36 L 38 -17 L 35 10 L 0 28 L -35 10 L -38 -17 L -26 -36 Z"
        3->"M 0 -48 L 27 -38 L 42 -15 L 34 10 L 0 29 L -34 10 L -42 -15 L -27 -38 Z"
        else->"M 0 -51 L 30 -40 L 44 -15 L 34 13 L 0 31 L -34 13 L -44 -15 L -30 -40 Z"
    }
    plate(shape,if(g==0)pale else slate)
    if(side){
        poly("M 8 -27 L 31 -23 L 36 -12 L 27 13 L 13 19 L 5 7 Z",visor)
        translate(-14f,-4f){hinge(a,9f)}
        glowLine(a,8f,-31f,28f,-26f)
    }else{
        poly("M -28 -23 L 28 -23 L 31 -9 L 24 12 L 0 20 L -24 12 L -31 -9 Z",visor)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 28 -26 L 36 -17 L 33 7 L 24 15 L 27 -4 Z",pale)
            glowLine(a,32f,-15f,31f,2f)
        }
    }
    if(g>=2)translate(if(side)-8f else 0f,-35f){reactor(a,5f,false)}
    if(g>=3){
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(38f+2f*sin(phase*6.28f),-20f){plate("M -2 -16 L 12 -8 L 9 11 L 0 16 L -4 0 Z");glowLine(a,4f,-8f,4f,7f)}
        }
    }
    nexusFace(a,side,f,blink,g)
    when(gear){
        RobotGear.SCANNER->{plate("M -11 -47 L 12 -47 L 15 -39 L -15 -39 Z");glowLine(a,-6f,-43f,7f,-43f)}
        RobotGear.HELMET->plate("M -27 -31 Q 0 -55 27 -31 L 21 -27 Q 0 -45 -21 -27 Z")
        else->Unit
    }
}
private fun DrawScope.nexusBody(f:NexusForm,side:Boolean,a:Color,e:RobotEquipment,phase:Float,sep:Float){
    val g=f.grade;val h=f.hips
    translate(0f,-12f){hinge(a,6f)}
    val width=if(side)23f else f.shoulder-3f
    val coreR=if(g>=3)15f else 10f+g
    plate("M ${-width} -9 L $width -9 L ${width+3} 10 L ${width*.48f} 34 L 14 ${h-9} L 21 ${h+5} L -21 ${h+5} L -14 ${h-9} L ${-width*.48f} 34 L ${-width-3} 10 Z",if(g==0)pale else dark)
    for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
        val gap=if(g>=3)3f+sep*13f else 0f
        translate(gap,0f){
            plate("M 5 -10 L ${width-1} -10 L ${width+4} 5 L ${width*.7f} 23 L 13 30 L 8 19 Z")
            glowLine(a,width-5f,1f,width*.7f,17f)
        }
        if(g>=1)translate(gap*.65f,0f){
            plate("M 5 ${h-20} L 17 ${h-24} L 25 ${h-4} L 18 ${h+7} L 6 ${h+4} Z",slate)
            glowLine(a,15f,h-13f,19f,h-3f)
        }
    }
    translate(if(side)12f else 0f,12f){
        drawCircle(a.copy(alpha=.25f),coreR+8f,Offset.Zero)
        rotate(phase*360f,Offset.Zero){drawArc(a,0f,85f,false,Offset(-coreR-5f,-coreR-5f),Size(coreR*2+10f,coreR*2+10f),style=Stroke(2f));drawArc(a,180f,85f,false,Offset(-coreR-5f,-coreR-5f),Size(coreR*2+10f,coreR*2+10f),style=Stroke(2f))}
        reactor(a,coreR,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
    }
    if(g>=2)for(i in 0..2)drawRect(a.copy(alpha=.55f+.3f*sin(phase*6.28f+i)),Offset(-7f,34f+i*5f),Size(14f,2f))
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR)for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 16 -8 L 30 -8 L 34 12 L 19 20 Z",slate)}
}
private fun DrawScope.nexusOrbit(f:NexusForm,side:Boolean,a:Color,phase:Float,sep:Float,gear:RobotGear?){
    if(f.grade>=2){
        val radius=if(f.grade==4)63f else 46f
        translate(if(side)-12f else 0f,16f){
            for(i in 0..1)rotate(phase*360f*(if(i==0)1f else -1f)+i*90f,Offset.Zero){
                drawOval(a.copy(alpha=.34f),Offset(-radius,-radius*.58f),Size(radius*2,radius*1.16f),style=Stroke(1.6f))
                drawCircle(a,3f,Offset(radius,0f))
            }
        }
    }
    if(f.grade==4)translate(if(side)-11f else 0f,-67f){
        for(i in 0..2)rotate(phase*180f+i*120f,Offset.Zero){
            drawArc(gold,10f,76f,false,Offset(-47f,-47f),Size(94f,94f),style=Stroke(3f))
            drawCircle(a,3f,Offset(47f,0f))
        }
    }
    if(f.grade>=3)for(i in 0 until (if(f.grade==4)6 else 4)){
        val t=i*6.28f/(if(f.grade==4)6 else 4)+phase*6.28f
        val r=64f+sep*23f
        translate(cos(t)*r,23f+sin(t)*49f){rotate(t*57.3f,Offset.Zero){
            plate("M 0 -15 L 12 -5 L 8 11 L 0 16 L -8 11 L -12 -5 Z",if(i%2==0)pale else slate)
            poly("M 0 -9 L 5 -2 L 0 7 L -5 -2 Z",a,.5f)
        }}
    }
    if(gear!=null)translate(if(side)-23f else 0f,4f){plate("M -14 -6 L 14 -6 L 17 24 L -17 24 Z");glowLine(a,0f,0f,0f,18f)}
}
private fun DrawScope.arm(form:NexusForm,x:Float,angle:Float,elbow:Float,wrist:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
    val g=form.grade
    translate(x,3f){rotate(angle,Offset.Zero){
        hinge(a,5.8f,debug)
        scale(1f,form.upper/30f,Offset.Zero){
            plate("M -6 0 L 6 0 L 8 14 L 4 30 L -4 30 L -8 14 Z",slate)
            val cap=when(g){
                0->"M -9 -6 Q 0 -11 9 -6 L 10 7 L -10 7 Z"
                1->"M -10 -7 L 11 -5 L 13 6 L 4 12 L -10 8 Z"
                2->"M -12 -8 L 7 -10 L 15 0 L 11 12 L -10 12 Z"
                3->"M -12 -10 L 5 -16 L 19 -5 L 13 12 L -9 14 L -15 3 Z"
                else->"M -13 -9 L 0 -18 L 15 -13 L 22 -5 L 12 15 L -10 15 L -17 3 Z"
            }
            plate(cap,if(far)dark else pale);glowLine(a,-7f,7f,7f,7f,1.5f)
            if(g>=3)plate("M -10 -5 L 10 -5 L 13 0 L -10 2 Z",slate)
        }
        translate(0f,form.upper){rotate(elbow,Offset.Zero){
            hinge(a,4.8f,debug)
            scale(1f,form.fore/32f,Offset.Zero){
                plate(when(g){0->"M -8 1 Q 0 -4 8 1 L 9 24 Q 0 35 -9 24 Z";1->"M -6 0 L 7 0 L 11 10 L 8 32 L -8 32 L -11 10 Z";2->"M -6 0 L 7 0 L 12 10 L 9 28 L 3 32 L -9 28 L -12 13 Z";else->"M -8 -1 L 8 -1 L 16 10 L 15 23 L 8 33 L -9 33 L -16 20 Z"},if(far)slate else pale)
                poly("M -5 7 L 5 7 L 5 24 L -5 24 Z",dark);glowLine(a,0f,10f,0f,21f)
            }
            translate(0f,form.fore){rotate(wrist,Offset.Zero){
                hinge(a,4f,debug);plate(if(g==0)"M -6 0 Q 0 -4 6 0 L 7 9 Q 0 14 -7 9 Z" else "M -6 0 L 6 0 L 8 7 L 3 13 L -5 12 L -8 6 Z",slate)
                when(gear){
                    RobotGear.SHIELD_L,RobotGear.SHIELD_R->{plate("M -15 -28 L 15 -28 L 19 -10 L 0 7 L -19 -10 Z");translate(0f,-12f){reactor(a,7f,false)}}
                    RobotGear.TOOL_L,RobotGear.TOOL_R->{poly("M -4 7 L 4 7 L 4 25 L -4 25 Z",slate);glowLine(a,-7f,24f,7f,24f,4f)}
                    RobotGear.BLASTER_L,RobotGear.BLASTER_R->{plate("M -8 -10 L 8 -10 L 8 24 L -8 24 Z");glowLine(a,-5f,22f,5f,22f,3f)}
                    else->Unit
                }
            }}
        }}
    }}
}
private fun DrawScope.leg(form:NexusForm,x:Float,t:Float,s:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
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
            plate(when(g){0->"M -8 -3 L 8 -3 Q 13 13 8 45 L -8 45 Q -13 13 -8 -3 Z";1->"M -9 -4 L 9 -4 L 12 15 L 8 45 L -8 45 L -12 15 Z";2->"M -10 -5 L 8 -5 L 14 12 L 10 25 L 5 45 L -6 45 L -13 24 L -15 10 Z";else->"M -9 -6 L 9 -6 L 13 13 L 9 31 L 7 45 L -7 45 L -10 31 L -13 13 Z"},if(far)slate else pale)
            poly("M -6 10 L 6 10 L 4 35 L -4 35 Z",dark);glowLine(a,0f,16f,0f,32f,2.5f)
        }
        translate(0f,v){rotate(-s,Offset.Zero){
            hinge(a,4.7f,debug)
            val toe=if(side)17 else 11
            plate("M -9 -4 L 7 -4 L $toe 7 L $toe 12 L -11 12 L -12 6 Z",if(far)slate else pale)
            glowLine(a,-6f,10f,toe-2f,10f)
            if(gear==RobotGear.BOOTS||gear==RobotGear.SPEED_BOOTS)plate("M -12 -2 L 10 -2 L ${toe+3} 8 L ${toe+3} 13 L -14 13 Z",slate)
        }}
    }}
}
@Composable
internal fun NexusPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=NexusCatalog.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val view=KageGait.view(requested,action);val side=view!=RobotView.FRONT
    val motion=RobotMotion.pose(action,phase);val bends=KageJointMotion.of(action,phase)
    val gait=if(KageGait.isLocomotion(action))KageGait.pose(KageForm(f.height,f.head,f.shoulder,1f,f.thigh,f.shin,f.upper,f.fore,0f,f.grade),action,phase)else null
    Canvas(modifier){
        val fit=min(size.width/(if(f.grade>=2)370f else 245f),size.height/(if(f.grade==4)370f else 320f))*f.height
        val ground=size.height*.89f
        val sep=if(action==RobotAction.REASSEMBLE&&f.grade>=3)NexusCatalog.separation(phase) else 0f
        val hover=if(f.grade==4&&!KageGait.isLocomotion(action))8f+3f*sin(phase*6.28f) else 0f
        drawOval(edge.copy(alpha=.30f),Offset(size.width/2-28f*fit,ground-2f),Size(56f*fit,5f*fit))
        translate(size.width/2,ground){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            val base=f.hips+f.thigh+f.shin+12f
            val nt=gait?.near?.thigh?:motion.nearLeg;val ft=gait?.far?.thigh?:motion.farLeg
            val ns=gait?.near?.shin?:nt+bends.nearKnee;val fs=gait?.far?.shin?:ft+bends.farKnee
            val na=gait?.nearArm?:motion.nearArm;val fa=gait?.farArm?:motion.farArm
            val lean=gait?.lean?:0f
            translate(0f,-base-hover+(gait?.drop?:0f)+(if(gait==null)motion.lift else 0f)){
                rotate(lean,Offset(0f,f.hips)){nexusOrbit(f,side,a,phase,sep,e.equipped(RobotSlot.BACK))}
                fun DrawScope.robot(){
                val boots=e.equipped(RobotSlot.FEET)
                val hipWidth=if(f.grade==0)11f else 15f
                leg(f,if(side)0f else -hipWidth,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,true,a,boots,debug)
                if(side)rotate(lean,Offset(0f,f.hips)){arm(f,0f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug)}
                leg(f,if(side)0f else hipWidth,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,boots,debug)
                rotate(lean,Offset(0f,f.hips)){
                    nexusBody(f,side,a,e,phase,sep)
                    if(!side){
                        for(s in listOf(-1f,1f))drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                        arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug)
                    }
                    arm(f,if(side)0f else f.shoulder,if(side)na else RobotMotion.frontArmAngle(na,true),bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand()else RobotSlot.LEFT_HAND),debug)
                    translate(if(side)-2f else 0f,-33f-sep*16f){scale(f.head,f.head,Offset(0f,23f)){nexusHead(f.grade,side,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,action==RobotAction.IDLE&&phase>.94f,e.equipped(RobotSlot.HEAD),phase)}}
                    if(debug)translate(0f,-12f){drawCircle(gold,5f,Offset.Zero,style=Stroke(1.4f))}
                }
                                }
                if(action==RobotAction.HOLO_CLONE&&f.grade>=2){
                    // Two translucent holograms, rendered from the same current pose.
                    for(s in listOf(-1f,1f))translate(s*(65f+10f*sin(phase*6.28f)),0f){
                        val bounds=androidx.compose.ui.geometry.Rect(-180f,-150f,180f,230f)
                        drawContext.canvas.saveLayer(bounds,Paint().apply{alpha=.20f})
                        try{robot()}finally{drawContext.canvas.restore()}
                        for(i in 0..7)drawLine(a.copy(alpha=.14f),Offset(-32f,-55f+i*24f),Offset(32f,-55f+i*24f),1f)
                    }
                }
                robot()
                if(action==RobotAction.REASSEMBLE&&f.grade>=3)for(i in 0..15){
                    val t=i*2.4f+phase*6.28f
                    drawRect(a.copy(alpha=sep*.75f),Offset(cos(t)*(25f+sep*65f),sin(t)*70f+25f),Size(3f,3f))
                }
                if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=.55f*(1f-phase)),14f+phase*25f,Offset(0f,12f),style=Stroke(2f))
                if(action==RobotAction.SCAN)glowLine(a,if(side)30f else 0f,-37f,if(side)75f else 0f,-60f+phase*40f,1.4f)
            }
        }}
    }
}
