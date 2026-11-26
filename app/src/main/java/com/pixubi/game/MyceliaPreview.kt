package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class MyceliaForm(val title:String,val grade:Int,val scale:Float,val length:Float,val depth:Float,val leg:Float,val tails:Int,val blooms:Int)
internal object MyceliaCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->MyceliaForm("PIXIE",0,.84f,33f,22f,28f,1,0)
        RobotTier.MK2->MyceliaForm("FLORA",1,.92f,45f,25f,43f,1,2)
        RobotTier.MK3->MyceliaForm("NYMPH",2,.98f,53f,29f,59f,2,3)
        RobotTier.MK4->MyceliaForm("MYCELIA",3,1f,62f,32f,67f,2,5)
        RobotTier.OMEGA->MyceliaForm("AETHER",4,1.02f,68f,35f,73f,3,6)
    }
    fun foot(phase:Float,index:Int,run:Boolean):Offset {
        val p=(phase+if(run)(if(index==0||index==3)0f else .5f) else listOf(0f,.5f,.75f,.25f)[index])*2f*PI.toFloat()
        return Offset(cos(p)*(if(run)25f else 12f),-max(0f,sin(p))*(if(run)25f else 12f))
    }
}
private val ink=Color(0xFF39274E)
private val shell=Color(0xFF716187)
private val pearl=Color(0xFFFFF2FF)
private val shade=Color(0xFFC7B0E5)
private fun DrawScope.plate(s:String,c:Color=pearl){
    val p=PathParser().parsePathString(s).toPath();val b=p.getBounds()
    drawPath(p,Brush.linearGradient(listOf(c,shade,shell),Offset(b.left,b.top),Offset(b.right,b.bottom)))
    drawPath(p,ink,style=Stroke(1.6f,join=StrokeJoin.Round))
}
private fun DrawScope.line(a:Color,x:Float,y:Float,u:Float,v:Float,w:Float=2f){
    drawLine(a.copy(alpha=.18f),Offset(x,y),Offset(u,v),w+4f,StrokeCap.Round)
    drawLine(a,Offset(x,y),Offset(u,v),w,StrokeCap.Round)
}
private fun DrawScope.node(a:Color,p:Offset,r:Float,debug:Boolean=false){
    drawCircle(ink,r+1f,p);drawCircle(shade,r,p);drawCircle(a,r*.37f,p)
    if(debug)drawCircle(Color(0xFFFFCC61),r+3f,p,style=Stroke(1f))
}
private val cyan=Color(0xFF83E7F4)
private fun DrawScope.petal(a:Color,w:Float,h:Float){
    val p=Path().apply{moveTo(0f,0f);cubicTo(-w,-h*.35f,-w*.65f,-h*.82f,0f,-h);cubicTo(w*.65f,-h*.82f,w,-h*.35f,0f,0f);close()}
    drawPath(p,Brush.linearGradient(listOf(pearl,a.copy(alpha=.86f),shade),Offset(0f,-h),Offset(0f,0f)))
    drawPath(p,ink,style=Stroke(.9f));line(cyan,0f,-2f,0f,-h*.8f,1f)
}
private fun DrawScope.flower(a:Color,r:Float,phase:Float){
    for(i in 0..4)rotate(i*72f+phase*22f,Offset.Zero){petal(a,r*.6f,r)}
    drawCircle(a.copy(alpha=.3f),r*.44f,Offset.Zero)
    drawCircle(cyan,r*.22f,Offset.Zero);drawCircle(Color.White,r*.10f,Offset.Zero)
}
private fun DrawScope.tendril(a:Color,dx:Float,dy:Float,phase:Float,leaf:Float){
    val tip=Offset(dx+sin(phase*6.28f)*5f,dy+cos(phase*6.28f)*3f)
    val p=Path().apply{moveTo(0f,0f);cubicTo(dx*.65f,-10f,dx*.25f,dy*.85f,tip.x,tip.y)}
    drawPath(p,cyan.copy(alpha=.23f),style=Stroke(4f,cap=StrokeCap.Round))
    drawPath(p,cyan,style=Stroke(1.5f,cap=StrokeCap.Round))
    translate(tip.x,tip.y){rotate(if(dx<0)-35f else 35f,Offset.Zero){petal(a,leaf*.5f,leaf)}}
}
private fun DrawScope.tails(f:MyceliaForm,a:Color,side:Boolean,phase:Float,action:RobotAction){
    val len=when(f.grade){0->36f;1->60f;2->78f;3->94f;else->114f}
    val sw=sin(phase*6.28f)*(if(action==RobotAction.RUN)10f else 5f)
    for(i in 0 until f.tails){
        val spread=(i-(f.tails-1)*.5f)*18f
        val end=if(side)Offset(-len, -len*.30f+spread+sw)else Offset((i-(f.tails-1)*.5f)*31f-30f,-len*.48f+sw)
        translate(if(side)-f.length*.77f else -12f,5f){
            val p=Path().apply{
                moveTo(0f,-7f);cubicTo(end.x*.3f,-len*.22f,end.x*.62f,end.y-28f,end.x,end.y)
                cubicTo(end.x*.8f,end.y+14f,end.x*.25f,16f,0f,7f);close()
            }
            drawPath(p,Brush.linearGradient(listOf(shade,a,pearl),Offset.Zero,end))
            drawPath(p,ink,style=Stroke(1.2f))
            val vein=Path().apply{moveTo(0f,0f);quadraticBezierTo(end.x*.5f,end.y-8f,end.x,end.y)}
            drawPath(vein,cyan,style=Stroke(1.3f))
            if(f.grade>=1)translate(end.x*.55f,end.y*.55f){tendril(a,-12f,-23f,phase+i*.16f,9f)}
        }
    }
}
private fun DrawScope.leg(f:MyceliaForm,a:Color,root:Offset,foot:Offset,front:Boolean,far:Boolean,debug:Boolean,boots:RobotGear?){
    val mid=Offset((root.x+foot.x)*.5f+(if(front)-5f else 10f),(root.y+foot.y)*.5f-3f)
    val r=if(f.grade==0)5.8f else 5f+f.grade*.38f
    fun seg(p:Offset,q:Offset,w:Float){
        val v=q-p;val len=v.getDistance()
        translate(p.x,p.y){rotate(atan2(v.y,v.x)*180f/PI.toFloat(),Offset.Zero){
            plate("M 0 ${-w} Q ${len*.4f} ${-w*1.5f} $len ${-w*.42f} L $len ${w*.42f} Q ${len*.45f} ${w*1.2f} 0 $w Z",if(far)shade else pearl)
            line(a,len*.22f,0f,len*.77f,0f,1.1f)
        }}
    }
    node(a,root,r,debug);seg(root,mid,r);node(a,mid,r*.59f,debug);seg(mid,foot,r*.6f)
    translate(foot.x,foot.y){
        plate(if(f.grade==0)"M -7 -3 Q 6 -6 10 3 Q 10 8 1 9 L -7 8 Q -11 4 -7 -3 Z" else "M -5 -5 L 5 -5 L 9 5 Q 1 10 -8 6 Z",if(far)shade else pearl)
        line(cyan,-4f,5f,5f,5f,1.4f)
        if(boots==RobotGear.BOOTS||boots==RobotGear.SPEED_BOOTS)plate("M -9 -4 L 8 -4 L 11 6 L -10 8 Z",shade)
    }
}
private fun DrawScope.wings(f:MyceliaForm,a:Color,phase:Float,action:RobotAction,side:Boolean){
    if(f.grade!=4)return
    val flap=sin(phase*6.28f)*(if(action==RobotAction.BIO_FLIGHT)19f else 4f)
    for(s in listOf(-1f,1f))scale(if(side)s*.62f else s,if(side&&s<0).82f else 1f,Offset.Zero){
        translate(if(side)-6f else 13f,-f.depth*.6f){rotate((if(side)22f else 36f)+flap,Offset.Zero){
            for(i in 0..1)rotate(i*35f,Offset.Zero){
                val h=if(i==0)104f else 79f
                val w=if(i==0)39f else 35f
                val p=Path().apply{moveTo(0f,0f);cubicTo(-w,-h*.2f,-w,-h*.68f,0f,-h);cubicTo(w*1.3f,-h*.83f,w,-h*.25f,0f,0f);close()}
                drawPath(p,Brush.linearGradient(listOf(cyan.copy(alpha=.37f),a.copy(alpha=.50f),pearl.copy(alpha=.18f)),Offset(-w,-h),Offset(w,0f)))
                drawPath(p,a.copy(alpha=.85f),style=Stroke(1.5f))
                line(cyan,0f,0f,0f,-h*.9f,1.3f)
                for(j in 1..3){val y=-h*j/4f;line(pearl,0f,y,-w*.5f,y-8f,.8f);line(pearl,0f,y,w*.55f,y-9f,.8f)}
            }
        }}
    }
}
private fun DrawScope.body(f:MyceliaForm,a:Color,side:Boolean,phase:Float,bloom:Float,gear:RobotGear?){
    val l=if(side)f.length else f.length*.43f;val h=f.depth
    plate("M ${-l} 0 Q ${-l} ${-h*.9f} ${-l*.35f} ${-h} Q ${l*.4f} ${-h*.7f} $l ${-h*.35f} Q ${l+4f} 15 ${l*.5f} 22 Q 0 12 ${-l*.6f} 20 Q ${-l-4f} 16 ${-l} 0 Z")
    for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
        translate(l*.56f,0f){rotate(s*16f,Offset.Zero){petal(a,12f,22f+f.grade*3f)}}
    }
    for(i in 0 until f.blooms){
        val x=if(side)-l*.77f+i*l*1.22f/(f.blooms-1f).coerceAtLeast(1f) else (i-(f.blooms-1)*.5f)*9f
        val y=-h*.75f
        translate(x,y){
            if(f.grade>=3&&i%2==0){
                val tall=18f+i*2f+bloom*17f
                line(cyan,0f,0f,0f,-tall,2.2f)
                translate(0f,-tall){
                    plate("M -15 0 Q -11 -17 0 -18 Q 11 -17 15 0 Q 0 7 -15 0 Z",a)
                    for(j in -2..2)line(pearl,j*4f,0f,j*3f,-4f,.7f)
                }
            }else flower(a,8f+f.grade*2f+bloom*6f,phase)
        }
    }
    if(f.grade>=2)translate(if(side)l*.67f else 0f,7f){flower(a,10f+f.grade,phase)}
    else drawCircle(cyan,5f,Offset(if(side)l*.67f else 0f,7f))
    if(gear==RobotGear.ARMOR||gear==RobotGear.REACTOR)drawCircle(cyan,13f,Offset(0f,0f),style=Stroke(1.5f))
}
private fun DrawScope.myceliaEye(a:Color,x:Float,y:Float,face:RobotFace){
    if(face==RobotFace.HAPPY){
        val p=Path().apply{moveTo(x-7f,y+2f);quadraticBezierTo(x,y-9f,x+7f,y+2f)}
        drawPath(p,ink,style=Stroke(2.2f,cap=StrokeCap.Round));return
    }
    val w=8f
    val top=when(face){RobotFace.ANGRY->-2f;RobotFace.SAD->-5f;RobotFace.SURPRISE->-9f;else->-7f}
    val p=Path().apply{
        moveTo(x-w,y+if(face==RobotFace.ANGRY)1f else 0f)
        quadraticBezierTo(x,y+top*2f,x+w,y+if(face==RobotFace.SAD)3f else if(face==RobotFace.ANGRY)-4f else 0f)
        quadraticBezierTo(x,y+14f,x-w,y+if(face==RobotFace.ANGRY)1f else 0f);close()
    }
    drawPath(p,a);clipPath(p){
        val pw=if(face==RobotFace.SURPRISE)4f else 6f
        drawOval(ink,Offset(x-pw*.5f,y-5f),Size(pw,12f))
        drawCircle(Color.White,1.8f,Offset(x-3f,y-3f))
    }
}
private fun DrawScope.myceliaMouth(x:Float,y:Float,face:RobotFace){
    if(face==RobotFace.SURPRISE){drawOval(ink,Offset(x-2.8f,y-2f),Size(5.6f,6f));return}
    val p=Path().apply{moveTo(x-4f,y);quadraticBezierTo(x,y+when(face){RobotFace.HAPPY->6f;RobotFace.SAD->-5f;RobotFace.ANGRY->-2f;else->0f},x+4f,y)}
    drawPath(p,ink,style=Stroke(1.4f,cap=StrokeCap.Round))
}
private fun DrawScope.head(f:MyceliaForm,a:Color,side:Boolean,face:RobotFace,phase:Float,action:RobotAction,gear:RobotGear?){
    val g=f.grade;val blink=face==RobotFace.SLEEP||action==RobotAction.SLEEP||(action==RobotAction.IDLE&&phase>.94f)
    val ear=if(g==0)37f else 32f+g*2f
    for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){
        translate(if(side)15f else 19f,-18f){rotate(25f+sin(phase*6.28f)*6f,Offset.Zero){
            petal(a,15f,ear)
            if(g>=2)rotate(26f,Offset.Zero){petal(a,9f,ear*.74f)}
        }}
    }
    if(g>=1){
        for(s in if(side)listOf(-1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){translate(5f,-24f){tendril(a,22f+g*3f,-30f-g*4f,phase+s*.12f,9f)}}
    }
    if(side){
        plate(if(g==0)"M -23 -12 Q -25 -32 1 -34 Q 27 -32 31 -10 L 35 1 Q 27 20 5 24 Q -25 19 -23 -12 Z" else "M -25 -9 Q -22 -35 4 -34 Q 22 -30 28 -15 L 44 -3 L 38 9 L 17 19 L -9 21 Q -27 8 -25 -9 Z")
        drawOval(shade,Offset(6f,-18f),Size(22f,17f))
        if(blink)line(ink,9f,-10f,25f,-10f,1.8f)else{
            myceliaEye(a,17f,-10f,face)
        }
        drawCircle(ink,2.8f,Offset(if(g==0)30f else 39f,0f))
        myceliaMouth(25f,10f,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face)
    }else{
        plate(if(g==0)"M 0 -35 Q 34 -34 35 -6 Q 31 24 0 28 Q -31 24 -35 -6 Q -34 -34 0 -35 Z" else "M 0 -36 Q 27 -34 34 -13 Q 37 12 18 23 L 0 28 L -18 23 Q -37 12 -34 -13 Q -27 -34 0 -36 Z")
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            if(blink)line(ink,6f,-8f,24f,-8f,1.7f)else{
                drawOval(shade,Offset(5f,-19f),Size(21f,20f))
                myceliaEye(a,15.5f,-10f,face)
            }
            drawOval(a.copy(alpha=.25f),Offset(17f,4f),Size(9f,4f))
        }
        drawOval(ink,Offset(-4f,7f),Size(8f,5f))
        myceliaMouth(0f,15f,if(action==RobotAction.SLEEP)RobotFace.SLEEP else face)
    }
    if(g>=2)translate(if(side)-7f else 0f,-28f){flower(a,9f+g*1.5f,phase)}
    if(g==4)translate(0f,-65f){drawOval(cyan,Offset(-24f,-6f),Size(48f,12f),style=Stroke(1.6f))}
    if(gear==RobotGear.SCANNER)translate(0f,-33f){plate("M -10 -5 L 10 -5 L 14 2 L -14 2 Z");line(a,-7f,-2f,7f,-2f)}
    if(gear==RobotGear.HELMET)plate("M -25 -26 Q 0 -48 25 -26 L 19 -20 Q 0 -35 -19 -20 Z")
}
@Composable
internal fun MyceliaPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=MyceliaCatalog.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val move=action==RobotAction.WALK||action==RobotAction.RUN
    val view=if(move&&requested==RobotView.FRONT)RobotView.RIGHT else requested
    val side=view!=RobotView.FRONT
    val flight=action==RobotAction.BIO_FLIGHT&&f.grade==4
    val bloom=if(action==RobotAction.MYCO_BLOOM)sin(phase*PI.toFloat()).coerceAtLeast(0f)else 0f
    val heal=action==RobotAction.BIO_REGENERATE
    Canvas(modifier){
        val fit=min(size.width/(if(side)390f else 310f),size.height/(if(f.grade==4)310f else 245f))*f.scale
        val floor=size.height*.88f
        val lift=when{flight->36f+5f*sin(phase*6.28f);action==RobotAction.JUMP->32f*sin(phase*PI.toFloat()).coerceAtLeast(0f);else->0f}
        val squat=if(action==RobotAction.SLEEP)f.leg*.46f else 0f
        val bob=sin(phase*6.28f*(if(move)2f else 1f))*1.6f
        drawOval(ink.copy(alpha=.28f),Offset(size.width/2-54f*fit,floor-2f),Size(108f*fit,5f*fit))
        translate(size.width/2+(if(side&&view==RobotView.RIGHT)24f*fit else if(side)-24f*fit else 0f),floor){
            scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
                translate(0f,-f.leg-9f-lift+squat+bob){
                    tails(f,a,side,phase,action);wings(f,a,phase,action,side)
                    fun DrawScope.paw(i:Int){
                        val front=i%2==1;val far=i<2
                        val x=if(side)(if(front)f.length*.62f else -f.length*.67f)
                            else (if(far)-1f else 1f)*(if(front)f.length*.37f else f.length*.29f)
                        val step=if(move)MyceliaCatalog.foot(phase,i,action==RobotAction.RUN)else Offset.Zero
                        val y=if(if(side)far else !front)-4f else 0f
                        val root=Offset(x,6f+y)
                        val foot=Offset(x+(if(side)step.x else 0f)+(if(flight&&side)(if(front)-12f else 9f)else 0f),f.leg-squat-bob+step.y+y-(if(flight)8f else 0f))
                        leg(f,a,root,foot,front,if(side)far else !front,debug,e.equipped(RobotSlot.FEET))
                    }
                    if(side){paw(0);paw(1)}else{paw(0);paw(2)}
                    body(f,a,side,phase,bloom,e.equipped(RobotSlot.CHEST))
                    if(side){paw(2);paw(3)}else{paw(1);paw(3)}
                    val neck=if(f.grade==0)9f else 12f+f.grade*5f
                    val hx=if(side)f.length*.82f else 0f
                    val hy=-f.depth*.65f-neck
                    translate(hx,hy+neck*.5f){
                        plate("M -10 ${-neck*.6f} L 9 ${-neck*.6f} Q 16 4 10 ${neck*.7f} L -13 ${neck*.7f} Z")
                        line(cyan,0f,-neck*.4f,0f,neck*.55f,1.7f)
                    }
                    translate(hx,hy){
                        rotate(if(side)sin(phase*6.28f)*1.8f else 0f,Offset.Zero){
                            val hs=if(f.grade==0)1.02f else .84f+f.grade*.025f
                            scale(hs,hs,Offset.Zero){head(f,a,side,face,phase,action,e.equipped(RobotSlot.HEAD))}
                        }
                    }
                    if(e.equipped(RobotSlot.BACK)!=null)translate(-f.length*.3f,-f.depth){plate("M -11 -7 L 11 -7 L 14 8 L -14 8 Z",shade);line(cyan,-7f,0f,7f,0f)}
                    for(slot in listOf(RobotSlot.LEFT_HAND,RobotSlot.RIGHT_HAND)){
                        val gear=e.equipped(slot)?:continue
                        val far=slot==RobotSlot.LEFT_HAND
                        val x=if(side)f.length*.56f+(if(far)-10f else 4f) else f.length*.46f*(if(far)-1f else 1f)
                        translate(x,if(side&&far)4f else 14f){
                            scale(if(side&&far).76f else 1f,if(side&&far).76f else 1f,Offset.Zero){
                                if(gear==RobotGear.BLADE_L||gear==RobotGear.BLADE_R||gear==RobotGear.CANNON_L||gear==RobotGear.CANNON_R) rotate(if(side)-75f else 0f,Offset.Zero){drawWorldWeapon(gear,a)}
                                else if(gear==RobotGear.SHIELD_L||gear==RobotGear.SHIELD_R){plate("M -10 -10 L 10 -10 L 13 3 L 0 14 L -13 3 Z",shade);line(a,-5f,-2f,5f,-2f)}
                                else if(gear==RobotGear.BLASTER_L||gear==RobotGear.BLASTER_R){plate("M -7 -7 L 9 -7 L 14 1 L 9 9 L -7 9 Z",shade);line(a,8f,0f,15f,0f)}
                                else {plate("M -7 -6 L 7 -6 L 9 10 L -9 10 Z",shade);line(a,0f,-2f,0f,7f)}
                            }
                        }
                    }
                    if(heal||bloom>0f||action==RobotAction.CHARGE)for(i in 0..15){
                        val t=i*2.4f+phase*6.28f;val r=35f+f.length*.6f
                        translate(cos(t)*r,sin(t)*f.depth-25f*phase){rotate(t*57.3f,Offset.Zero){petal(a,2f,5f+(i%3)*1.5f)}}
                    }
                    if(action==RobotAction.WAVE)translate(hx+if(side)35f else 20f,hy-28f){flower(a,7f+3f*sin(phase*6.28f),phase)}
                    if(action==RobotAction.SCAN)line(cyan,hx,hy,hx+if(side)65f else 40f,hy-20f+phase*35f,1.4f)
                }
            }
        }
    }
}
