package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class SymbiontForm(val title:String,val grade:Int,val scale:Float,val length:Float,val depth:Float,val leg:Float,val tail:Int,val horns:Int)
internal object SymbiontCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->SymbiontForm("MITE",0,.83f,42f,27f,37f,3,0)
        RobotTier.MK2->SymbiontForm("CRAWLER",1,.91f,57f,32f,46f,5,1)
        RobotTier.MK3->SymbiontForm("STALKER",2,.97f,69f,37f,57f,6,2)
        RobotTier.MK4->SymbiontForm("SYMBIONT",3,1f,79f,41f,64f,7,3)
        RobotTier.OMEGA->SymbiontForm("LEVIATHAN",4,1.04f,88f,44f,70f,9,4)
    }
    // Diagonal trot and four-beat walking. One full cycle returns exactly to its starting pose.
    fun foot(phase:Float,index:Int,run:Boolean):Offset{
        val t=(phase+if(run) (if(index==0||index==3)0f else .5f) else listOf(0f,.5f,.75f,.25f)[index])*2f*PI.toFloat()
        return Offset(cos(t)*(if(run)24f else 13f),-max(0f,sin(t))*(if(run)20f else 9f))
    }
}
private val ink=Color(0xFF0B1920)
private val shell=Color(0xFF243C3D)
private val pearl=Color(0xFFB5D0C8)
private val shade=Color(0xFF536C66)
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
private fun DrawScope.crystal(a:Color,r:Float,h:Float){
    val p=Path().apply{moveTo(0f,-h);lineTo(r,-h*.35f);lineTo(r*.65f,3f);lineTo(-r*.65f,3f);lineTo(-r,-h*.35f);close()}
    drawPath(p,Brush.linearGradient(listOf(Color.White,a,shell),Offset(-r,-h),Offset(r,3f)))
    drawPath(p,ink,style=Stroke(1.1f));line(a,0f,-h+2f,0f,0f,1f)
}
private fun DrawScope.tail(f:SymbiontForm,a:Color,phase:Float,action:RobotAction,side:Boolean){
    var p=Offset(if(side)-f.length+8f else -12f,-f.depth*.1f)
    for(i in 0 until f.tail){
        val t=i/(f.tail-1f).coerceAtLeast(1f)
        val len=13f+f.grade*1.4f
        val swing=sin(phase*6.28f-i*.56f)*(if(action==RobotAction.RUN)17f else 9f)
        val next=if(side) p+Offset(-len*(1f-t*.28f),-sin(t*2.3f)*10f+swing*.22f)
        else p+Offset(-len*.38f+swing*.22f,7f-t*10f)
        val ang=atan2(next.y-p.y,next.x-p.x)*180f/PI.toFloat()
        translate(p.x,p.y){rotate(ang,Offset.Zero){
            val segmentLength=(next-p).getDistance()
            val r=8f*(1f-t*.7f)
            plate("M 0 ${-r} L ${segmentLength*.8f} ${-r*.75f} L $segmentLength 0 L ${segmentLength*.8f} ${r*.75f} L 0 $r Z",if(i%2==0)shade else pearl)
            line(a,segmentLength*.15f,0f,segmentLength*.78f,0f,1.5f)
        }}
        p=next
    }
    if(f.grade>=2)translate(p.x,p.y){rotate(-65f,Offset.Zero){crystal(a,6f,19f+f.grade*3f)}}
}
private fun DrawScope.limb(f:SymbiontForm,a:Color,root:Offset,foot:Offset,front:Boolean,far:Boolean,debug:Boolean,boots:RobotGear?){
    val mid=Offset((root.x+foot.x)*.5f+(if(front)-10f else 12f),(root.y+foot.y)*.5f-5f)
    fun segment(p:Offset,q:Offset,r:Float){
        val v=q-p;val len=v.getDistance()
        translate(p.x,p.y){rotate(atan2(v.y,v.x)*180f/PI.toFloat(),Offset.Zero){
            plate("M 0 ${-r} L ${len*.45f} ${-r*1.25f} L $len ${-r*.55f} L $len ${r*.55f} L ${len*.3f} $r L 0 ${r*.6f} Z",if(far)shell else pearl)
            line(a,len*.15f,0f,len*.8f,0f,1.4f)
        }}
    }
    val r=if(f.grade==0)6f else 7f+f.grade*.6f
    node(a,root,r,debug);segment(root,mid,r);node(a,mid,r*.7f,debug);segment(mid,foot,r*.75f);node(a,foot,r*.55f,debug)
    translate(foot.x,foot.y){
        plate("M -8 -5 L 6 -5 L 13 3 L 11 9 L -10 9 L -13 4 Z",if(far)shell else shade)
        for(i in 0..2)translate(2f+i*4f,7f){plate("M -2 -3 L 4 -2 L 5 4 L -1 3 Z",pearl)}
        if(boots==RobotGear.BOOTS||boots==RobotGear.SPEED_BOOTS)plate("M -12 -4 L 8 -4 L 15 5 L 12 11 L -13 11 Z")
    }
}
private fun DrawScope.wings(f:SymbiontForm,a:Color,phase:Float,action:RobotAction,side:Boolean){
    if(f.grade!=4)return
    val open=if(action==RobotAction.BIO_FLIGHT||action==RobotAction.JUMP||action==RobotAction.ATTACK)1f else .38f
    val wave=sin(phase*6.28f)*if(action==RobotAction.BIO_FLIGHT)15f else 3f
    for(s in listOf(-1f,1f))scale(if(side)s*.65f else s,if(side&&s<0).82f else 1f,Offset.Zero){
        translate(if(side)-5f else 14f,-f.depth*.4f){rotate(-open*28f+wave+(if(side&&s<0)12f else 0f),Offset.Zero){
            val reach=if(side)96f else 103f
            val p=Path().apply{moveTo(0f,0f);quadraticBezierTo(35f,-82f,reach,-70f);lineTo(reach-19f,-21f);lineTo(reach-42f,-34f);lineTo(31f,3f);lineTo(18f,-13f);close()}
            drawPath(p,a.copy(alpha=.18f));drawPath(p,a.copy(alpha=.65f),style=Stroke(1.5f))
            plate("M -7 2 L 24 -50 L 57 -70 L $reach -70 L ${reach-10f} -63 L 56 -61 L 30 -39 L 7 7 Z",shade)
            line(a,0f,0f,27f,-44f,2.6f);line(a,27f,-44f,reach-6f,-68f,2.2f)
            line(a,27f,-44f,reach-19f,-21f,1.6f);line(a,27f,-44f,reach-42f,-34f,1.2f)
        }}
    }
}
private fun DrawScope.body(f:SymbiontForm,a:Color,side:Boolean,phase:Float,grow:Float,gear:RobotGear?){
    val l=if(side)f.length else f.length*.45f
    val h=f.depth
    val shape=when(f.grade){
        0->"M ${-l} 0 Q ${-l} ${-h} 0 ${-h} Q $l ${-h} $l 0 Q $l 22 0 25 Q ${-l} 23 ${-l} 0 Z"
        1->"M ${-l} 0 L ${-l*.7f} ${-h*.8f} L 0 ${-h} L ${l*.74f} ${-h*.75f} L $l 0 L ${l*.64f} 23 L ${-l*.65f} 23 Z"
        else->"M ${-l} 3 L ${-l*.8f} ${-h*.75f} L ${-l*.25f} ${-h} L ${l*.55f} ${-h*.86f} L $l -6 L ${l*.8f} 19 L 0 26 L ${-l*.68f} 21 Z"
    }
    plate(shape,shell)
    val count=if(side)f.grade+3 else 2+f.grade
    for(i in 0 until count){
        val x=-l*.75f+(l*1.5f)*i/(count-1f)
        translate(x,-h*.58f){
            val w=l*.9f/count+6f
            plate("M ${-w} 0 L 0 -12 L $w 0 L ${w*.8f} 17 L 0 24 L ${-w*.8f} 17 Z",if(i%2==0)pearl else shade)
            line(a,-w*.65f,3f,0f,-6f,1.6f);line(a,0f,-6f,w*.65f,3f,1.6f)
        }
        if(f.grade>=2)translate(x,-h*.9f){rotate(if(side)-18f else 0f,Offset.Zero){crystal(a,5f+f.grade,9f+f.grade*4f+grow*15f)}}
    }
    val core=Offset(if(side)l*.42f else 0f,8f)
    drawCircle(ink,11f,core);drawCircle(a.copy(alpha=.22f),15f,core)
    drawCircle(a,8f,core,style=Stroke(2f));drawCircle(a,4f+sin(phase*6.28f),core)
    if(gear==RobotGear.ARMOR||gear==RobotGear.REACTOR)drawCircle(Color.White,12f,core,style=Stroke(1.5f))
}
private fun DrawScope.symbiontEye(a:Color,x:Float,y:Float,face:RobotFace){
    if(face==RobotFace.HAPPY){
        val p=Path().apply{moveTo(x-7f,y+2f);quadraticBezierTo(x,y-8f,x+7f,y+2f)}
        drawPath(p,a,style=Stroke(2.2f,cap=StrokeCap.Round));return
    }
    val inner=if(face==RobotFace.ANGRY)1f else if(face==RobotFace.SAD)-5f else -3f
    val outer=if(face==RobotFace.ANGRY)-6f else if(face==RobotFace.SAD)2f else -3f
    val p=Path().apply{
        moveTo(x-7f,y+inner);quadraticBezierTo(x,y+if(face==RobotFace.SURPRISE)-13f else -6f,x+7f,y+outer)
        quadraticBezierTo(x,y+if(face==RobotFace.SURPRISE)13f else 7f,x-7f,y+inner);close()
    }
    drawPath(p,a);clipPath(p){
        drawOval(ink,Offset(x-1.7f,y-5f),Size(3.4f,11f))
        drawCircle(Color.White,1.4f,Offset(x-3f,y-2f))
    }
}
private fun DrawScope.head(f:SymbiontForm,a:Color,side:Boolean,face:RobotFace,phase:Float,action:RobotAction,gear:RobotGear?){
    val g=f.grade;val sleepy=face==RobotFace.SLEEP||action==RobotAction.SLEEP
    val blink=sleepy||(action==RobotAction.IDLE&&phase>.94f)
    val jaw=if(action==RobotAction.ATTACK)12f*sin(phase*PI.toFloat()).coerceAtLeast(0f) else if(sleepy)0f else when(face){RobotFace.HAPPY->4f;RobotFace.SURPRISE->9f;RobotFace.ANGRY->2f;else->0f}
    if(side){
        val shape=when(g){
            0->"M -25 -11 Q -30 -34 0 -36 Q 28 -36 34 -12 L 39 2 Q 32 22 8 23 L -17 17 Z"
            1->"M -28 -12 L -15 -35 L 15 -37 L 34 -20 L 46 1 L 31 16 L 2 24 L -20 17 Z"
            2->"M -31 -12 L -18 -38 L 15 -35 L 37 -16 L 53 0 L 43 12 L 16 25 L -22 18 Z"
            else->"M -35 -12 L -22 -40 L 17 -38 L 36 -20 L 59 -5 L 56 9 L 24 22 L 2 28 L -24 17 Z"
        }
        if(g<2)for(i in 0..1)translate(-13f+i*9f,-27f){rotate(-16f+sin(phase*6.28f+i)*7f,Offset.Zero){plate("M -6 4 L -12 -24 L 7 -11 L 12 2 Z");line(a,-5f,-15f,4f,-2f)}}
        else for(i in 0 until f.horns)translate(-24f+i*10f,-26f){rotate(-35f+i*12f,Offset.Zero){crystal(a,7f,22f+g*3f)}}
        plate(shape)
        plate("M 0 -25 L 23 -25 L 37 -13 L 28 0 L 6 7 L -5 -6 Z",ink)
        if(blink)line(a,9f,-12f,26f,-12f,2f)
        else{
            symbiontEye(a,17f,-10f,face)
        }
        translate(12f,10f){rotate(jaw,Offset.Zero){plate("M -7 -2 L ${if(g>=2)40 else 23} -2 L ${if(g>=2)28 else 17} 9 L 2 12 L -8 6 Z",shade)
            if(g>=2)for(i in 0..2)translate(12f+i*8f,0f){plate("M -2 -2 L 3 -2 L 1 5 Z",pearl)}
        }}
        drawCircle(ink,2.8f,Offset(if(g>=2)48f else 32f,-2f))
    }else{
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            translate(22f,-24f){rotate(15f+sin(phase*6.28f)*5f,Offset.Zero){
                if(g<2)plate("M -8 8 L -6 -29 L 18 -15 L 14 4 Z")else crystal(a,9f,30f+g*4f)
            }}
        }
        plate(if(g==0)"M 0 -37 Q 34 -36 36 -8 Q 33 25 0 28 Q -33 25 -36 -8 Q -34 -36 0 -37 Z" else "M 0 -40 L 31 -27 L 41 -5 L 25 18 L 0 30 L -25 18 L -41 -5 L -31 -27 Z")
        for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){
            plate("M 5 -20 L 29 -18 L 33 -7 L 22 4 L 6 0 Z",ink)
            if(blink)line(a,10f,-10f,27f,-10f)else{
                symbiontEye(a,18.5f,-10f,face)
            }
        }
        if(g>=2)translate(0f,-30f){crystal(a,6f,15f+g*3f)}
        plate("M -13 5 L 13 5 L 18 17 L 0 25 L -18 17 Z",shade)
        drawOval(ink,Offset(-6f,8f),Size(12f,5f))
        if(sleepy)line(a,-6f,17f,6f,17f,1.4f)
        else if(face==RobotFace.SURPRISE)drawOval(ink,Offset(-4f,13f),Size(8f,9f))
        else if(action==RobotAction.ATTACK)drawOval(ink,Offset(-8f,13f),Size(16f,3f+jaw*.5f))
        else {
            val mouth=Path().apply{moveTo(-7f,17f);quadraticBezierTo(0f,17f+when(face){RobotFace.HAPPY->7f;RobotFace.SAD->-5f;RobotFace.ANGRY->-2f;else->0f},7f,17f)}
            drawPath(mouth,a,style=Stroke(1.5f,cap=StrokeCap.Round))
        }
    }
    if(gear==RobotGear.SCANNER)translate(0f,-36f){plate("M -9 -6 L 9 -6 L 14 2 L -14 2 Z");line(a,-7f,-2f,7f,-2f)}
    if(gear==RobotGear.HELMET)plate("M -23 -26 Q 0 -47 23 -26 L 17 -21 Q 0 -36 -17 -21 Z")
}
@Composable
internal fun SymbiontPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=SymbiontCatalog.of(loadout.tier);val a=loadout.accent;val e=loadout.equipment
    val move=action==RobotAction.WALK||action==RobotAction.RUN
    val view=if(move&&requested==RobotView.FRONT)RobotView.RIGHT else requested
    val side=view!=RobotView.FRONT
    val flight=action==RobotAction.BIO_FLIGHT&&f.grade==4
    val hide=action==RobotAction.BIO_CAMOUFLAGE&&f.grade>=2
    val grow=if(action==RobotAction.BIO_GROW&&f.grade>=3)sin(phase*PI.toFloat()).coerceAtLeast(0f) else 0f
    val heal=action==RobotAction.BIO_REGENERATE&&f.grade>=2
    Canvas(modifier){
        val fit=min(size.width/(if(side)460f else 330f),size.height/245f)*f.scale
        val floor=size.height*.87f
        val lift=when{flight->38f+6f*sin(phase*6.28f);action==RobotAction.JUMP->sin(phase*PI.toFloat()).coerceAtLeast(0f)*35f;else->0f}
        val squat=if(action==RobotAction.SLEEP)f.leg*.42f else 0f
        val bob=if(move)2f*sin(phase*12.56f)else sin(phase*6.28f)*1.4f
        drawOval(ink.copy(alpha=.35f),Offset(size.width/2-72f*fit,floor-2f),Size(144f*fit,6f*fit))
        translate(size.width/2+(if(view==RobotView.RIGHT)20f*fit else if(view==RobotView.LEFT)-20f*fit else 0f),floor){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            translate(0f,-f.leg-9f-lift+squat+bob){
                fun DrawScope.creature(){
                    tail(f,a,phase,action,side)
                    wings(f,a,phase,action,side)
                    val rootY=9f
                    for(i in 0..3){
                        val front=i%2==1;val far=i<2
                        val x=if(side)(if(front)f.length*.67f else -f.length*.66f) else (if(i<2)-1f else 1f)*(if(front)f.length*.38f else f.length*.30f)
                        val step=if(move)SymbiontCatalog.foot(phase,i,action==RobotAction.RUN)else Offset.Zero
                        val root=Offset(x,rootY+if(if(side)far else !front)-4f else 0f)
                        val foot=Offset(x+(if(side)step.x else 0f)+if(flight) (if(front)-10f else 10f) else 0f,f.leg-squat-bob+step.y+(if(if(side)far else !front)-3f else 0f))
                        if(if(side)far else !front)limb(f,a,root,foot,front,true,debug,e.equipped(RobotSlot.FEET))
                    }
                    body(f,a,side,phase,grow,e.equipped(RobotSlot.CHEST))
                    for(i in if(side)listOf(2,3)else listOf(1,3)){
                        val front=i%2==1
                        val x=if(side)(if(front)f.length*.67f else -f.length*.66f) else (if(i<2)-1f else 1f)*f.length*.38f
                        val step=if(move)SymbiontCatalog.foot(phase,i,action==RobotAction.RUN)else Offset.Zero
                        limb(f,a,Offset(x,rootY),Offset(x+(if(side)step.x else 0f),f.leg-squat-bob+step.y),front,false,debug,e.equipped(RobotSlot.FEET))
                    }
                    translate(if(side)f.length*.88f else 0f,-f.depth*.65f-(if(side)0f else 9f)){
                        rotate(if(side&&action==RobotAction.ATTACK)sin(phase*PI.toFloat())*9f else 0f,Offset.Zero){
                            val headScale=if(f.grade==0)1.06f else .90f+f.grade*.035f
                            scale(headScale,headScale,Offset.Zero){head(f,a,side,face,phase,action,e.equipped(RobotSlot.HEAD))}
                        }
                    }
                    if(e.equipped(RobotSlot.BACK)!=null)translate(-f.length*.35f,-f.depth){plate("M -13 -8 L 13 -8 L 18 8 L -18 8 Z");line(a,-8f,0f,8f,0f)}
                    if(e.equipped(RobotSlot.LEFT_HAND)!=null||e.equipped(RobotSlot.RIGHT_HAND)!=null)translate(if(side)f.length*.68f else f.length*.35f,15f){plate("M -7 -6 L 7 -6 L 12 12 L -9 12 Z");line(a,0f,-2f,0f,9f)}
                }
                if(hide){
                    drawContext.canvas.saveLayer(Rect(-300f,-180f,300f,160f),Paint().apply{alpha=.22f+.28f*abs(cos(phase*PI.toFloat()))})
                    try{creature()}finally{drawContext.canvas.restore()}
                }else creature()
                if(heal||action==RobotAction.CHARGE)for(i in 0..13){
                    val t=i*2.4f+phase*6.28f
                    val p=Offset(cos(t)*(f.length+8f),sin(t)*f.depth-18f*phase)
                    drawCircle(a.copy(alpha=.72f),2f+(i%3),p)
                }
                if(action==RobotAction.SCAN)line(a,if(side)f.length+25f else 0f,-f.depth,if(side)f.length+75f else 55f,-f.depth+phase*30f,1.4f)
                if(action==RobotAction.WAVE)for(i in 0..2)drawCircle(a.copy(alpha=.5f-i*.13f),8f+i*5f,Offset(if(side)f.length+44f else 32f,-f.depth-25f),style=Stroke(1.4f))
            }
        }}
    }
}
