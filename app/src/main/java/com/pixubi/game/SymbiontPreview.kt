package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.*

internal data class SymbiontForm(val title:String,val grade:Int,val scale:Float,val length:Float,val depth:Float,val leg:Float,val tail:Int,val horns:Int){
    val frontBulk:Float get()=1.25f+grade*.12f
    val rearBulk:Float get()=.78f-grade*.035f
    val shoulderRise:Float get()=7f+grade*3f
    val rumpDrop:Float get()=4f+grade*1.5f
    val clawReach:Float get()=4f+grade*2f
}
internal object SymbiontCatalog {
    fun of(t:RobotTier)=when(t){
        RobotTier.MK1->SymbiontForm("MITE",0,.83f,42f,27f,37f,3,0)
        RobotTier.MK2->SymbiontForm("CRAWLER",1,.91f,57f,32f,46f,5,1)
        RobotTier.MK3->SymbiontForm("STALKER",2,.97f,69f,37f,57f,6,2)
        RobotTier.MK4->SymbiontForm("SYMBIONT",3,1f,79f,41f,64f,7,3)
        RobotTier.OMEGA->SymbiontForm("LEVIATHAN",4,1.04f,88f,44f,70f,9,4)
    }
    // Both wings grow from the thoracic girdle, never from the canvas origin.
    fun wingRoot(f:SymbiontForm,side:Boolean,far:Boolean):Offset =
        if(side) Offset(f.length*(if(far).27f else .40f),-f.depth*.64f-f.shoulderRise*.50f+(if(far)-4f else 0f))
        else Offset(f.length*.36f*(if(far)-1f else 1f),-f.depth*.61f-f.shoulderRise*.48f)
    fun wingOpening(action:RobotAction):Float=when(action){
        RobotAction.BIO_FLIGHT->1f
        RobotAction.ATTACK,RobotAction.JUMP->.86f
        RobotAction.SLEEP->.12f
        else->.30f
    }
    fun wingAngle(phase:Float,action:RobotAction):Float {
        val spread=wingOpening(action)
        val amplitude=if(action==RobotAction.BIO_FLIGHT)24f else if(action==RobotAction.SLEEP)0f else 2f
        return 39f*(1f-spread)+sin(phase*2f*PI.toFloat())*amplitude
    }
    // Model faces local +X. Grounded paws push toward -X, lifted paws recover toward +X.
    // Mirroring the entire rig handles left-facing movement without reversing the gait.
    fun foot(phase:Float,index:Int,run:Boolean):Offset{
        val offset=if(run){if(index==0||index==3)0f else .5f}
            else when(index){0->0f;1->.75f;2->.5f;else->.25f}
        val p=phase+offset
        val t=p-floor(p)
        val stance=if(run).52f else .72f
        val stride=if(run)24f else 13f
        val lift=if(run)20f else 9f
        return if(t<stance){
            Offset(stride*(1f-2f*t/stance),0f)
        }else{
            val u=(t-stance)/(1f-stance)
            val smooth=u*u*(3f-2f*u)
            Offset(stride*(2f*smooth-1f),-lift*sin(PI.toFloat()*u).coerceAtLeast(0f))
        }
    }
}
private val ink=Color(0xFF080D12)
private val shell=Color(0xFF252E48)
private val pearl=Color(0xFFA7B9D2)
private val shade=Color(0xFF526783)
private val tendon=Color(0xFFA54D67)
private val fang=Color(0xFFE1DCC4)
private fun DrawScope.plate(s:String,c:Color=pearl){
    val p=PathParser().parsePathString(s).toPath();val b=p.getBounds()
    drawPath(p,Brush.linearGradient(listOf(c, c.copy(alpha=.85f).compositeOver(shade),shell),Offset(b.left,b.top),Offset(b.right,b.bottom)))
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
    var p=Offset(if(side)-f.length+12f else 0f,if(side)f.rumpDrop+3f else -f.depth*.1f)
    for(i in 0 until f.tail){
        val t=i/(f.tail-1f).coerceAtLeast(1f)
        val len=13f+f.grade*1.4f
        val swing=sin(phase*6.28f-i*.56f)*(if(action==RobotAction.RUN)17f else 9f)
        val next=if(side) p+Offset(-len*(1f-t*.28f),-sin(t*2.3f)*10f+swing*.22f)
        else p+Offset(-len*(.13f+t*.28f)+swing*.18f,3f-t*8f)
        val ang=atan2(next.y-p.y,next.x-p.x)*180f/PI.toFloat()
        translate(p.x,p.y){rotate(ang,Offset.Zero){
            val segmentLength=(next-p).getDistance()
            val r=(8f+f.grade*.6f)*(1f-t*.72f)
            drawCircle(shell,r*.78f,Offset(segmentLength,0f))
            plate("M ${-r*.28f} ${-r*.74f} L ${segmentLength*.55f} ${-r} L ${segmentLength+r*.23f} ${-r*.45f} L ${segmentLength+r*.23f} ${r*.45f} L ${segmentLength*.55f} $r L ${-r*.28f} ${r*.74f} Z",if(i%2==0)shade else pearl)
            line(a,segmentLength*.15f,0f,segmentLength*.78f,0f,1.5f)
        }}
        p=next
    }
    if(f.grade>=2)translate(p.x,p.y){rotate(if(side)-72f else -36f,Offset.Zero){crystal(a,6f,19f+f.grade*3f)}}
}
private fun DrawScope.limb(f:SymbiontForm,a:Color,root:Offset,foot:Offset,front:Boolean,far:Boolean,side:Boolean,debug:Boolean,boots:RobotGear?){
    val bend=if(side){if(front)-12f-f.grade*2f else 11f}else (if(root.x<0f)-1f else 1f)*(if(front)5f+f.grade else 2f)
    val mid=Offset((root.x+foot.x)*.5f+bend,(root.y+foot.y)*.5f-(if(front)7f else 3f))
    fun segment(p:Offset,q:Offset,r:Float){
        val v=q-p;val len=v.getDistance()
        translate(p.x,p.y){rotate(atan2(v.y,v.x)*180f/PI.toFloat(),Offset.Zero){
            plate("M 0 ${-r} L ${len*.45f} ${-r*1.25f} L $len ${-r*.55f} L $len ${r*.55f} L ${len*.3f} $r L 0 ${r*.6f} Z",if(far)shell else pearl)
            line(a,len*.15f,0f,len*.8f,0f,if(front)1.7f else 1.1f)
            if(front&&f.grade>=1){
                drawLine(tendon,Offset(len*.12f,r*.55f),Offset(len*.85f,r*.32f),2.1f,StrokeCap.Round)
                for(i in 1..3)drawLine(ink,Offset(len*i/4f,-r*.5f),Offset(len*i/4f+3f,r*.32f),1.2f,StrokeCap.Round)
            }
        }}
    }
    val r=(if(f.grade==0)6f else 7f+f.grade*.6f)*(if(front)f.frontBulk else f.rearBulk)
    node(a,root,r,debug);segment(root,mid,r);node(a,mid,r*.7f,debug);segment(mid,foot,r*(if(front).83f else .66f));node(a,foot,r*.55f,debug)
    if(front){
        translate(root.x,root.y){
            plate("M ${-r*1.2f} 0 Q ${-r} ${-r*1.5f} 0 ${-r*1.4f} L ${r*1.25f} ${-r*.6f} L ${r*.95f} ${r*.65f} L 0 ${r*1.1f} Z",if(far)shell else pearl)
            if(f.grade>=2){val sign=if(side) -1f else if(root.x<0f)-1f else 1f;scale(sign,1f,Offset.Zero){plate("M 2 -7 L ${r*1.8f} ${-r*1.8f} L ${r*1.4f} 1 L 5 5 Z",shade)}}
        }
    }
    translate(foot.x,foot.y){
        val w=if(front)11f+f.grade*1.8f else 6f+f.grade*.45f
        val toe=if(side)w+3f else w
        plate("M ${-w*.7f} -5 L ${w*.7f} -5 L $toe 3 L ${w*.8f} 8 L ${-w*.8f} 8 L ${-w} 3 Z",if(far)shell else shade)
        if(boots==RobotGear.BOOTS||boots==RobotGear.SPEED_BOOTS)plate("M ${-w} -4 L $w -4 L ${w+2f} 5 L $w 8 L ${-w} 8 Z")
        for(i in 0 until if(front)3 else 2){
            val x=(i-(if(front)1f else .5f))*w*.65f
            val reach=if(front)f.clawReach else 3f+f.grade*.5f
            if(side)translate(x*.5f,0f){plate("M ${w*.35f} 1 Q ${w+reach} 0 ${w+reach+2f} 8 L ${w*.7f} 7 Z",if(far)shade else fang)}
            else translate(x,0f){plate("M -3 3 Q 0 ${-reach*.45f} 3 3 L 1 9 L -2 8 Z",if(far)shade else fang)}
        }
    }
}
private fun DrawScope.wings(f:SymbiontForm,a:Color,phase:Float,action:RobotAction,side:Boolean){
    if(f.grade!=4)return
    val spread=SymbiontCatalog.wingOpening(action)
    val angle=SymbiontCatalog.wingAngle(phase,action)
    // In profile both wings sweep toward the tail; the far wing is foreshortened.
    // In front view the same anatomy is mirrored about the creature's spine.
    for(far in listOf(true,false)){
        val root=SymbiontCatalog.wingRoot(f,side,far)
        val sx=if(side) -(if(far).72f else 1f) else if(far)-1f else 1f
        val sy=if(side&&far).78f else 1f
        val reach=66f+spread*49f
        val elbow=31f+spread*13f
        val wristY=-35f-spread*30f
        val tipY=-20f-spread*54f
        val lower=13f+spread*18f
        val membrane=if(side&&far)shell else Color(0xFF35383E)
        translate(root.x,root.y){
            scale(sx,sy,Offset.Zero){
                rotate(angle+(if(side&&far)10f else 0f),Offset.Zero){
                    val p=Path().apply{
                        moveTo(0f,0f)
                        quadraticBezierTo(elbow*.45f,wristY*.68f,elbow,wristY)
                        quadraticBezierTo(reach*.72f,tipY-9f,reach,tipY)
                        quadraticBezierTo(reach*.85f,tipY+24f,reach*.82f,lower)
                        quadraticBezierTo(reach*.65f,lower-17f,reach*.56f,lower+5f)
                        quadraticBezierTo(reach*.36f,lower-17f,reach*.28f,lower)
                        quadraticBezierTo(7f,13f,0f,0f);close()
                    }
                    drawPath(p,Brush.linearGradient(listOf(shade.copy(alpha=.94f),membrane,a.copy(alpha=.25f)),Offset(0f,0f),Offset(reach,tipY)))
                    drawPath(p,ink,style=Stroke(2.2f,join=StrokeJoin.Round))
                    val bone=Path().apply{moveTo(0f,0f);quadraticBezierTo(elbow*.45f,wristY*.68f,elbow,wristY);quadraticBezierTo(reach*.72f,tipY-9f,reach,tipY)}
                    drawPath(bone,ink,style=Stroke(7f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                    drawPath(bone,if(side&&far)shade else pearl,style=Stroke(4f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                    for(i in 0..2){
                        val tx=reach*listOf(.28f,.56f,.82f)[i]
                        val ty=lower+if(i==1)5f else 0f
                        val rib=Path().apply{moveTo(elbow,wristY);quadraticBezierTo(tx,wristY*.35f,tx,ty)}
                        drawPath(rib,ink,style=Stroke(3.4f,cap=StrokeCap.Round))
                        drawPath(rib,shade,style=Stroke(1.8f,cap=StrokeCap.Round))
                        drawPath(rib,a.copy(alpha=.36f),style=Stroke(.65f))
                    }
                    node(a,Offset(elbow,wristY),4f)
                    plate("M ${reach-5f} ${tipY+1f} L ${reach+7f} ${tipY-8f} L ${reach+3f} ${tipY+8f} Z",fang)
                }
            }
        }
    }
}
private fun DrawScope.wingGirdle(f:SymbiontForm,a:Color,side:Boolean,debug:Boolean){
    if(f.grade!=4)return
    for(far in if(side)listOf(false)else listOf(true,false)){
        val root=SymbiontCatalog.wingRoot(f,side,far)
        translate(root.x,root.y){
            val sign=if(side||far)-1f else 1f
            scale(sign,1f,Offset.Zero){
                plate("M -8 8 Q -13 -3 -5 -10 L 7 -11 L 15 -19 L 13 -2 L 6 10 Z",shade)
                line(a,-3f,4f,5f,-5f,1.6f)
            }
        }
        node(a,root,5.5f,debug)
    }
}
private fun DrawScope.neck(f:SymbiontForm,a:Color,side:Boolean){
    val x=if(side)f.length*.72f else 0f
    val y=-f.depth*.49f-f.shoulderRise*.54f
    translate(x,y){
        if(side){
            plate("M -23 13 L -18 -12 Q 0 -24 22 -17 L 29 4 L 14 19 L -8 24 Z",shell)
            for(i in 0..2){val xx=-13f+i*10f;plate("M $xx -11 L ${xx+7f} -15 L ${xx+11f} 7 L ${xx+3f} 14 Z",shade)}
            line(a,-13f,9f,17f,3f,1.3f)
        }else{
            plate("M -19 16 L -23 -7 L -14 -19 L 14 -19 L 23 -7 L 19 16 L 0 23 Z",shell)
            for(sign in listOf(-1f,1f))scale(sign,1f,Offset.Zero){plate("M 7 -15 L 18 -9 L 15 12 L 5 15 Z",shade);line(a,10f,-7f,9f,10f,1.2f)}
        }
    }
}
private fun DrawScope.body(f:SymbiontForm,a:Color,side:Boolean,phase:Float,grow:Float,gear:RobotGear?){
    val l=if(side)f.length else f.length*.53f
    val h=f.depth
    val rise=f.shoulderRise
    val drop=f.rumpDrop
    val shape=if(side)
        "M ${-l} $drop Q ${-l*.95f} ${-h*.45f} ${-l*.45f} ${-h*.53f} Q ${l*.05f} ${-h*.92f-rise} ${l*.52f} ${-h-rise} Q ${l*1.07f} ${-h-rise*.6f} ${l*1.06f} -5 L ${l*.83f} 22 L ${l*.25f} 25 L ${-l*.68f} ${18f+drop} Q ${-l} ${20f+drop} ${-l} $drop Z"
    else
        "M 0 ${-h-rise} Q ${l*.9f} ${-h-rise} ${l*1.12f} ${-h*.28f} L ${l*.94f} 15 L ${l*.55f} 25 L 0 28 L ${-l*.55f} 25 L ${-l*.94f} 15 L ${-l*1.12f} ${-h*.28f} Q ${-l*.9f} ${-h-rise} 0 ${-h-rise} Z"
    plate(shape,shell)
    val count=if(side)f.grade+3 else 2+f.grade
    for(i in 0 until count){
        val x=-l*.75f+(l*1.5f)*i/(count-1f)
        val ramp=if(side)(x/l+1f)*.5f else 1f
        val shellY=-h*(.35f+ramp*.33f)-rise*ramp*.55f
        translate(x,shellY){
            val w=(l*.9f/count+6f)*(.7f+ramp*.5f)
            plate("M ${-w} 0 L 0 -12 L $w 0 L ${w*.8f} 17 L 0 24 L ${-w*.8f} 17 Z",if(i%2==0)pearl else shade)
            line(a,-w*.65f,3f,0f,-6f,1.6f);line(a,0f,-6f,w*.65f,3f,1.6f)
        }
        if(f.grade>=1)translate(x,shellY-6f){rotate(if(side)-22f else 0f,Offset.Zero){
            val spike=(7f+f.grade*4f+grow*15f)*(.65f+ramp*.6f)
            plate("M -5 4 L -4 -7 L ${-spike*.25f} ${-spike} L 5 -8 L 7 4 Z",pearl)
            line(a,-1f,0f,-spike*.2f,-spike*.75f,1.2f)
        }}
    }
    for(i in 0..2){
        val x=if(side)l*(.08f+i*.23f) else (i-1)*l*.36f
        val p=Path().apply{moveTo(x,-h*.38f);quadraticBezierTo(x+l*.12f,0f,x-3f,17f)}
        drawPath(p,tendon,style=Stroke(3f+f.grade*.4f,cap=StrokeCap.Round));drawPath(p,ink,style=Stroke(1f))
    }
    val core=Offset(if(side)l*.42f else 0f,8f)
    drawCircle(ink,11f,core);drawCircle(a.copy(alpha=.22f),15f,core)
    drawCircle(a,8f,core,style=Stroke(2f));drawCircle(a,4f+sin(phase*6.28f),core)
    if(gear==RobotGear.ARMOR||gear==RobotGear.REACTOR)drawCircle(Color.White,12f,core,style=Stroke(1.5f))
    if(side){
        val panel=Path().apply{moveTo(-l*.85f,-h*.22f);quadraticBezierTo(-l*.15f,-h*.49f,l*.82f,-h*.62f);lineTo(l*.62f,-h*.34f);quadraticBezierTo(-l*.15f,-h*.14f,-l*.85f,-h*.05f);close()}
        drawPath(panel,Color(0xFF1C304A));drawPath(panel,a.copy(alpha=.8f),style=Stroke(1.3f));for(i in 0..2){val x=-l*.5f+i*l*.37f;line(a,x,-h*.23f,x+7f,-h*.35f,2f)}
    }
    drawVariantArmor(gear,side,a,f.grade,true)
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
    val jaw=if(sleepy)0f else if(action==RobotAction.ATTACK)(18f+g*3f)*sin(phase*PI.toFloat()).coerceAtLeast(0f) else when(face){RobotFace.HAPPY->7f;RobotFace.SURPRISE->14f;RobotFace.ANGRY->6f;else->2f+g*1.1f}
    if(side){
        val shape=when(g){
            0->"M -25 -11 L -17 -34 L 8 -32 L 28 -13 L 49 -1 L 42 7 L 15 18 L -17 17 Z"
            1->"M -28 -12 L -15 -35 L 15 -37 L 34 -20 L 46 1 L 31 16 L 2 24 L -20 17 Z"
            2->"M -31 -12 L -18 -38 L 15 -35 L 37 -16 L 53 0 L 43 12 L 16 25 L -22 18 Z"
            else->"M -35 -12 L -22 -40 L 17 -38 L 36 -20 L 59 -5 L 56 9 L 24 22 L 2 28 L -24 17 Z"
        }
        if(g<2)for(i in 0..1)translate(-13f+i*9f,-27f){rotate(-16f+sin(phase*6.28f+i)*7f,Offset.Zero){plate("M -7 4 L -22 -34 L 10 -15 L 13 2 Z");line(a,-5f,-15f,4f,-2f)}}
        else for(i in 0 until f.horns)translate(-24f+i*10f,-26f){rotate(-35f+i*12f,Offset.Zero){crystal(a,7f,22f+g*3f)}}
        plate(shape)
        plate("M -8 -24 L 22 -29 L 38 -15 L 30 -5 L 4 3 L -10 -7 Z",ink)
        line(a,-3f,-22f,25f,-23f,2.3f)
        if(blink)line(a,9f,-12f,26f,-12f,2f)
        else{
            symbiontEye(a,17f,-10f,face)
        }
        val snout=if(g==0)46f else if(g==1)43f else 51f+g
        val rad=jaw*PI.toFloat()/180f
        val hinge=Offset(12f,10f)
        val lowerTip=hinge+Offset((snout-12f)*cos(rad)+2f*sin(rad),(snout-12f)*sin(rad)-2f*cos(rad))
        val cavity=Path().apply{moveTo(10f,5f);lineTo(snout,3f);lineTo(lowerTip.x,lowerTip.y);quadraticBezierTo(20f,lowerTip.y*.6f+5f,10f,5f);close()}
        drawPath(cavity,ink);drawPath(cavity,tendon,style=Stroke(1.8f))
        translate(12f,10f){rotate(jaw,Offset.Zero){
            plate("M -7 -2 L ${snout-12f} -2 L ${snout-22f} 9 L 2 12 L -8 6 Z",shade)
            if(!sleepy)for(i in 0 until (2+g/2))translate(8f+i*(if(g<2)6f else 9f),-1f){plate("M -2 1 L 3 1 L 0 ${-4f-g} Z",fang)}
        }}
        if(!sleepy)for(i in 0 until (3+g)){
            val tx=15f+(snout-19f)*i/(2+g).toFloat()
            plate("M ${tx-2f} 3 L ${tx+2f} 3 L ${tx+.5f} ${8f+g*.9f} Z",fang)
        }
        if(g>=2){plate("M -4 -24 L 14 -30 L 36 -16 L 28 -13 L 7 -18 Z",shade);for(i in 0..2)drawOval(ink,Offset(-12f+i*4f,1f+i*3f),Size(3f,6f))}
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
            if(g>=2)plate("M 3 -24 L 22 -29 L 36 -17 L 30 -13 L 11 -18 Z",shade)
            if(blink)line(a,10f,-10f,27f,-10f)else{
                symbiontEye(a,18.5f,-10f,face)
            }
        }
        if(g>=2)translate(0f,-30f){plate("M -7 4 L -4 -13 L 0 ${-17f-g*4f} L 5 -12 L 7 4 Z",fang);line(a,0f,-4f,0f,-12f,1.2f)}
        plate("M -13 5 L 13 5 L 18 17 L 0 25 L -18 17 Z",shade)
        drawOval(ink,Offset(-6f,8f),Size(12f,5f))
        if(!sleepy&&g>=1){
            val bottom=25f+jaw*.32f
            val maw=Path().apply{moveTo(-15f,13f);quadraticBezierTo(0f,9f,15f,13f);quadraticBezierTo(10f,bottom-1f,0f,bottom);quadraticBezierTo(-10f,bottom-1f,-15f,13f);close()}
            drawPath(maw,ink);drawPath(maw,tendon,style=Stroke(1.5f))
            clipPath(maw){for(i in -2..2){val x=i*5f;plate("M ${x-2f} 11 L ${x+2f} 11 L $x ${18f+g*.5f} Z",fang);plate("M ${x-2f} ${bottom+1f} L ${x+2f} ${bottom+1f} L $x ${bottom-5f-g*.3f} Z",fang)}}
        }
        if(sleepy)line(a,-6f,17f,6f,17f,1.4f)
        else if(face==RobotFace.SURPRISE&&g==0)drawOval(ink,Offset(-4f,13f),Size(8f,9f))
        else if(action==RobotAction.ATTACK&&g==0)drawOval(ink,Offset(-8f,13f),Size(16f,3f+jaw*.5f))
        else if(g==0) {
            val mouth=Path().apply{moveTo(-7f,17f);quadraticBezierTo(0f,17f+when(face){RobotFace.HAPPY->7f;RobotFace.SAD->-5f;RobotFace.ANGRY->-2f;else->0f},7f,17f)}
            drawPath(mouth,a,style=Stroke(1.5f,cap=StrokeCap.Round))
        }
    }
    if(gear==RobotGear.SCANNER)translate(0f,-36f){plate("M -9 -6 L 9 -6 L 14 2 L -14 2 Z");line(a,-7f,-2f,7f,-2f)}
    if(gear==RobotGear.HELMET)plate("M -23 -26 Q 0 -47 23 -26 L 17 -21 Q 0 -36 -17 -21 Z")
    drawVariantHelmet(gear,side,a,g,true)
}
@Composable
internal fun SymbiontPreview(requested:RobotView,action:RobotAction,face:RobotFace,loadout:RobotLoadout,phase:Float,debug:Boolean,modifier:Modifier){
    val f=SymbiontCatalog.of(loadout.tier);val a=loadout.accent.copy(alpha=.82f).compositeOver(Color(0xFFFF537C));val e=loadout.equipment
    val move=action==RobotAction.WALK||action==RobotAction.RUN
    val view=if(move&&requested==RobotView.FRONT)RobotView.RIGHT else requested
    val side=view!=RobotView.FRONT
    val flight=action==RobotAction.BIO_FLIGHT&&f.grade==4
    val hide=action==RobotAction.BIO_CAMOUFLAGE&&f.grade>=2
    val grow=if(action==RobotAction.BIO_GROW&&f.grade>=3)sin(phase*PI.toFloat()).coerceAtLeast(0f) else 0f
    val heal=action==RobotAction.BIO_REGENERATE&&f.grade>=2
    Canvas(modifier){
        val frameWidth=if(side)if(f.grade==4)510f else 470f else if(f.grade==4)360f else 340f
        val frameHeight=if(f.grade==4){if(flight)405f else 350f}else 285f
        val fit=if(loadout.arena)min(size.height*.45f/(f.leg+f.depth*1.1f+38f+f.grade*4f),size.width*.58f/frameWidth) else min(size.width/frameWidth,size.height/frameHeight)*f.scale
        val floor=size.height*(if(loadout.arena).89f else .87f)
        val lift=when{flight->38f+9f*sin(phase*6.28f);action==RobotAction.JUMP->sin(phase*PI.toFloat()).coerceAtLeast(0f)*35f;else->0f}
        val squat=if(action==RobotAction.SLEEP)f.leg*.42f else 0f
        val bob=if(move)2.5f*sin(phase*12.56f)else sin(phase*6.28f)*2.1f
        val shadowWidth=(if(side)f.length*1.65f else f.length*.95f)*(if(flight).73f else 1f)
        drawOval(ink.copy(alpha=if(flight).19f else .35f),Offset(size.width/2-shadowWidth*fit*.5f,floor-2f),Size(shadowWidth*fit,6f*fit))
        translate(size.width/2+(if(view==RobotView.RIGHT)20f*fit else if(view==RobotView.LEFT)-20f*fit else 0f),floor){scale(fit*(if(view==RobotView.LEFT)-1f else 1f),fit,Offset.Zero){
            translate(0f,-f.leg-9f-lift+squat+bob){
                fun DrawScope.creature(){
                    tail(f,a,phase,action,side)
                    wings(f,a,phase,action,side)
                    fun DrawScope.paw(i:Int){
                        val front=i%2==1
                        val far=if(side)i<2 else !front
                        val sign=if(i<2)-1f else 1f
                        val x=if(side){if(front)f.length*.67f else -f.length*.73f}
                            else sign*f.length*(if(front).46f else .28f)
                        val rootY=if(front)7f-f.shoulderRise else 9f+f.rumpDrop
                        val step=if(move)SymbiontCatalog.foot(phase,i,action==RobotAction.RUN)else Offset.Zero
                        val root=Offset(x,rootY+if(far)-3f else 0f)
                        val spread=if(side){if(front)f.length*.13f else -f.length*.08f}
                            else sign*(if(front)7f+f.grade*2f else 2f)
                        val tuck=if(flight)(if(front)f.leg*.34f else f.leg*.44f)+sin(phase*6.28f)*2f else 0f
                        val foot=Offset(x+spread+(if(side)step.x*(if(front)1f else .72f)else 0f)+(if(flight&&side)(if(front)-16f else 18f)else 0f),
                            f.leg-squat-bob+step.y-tuck+(if(far)-3f else 0f))
                        limb(f,a,root,foot,front,far,side,debug,e.equipped(RobotSlot.FEET))
                    }
                    for(i in if(side)listOf(0,1)else listOf(0,2))paw(i)
                    body(f,a,side,phase,grow,e.equipped(RobotSlot.CHEST))
                    wingGirdle(f,a,side,debug)
                    neck(f,a,side)
                    for(i in if(side)listOf(2,3)else listOf(1,3))paw(i)
                    translate(if(side)f.length*.90f else 0f,-f.depth*.68f-f.shoulderRise*.75f-(if(side)0f else 5f)){
                        rotate(if(side&&action==RobotAction.ATTACK)sin(phase*PI.toFloat())*9f else if(side&&flight)-4f+sin(phase*6.28f)*2f else 0f,Offset.Zero){
                            val headScale=if(f.grade==0).96f else .88f+f.grade*.025f
                            scale(headScale,headScale,Offset.Zero){head(f,a,side,face,phase,action,e.equipped(RobotSlot.HEAD))}
                        }
                    }
                    if(e.equipped(RobotSlot.BACK)!=null)translate(-f.length*.35f,-f.depth){plate("M -13 -8 L 13 -8 L 18 8 L -18 8 Z");line(a,-8f,0f,8f,0f)}
                    for(slot in listOf(RobotSlot.LEFT_HAND,RobotSlot.RIGHT_HAND)){
                        val gear=e.equipped(slot)?:continue
                        val far=slot==RobotSlot.LEFT_HAND
                        val x=if(side)f.length*.63f+(if(far)-10f else 4f) else f.length*.46f*(if(far)-1f else 1f)
                        translate(x,if(side&&far)4f else 14f){
                            scale(if(side&&far).76f else 1f,if(side&&far).76f else 1f,Offset.Zero){
                                if(gear in listOf(RobotGear.BLADE_L,RobotGear.BLADE_R,RobotGear.CANNON_L,RobotGear.CANNON_R,RobotGear.RIFLE_L,RobotGear.RIFLE_R,RobotGear.ARC_L,RobotGear.ARC_R,RobotGear.REPAIR_L,RobotGear.REPAIR_R)) rotate(if(side)-75f else 0f,Offset.Zero){drawWorldWeapon(gear,a)}
                                else if(gear==RobotGear.SHIELD_L||gear==RobotGear.SHIELD_R){plate("M -10 -10 L 10 -10 L 13 3 L 0 14 L -13 3 Z",shade);line(a,-5f,-2f,5f,-2f)}
                                else if(gear==RobotGear.BLASTER_L||gear==RobotGear.BLASTER_R){plate("M -7 -7 L 9 -7 L 14 1 L 9 9 L -7 9 Z",shade);line(a,8f,0f,15f,0f)}
                                else {plate("M -7 -6 L 7 -6 L 9 10 L -9 10 Z",shade);line(a,0f,-2f,0f,7f)}
                            }
                        }
                    }
                }
                if(hide){
                    drawContext.canvas.saveLayer(Rect(-320f,-250f,320f,180f),Paint().apply{alpha=.22f+.28f*abs(cos(phase*PI.toFloat()))})
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
