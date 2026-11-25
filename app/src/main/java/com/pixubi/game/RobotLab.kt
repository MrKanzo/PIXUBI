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
import androidx.compose.ui.platform.LocalContext
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
internal enum class RobotSlot(val title:String) { HEAD("Голова"), CHEST("Корпус"), LEFT_HAND("Ліва рука"), RIGHT_HAND("Права рука"), BACK("Спина"), FEET("Ноги") }
internal data class RobotLoadout(val accent: Color = Color(0xFF268ED2), val equipment: RobotEquipment = RobotEquipment())
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
    // Front-facing shoulders swing away from the torso, not through it.
    fun frontArmAngle(motion:Float, right:Boolean):Float {
        val outward = when {
            right && abs(motion)>45f -> abs(motion).coerceIn(50f,165f)
            else -> 8f+motion.coerceIn(0f,36f)*0.55f
        }
        return if(right) -outward else outward
    }
    fun sockets(view:RobotView): Map<RobotSlot,Offset> {
        val front = view == RobotView.FRONT
        val points = if(front) listOf(Offset(100f,32f),Offset(100f,105f),Offset(141f,140f),Offset(59f,140f),Offset(100f,100f),Offset(100f,180f))
        else listOf(Offset(98f,32f),Offset(125f,105f),Offset(100f,133f),Offset(97f,133f),Offset(74f,100f),Offset(111f,180f))
        val raw=RobotSlot.entries.zip(points).toMap().toMutableMap()
        if(view==RobotView.LEFT) {
            val left=raw.getValue(RobotSlot.LEFT_HAND)
            raw[RobotSlot.LEFT_HAND]=raw.getValue(RobotSlot.RIGHT_HAND)
            raw[RobotSlot.RIGHT_HAND]=left
        }
        return raw.mapValues { (_,p)->if(view==RobotView.LEFT) Offset(200f-p.x,p.y) else p }
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
private fun DrawScope.leg(x:Float,y:Float,angle:Float,side:Boolean,far:Boolean,accent:Color,gear:RobotGear?=null) {
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
        if(gear==RobotGear.BOOTS || gear==RobotGear.SPEED_BOOTS) {
            val left=x-12; val width=if(side) 33f else 24f
            panel(left,y+31,width,17f,5f,if(far) shade else joint)
            panel(left+3,y+34,width-6,8f,3f,accent)
            if(gear==RobotGear.SPEED_BOOTS) drawLine(glow,Offset(left+3,y+44),Offset(left+width-3,y+44),2f)
        }
    }
}
private fun DrawScope.arm(x:Float,y:Float,angle:Float,far:Boolean,accent:Color,gear:RobotGear?) {
    val c=if(far) shade else shell
    rotate(angle,Offset(x,y)) {
        bolt(x,y,7f)
        panel(x-7,y+5,14f,17f,5f,joint)
        bolt(x,y+20,4f)
        panel(x-9,y+23,18f,17f,6f,c)
        drawRoundRect(accent,Offset(x-6,y+26),Size(12f,5f),CornerRadius(2f))
        panel(x-7,y+39,14f,10f,4f,joint)
        if(gear==RobotGear.SHIELD_L || gear==RobotGear.SHIELD_R) {
            panel(x-14,y+19,28f,29f,7f,accent)
            drawCircle(glow,5f,Offset(x,y+33))
        }
        if(gear==RobotGear.TOOL_L || gear==RobotGear.TOOL_R) {
            panel(x-5,y+45,10f,16f,3f,shade)
            panel(x-9,y+58,18f,6f,2f,amber)
        }
        if(gear==RobotGear.BLASTER_L || gear==RobotGear.BLASTER_R) {
            panel(x-11,y+27,22f,24f,5f,joint)
            panel(x-8,y+48,16f,12f,3f,accent)
            drawRoundRect(glow,Offset(x-5,y+55),Size(10f,3f),CornerRadius(1.5f))
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
private fun DrawScope.robotFront(p:RobotPose,a:Color,e:RobotEquipment,f:RobotFace,blink:Boolean) {
    backGear(e.equipped(RobotSlot.BACK),false,a)
    val boots=e.equipped(RobotSlot.FEET)
    leg(85f,133f,p.farLeg*0.12f,false,false,a,boots)
    leg(115f,133f,p.nearLeg*0.12f,false,false,a,boots)
    // Fixed shoulder mounts overlap the torso shell; the arm pivots stay at their centers.
    panel(63f,87f,22f,13f,5f,joint)
    panel(115f,87f,22f,13f,5f,joint)
    panel(74f,78f,52f,60f,14f,shell)
    panel(80f,126f,40f,9f,4f,shade)
    arm(65f,94f,RobotMotion.frontArmAngle(p.farArm,false),false,a,e.equipped(RobotSlot.RIGHT_HAND))
    arm(135f,94f,RobotMotion.frontArmAngle(p.nearArm,true),false,a,e.equipped(RobotSlot.LEFT_HAND))
    // Stationary bearing caps sit over the rotating arm roots.
    drawCircle(ink,9f,Offset(65f,94f));drawCircle(shade,7.5f,Offset(65f,94f))
    drawCircle(ink,9f,Offset(135f,94f));drawCircle(shade,7.5f,Offset(135f,94f))
    bolt(65f,94f,4f);bolt(135f,94f,4f)
    drawCircle(ink,13f,Offset(100f,104f));drawCircle(a,10f,Offset(100f,104f));drawCircle(glow,6f,Offset(100f,104f))
    chestGear(e.equipped(RobotSlot.CHEST),false,a)
    panel(94f,70f,12f,13f,4f,joint)
    panel(56f,42f,12f,27f,5f,amber);panel(132f,42f,12f,27f,5f,amber)
    panel(65f,30f,70f,49f,18f,shell)
    panel(72f,39f,56f,33f,12f,screen)
    face(100f,50f,false,f,blink)
    panel(92f,29f,16f,5f,2f,a)
    drawLine(joint,Offset(100f,29f),Offset(104f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(104f,14f));drawCircle(glow,3.5f,Offset(104f,14f))
    headGear(e.equipped(RobotSlot.HEAD),false,a)
}
private fun DrawScope.robotSide(p:RobotPose,a:Color,e:RobotEquipment,view:RobotView,f:RobotFace,blink:Boolean) {
    backGear(e.equipped(RobotSlot.BACK),true,a)
    val boots=e.equipped(RobotSlot.FEET)
    // True right profile: far limbs first, then depth-specific torso and near limbs.
    leg(94f,132f,p.farLeg,true,true,a,boots)
    arm(94f,91f,p.farArm,true,a,e.equipped(view.farHand()))
    leg(102f,133f,p.nearLeg,true,false,a,boots)
    panel(76f,82f,46f,54f,13f,shell)
    panel(70f,91f,10f,34f,4f,shade)
    panel(117f,95f,8f,22f,3f,a)
    drawRoundRect(glow,Offset(121f,100f),Size(3f,12f),CornerRadius(1.5f))
    chestGear(e.equipped(RobotSlot.CHEST),true,a)
    panel(92f,72f,13f,13f,4f,joint)
    panel(67f,30f,65f,49f,18f,shell)
    panel(112f,39f,23f,33f,9f,screen)
    face(121f,50f,true,f,blink)
    drawCircle(ink,12f,Offset(87f,54f));drawCircle(amber,10f,Offset(87f,54f));drawCircle(a,5f,Offset(87f,54f))
    panel(89f,29f,13f,5f,2f,a)
    drawLine(joint,Offset(95f,29f),Offset(92f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(92f,14f));drawCircle(glow,3.5f,Offset(92f,14f))
    arm(99f,92f,p.nearArm,false,a,e.equipped(view.nearHand()))
    headGear(e.equipped(RobotSlot.HEAD),true,a)
}
private fun DrawScope.headGear(g:RobotGear?,side:Boolean,a:Color) {
    when(g) {
        RobotGear.SCANNER -> {
            val x=if(side)112f else 114f
            panel(x,22f,17f,8f,3f,shade)
            drawCircle(glow,2.5f,Offset(x+12f,26f))
        }
        RobotGear.HELMET -> {
            if(side) {
                panel(65f,27f,61f,10f,4f,a)
                panel(65f,35f,11f,43f,4f,joint)
            } else {
                panel(63f,26f,74f,10f,4f,a)
                panel(60f,36f,10f,40f,4f,joint)
                panel(130f,36f,10f,40f,4f,joint)
            }
        }
        else -> Unit
    }
}
private fun DrawScope.chestGear(g:RobotGear?,side:Boolean,a:Color) {
    if(g==RobotGear.ARMOR) {
        if(side) { panel(110f,85f,14f,40f,5f,a);panel(117f,97f,8f,19f,3f,joint) }
        else {
            panel(76f,83f,14f,45f,5f,a);panel(110f,83f,14f,45f,5f,a)
            panel(89f,122f,22f,10f,4f,shade)
        }
    }
    if(g==RobotGear.REACTOR) {
        if(side) {
            panel(117f,91f,12f,27f,4f,joint)
            drawRoundRect(glow,Offset(122f,96f),Size(4f,17f),CornerRadius(2f))
        } else {
            drawCircle(ink,15f,Offset(100f,104f))
            drawCircle(amber,12f,Offset(100f,104f))
            drawCircle(glow,8f,Offset(100f,104f))
            drawCircle(shell,3f,Offset(100f,104f))
        }
    }
}
private fun DrawScope.backGear(g:RobotGear?,side:Boolean,a:Color) {
    if(g!=RobotGear.BATTERY && g!=RobotGear.BOOSTER)return
    if(side) {
        panel(57f,84f,24f,48f,6f,joint)
        panel(60f,89f,13f,24f,4f,a)
        drawRoundRect(glow,Offset(63f,93f),Size(7f,15f),CornerRadius(2f))
        if(g==RobotGear.BOOSTER) panel(57f,126f,17f,13f,4f,amber)
    } else {
        for(x in listOf(65f,120f)) {
            panel(x,80f,15f,48f,5f,joint)
            panel(x+3,83f,9f,27f,3f,a)
            if(g==RobotGear.BOOSTER)panel(x,124f,15f,12f,3f,amber)
        }
    }
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
                        if(view==RobotView.FRONT) robotFront(p,loadout.accent,loadout.equipment,actualFace,blink)
                        else robotSide(p,loadout.accent,loadout.equipment,view,actualFace,blink)
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
    val context=LocalContext.current
    val initial=remember(context){RobotLabStorage.load(context)}
    var equipment by remember { mutableStateOf(initial.first) }
    var skin by remember { mutableStateOf(initial.second) }
    var selectedSlot by remember { mutableStateOf(RobotSlot.HEAD) }
    var progress by remember(context) { mutableStateOf(RobotProgressStorage.load(context)) }
    LaunchedEffect(progress){RobotProgressStorage.save(context,progress)}
    var skills by remember(context){mutableStateOf(RobotSkillsStorage.load(context))}
    LaunchedEffect(skills){RobotSkillsStorage.save(context,skills)}
    val stats=remember(equipment,progress.level,skills){RobotBalance.total(RobotTier.MK1,equipment,progress.level,skills.bonuses())}
    val baseStats=remember(progress.level){RobotBalance.total(RobotTier.MK1,RobotEquipment(),progress.level)}
    LaunchedEffect(equipment,skin){RobotLabStorage.save(context,equipment,skin)}
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
        Card { RobotPreview(view,action,face,RobotLoadout(skin.accent,equipment),phase,sockets,
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
        HorizontalDivider()
        RobotStatsPanel(stats,baseStats,progress,onTrain={progress=progress.gain(25)},onReset={progress=RobotProgress()})
        HorizontalDivider()
        RobotSkillsPanel(skills,onActivity={ event ->
            skills=skills.record(event)
            progress=progress.gain(event.robotXp)
            action=event.animation; phase=0f; running=true
        },onReset={skills=RobotSkills()})
        HorizontalDivider()
        Text("Інвентар · ${RobotSlot.entries.count { equipment.equipped(it)!=null }}/6 слотів",style=MaterialTheme.typography.titleMedium)
        Text("Усі деталі доступні в лабораторії. Можна комбінувати та знімати незалежно.")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            RobotSlot.entries.forEach { slot ->
                FilterChip(selected=selectedSlot==slot,onClick={selectedSlot=slot},
                    label={Text(slot.title+if(equipment.equipped(slot)!=null)" ✓" else "")})
            }
        }
        Text(selectedSlot.title+": "+(equipment.equipped(selectedSlot)?.title?:"порожньо"))
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            FilterChip(selected=equipment.equipped(selectedSlot)==null,onClick={equipment=equipment.remove(selectedSlot)},label={Text("Зняти")})
            RobotGear.forSlot(selectedSlot).forEach { g ->
                FilterChip(selected=equipment.equipped(selectedSlot)==g,enabled=RobotBalance.compatible(g,RobotTier.MK1),onClick={equipment=equipment.equip(g)},label={Text(g.title)})
            }
        }
        equipment.equipped(selectedSlot)?.let { gear ->
            Text(RobotBalance.spec(gear).delta.summary(),style=MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick={equipment=RobotEquipment()}){Text("Зняти все")}
        Text("Скін · колір акцентів")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            RobotSkin.entries.forEach { s ->
                FilterChip(selected=skin==s,onClick={skin=s},label={Text(s.title)})
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Точки кріплення (у нейтральній позі)",Modifier.weight(1f))
            Switch(checked=sockets,onCheckedChange={sockets=it})
        }
        Text("MK-2–MK-4, справжні мініігри та автономний пошук ще не підключені.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RobotStatsPanel(stats:RobotStats,base:RobotStats,progress:RobotProgress,onTrain:()->Unit,onReset:()->Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text("Характеристики · MK-1",style=MaterialTheme.typography.titleMedium)
            RobotStat.entries.forEach { stat ->
                val change=stats[stat]-base[stat]
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(stat.title,Modifier.weight(1f))
                    Text(stats[stat].toString(),style=MaterialTheme.typography.titleMedium)
                    if(change!=0) Text("  (${if(change>0)"+" else ""}$change)",color=if(change>0)Color(0xFF69D4A9) else Color(0xFFFFA59C))
                }
            }
            HorizontalDivider()
            Text("Рівень ${progress.level} · ${progress.safeXp} XP",style=MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress={progress.fraction},modifier=Modifier.fillMaxWidth())
            Text(if(progress.remaining==0)"Максимальний рівень" else "До нового рівня: ${progress.remaining} XP")
            Text("Доступний за рівнем ранг: ${progress.nextTier.title}. Модель залишається MK-1; еволюції ще не реалізовано.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onTrain,enabled=progress.remaining>0){Text("Тест +25 XP")}
                OutlinedButton(onClick={confirmReset=true}){Text("Скинути XP")}
            }
            Text("Тестовий XP окремий від гри. Скіни не змінюють показники. Зміни показників від спорядження й навичок. Реальні витрати енергії, бої та швидкість ще не підключені.",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(confirmReset) AlertDialog(onDismissRequest={confirmReset=false},title={Text("Скинути тестовий XP?")},
        text={Text("Обладнання, скіни та збереження основної гри не зміняться.")},
        confirmButton={TextButton(onClick={confirmReset=false;onReset()}){Text("Скинути")}},
        dismissButton={TextButton(onClick={confirmReset=false}){Text("Скасувати")}})
}

@Composable
private fun RobotSkillsPanel(skills:RobotSkills,onActivity:(RobotActivity)->Unit,onReset:()->Unit) {
    var confirmReset by remember{mutableStateOf(false)}
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Адаптивні навички",style=MaterialTheme.typography.titleMedium)
            Text(if(skills.specialties.isEmpty())"Спеціалізація: ще не визначена" else
                "Провідні навички: "+skills.specialties.joinToString(", "){it.title})
            Text("Тестові завершені дії. Кнопки перевіряють розвиток, але не витрачають ресурси і не запускають справжні мініігри.",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                RobotActivity.entries.forEach { event ->
                    OutlinedButton(onClick={onActivity(event)}){Text(event.title+" +"+event.points)}
                }
            }
            RobotSkill.entries.forEach { skill ->
                val lvl=skills.level(skill)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(skill.title,Modifier.weight(1f))
                    Text("Рівень $lvl/${RobotSkills.MAX_LEVEL}")
                }
                LinearProgressIndicator(progress={skills.fraction(skill)},modifier=Modifier.fillMaxWidth())
                Text(skill.description,style=MaterialTheme.typography.bodySmall)
            }
            Text("Сумарні бонуси: "+skills.bonuses().summary(),style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={confirmReset=true}){Text("Скинути тестові навички")}
        }
    }
    if(confirmReset)AlertDialog(onDismissRequest={confirmReset=false},title={Text("Скинути навички?")},
        text={Text("Лише тестові навички. Загальний XP, обладнання та скіни залишаться.")},
        confirmButton={TextButton(onClick={confirmReset=false;onReset()}){Text("Скинути")}},
        dismissButton={TextButton(onClick={confirmReset=false}){Text("Скасувати")}})
}
