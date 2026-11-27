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
// A single expressive optical aperture, not a human face or a conventional robot visor.
private fun DrawScope.nexusFace(a:Color,face:RobotFace,blink:Boolean,g:Int,phase:Float){
    val r=if(g==0)13f else 12f
    val pupil=if(face==RobotFace.SURPRISE)3f else 5f
    drawCircle(visor,r+4f,Offset.Zero)
    drawCircle(a.copy(alpha=.16f),r+3f,Offset.Zero)
    if(blink||face==RobotFace.SLEEP){glowLine(a,-r,0f,r,0f,2.2f)}
    else {
    val upper=when(face){RobotFace.ANGRY->-2f;RobotFace.SAD->-8f;RobotFace.HAPPY->-20f;RobotFace.SURPRISE->-r*1.6f;else->-r}
    val lower=when(face){RobotFace.HAPPY->-3f;RobotFace.SAD->r*1.2f;RobotFace.ANGRY->7f;RobotFace.SURPRISE->r*1.6f;else->r}
    val lens=Path().apply{
        moveTo(-r,0f);quadraticBezierTo(0f,upper,r,0f)
        quadraticBezierTo(0f,lower,-r,0f);close()
    }
    drawPath(lens,a.copy(alpha=.8f));drawPath(lens,a,style=Stroke(1.1f))
    clipPath(lens){
        drawCircle(visor,pupil+2f,Offset(0f,if(face==RobotFace.SAD)2f else -1f))
        drawCircle(a,pupil,Offset(0f,-1f),style=Stroke(1.5f))
        drawCircle(Color.White,2f,Offset(-2f,-3f))
    }
    }
    if(g>=1)rotate(phase*360f,Offset.Zero){
        for(i in 0..2)rotate(i*120f,Offset.Zero){
            drawArc(a,8f,52f,false,Offset(-r-8f,-r-8f),Size(2*r+16f,2*r+16f),style=Stroke(1.5f))
        }
    }
}
private fun DrawScope.nexusHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?,phase:Float){
    // BIT: floating seed, LOGIC: lens in a triangular processor,
    // NEURAL: exposed neural crystal, NEXUS: split cage, SINGULARITY: energy nucleus.
    val drift=sin(phase*6.28f)*2f
    if(g==0){
        plate(if(side)"M -24 -30 Q -5 -45 20 -33 L 34 -10 L 24 13 L 2 24 L -22 9 Z"
            else "M 0 -43 Q 32 -34 36 -9 Q 32 20 0 28 Q -32 20 -36 -9 Q -32 -34 0 -43 Z",pale)
        translate(if(side)16f else 0f,-6f){scale(if(side).73f else 1f,1f,Offset.Zero){nexusFace(a,f,blink,g,phase)}}
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(32f,-23f+drift){plate("M -7 -9 L 7 -12 L 11 1 L 0 9 L -8 4 Z",slate);drawCircle(a,3f,Offset.Zero)}
        }
    }else if(g==1){
        plate(if(side)"M -27 -34 L 10 -43 L 38 -12 L 22 24 L -12 19 Z"
            else "M 0 -48 L 39 12 L 21 28 L -21 28 L -39 12 Z",dark)
        plate(if(side)"M -17 -29 L 8 -35 L 29 -13 L 21 -5 L 3 -25 Z"
            else "M 0 -48 L 39 12 L 30 13 L 0 -33 L -30 13 L -39 12 Z",pale)
        translate(if(side)16f else 0f,-2f){scale(if(side).77f else 1f,1f,Offset.Zero){nexusFace(a,f,blink,g,phase)}}
        glowLine(a,-12f,21f,12f,21f)
    }else{
        val r=if(g==4)24f else 20f
        translate(if(side)8f else 0f,-8f){
            drawCircle(a.copy(alpha=.10f),r+13f,Offset.Zero)
            val crystal=Path().apply{moveTo(0f,-r-14f);lineTo(r,-6f);lineTo(r*.72f,r*.62f);lineTo(0f,r+9f);lineTo(-r*.72f,r*.62f);lineTo(-r,-6f);close()}
            drawPath(crystal,Brush.linearGradient(listOf(a.copy(alpha=.55f),visor,a.copy(alpha=.3f)),Offset(-r,-r),Offset(r,r)))
            drawPath(crystal,a,style=Stroke(1.5f))
            if(g>=3){
                for(i in 0..2)rotate(phase*360f+i*120f,Offset.Zero){
                    val radius=r+14f
                    drawArc(if(g==4)gold else a,0f,76f,false,Offset(-radius,-radius),Size(radius*2,radius*2),style=Stroke(if(g==4)2.5f else 1.5f))
                }
            }
            scale(if(side).8f else 1f,1f,Offset.Zero){nexusFace(a,f,blink,g,phase)}
        }
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            val x=if(side)24f else 36f
            translate(x+if(g>=3)4f+drift else 0f,if(side&&s<0)-15f else -9f){
                if(g==2)plate("M -7 -28 L 9 -20 L 12 2 L 2 27 L -6 15 L -3 -8 Z")
                else plate("M -9 -27 L 7 -31 L 16 -14 L 13 14 L 0 30 L -5 21 L 4 0 Z",if(g==4)gold else pale)
                glowLine(a,4f,-18f,7f,10f,2f)
            }
        }
    }
    when(gear){
        RobotGear.SCANNER->{plate("M -11 -49 L 12 -49 L 15 -42 L -15 -42 Z");glowLine(a,-6f,-45f,7f,-45f)}
        RobotGear.HELMET->plate("M -27 -31 Q 0 -55 27 -31 L 21 -27 Q 0 -45 -21 -27 Z")
        else->Unit
    }
    drawVariantHelmet(gear,side,a,g)
}
private fun DrawScope.nexusBody(f:NexusForm,side:Boolean,a:Color,e:RobotEquipment,phase:Float,sep:Float){
    val g=f.grade;val h=f.hips;val w=if(side)23f else f.shoulder-2f
    val coreR=if(g>=3)17f else 10f+g
    if(g<=1){
        plate(if(g==0)"M 0 -11 Q $w -8 ${w+5} 13 Q $w 40 0 ${h+6} Q ${-w} 40 ${-w-5} 13 Q ${-w} -8 0 -11 Z"
            else "M 0 -14 L ${w+5} 5 L $w 25 L 16 ${h+5} L -16 ${h+5} L ${-w} 25 L ${-w-5} 5 Z",if(g==0)pale else dark)
        for(s in if(side)listOf(1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 5 -8 L $w 4 L ${w-5} 20 L 10 24 L 5 12 Z");glowLine(a,w-7f,7f,14f,18f)}
    }else{
        // Open chassis: visible energy spine and separate shell pieces; no solid torso behind the core.
        val spine=Path().apply{moveTo(0f,-10f);quadraticBezierTo(-7f,h*.45f,0f,h);quadraticBezierTo(7f,h*.45f,0f,-10f);close()}
        drawPath(spine,a.copy(alpha=.28f));drawPath(spine,a,style=Stroke(1.8f))
        val gap=if(g>=3)6f+sep*18f else 2f
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(if(side)gap*.55f else gap,0f){
                plate("M 8 -12 L ${w-5} -10 L ${w+1} 3 L ${w-4} 18 L 12 24 L 16 7 Z",if(g==4)gold else pale)
                glowLine(a,w-8f,1f,w-11f,13f)
                if(g>=3)plate("M 12 28 L ${w-9} 24 L ${w-6} 36 L 15 ${h-13} L 8 ${h-18} Z",slate)
            }
            translate(gap*.65f,0f){
                plate("M 4 ${h-10} L 17 ${h-16} L 25 ${h-5} L 17 ${h+6} L 5 ${h+2} Z")
                glowLine(a,14f,h-8f,18f,h)
            }
        }
    }
    translate(if(side)8f else 0f,14f){
        drawCircle(a.copy(alpha=.13f),coreR+10f,Offset.Zero)
        for(i in 0..1)rotate(phase*360f+i*180f,Offset.Zero){
            drawArc(a,0f,105f,false,Offset(-coreR-6f,-coreR-6f),Size(coreR*2+12f,coreR*2+12f),style=Stroke(2f))
        }
        reactor(a,coreR,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
    }
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR)for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 16 -8 L 30 -8 L 34 12 L 19 20 Z",slate)}
    drawVariantArmor(e.equipped(RobotSlot.CHEST),side,a,f.grade)
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
    if(f.grade==4)translate(if(side)-11f else 0f,-84f){
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
    if(gear!=null)translate(if(side)-31f else 0f,-12f){drawVariantPack(gear,side,a,f.grade)}
}
private fun DrawScope.arm(form:NexusForm,x:Float,angle:Float,elbow:Float,wrist:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
    val g=form.grade
    translate(x,3f){rotate(angle,Offset.Zero){
        if(g>=3){drawCircle(a.copy(alpha=.65f),6f,Offset.Zero,style=Stroke(1.5f));if(debug)drawCircle(gold,8f,Offset.Zero,style=Stroke(1f))}else hinge(a,5.8f,debug)
        scale(1f,form.upper/30f,Offset.Zero){
            if(g>=3){glowLine(a,0f,5f,0f,28f,1.2f);plate("M -8 10 L 8 10 L 11 18 L 5 25 L -5 25 L -11 18 Z",slate)}else plate("M -6 0 L 6 0 L 8 14 L 4 30 L -4 30 L -8 14 Z",slate)
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
            if(g>=3)drawCircle(a,4.6f,Offset.Zero,style=Stroke(1.6f))else hinge(a,4.8f,debug)
            scale(1f,form.fore/32f,Offset.Zero){
                plate(when(g){0->"M -8 1 Q 0 -4 8 1 L 9 24 Q 0 35 -9 24 Z";1->"M -6 0 L 7 0 L 11 10 L 8 32 L -8 32 L -11 10 Z";2->"M -6 0 L 7 0 L 12 10 L 9 28 L 3 32 L -9 28 L -12 13 Z";else->"M 0 4 L 14 10 L 12 23 L 0 29 L -12 23 L -14 10 Z"},if(far)slate else pale)
                poly("M -5 7 L 5 7 L 5 24 L -5 24 Z",dark);glowLine(a,0f,10f,0f,21f)
            }
            translate(0f,form.fore){rotate(wrist,Offset.Zero){
                if(g>=3)drawCircle(a,3.5f,Offset.Zero,style=Stroke(1.3f))else hinge(a,4f,debug);plate(if(g==0)"M -6 0 Q 0 -4 6 0 L 7 9 Q 0 14 -7 9 Z" else "M -6 0 L 6 0 L 8 7 L 3 13 L -5 12 L -8 6 Z",slate)
                scale(1f+g*.045f,1f+g*.045f,Offset.Zero){drawWorldWeapon(gear,a)}
            }}
        }}
    }}
}
private fun DrawScope.leg(form:NexusForm,x:Float,t:Float,s:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
    val g=form.grade;val u=form.thigh;val v=form.shin
    val rad=t*PI.toFloat()/180f
    translate(x,form.hips){rotate(t,Offset.Zero){
        if(g>=3)drawCircle(a,6f,Offset.Zero,style=Stroke(1.5f))else hinge(a,6f,debug)
        scale(1f,u/40f,Offset.Zero){
            if(g>=3){glowLine(a,0f,0f,0f,40f,1.5f);plate("M 0 5 L 12 15 L 7 30 L 0 35 L -7 30 L -12 15 Z")}else plate(if(g==0)"M -8 0 Q 0 -5 8 0 L 10 18 L 6 40 L -6 40 L -10 18 Z" else "M -10 0 L 10 0 L 13 12 L 6 40 L -6 40 L -13 12 Z",slate)
            if(g==2)plate("M -8 5 L -1 3 L 3 19 L -4 30 L -9 20 Z")
        }
    }}
    translate(x-u*sin(rad),form.hips+u*cos(rad)){rotate(s,Offset.Zero){
        if(g>=3)drawCircle(a,5.6f,Offset.Zero,style=Stroke(1.5f))else hinge(a,5.6f,debug)
        scale(1f,v/45f,Offset.Zero){
            plate(when(g){0->"M -8 -3 L 8 -3 Q 13 13 8 45 L -8 45 Q -13 13 -8 -3 Z";1->"M -9 -4 L 9 -4 L 12 15 L 8 45 L -8 45 L -12 15 Z";2->"M -10 -5 L 8 -5 L 14 12 L 10 25 L 5 45 L -6 45 L -13 24 L -15 10 Z";else->"M 0 4 L 14 13 L 9 31 L 0 40 L -9 31 L -14 13 Z"},if(far)slate else pale)
            poly("M -6 10 L 6 10 L 4 35 L -4 35 Z",dark);glowLine(a,0f,16f,0f,32f,2.5f)
        }
        translate(0f,v){rotate(-s,Offset.Zero){
            if(g>=3)drawCircle(a,4.7f,Offset.Zero,style=Stroke(1.5f))else hinge(a,4.7f,debug)
            val toe=if(side)17 else 11
            plate(if(!side)"M -8 -4 L 8 -4 L 12 6 L 10 12 L -10 12 L -12 6 Z" else if(g>=3)"M -8 1 L 6 1 L $toe 7 L 5 12 L -10 12 L -13 6 Z" else "M -9 -4 L 7 -4 L $toe 7 L $toe 12 L -11 12 L -12 6 Z",if(far)slate else pale)
            glowLine(a,if(side)-6f else -8f,10f,if(side)toe-2f else 8f,10f)
            drawVariantBoot(gear,side,a,g)
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
        val fit=if(loadout.arena)size.height*.70f/(f.hips+f.thigh+f.shin+12f+38f+65f*f.head) else min(size.width/(if(f.grade>=2)370f else 245f),size.height/(if(f.grade==4)390f else if(f.grade>=2)350f else 320f))*f.height
        val ground=size.height*.89f
        val sep=if(action==RobotAction.REASSEMBLE&&f.grade>=3)NexusCatalog.separation(phase) else 0f
        val hover=if(f.grade>=3&&!KageGait.isLocomotion(action)&&action!=RobotAction.SLEEP)(if(f.grade==4)12f else 7f)+3f*sin(phase*6.28f) else 0f
        drawOval(edge.copy(alpha=.30f),Offset(size.width/2-28f*fit,ground-2f),Size(56f*fit,5f*fit))
        translate(size.width/2,ground){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            val base=f.hips+f.thigh+f.shin+12f
            val nt=gait?.near?.thigh?:motion.nearLeg;val ft=gait?.far?.thigh?:motion.farLeg
            val ns=gait?.near?.shin?:nt+bends.nearKnee;val fs=gait?.far?.shin?:ft+bends.farKnee
            val na=gait?.nearArm?:motion.nearArm;val fa=gait?.farArm?:motion.farArm
            val lean=gait?.lean?:WorldMotion.idleLean(loadout.series,action,phase,side)
            translate(0f,-base-hover+(gait?.drop?:0f)+(if(gait==null)motion.lift else 0f)){
                rotate(lean,Offset(0f,f.hips)){nexusOrbit(f,side,a,phase,sep,e.equipped(RobotSlot.BACK))}
                fun DrawScope.robot(){
                val boots=e.equipped(RobotSlot.FEET)
                val hipWidth=if(f.grade==0)11f else 15f
                leg(f,if(side)-4f else -hipWidth,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,side,a,boots,debug)
                if(side)rotate(lean,Offset(0f,f.hips)){arm(f,-3f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug)}
                leg(f,if(side)4f else hipWidth,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,boots,debug)
                rotate(lean,Offset(0f,f.hips)){
                    nexusBody(f,side,a,e,phase,sep)
                    if(!side){
                        for(s in listOf(-1f,1f))if(f.grade>=3)glowLine(a,s*18f,3f,s*f.shoulder,3f,1.2f)else drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                        arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug)
                    }
                    arm(f,if(side)3f else f.shoulder,if(side)na else RobotMotion.frontArmAngle(na,true),bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand()else RobotSlot.LEFT_HAND),debug)
                    translate(if(side)-2f else 0f,-33f-(if(f.grade>=2)8f+2f*sin(phase*6.28f)else 0f)-sep*16f){scale(f.head,f.head,Offset(0f,23f)){nexusHead(f.grade,side,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,action==RobotAction.IDLE&&phase>.94f,e.equipped(RobotSlot.HEAD),phase)}}
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
