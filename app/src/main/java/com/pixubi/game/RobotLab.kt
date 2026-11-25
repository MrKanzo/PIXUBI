package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlin.math.*

internal enum class RobotView(val title: String) { FRONT("Перед"), RIGHT("Праворуч"), LEFT("Ліворуч") }
internal enum class RobotAction(val title: String) {
    IDLE("Спокій"), WALK("Ходьба"), RUN("Біг"), JUMP("Стрибок"), WAVE("Привітання"),
    SLEEP("Сон"), CHARGE("Зарядка"), SCAN("Сканування"), ATTACK("Атака")
}
internal enum class RobotFace(val title: String) {
    NEUTRAL("Спокій"), HAPPY("Радість"), SAD("Сум"), ANGRY("Злість"), SURPRISE("Здивування"), SLEEP("Сон")
}
internal enum class RobotSlot { HEAD, CHEST, LEFT_HAND, RIGHT_HAND, BACK, FEET }
internal enum class RobotModule(val title:String) { NONE("Без модуля"), SCANNER("Сканер"), SHIELD("Щит"), TOOL("Інструмент") }
internal data class RobotLoadout(val accent: Color = Color(0xFF268ED2), val module: RobotModule = RobotModule.NONE)
internal data class RobotPose(val nearLeg:Float, val farLeg:Float, val nearArm:Float, val farArm:Float, val lift:Float)
internal object RobotMotion {
    fun pose(action:RobotAction, phase:Float):RobotPose {
        val t = (phase * 2f * PI).toFloat()
        val s = sin(t)
        return when(action) {
            RobotAction.WALK -> RobotPose(26*s,-26*s,-20*s,20*s, -abs(cos(t))*2)
            RobotAction.RUN -> RobotPose(42*s,-42*s,-36*s,36*s,-abs(cos(t))*6)
            RobotAction.JUMP -> RobotPose(-16f,16f,-30f,30f,-sin(phase*PI.toFloat()).coerceAtLeast(0f)*37)
            RobotAction.WAVE -> RobotPose(0f,0f,145f+15*s,0f,0f)
            RobotAction.ATTACK -> RobotPose(-9f,9f, -80f+25*s,20f,0f)
            RobotAction.SLEEP -> RobotPose(0f,0f,0f,0f,0f)
            else -> RobotPose(0f,0f,0f,0f,0f)
        }
    }
    fun sockets(view:RobotView): Map<RobotSlot,Offset> {
        val front = view == RobotView.FRONT
        val points = if(front) listOf(Offset(100f,32f),Offset(100f,105f),Offset(58f,133f),Offset(142f,133f),Offset(100f,100f),Offset(100f,180f))
        else listOf(Offset(98f,32f),Offset(125f,105f),Offset(100f,133f),Offset(97f,133f),Offset(74f,100f),Offset(111f,180f))
        return RobotSlot.entries.zip(points.map { if(view==RobotView.LEFT) Offset(200f-it.x,it.y) else it }).toMap()
    }
}
private val ink=Color(0xFF142338)
private val shell=Color(0xFFE5EDF4)
private val shade=Color(0xFF9AAEC3)
private val joint=Color(0xFF354960)
private val screen=Color(0xFF0B1B2C)
private val glow=Color(0xFF57E7FF)
private val amber=Color(0xFFFFA444)
private fun DrawScope.panel(x:Float,y:Float,w:Float,h:Float,r:Float,fill:Color) {
    drawRoundRect(ink,Offset(x-1.7f,y-1.7f),Size(w+3.4f,h+3.4f),CornerRadius(r+1.7f))
    drawRoundRect(fill,Offset(x,y),Size(w,h),CornerRadius(r))
}
private fun DrawScope.bolt(x:Float,y:Float,r:Float=4f) {
    drawCircle(ink,r+1,Offset(x,y));drawCircle(amber,r,Offset(x,y));drawCircle(joint,r*0.45f,Offset(x,y))
}
private fun DrawScope.leg(x:Float,y:Float,angle:Float,side:Boolean,far:Boolean,accent:Color) {
    val c=if(far) shade else shell
    rotate(angle,Offset(x,y)) {
        panel(x-6,y,12f,19f,5f,joint)
        bolt(x,y+16,4f)
        panel(x-8,y+19,16f,19f,5f,c)
        if(side) {
            panel(x-10,y+35,29f,12f,5f,c)
            drawRoundRect(accent,Offset(x+5,y+37),Size(12f,6f),CornerRadius(2f))
        } else {
            panel(x-12,y+35,24f,12f,5f,c)
            drawRoundRect(accent,Offset(x-9,y+37),Size(18f,6f),CornerRadius(2f))
        }
    }
}
private fun DrawScope.arm(x:Float,y:Float,angle:Float,far:Boolean,accent:Color,module:RobotModule) {
    val c=if(far) shade else shell
    rotate(angle,Offset(x,y)) {
        bolt(x,y,7f)
        panel(x-7,y+5,14f,17f,5f,joint)
        bolt(x,y+20,4f)
        panel(x-9,y+23,18f,17f,6f,c)
        drawRoundRect(accent,Offset(x-6,y+26),Size(12f,5f),CornerRadius(2f))
        panel(x-7,y+39,14f,10f,4f,joint)
        if(!far && module==RobotModule.SHIELD) {
            panel(x-14,y+19,28f,29f,7f,accent)
            drawCircle(glow,5f,Offset(x,y+33))
        }
        if(!far && module==RobotModule.TOOL) {
            panel(x-5,y+45,10f,16f,3f,shade)
            panel(x-9,y+58,18f,6f,2f,amber)
        }
    }
}
private fun DrawScope.face(x:Float,y:Float,side:Boolean,face:RobotFace,blink:Boolean) {
    val eyes=if(side) listOf(x+3) else listOf(x-12,x+12)
    eyes.forEachIndexed { i,ex ->
        when {
            blink || face==RobotFace.SLEEP -> drawLine(glow,Offset(ex-4,y),Offset(ex+4,y),2.8f,StrokeCap.Round)
            face==RobotFace.ANGRY -> {
                val d=if(i==0) 3f else -3f
                drawLine(glow,Offset(ex-5,y-d),Offset(ex+5,y+d),4f,StrokeCap.Round)
            }
            face==RobotFace.HAPPY -> {
                val p=Path().apply { moveTo(ex-5,y+2); quadraticTo(ex,y-9,ex+5,y+2) }
                drawPath(p,glow,style=Stroke(3f,cap=StrokeCap.Round))
            }
            else -> drawRoundRect(glow,Offset(ex-4,y-6),Size(8f,if(face==RobotFace.SURPRISE) 14f else 11f),CornerRadius(4f))
        }
    }
    val mx=if(side) x+5 else x
    when(face) {
        RobotFace.SURPRISE -> drawCircle(glow,3f,Offset(mx,y+14),style=Stroke(2f))
        RobotFace.SLEEP -> Unit
        else -> {
            val p=Path().apply { moveTo(mx-5,y+12);quadraticTo(mx,y+if(face==RobotFace.SAD) 6f else 19f,mx+5,y+12) }
            drawPath(p,glow,style=Stroke(2f,cap=StrokeCap.Round))
        }
    }
}
private fun DrawScope.robotFront(p:RobotPose,a:Color,m:RobotModule,f:RobotFace,blink:Boolean) {
    leg(85f,133f,p.farLeg*0.12f,false,false,a)
    leg(115f,133f,p.nearLeg*0.12f,false,false,a)
    arm(60f,91f,p.farArm,false,a,RobotModule.NONE)
    arm(140f,91f,-p.nearArm,false,a,m)
    panel(74f,78f,52f,60f,14f,shell)
    panel(80f,126f,40f,9f,4f,shade)
    drawCircle(ink,13f,Offset(100f,104f));drawCircle(a,10f,Offset(100f,104f));drawCircle(glow,6f,Offset(100f,104f))
    panel(94f,70f,12f,13f,4f,joint)
    panel(56f,42f,12f,27f,5f,amber);panel(132f,42f,12f,27f,5f,amber)
    panel(65f,30f,70f,49f,18f,shell)
    panel(72f,39f,56f,33f,12f,screen)
    face(100f,50f,false,f,blink)
    panel(92f,29f,16f,5f,2f,a)
    drawLine(joint,Offset(100f,29f),Offset(104f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(104f,14f));drawCircle(glow,3.5f,Offset(104f,14f))
    if(m==RobotModule.SCANNER) { panel(114f,22f,15f,8f,3f,shade);drawCircle(glow,2.5f,Offset(124f,26f)) }
}
private fun DrawScope.robotSide(p:RobotPose,a:Color,m:RobotModule,f:RobotFace,blink:Boolean) {
    // True right profile: far limbs first, then depth-specific torso and near limbs.
    leg(94f,132f,p.farLeg,true,true,a)
    arm(94f,91f,p.farArm,true,a,RobotModule.NONE)
    leg(102f,133f,p.nearLeg,true,false,a)
    panel(76f,82f,46f,54f,13f,shell)
    panel(70f,91f,10f,34f,4f,shade)
    panel(117f,95f,8f,22f,3f,a)
    drawRoundRect(glow,Offset(121f,100f),Size(3f,12f),CornerRadius(1.5f))
    panel(92f,72f,13f,13f,4f,joint)
    panel(67f,30f,65f,49f,18f,shell)
    panel(112f,39f,23f,33f,9f,screen)
    face(121f,50f,true,f,blink)
    drawCircle(ink,12f,Offset(87f,54f));drawCircle(amber,10f,Offset(87f,54f));drawCircle(a,5f,Offset(87f,54f))
    panel(89f,29f,13f,5f,2f,a)
    drawLine(joint,Offset(95f,29f),Offset(92f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(92f,14f));drawCircle(glow,3.5f,Offset(92f,14f))
    arm(99f,92f,p.nearArm,false,a,m)
    if(m==RobotModule.SCANNER) { panel(112f,23f,17f,8f,3f,shade);drawCircle(glow,2.5f,Offset(126f,27f)) }
}
@Composable
internal fun RobotPreview(view:RobotView, action:RobotAction, face:RobotFace, loadout:RobotLoadout,
                          phase:Float, showSockets:Boolean, modifier:Modifier=Modifier) {
    val p=RobotMotion.pose(action,phase)
    Canvas(modifier) {
        val s=min(size.width/200f,size.height/215f)
        val dx=(size.width-200*s)/2; val dy=(size.height-215*s)/2
        translate(dx,dy) {
            scale(s,s,Offset.Zero) {
                drawOval(ink.copy(alpha=.35f),Offset(63f,181f),Size(74f,8f))
                translate(top=p.lift) {
                    scale(if(view==RobotView.LEFT) -1f else 1f,1f,Offset(100f,100f)) {
                        val actualFace=if(action==RobotAction.SLEEP) RobotFace.SLEEP else face
                        val blink=phase>.94f && action==RobotAction.IDLE
                        if(view==RobotView.FRONT) robotFront(p,loadout.accent,loadout.module,actualFace,blink)
                        else robotSide(p,loadout.accent,loadout.module,actualFace,blink)
                        if(action==RobotAction.SCAN) {
                            val x=if(view==RobotView.FRONT) 100f else 134f
                            drawLine(glow.copy(alpha=.6f),Offset(x,48f),Offset(x+if(view==RobotView.FRONT) 0f else 40f,30f+phase*50),1.5f)
                        }
                        if(action==RobotAction.CHARGE) drawCircle(glow.copy(alpha=.4f*(1-phase)),18f+phase*15,Offset(100f,105f),style=Stroke(2f))
                    }
                    if(showSockets) RobotMotion.sockets(view).values.forEach {
                        drawCircle(amber,3f,it,style=Stroke(1.5f))
                    }
                }
            }
        }
    }
}
@Composable
internal fun RobotLab(onBack:()->Unit) {
    BackHandler(onBack=onBack)
    var view by remember { mutableStateOf(RobotView.FRONT) }
    var action by remember { mutableStateOf(RobotAction.IDLE) }
    var face by remember { mutableStateOf(RobotFace.NEUTRAL) }
    var accent by remember { mutableStateOf(Color(0xFF268ED2)) }
    var module by remember { mutableStateOf(RobotModule.NONE) }
    var sockets by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(true) }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running,action) {
        if(running) {
            var last=0L
            while(true) withFrameNanos { now ->
                if(last!=0L) {
                    val duration=when(action){RobotAction.RUN->480f;RobotAction.WALK->950f;RobotAction.IDLE->3200f;else->1600f}
                    phase=(phase+(now-last).coerceAtMost(50_000_000L)/1_000_000f/duration)%1f
                }
                last=now
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Robot Lab · MK-1",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick=onBack){Text("Назад")}
        }
        Text("Незалежний тестовий стенд. Не змінює збереження гри.")
        Card { RobotPreview(view,action,face,RobotLoadout(accent,module),phase,sockets,
            Modifier.fillMaxWidth().height(300.dp)) }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            RobotView.entries.forEach { v -> FilterChip(selected=view==v,onClick={view=v},label={Text(v.title)}) }
        }
        Text("Рухи")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            RobotAction.entries.forEach { a -> FilterChip(selected=action==a,onClick={action=a;phase=0f},label={Text(a.title)}) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={running=!running}){Text(if(running)"Пауза" else "Відтворити")}
            OutlinedButton(onClick={phase=(phase+0.0625f)%1f;running=false}){Text("Крок кадру")}
        }
        Slider(value=phase,onValueChange={phase=it;running=false})
        Text("Міміка дисплея")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            RobotFace.entries.forEach { f -> FilterChip(selected=face==f,onClick={face=f},label={Text(f.title)}) }
        }
        Text("Модулі · тест сумісності")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            RobotModule.entries.forEach { m -> FilterChip(selected=module==m,onClick={module=m},label={Text(m.title)}) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Синій" to Color(0xFF268ED2),"Зелений" to Color(0xFF29AB83),"Червоний" to Color(0xFFC94A5C)).forEach { (n,c) ->
                FilterChip(selected=accent==c,onClick={accent=c},label={Text(n)})
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Точки кріплення (у нейтральній позі)",Modifier.weight(1f))
            Switch(checked=sockets,onCheckedChange={sockets=it})
        }
        Text("MK-2–MK-4 та адаптивні навички ще не підключені.",style=MaterialTheme.typography.bodySmall)
    }
}
