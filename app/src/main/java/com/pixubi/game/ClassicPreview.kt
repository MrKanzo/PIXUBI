package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class ClassicForm(val grade:Int,val height:Float,val head:Float,val shoulder:Float,val hips:Float,val thigh:Float,val shin:Float,val upper:Float,val fore:Float)
internal object ClassicForms {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->ClassicForm(0,.80f,1.18f,23f,43f,21f,24f,17f,19f)
        RobotTier.MK2->ClassicForm(1,.88f,1.06f,28f,52f,31f,34f,23f,25f)
        RobotTier.MK3->ClassicForm(2,.95f,.98f,34f,60f,39f,44f,28f,31f)
        RobotTier.MK4->ClassicForm(3,1f,.91f,40f,66f,46f,51f,33f,36f)
        RobotTier.OMEGA->ClassicForm(4,1.04f,.92f,44f,68f,49f,55f,35f,38f)
    }
}
private val edge=Color(0xFF152337)
private val dark=Color(0xFF263A50)
private val slate=Color(0xFF728CA5)
private val pale=Color(0xFFF0F6FC)
private val visor=Color(0xFF091A2B)
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
private val gold=Color(0xFFFFB548)
private fun DrawScope.classicFace(a:Color,g:Int,side:Boolean,f:RobotFace,blink:Boolean){
    for(x in if(side)listOf(19f)else listOf(-13f,13f)){
        val sign=if(side)1f else if(x<0)-1f else 1f
        if(blink||f==RobotFace.SLEEP)glowLine(a,x-5f,0f,x+5f,0f,2.3f)
        else if(f==RobotFace.HAPPY){
            glowLine(a,x-6f,1f,x,-5f);glowLine(a,x,-5f,x+6f,1f)
        }else if(f==RobotFace.SURPRISE){
            drawRoundRect(a,Offset(x-5f,-9f),Size(10f,15f),CornerRadius(4f),style=Stroke(2.5f))
        }else if(f==RobotFace.ANGRY||f==RobotFace.SAD){
            val oy=if(f==RobotFace.ANGRY)-8f else 1f
            val iy=if(f==RobotFace.ANGRY)-1f else -7f
            glowLine(a,x+6f*sign,oy,x-5f*sign,iy,3.5f)
        }else if(g<2){
            drawRoundRect(a,Offset(x-4f,-8f),Size(8f,13f),CornerRadius(3f))
            drawCircle(Color.White,1.5f,Offset(x-1f,-5f))
        }else{
            poly("M ${x-6} -7 L ${x+6} -7 L ${x+4} 0 L ${x-4} 0 Z",a,.4f)
            glowLine(a,x-4f,-5f,x+4f,-5f,1.8f)
        }
    }
    if(!side&&!blink&&f!=RobotFace.SLEEP){
        if(f==RobotFace.HAPPY){glowLine(a,-4f,7f,0f,10f,1.4f);glowLine(a,0f,10f,4f,7f,1.4f)}
        else glowLine(a,-3f,9f,3f,9f,1.3f)
    }
}
private fun DrawScope.classicHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?){
    val silhouette=if(side)when(g){
        0->"M -30 -20 Q -30 -38 -8 -38 L 15 -38 Q 35 -35 35 -14 L 35 9 Q 31 22 10 23 L -14 23 Q -30 18 -30 -3 Z"
        1->"M -32 -20 L -22 -37 L 9 -41 L 29 -29 L 38 -12 L 34 13 L 18 24 L -21 20 L -32 4 Z"
        2->"M -34 -18 L -24 -37 L 5 -43 L 26 -35 L 39 -17 L 37 8 L 23 22 L 1 27 L -25 18 Z"
        3->"M -36 -20 L -25 -42 L 12 -45 L 29 -34 L 42 -14 L 37 9 L 22 25 L -17 25 L -34 12 Z"
        else->"M -36 -18 L -25 -40 L 5 -47 L 28 -38 L 43 -19 L 40 -4 L 28 18 L 12 28 L -22 21 L -34 8 Z"
    }else when(g){
        0->"M -36 -20 Q -36 -39 -16 -39 L 16 -39 Q 36 -39 36 -20 L 36 8 Q 36 25 18 25 L -18 25 Q -36 25 -36 8 Z"
        1->"M -27 -39 L 27 -39 L 38 -22 L 38 10 L 24 24 L -24 24 L -38 10 L -38 -22 Z"
        2->"M -20 -43 L 20 -43 L 37 -27 L 41 -10 L 32 17 L 16 27 L -16 27 L -32 17 L -41 -10 L -37 -27 Z"
        3->"M -24 -44 L 24 -44 L 42 -25 L 43 5 L 27 25 L 0 29 L -27 25 L -43 5 L -42 -25 Z"
        else->"M 0 -49 L 25 -43 L 43 -24 L 45 0 L 30 22 L 0 30 L -30 22 L -45 0 L -43 -24 L -25 -43 Z"
    }
    if(g==4){
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 24 -30 L 44 -53 L 41 -24 L 32 -15 Z")
            poly("M 33 -32 L 42 -46 L 37 -26 Z",a,.6f)
        }
    }
    plate(silhouette,if(g==3)slate else pale)
    if(side){
        poly(if(g==0)"M 7 -27 Q 31 -29 33 -16 L 33 8 Q 28 17 11 17 L 7 8 Z" else "M 9 -29 L 34 -21 L 36 -10 L 28 12 L 15 19 L 6 7 Z",visor)
        if(g>=2)plate("M 10 10 L 28 5 L 24 17 L 15 21 Z")
        translate(-15f,-3f){drawCircle(edge,12f,Offset.Zero);drawCircle(gold,9f,Offset.Zero);hinge(a,6f)}
        glowLine(a,11f,-32f,28f,-27f,2f)
        if(g>=2){drawLine(slate,Offset(-27f,-25f),Offset(-7f,-36f),1.4f);drawLine(dark,Offset(-24f,12f),Offset(-8f,19f),1.4f)}
    }else{
        poly(if(g==0)"M -28 -18 Q -28 -29 -16 -29 L 16 -29 Q 28 -29 28 -18 L 28 9 Q 28 17 16 17 L -16 17 Q -28 17 -28 9 Z" else "M -29 -23 L 29 -23 L 33 -10 L 26 11 L 0 20 L -26 11 L -33 -10 Z",visor)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(36f,-3f){drawRoundRect(gold,Offset(-3f,-10f),Size(8f,21f),CornerRadius(3f));drawCircle(edge,5f,Offset(1f,0f));drawCircle(a,3f,Offset(1f,0f))}
            if(g>=2){plate("M 26 8 L 35 0 L 30 17 L 16 22 Z");glowLine(a,29f,8f,24f,15f)}
        }
        glowLine(a,-10f,-34f,10f,-34f,2.8f)
    }
    if(g<=1){
        val x=if(side)-10f else 4f
        drawLine(dark,Offset(x,-37f),Offset(x+3f,-49f),3.5f,StrokeCap.Round)
        drawCircle(edge,5f,Offset(x+3f,-50f));drawCircle(a,3.4f,Offset(x+3f,-50f))
    }
    if(g>=3&&!side)poly("M -7 -42 L 7 -42 L 4 -34 L -4 -34 Z",gold)
    classicFace(a,g,side,f,blink)
    when(gear){
        RobotGear.SCANNER->{plate("M -12 -46 L 12 -46 L 17 -39 L -17 -39 Z",slate);glowLine(a,-8f,-42f,8f,-42f)}
        RobotGear.HELMET->plate("M -28 -31 Q 0 -55 28 -31 L 22 -27 Q 0 -45 -22 -27 Z")
        else->Unit
    }
}
private fun DrawScope.reactor(a:Color,r:Float,upgraded:Boolean){
    drawCircle(edge,r+3f,Offset.Zero);drawCircle(slate,r+1f,Offset.Zero)
    drawCircle(if(upgraded)gold else a,r,Offset.Zero,style=Stroke(3f))
    drawCircle(visor,r*.64f,Offset.Zero);drawCircle(a,r*.46f,Offset.Zero);drawCircle(Color.White,r*.20f,Offset.Zero)
}
private fun DrawScope.classicBody(f:ClassicForm,side:Boolean,a:Color,e:RobotEquipment){
    val g=f.grade;val h=f.hips
    translate(0f,-12f){hinge(a,7f)}
    val shell=if(side)when(g){
        0->"M -19 -9 Q 19 -16 23 5 L 21 30 Q 15 46 -16 41 L -23 20 Z"
        1->"M -20 -9 L 15 -10 L 26 6 L 21 31 L 14 44 L 17 52 L -18 54 L -22 42 L -19 26 Z"
        else->"M -23 -12 L 14 -12 L 29 1 L 27 19 L 13 34 L 10 ${h-15} L 20 ${h-5} L 11 ${h+7} L -15 ${h+7} L -23 ${h-4} L -12 ${h-16} L -17 31 L -27 11 Z"
    }else when(g){
        0->"M -23 -9 Q 0 -17 23 -9 Q 30 5 26 28 Q 23 45 0 48 Q -23 45 -26 28 Q -30 5 -23 -9 Z"
        1->"M -25 -10 L 25 -10 L 31 5 L 24 29 L 16 38 L 22 51 L 0 59 L -22 51 L -16 38 L -24 29 L -31 5 Z"
        2->"M -28 -12 L 28 -12 L 35 6 L 26 24 L 16 38 L 16 47 L 25 57 L 0 68 L -25 57 L -16 47 L -16 38 L -26 24 L -35 6 Z"
        3->"M -32 -14 L 32 -14 L 40 5 L 30 24 L 19 35 L 14 48 L 29 60 L 23 71 L 0 74 L -23 71 L -29 60 L -14 48 L -19 35 L -30 24 L -40 5 Z"
        else->"M -34 -14 L 34 -14 L 43 5 L 32 25 L 18 37 L 13 49 L 28 61 L 21 74 L 0 78 L -21 74 L -28 61 L -13 49 L -18 37 L -32 25 L -43 5 Z"
    }
    plate(shell,if(g==3)slate else pale)
    if(side){
        plate("M -16 -7 L 17 -7 L 25 3 L 23 18 L 12 27 L -11 22 Z",pale)
        translate(19f,10f){scale(.6f,1f,Offset.Zero){reactor(a,if(g==4)12f else 9f,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}}
    }else{
        if(g>=1)for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 7 -7 L ${if(g>=3)32 else 24} -7 L ${if(g>=3)35 else 28} 8 L 19 25 L 8 18 Z",pale)
            if(g>=2)glowLine(a,18f,18f,12f,28f)
        }
        translate(0f,if(g==0)16f else 11f){reactor(a,if(g==4)14f else 10f,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}
        if(g==4)translate(0f,35f){reactor(a,5f,false)}
    }
    if(g>=2){
        for(y in (if(g==4)42 else 32) until (h.toInt()-10) step 6)poly("M -12 $y L 12 $y L 9 ${y+4} L -9 ${y+4} Z",dark)
        plate("M -21 ${h-10} L 21 ${h-10} L 19 ${h-2} L -19 ${h-2} Z",slate)
        if(!side)for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 12 ${h-4} L 25 ${h-6} L 29 ${h+12} L 17 ${h+15} Z")}
        else plate("M -19 ${h-5} L -7 ${h-7} L 0 ${h+9} L -12 ${h+12} Z",slate)
    }
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR){plate("M -27 -7 L -15 -11 L -12 18 L -24 27 L -32 7 Z");if(!side)scale(-1f,1f,Offset.Zero){plate("M -27 -7 L -15 -11 L -12 18 L -24 27 L -32 7 Z")}}
}
private fun DrawScope.arm(form:ClassicForm,x:Float,angle:Float,elbow:Float,wrist:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
    val g=form.grade
    translate(x,3f){rotate(angle,Offset.Zero){
        hinge(a,5.8f,debug)
        scale(1f,form.upper/30f,Offset.Zero){
            plate("M -6 0 L 6 0 L 8 14 L 4 30 L -4 30 L -8 14 Z",slate)
            val cap=when(g){
                0->"M -9 -6 Q 0 -11 9 -6 L 10 7 L -10 7 Z"
                1->"M -10 -7 L 11 -5 L 13 6 L 4 12 L -10 8 Z"
                2->"M -12 -8 L 7 -10 L 15 0 L 11 12 L -10 12 Z"
                3->"M -14 -12 L 13 -12 L 20 0 L 15 14 L -14 14 L -20 1 Z"
                else->"M -15 -9 L -4 -16 L 17 -13 L 25 -5 L 17 13 L -12 16 L -21 4 Z"
            }
            plate(cap,if(far)slate else pale);glowLine(a,-7f,7f,7f,7f,1.5f)
            if(g>=3)plate("M -10 -8 L 11 -9 L 15 -3 L -7 -2 Z",slate)
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
private fun DrawScope.leg(form:ClassicForm,x:Float,t:Float,s:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
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
            plate(when(g){0->"M -8 -3 L 8 -3 Q 13 13 8 45 L -8 45 Q -13 13 -8 -3 Z";1->"M -9 -4 L 9 -4 L 12 15 L 8 45 L -8 45 L -12 15 Z";2->"M -10 -5 L 8 -5 L 14 12 L 10 25 L 5 45 L -6 45 L -13 24 L -15 10 Z";else->"M -12 -6 L 12 -6 L 19 13 L 12 31 L 9 45 L -9 45 L -12 31 L -19 13 Z"},if(far)slate else pale)
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
private fun DrawScope.classicBack(f:ClassicForm,a:Color,side:Boolean,action:RobotAction,phase:Float,gear:RobotGear?){
    if(f.grade==4){
        val p=OmegaWingMotion.pose(action,phase)
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(if(side)25f else 34f,3f){
                for(i in 0..3)rotate(-(18f+i*19f+p.spread*(15f+i*11f)),Offset.Zero){
                    scale(1f,1f-i*.10f,Offset.Zero){
                        plate("M -11 1 L 5 -7 L 18 16 L 12 43 L 0 105 L -9 72 L -13 25 Z")
                        poly("M 4 28 L 12 18 L 7 53 L 0 103 L -5 62 Z",a.copy(alpha=.82f),.7f)
                        glowLine(a,5f,33f,0f,84f,2.2f)
                        plate("M -8 4 L 5 1 L 11 16 L -5 32 Z",slate)
                    }
                }
                hinge(a,12f)
            }
        }
    }else if(f.grade>=2||gear!=null){
        translate(if(side)-23f else 0f,7f){
            plate("M -18 -9 L 18 -9 L 21 34 L -21 34 Z",slate)
            for(x in listOf(-10f,10f))glowLine(a,x,2f,x,25f)
            if(gear==RobotGear.BOOSTER||f.grade==3){
                for(x in listOf(-12f,12f))translate(x,32f){
                    plate("M -7 -4 L 7 -4 L 5 8 L -5 8 Z")
                    if(action==RobotAction.RUN||action==RobotAction.JUMP)poly("M -4 8 L 4 8 L 0 ${18f+8f*sin(phase*6.28f)} Z",a,.5f)
                }
            }
        }
    }
}
@Composable
internal fun ClassicPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=ClassicForms.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val view=KageGait.view(requested,action);val side=view!=RobotView.FRONT
    val motion=RobotMotion.pose(action,phase);val bends=KageJointMotion.of(action,phase)
    val gait=if(KageGait.isLocomotion(action))KageGait.pose(KageForm(f.height,f.head,f.shoulder,1f,f.thigh,f.shin,f.upper,f.fore,0f,f.grade),action,phase)else null
    Canvas(modifier){
        val fit=min(size.width/(if(f.grade==4)350f else 230f),size.height/320f)*f.height
        val ground=size.height*.89f
        drawOval(edge.copy(alpha=.30f),Offset(size.width/2-28f*fit,ground-2f),Size(56f*fit,5f*fit))
        translate(size.width/2,ground){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            val base=f.hips+f.thigh+f.shin+12f
            val nt=gait?.near?.thigh?:motion.nearLeg;val ft=gait?.far?.thigh?:motion.farLeg
            val ns=gait?.near?.shin?:nt+bends.nearKnee;val fs=gait?.far?.shin?:ft+bends.farKnee
            val na=gait?.nearArm?:motion.nearArm;val fa=gait?.farArm?:motion.farArm
            val lean=gait?.lean?:0f
            translate(0f,-base+(gait?.drop?:0f)+(if(gait==null)motion.lift else 0f)){
                rotate(lean,Offset(0f,f.hips)){classicBack(f,a,side,action,phase,e.equipped(RobotSlot.BACK))}
                val boots=e.equipped(RobotSlot.FEET)
                val hipWidth=if(f.grade==0)11f else 15f
                leg(f,if(side)0f else -hipWidth,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,true,a,boots,debug)
                if(side)rotate(lean,Offset(0f,f.hips)){arm(f,0f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug)}
                leg(f,if(side)0f else hipWidth,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,boots,debug)
                rotate(lean,Offset(0f,f.hips)){
                    classicBody(f,side,a,e)
                    if(!side){
                        for(s in listOf(-1f,1f))drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                        arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug)
                    }
                    arm(f,if(side)0f else f.shoulder,if(side)na else RobotMotion.frontArmAngle(na,true),bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand()else RobotSlot.LEFT_HAND),debug)
                    translate(if(side)-2f else 0f,-33f){scale(f.head,f.head,Offset(0f,23f)){classicHead(f.grade,side,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,action==RobotAction.IDLE&&phase>.94f,e.equipped(RobotSlot.HEAD))}}
                    if(debug)translate(0f,-12f){drawCircle(gold,5f,Offset.Zero,style=Stroke(1.4f))}
                }
                if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=.55f*(1f-phase)),14f+phase*25f,Offset(0f,12f),style=Stroke(2f))
                if(action==RobotAction.SCAN)glowLine(a,if(side)30f else 0f,-37f,if(side)75f else 0f,-60f+phase*40f,1.4f)
            }
        }}
    }
}
