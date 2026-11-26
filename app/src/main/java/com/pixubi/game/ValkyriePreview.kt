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
// The face and mechanical hair belong to the head rig, including during turns.
private fun DrawScope.valkyrieFace(a:Color,g:Int,side:Boolean,f:RobotFace,blink:Boolean){
    for(x in if(side)listOf(18f)else listOf(-12f,12f)){
        val sign=if(side||x>0f)1f else -1f
        val w=if(g==0)6.2f else 6.8f
        if(blink||f==RobotFace.SLEEP){
            glowLine(a,x-w,1f,x+w,1f,1.8f)
        }else if(f==RobotFace.HAPPY){
            val p=Path().apply{moveTo(x-w,1f);quadraticBezierTo(x,-11f,x+w,1f)}
            drawPath(p,a,style=Stroke(2.7f,cap=StrokeCap.Round))
            drawLine(a,Offset(x+sign*w,0f),Offset(x+sign*(w+2f),-3f),1.3f)
        }else if(f==RobotFace.SURPRISE){
            drawOval(a,Offset(x-5f,-10f),Size(10f,15f),style=Stroke(2.3f))
            drawCircle(Color.White,1.5f,Offset(x,-5f))
        }else if(f==RobotFace.SAD||f==RobotFace.ANGRY){
            val outer=if(f==RobotFace.ANGRY)-8f else -1f
            val inner=if(f==RobotFace.ANGRY)-1f else -8f
            poly("M ${x+sign*w} $outer L ${x-sign*w} $inner Q $x 8 ${x+sign*w} $outer Z",a,.4f)
            glowLine(a,x+sign*w,outer,x-sign*w,inner,1.6f)
        }else{
            val eye=Path().apply{
                moveTo(x-sign*w,-4f);quadraticBezierTo(x,-13f,x+sign*w,-8f)
                quadraticBezierTo(x+sign*(w-1f),6f,x,6f)
                quadraticBezierTo(x-sign*w,5f,x-sign*w,-4f);close()
            }
            drawPath(eye,a.copy(alpha=.93f))
            drawOval(visor,Offset(x-2.4f,-7f),Size(4.8f,9f))
            drawCircle(Color.White,1.9f,Offset(x-1.8f,-5f))
            drawCircle(Color.White.copy(alpha=.75f),.9f,Offset(x+2f,1f))
            glowLine(a,x+sign*(w-1f),-7f,x+sign*(w+2f),-10f,1.2f)
        }
        if(!blink&&f!=RobotFace.SLEEP&&(g<=1||f==RobotFace.HAPPY)){
            drawOval(a.copy(alpha=.24f),Offset(x+sign*3f-3f,8f),Size(6f,3f))
        }
    }
    if(f!=RobotFace.SLEEP){
        val mx=if(side)18f else 0f
        val mouth=Path().apply{
            when(f){
                RobotFace.HAPPY->{moveTo(mx-4f,11f);quadraticBezierTo(mx,16f,mx+4f,11f)}
                RobotFace.SAD->{moveTo(mx-3f,14f);quadraticBezierTo(mx,8f,mx+3f,14f)}
                RobotFace.ANGRY->{moveTo(mx-3f,12f);quadraticBezierTo(mx,9f,mx+3f,12f)}
                else->{moveTo(mx-3f,12f);quadraticBezierTo(mx,13f,mx+3f,12f)}
            }
        }
        if(f==RobotFace.SURPRISE)drawCircle(a,2.5f,Offset(mx,12f),style=Stroke(1.3f))
        else drawPath(mouth,a,style=Stroke(1.7f,cap=StrokeCap.Round))
    }
}
private fun DrawScope.valkHair(g:Int,side:Boolean,a:Color,phase:Float,action:RobotAction){
    val swing=sin(phase*6.28f)*when(action){RobotAction.RUN->12f;RobotAction.WALK->7f;RobotAction.JUMP,RobotAction.HOVER->9f;else->2.4f}
    if(g==0){
        // Two round flower-shaped sensor buns, not cat ears.
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(if(side)26f else 32f,-34f){
                rotate(swing*.35f,Offset.Zero){
                    for(i in 0..4)rotate(i*72f,Offset.Zero){
                        plate("M -5 -4 Q -13 -17 0 -19 Q 13 -17 5 -4 Z",pale,gold)
                    }
                    drawCircle(edge,10f,Offset.Zero);drawCircle(gold,8.7f,Offset.Zero)
                    drawCircle(a,5.5f,Offset.Zero);drawCircle(Color.White,2f,Offset(-1f,-2f))
                }
            }
        }
    }else{
        // Twin bob locks -> swept ponytail -> flowing segmented ceremonial crest.
        val len=when(g){1->39f;2->63f;3->79f;else->92f}
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(if(side)25f else 30f,-23f){
                rotate(swing*(if(side)1f else .55f),Offset.Zero){
                    val flare=if(g>=3)26f else 17f
                    plate("M -8 -14 Q 12 -18 19 -3 Q $flare ${len*.33f} ${flare-3f} ${len*.68f} L 10 $len L -1 ${len*.68f} Q 8 12 -8 -14 Z",pale,gold)
                    poly("M 8 -7 Q ${flare-3f} ${len*.28f} 13 ${len*.67f} L 10 ${len-7f} L 7 ${len*.63f} Q 12 10 8 -7 Z",a.copy(alpha=.72f),.6f)
                    for(i in 1..g) {
                        val y=len*i/(g+1f)
                        glowLine(gold,7f,y,16f,y-3f,1.5f)
                    }
                }
            }
        }
        if(g>=2&&side)translate(-20f,-35f){rotate(swing*.7f,Offset.Zero){
            plate("M -4 -9 Q -25 -19 -37 0 Q -39 19 -55 ${len*.67f} L -38 ${len*.62f} Q -11 20 5 5 Z",gold)
            glowLine(a,-11f,-3f,-32f,21f,2f)
        }}
    }
}
private fun DrawScope.valkHead(g:Int,side:Boolean,a:Color,f:RobotFace,blink:Boolean,gear:RobotGear?,phase:Float,action:RobotAction){
    valkHair(g,side,a,phase,action)
    val path=if(side)when(g){
        0->"M -30 -13 Q -33 -39 -5 -43 Q 25 -43 34 -20 Q 39 -5 30 10 Q 18 26 1 27 Q -30 22 -30 -13 Z"
        1->"M -31 -17 Q -32 -41 -2 -45 Q 27 -43 36 -20 L 34 8 Q 20 26 5 28 Q -23 21 -31 -17 Z"
        2->"M -32 -16 Q -30 -45 2 -47 Q 24 -44 37 -22 L 36 1 Q 29 19 9 29 L -22 17 Z"
        3->"M -34 -18 Q -30 -46 2 -49 L 25 -39 L 41 -20 L 37 5 Q 28 21 10 30 L -24 17 Z"
        else->"M -35 -19 Q -29 -49 2 -51 L 28 -41 L 43 -20 L 38 5 Q 27 23 10 31 L -24 18 Z"
    }else when(g){
        0->"M 0 -42 Q 34 -43 35 -12 Q 38 13 20 23 Q 0 32 -20 23 Q -38 13 -35 -12 Q -34 -43 0 -42 Z"
        1->"M 0 -45 Q 31 -45 36 -18 Q 39 7 21 22 L 0 29 L -21 22 Q -39 7 -36 -18 Q -31 -45 0 -45 Z"
        2->"M 0 -47 Q 27 -47 36 -23 L 37 3 Q 25 20 0 30 Q -25 20 -37 3 L -36 -23 Q -27 -47 0 -47 Z"
        3->"M 0 -49 Q 29 -47 39 -25 L 39 0 L 27 18 Q 12 28 0 31 Q -12 28 -27 18 L -39 0 L -39 -25 Q -29 -47 0 -49 Z"
        else->"M 0 -51 Q 30 -49 41 -23 L 41 -2 L 28 19 Q 12 30 0 32 Q -12 30 -28 19 L -41 -2 L -41 -23 Q -30 -49 0 -51 Z"
    }
    plate(path,pale,gold)
    // Soft tapered porcelain mask; no duplicated eyes or external eye overlays.
    if(side){
        poly("M 8 -29 Q 29 -29 34 -17 Q 37 0 26 13 Q 20 21 11 21 L 6 9 Q 2 -12 8 -29 Z",visor)
        plate("M 5 -33 Q 22 -38 33 -26 L 28 -20 Q 17 -29 7 -24 Z",pale,gold)
        translate(-13f,-1f){hinge(a,if(g<2)9f else 7f)}
        glowLine(a,14f,-33f,27f,-28f,1.6f)
    }else{
        poly("M -28 -17 Q -24 -29 0 -29 Q 24 -29 28 -17 Q 32 4 22 15 Q 0 29 -22 15 Q -32 4 -28 -17 Z",visor)
        // A distinct side-swept fringe evolves into a split crown.
        if(g<=1){
            plate("M -33 -20 Q -32 -43 0 -43 Q 18 -43 31 -31 Q 14 -35 -6 -24 L -22 -18 L -16 -31 Q -27 -26 -33 -20 Z",pale,gold)
            poly("M -25 -30 Q -7 -44 16 -38 L -3 -32 L -16 -25 Z",gold,.6f)
        }else{
            for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
                plate("M 0 -43 Q 23 -48 35 -28 L 39 -17 L 31 -13 L 23 -29 Q 14 -36 0 -34 Z",pale,gold)
                glowLine(a,17f,-37f,29f,-28f,1.5f)
            }
            val crown=if(g==2)9f else if(g==3)15f else 21f
            poly("M -17 -39 L -11 ${-40f-crown*.55f} L 0 ${-40f-crown} L 11 ${-40f-crown*.55f} L 17 -39 L 0 -30 Z",gold)
            poly("M 0 ${-37f-crown*.65f} L 5 -38 L 0 -32 L -5 -38 Z",a,.5f)
        }
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 29 5 L 35 -3 Q 34 14 23 21 L 18 18 Z",gold)
            glowLine(a,30f,6f,24f,14f,1.2f)
        }
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
        0->"M -21 -9 Q 0 -16 21 -9 Q 27 8 23 27 Q 26 43 0 49 Q -26 43 -23 27 Q -27 8 -21 -9 Z"
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
        translate(0f,10f){
            val r=if(g==4)13f else 9f
            if(g>=2)for(i in 0..4)rotate(i*72f,Offset.Zero){plate("M -3 -7 Q -8 -17 0 -20 Q 8 -17 3 -7 Z",pale,gold)}
            reactor(a,r,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
        }
    }
    if(g==0&&!side){
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){plate("M 1 32 Q 15 28 22 35 Q 17 44 0 47 Z",pale,gold)}
    }
    if(g>=1){
        for(y in 30 until h.toInt()-9 step 7)poly("M -10 $y L 10 $y L 8 ${y+5} L -8 ${y+5} Z",dark)
        plate("M -19 ${h-11} L 19 ${h-11} L 18 ${h-4} L -18 ${h-4} Z",gold)
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
            val skirt=when(g){1->14f;2->20f;3->26f;else->32f}
            val flare=if(g>=3)32f else 25f
            plate("M 9 ${h-10} Q 23 ${h-10} 24 ${h+2} Q $flare ${h+skirt*.55f} ${flare-7f} ${h+skirt} L 11 ${h+skirt*.65f} Z",pale,gold)
            poly("M 15 ${h-5} Q 22 ${h+3} ${flare-9f} ${h+skirt-5f} L 16 ${h+skirt*.5f} Z",a.copy(alpha=.65f),.5f)
            if(g>=2)plate("M 17 ${h-9} L 26 ${h-6} L ${flare+2f} ${h+skirt*.5f} L ${flare-1f} ${h+skirt*.73f} Z",gold)
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
                    RobotGear.BLADE_L,RobotGear.BLADE_R,RobotGear.CANNON_L,RobotGear.CANNON_R,RobotGear.RIFLE_L,RobotGear.RIFLE_R,RobotGear.ARC_L,RobotGear.ARC_R,RobotGear.REPAIR_L,RobotGear.REPAIR_R->drawWorldWeapon(gear,a)
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
            plate(if(side)"M -9 -4 L 7 -4 L $toe 7 L $toe 12 L -11 12 L -12 6 Z" else "M -8 -4 L 8 -4 L 12 6 L 10 12 L -10 12 L -12 6 Z",if(far)slate else pale)
            glowLine(a,if(side)-6f else -8f,10f,if(side)toe-2f else 8f,10f)
            if(gear==RobotGear.BOOTS||gear==RobotGear.SPEED_BOOTS)plate(if(side)"M -12 -2 L 10 -2 L ${toe+3} 8 L ${toe+3} 13 L -14 13 Z" else "M -10 -2 L 10 -2 L 14 7 L 12 13 L -12 13 L -14 7 Z",slate)
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
                    plate("M -8 -5 Q 19 -12 25 16 Q 29 48 0 $length Q -22 ${length*.60f} -17 26 Z",pale,gold)
                    poly("M 4 12 Q 19 13 17 35 Q 11 ${length*.66f} 0 ${length-6f} Q -10 ${length*.54f} 4 12 Z",a.copy(alpha=.70f),.7f)
                    plate("M -8 0 Q 8 -7 15 13 Q 10 28 3 34 L -8 26 Z",gold)
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
        val fit=min(size.width/(if(f.grade>=3)350f else 245f),size.height/(if(f.grade>=3)350f else 320f))*f.height
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
                leg(f,if(side)-4f else -hipWidth,if(side)ft else ft*.13f,if(side)fs else fs*.13f,side,side,a,boots,debug)
                if(side)rotate(lean,Offset(0f,f.hips)){arm(f,-3f,fa,bends.farElbow,bends.wrist,true,true,a,e.equipped(view.farHand()),debug)}
                leg(f,if(side)4f else hipWidth,if(side)nt else nt*.13f,if(side)ns else ns*.13f,side,false,a,boots,debug)
                rotate(lean,Offset(0f,f.hips)){
                    valkBody(f,side,a,e)
                    if(!side){
                        for(s in listOf(-1f,1f))drawLine(dark,Offset(s*18f,3f),Offset(s*f.shoulder,3f),9f)
                        arm(f,-f.shoulder,RobotMotion.frontArmAngle(fa,false),-bends.farElbow,bends.wrist,false,false,a,e.equipped(RobotSlot.RIGHT_HAND),debug)
                    }
                    arm(f,if(side)3f else f.shoulder,if(side)na else RobotMotion.frontArmAngle(na,true),bends.nearElbow,bends.wrist,side,false,a,e.equipped(if(side)view.nearHand()else RobotSlot.LEFT_HAND),debug)
                    translate(if(side)-2f else 0f,-33f){scale(f.head,f.head,Offset(0f,23f)){valkHead(f.grade,side,a,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face,action==RobotAction.IDLE&&phase>.94f,e.equipped(RobotSlot.HEAD),phase,action)}}
                    if(debug)translate(0f,-12f){drawCircle(gold,5f,Offset.Zero,style=Stroke(1.4f))}
                }
                if(action==RobotAction.ENERGY_SHIELD&&f.grade>=2)auraShield(a,side,phase)
                if(action==RobotAction.CHARGE)drawCircle(a.copy(alpha=.55f*(1f-phase)),14f+phase*25f,Offset(0f,12f),style=Stroke(2f))
                if(action==RobotAction.SCAN)glowLine(a,if(side)30f else 0f,-37f,if(side)75f else 0f,-60f+phase*40f,1.4f)
            }
        }}
    }
}
