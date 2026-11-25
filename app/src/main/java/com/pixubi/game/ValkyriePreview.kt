package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class ValkyrieForm(val title:String,val grade:Int,val height:Float,val head:Float,val shoulder:Float,val hips:Float,val thigh:Float,val shin:Float,val upper:Float,val fore:Float)
internal object ValkyrieCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->ValkyrieForm("LUMI",0,.79f,1.14f,22f,43f,20f,24f,17f,19f)
        RobotTier.MK2->ValkyrieForm("NOVA",1,.87f,1.04f,26f,52f,30f,35f,23f,25f)
        RobotTier.MK3->ValkyrieForm("AURA",2,.95f,.96f,30f,61f,39f,45f,27f,30f)
        RobotTier.MK4->ValkyrieForm("VALKYRIE",3,1f,.90f,35f,65f,46f,53f,31f,35f)
        RobotTier.OMEGA->ValkyrieForm("SERAPH",4,1.03f,.89f,38f,68f,49f,56f,34f,37f)
    }
    fun hover(phase:Float)=18f+4f*sin(phase*2f*PI.toFloat())
}
private val edge=Color(0xFF30233C)
private val dark=Color(0xFF46334E)
private val slate=Color(0xFFB18A9B)
private val pale=Color(0xFFFFF4F1)
private val visor=Color(0xFF22192D)
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
private val gold=Color(0xFFD9A080)
private fun DrawScope.valkyrieFace(a:Color,g:Int,side:Boolean,f:RobotFace,blink:Boolean){
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
private fun DrawScope.valkHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?){
    val path=if(side)when(g){
        0->"M -30 -13 Q -34 -40 0 -42 Q 30 -42 34 -16 L 31 8 Q 18 26 -7 25 Q -31 21 -30 -13 Z"
        1->"M -32 -16 Q -30 -40 -5 -45 L 16 -39 L 37 -17 L 32 10 L 16 25 L -20 20 Z"
        2->"M -33 -19 L -21 -39 L 3 -46 L 23 -37 L 39 -16 L 31 13 L 12 27 L -23 19 Z"
        3->"M -35 -17 Q -34 -43 0 -47 L 22 -39 L 42 -18 L 34 9 L 14 27 L -23 21 Z"
        else->"M -35 -19 L -24 -40 L 0 -49 L 23 -41 L 44 -19 L 35 8 L 14 28 L -24 21 Z"
    }else when(g){
        0->"M -34 -12 Q -35 -42 0 -42 Q 35 -42 34 -12 Q 35 25 0 26 Q -35 25 -34 -12 Z"
        1->"M 0 -45 Q 28 -43 36 -17 L 34 10 Q 23 24 0 27 Q -23 24 -34 10 L -36 -17 Q -28 -43 0 -45 Z"
        2->"M 0 -47 L 22 -40 L 37 -20 L 37 8 L 18 24 L 0 29 L -18 24 L -37 8 L -37 -20 L -22 -40 Z"
        3->"M 0 -48 Q 27 -45 39 -21 L 41 -8 L 31 15 L 0 29 L -31 15 L -41 -8 L -39 -21 Q -27 -45 0 -48 Z"
        else->"M 0 -51 L 24 -44 L 41 -22 L 43 -8 L 32 16 L 0 31 L -32 16 L -43 -8 L -41 -22 L -24 -44 Z"
    }
    // Petal-shaped antennae evolve into a crown, never a human hairstyle.
    for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
        if(g<=1){plate("M 22 -28 Q 21 -49 32 -51 Q 45 -41 34 -26 Z",gold);glowLine(a,29f,-43f,32f,-31f)}
        else {plate("M 24 -30 L 36 ${if(g==4)-63 else -49} L 44 -34 L 34 -17 Z",gold);poly("M 34 -37 L 36 ${if(g==4)-55 else -44} L 39 -34 Z",a,.5f)}
    }
    plate(path)
    if(side){
        poly("M 8 -28 Q 27 -26 33 -17 L 30 7 L 15 19 L 7 8 Z",visor)
        plate("M 10 10 L 30 3 L 25 17 L 16 22 Z",gold)
        translate(-15f,-3f){hinge(a,if(g<2)9f else 7f)}
        glowLine(a,13f,-33f,28f,-26f,2.3f)
    }else{
        poly("M -29 -18 Q -23 -31 0 -29 Q 23 -31 29 -18 L 27 9 L 0 21 L -27 9 Z",visor)
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 27 5 L 35 -2 L 30 16 L 17 21 Z",gold)
            glowLine(a,30f,6f,24f,15f,1.5f)
        }
        if(g>=2){poly("M 0 -44 L 7 -36 L 0 -28 L -7 -36 Z",gold);poly("M 0 -41 L 4 -36 L 0 -32 L -4 -36 Z",a,.5f)}
    }
    valkyrieFace(a,g,side,f,blink)
    when(gear){
        RobotGear.SCANNER->{plate("M -11 -47 L 12 -47 L 15 -39 L -15 -39 Z",gold);glowLine(a,-6f,-43f,7f,-43f)}
        RobotGear.HELMET->plate("M -27 -31 Q 0 -55 27 -31 L 21 -27 Q 0 -45 -21 -27 Z")
        else->Unit
    }
}
private fun DrawScope.reactor(a:Color,r:Float,upgraded:Boolean){
    drawCircle(edge,r+3f,Offset.Zero);drawCircle(gold,r+1f,Offset.Zero)
    poly("M 0 ${-r} L $r 0 L 0 $r L ${-r} 0 Z",if(upgraded)gold else a)
    poly("M 0 ${-r*.5f} L ${r*.5f} 0 L 0 ${r*.5f} L ${-r*.5f} 0 Z",Color.White,.5f)
}
private fun DrawScope.valkBody(f:ValkyrieForm,side:Boolean,a:Color,e:RobotEquipment){
    val h=f.hips;val g=f.grade
    translate(0f,-12f){hinge(a,6f)}
    val shape=if(side) "M -18 -10 L 14 -10 Q 26 2 24 16 L 10 33 L 8 ${h-13} L 17 ${h-3} L 10 ${h+7} L -16 ${h+7} L -21 ${h-4} L -11 ${h-15} L -17 26 L -23 7 Z"
    else when(g){
        0->"M -21 -9 Q 0 -16 21 -9 Q 27 5 22 28 L 18 38 Q 0 49 -18 38 L -22 28 Q -27 5 -21 -9 Z"
        1->"M -23 -10 L 23 -10 L 28 7 L 17 27 L 12 38 L 23 49 L 0 59 L -23 49 L -12 38 L -17 27 L -28 7 Z"
        2->"M -26 -12 L 26 -12 L 31 5 L 20 26 L 12 39 L 13 48 L 26 59 L 0 68 L -26 59 L -13 48 L -12 39 L -20 26 L -31 5 Z"
        3->"M -29 -14 L 29 -14 L 35 4 L 21 27 L 12 42 L 13 51 L 30 63 L 20 72 L 0 74 L -20 72 L -30 63 L -13 51 L -12 42 L -21 27 L -35 4 Z"
        else->"M -31 -15 L 31 -15 L 38 4 L 23 26 L 12 42 L 13 52 L 31 65 L 20 75 L 0 78 L -20 75 L -31 65 L -13 52 L -12 42 L -23 26 L -38 4 Z"
    }
    plate(shape)
    if(side){
        plate("M -13 -7 L 15 -7 L 23 4 L 20 18 L 8 26 L -9 19 Z",gold)
        translate(17f,10f){scale(.7f,1f,Offset.Zero){reactor(a,9f,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}}
    }else{
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 5 -7 L ${if(g<2)20 else 28} -8 L ${if(g<2)23 else 31} 6 L 15 22 L 6 16 Z",gold)
            if(g>=2)glowLine(a,19f,17f,11f,28f,1.8f)
        }
        translate(0f,10f){reactor(a,if(g==4)13f else 9f,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)}
    }
    if(g>=1){
        for(y in 30 until h.toInt()-9 step 7)poly("M -10 $y L 10 $y L 8 ${y+5} L -8 ${y+5} Z",dark)
        plate("M -19 ${h-11} L 19 ${h-11} L 18 ${h-4} L -18 ${h-4} Z",gold)
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 10 ${h-8} L 22 ${h-5} L ${if(g>=3)32 else 25} ${h+16} L 17 ${h+22} L 10 ${h+9} Z")
            glowLine(a,18f,h+1f,22f,h+12f)
        }
    }
    if(e.equipped(RobotSlot.CHEST)==RobotGear.ARMOR){plate("M -25 -7 L -13 -11 L -10 17 L -22 24 L -29 6 Z");if(!side)scale(-1f,1f,Offset.Zero){plate("M -25 -7 L -13 -11 L -10 17 L -22 24 L -29 6 Z")}}
}
private fun DrawScope.arm(form:ValkyrieForm,x:Float,angle:Float,elbow:Float,wrist:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
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
            plate(cap,if(far)slate else gold);glowLine(a,-7f,7f,7f,7f,1.5f)
            if(g>=3)plate("M -8 -7 L 9 -9 L 12 -3 L -6 0 Z")
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
private fun DrawScope.leg(form:ValkyrieForm,x:Float,t:Float,s:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,debug:Boolean){
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
private fun DrawScope.valkBack(f:ValkyrieForm,a:Color,side:Boolean,action:RobotAction,phase:Float,gear:RobotGear?){
    if(f.grade==4||f.grade==3){
        val count=if(f.grade==4)3 else 1
        val spread=if(action==RobotAction.HOVER||action==RobotAction.ATTACK||action==RobotAction.JUMP).9f else .18f+.05f*sin(phase*6.28f)
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            for(i in 0 until count)translate(if(side)24f else 30f,i*11f-4f){
                rotate(-(12f+i*28f+spread*(27f+i*10f)),Offset.Zero){
                    val length=if(i==0)112f else 92f-i*10f
                    plate("M -8 -5 L 7 -7 L 20 13 L 15 38 L 6 66 L 0 $length L -10 ${length*.63f} L -15 27 Z",gold)
                    poly("M 1 20 L 13 13 L 10 40 L 0 ${length-4f} L -6 59 Z",a.copy(alpha=.72f),.7f)
                    plate("M -8 0 L 5 -3 L 12 15 L 4 34 L -8 26 Z")
                    glowLine(a,6f,35f,0f,length-12f,2f)
                }
                hinge(a,if(f.grade==4)9f else 7f)
            }
        }
    }
    if(gear!=null)translate(if(side)-20f else 0f,4f){plate("M -14 -6 L 14 -6 L 17 24 L -17 24 Z",gold);glowLine(a,0f,0f,0f,18f)}
}
private fun DrawScope.auraShield(a:Color,side:Boolean,phase:Float){
    translate(if(side)43f else 55f,38f){
        val r=35f+sin(phase*6.28f)*3f
        drawCircle(a.copy(alpha=.12f),r,Offset.Zero)
        drawCircle(a.copy(alpha=.85f),r,Offset.Zero,style=Stroke(2f))
        rotate(phase*60f,Offset.Zero){
            val p=Path()
            for(i in 0..5){val t=i*PI.toFloat()/3f;val x=cos(t)*r*.87f;val y=sin(t)*r*.87f;if(i==0)p.moveTo(x,y)else p.lineTo(x,y)}
            p.close();drawPath(p,a,style=Stroke(1.6f))
        }
        drawCircle(a,9f,Offset.Zero,style=Stroke(1.4f))
    }
}
@Composable
internal fun ValkyriePreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=ValkyrieCatalog.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val view=KageGait.view(requested,action);val side=view!=RobotView.FRONT
    val motion=RobotMotion.pose(action,phase);val bends=KageJointMotion.of(action,phase)
    val gait=if(KageGait.isLocomotion(action))KageGait.pose(KageForm(f.height,f.head,f.shoulder,1f,f.thigh,f.shin,f.upper,f.fore,0f,f.grade),action,phase)else null
    Canvas(modifier){
        val fit=min(size.width/(if(f.grade>=3)350f else 245f),size.height/320f)*f.height
        val ground=size.height*.89f
        val hover=if(action==RobotAction.HOVER&&f.grade>=3)ValkyrieCatalog.hover(phase) else 0f
        drawOval(edge.copy(alpha=.30f),Offset(size.width/2-28f*fit,ground-2f),Size(56f*fit,5f*fit))
        translate(size.width/2,ground){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            val base=f.hips+f.thigh+f.shin+12f
            val nt=gait?.near?.thigh?:motion.nearLeg;val ft=gait?.far?.thigh?:motion.farLeg
            val ns=gait?.near?.shin?:nt+bends.nearKnee;val fs=gait?.far?.shin?:ft+bends.farKnee
            val na=gait?.nearArm?:motion.nearArm;val fa=gait?.farArm?:motion.farArm
            val lean=gait?.lean?:0f
            translate(0f,-base-hover+(gait?.drop?:0f)+(if(gait==null)motion.lift else 0f)){
                rotate(lean,Offset(0f,f.hips)){valkBack(f,a,side,action,phase,e.equipped(RobotSlot.BACK))}
                val boots=e.equipped(RobotSlot.FEET)
                val hipWidth=if(f.grade==0)11f else 15f
                leg(f,if(side)0f else -hipWidth,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,true,a,boots,debug)
                if(side)rotate(lean,Offset(0f,f.hips)){arm(f,0f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug)}
                leg(f,if(side)0f else hipWidth,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,boots,debug)
                rotate(lean,Offset(0f,f.hips)){
                    valkBody(f,side,a,e)
                    if(!side){
                        for(s in listOf(-1f,1f))drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                        arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug)
                    }
                    arm(f,if(side)0f else f.shoulder,if(side)na else RobotMotion.frontArmAngle(na,true),bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand()else RobotSlot.LEFT_HAND),debug)
                    translate(if(side)-2f else 0f,-33f){scale(f.head,f.head,Offset(0f,23f)){valkHead(f.grade,side,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,action==RobotAction.IDLE&&phase>.94f,e.equipped(RobotSlot.HEAD))}}
                    if(debug)translate(0f,-12f){drawCircle(gold,5f,Offset.Zero,style=Stroke(1.4f))}
                }
                if(action==RobotAction.ENERGY_SHIELD&&f.grade>=2)auraShield(a,side,phase)
                if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=.55f*(1f-phase)),14f+phase*25f,Offset(0f,12f),style=Stroke(2f))
                if(action==RobotAction.SCAN)glowLine(a,if(side)30f else 0f,-37f,if(side)75f else 0f,-60f+phase*40f,1.4f)
            }
        }}
    }
}
