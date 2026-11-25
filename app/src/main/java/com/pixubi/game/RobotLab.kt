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
import androidx.compose.ui.graphics.Brush
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
internal data class RobotLoadout(val accent: Color = Color(0xFF268ED2), val equipment: RobotEquipment = RobotEquipment(), val tier:RobotTier=RobotTier.MK1, val series:RobotSeries=RobotSeries.CLASSIC)
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
// Faceted plating, drawn inside the same limb rotation as existing equipment.
private fun DrawScope.omegaPlate(fill:Color,vertices:List<Offset>) {
    if(vertices.size<3)return
    val shape=Path().apply {
        moveTo(vertices[0].x,vertices[0].y)
        vertices.drop(1).forEach{lineTo(it.x,it.y)}
        close()
    }
    drawPath(shape,fill)
    drawPath(shape,ink,style=Stroke(1.8f,join=androidx.compose.ui.graphics.StrokeJoin.Round))
}
private fun DrawScope.leg(x:Float,y:Float,angle:Float,side:Boolean,far:Boolean,accent:Color,gear:RobotGear?=null,tier:RobotTier=RobotTier.MK1) {
    val style=RobotChassisCatalog.of(tier)
    val c=if(far) style.shade else style.shell
    rotate(angle,Offset(x,y)) {
        panel(x-6,y,12f,19f,5f,joint)
        bolt(x,y+16,4f)
        panel(x-8-style.limbWidth,y+19,16f+2*style.limbWidth,19f,if(tier==RobotTier.MK1)5f else 3f,c)
        if(tier!=RobotTier.MK1) {
            panel(x-5,y+22,10f,10f,2f,style.shade)
            drawLine(accent,Offset(x-3,y+24),Offset(x+3,y+30),2f,StrokeCap.Round)
        }
        if(side) {
            panel(x-10-style.limbWidth/2,y+35,29f+style.limbWidth,12f,if(tier==RobotTier.MK1)5f else 3f,c)
            drawRoundRect(accent,Offset(x+5,y+37),Size(12f,6f),CornerRadius(2f))
        } else {
            panel(x-12-style.limbWidth/2,y+35,24f+style.limbWidth,12f,if(tier==RobotTier.MK1)5f else 3f,c)
            drawRoundRect(accent,Offset(x-9,y+37),Size(18f,6f),CornerRadius(2f))
        }
        if(tier==RobotTier.OMEGA) {
            omegaPlate(c,listOf(Offset(x-12,y+19),Offset(x+12,y+19),Offset(x+14,y+29),Offset(x+8,y+36),Offset(x-8,y+36),Offset(x-14,y+29)))
            omegaPlate(accent,listOf(Offset(x-5,y+22),Offset(x+5,y+22),Offset(x+6,y+30),Offset(x,y+34),Offset(x-6,y+30)))
            drawLine(glow,Offset(x,y+23),Offset(x,y+30),2.6f,StrokeCap.Round)
        }
        if(gear==RobotGear.BOOTS || gear==RobotGear.SPEED_BOOTS) {
            val left=x-12; val width=if(side) 33f else 24f
            panel(left,y+31,width,17f,5f,if(far) shade else joint)
            panel(left+3,y+34,width-6,8f,3f,accent)
            if(gear==RobotGear.SPEED_BOOTS) drawLine(glow,Offset(left+3,y+44),Offset(left+width-3,y+44),2f)
        }
    }
}
private fun DrawScope.arm(x:Float,y:Float,angle:Float,far:Boolean,accent:Color,gear:RobotGear?,tier:RobotTier=RobotTier.MK1) {
    val style=RobotChassisCatalog.of(tier)
    val c=if(far) style.shade else style.shell
    rotate(angle,Offset(x,y)) {
        bolt(x,y,7f)
        panel(x-7,y+5,14f,17f,5f,joint)
        bolt(x,y+20,4f)
        panel(x-9-style.limbWidth,y+23,18f+2*style.limbWidth,17f,if(tier==RobotTier.MK1)6f else 3f,c)
        if(tier!=RobotTier.MK1) {
            panel(x-7,y+8,14f,8f,3f,style.shade)
            drawLine(accent,Offset(x-4,y+10),Offset(x+4,y+10),2f,StrokeCap.Round)
        }
        drawRoundRect(accent,Offset(x-6,y+26),Size(12f,5f),CornerRadius(2f))
        panel(x-7,y+39,14f,10f,4f,joint)
        if(tier==RobotTier.OMEGA) {
            omegaPlate(c,listOf(Offset(x-10,y+3),Offset(x+10,y+3),Offset(x+13,y+12),Offset(x+5,y+18),Offset(x-5,y+18),Offset(x-13,y+12)))
            omegaPlate(c,listOf(Offset(x-13,y+23),Offset(x+13,y+23),Offset(x+15,y+32),Offset(x+9,y+39),Offset(x-9,y+39),Offset(x-15,y+32)))
            panel(x-6,y+25,12f,9f,2f,accent)
            drawLine(glow,Offset(x-3,y+29),Offset(x+3,y+29),2.5f,StrokeCap.Round)
        }
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
private fun DrawScope.face(x:Float,y:Float,side:Boolean,face:RobotFace,blink:Boolean,light:Color=glow) {
    val eyes=if(side) listOf(x+3) else listOf(x-12,x+12)
    eyes.forEachIndexed { i,ex ->
        when {
            blink || face==RobotFace.SLEEP -> drawLine(light,Offset(ex-4,y),Offset(ex+4,y),2.8f,StrokeCap.Round)
            face==RobotFace.ANGRY -> {
                val d=if(i==0) 3f else -3f
                drawLine(light,Offset(ex-5,y-d),Offset(ex+5,y+d),4f,StrokeCap.Round)
            }
            face==RobotFace.HAPPY -> {
                val p=Path().apply { moveTo(ex-5,y+2); quadraticTo(ex,y-9,ex+5,y+2) }
                drawPath(p,light,style=Stroke(3f,cap=StrokeCap.Round))
            }
            else -> drawRoundRect(light,Offset(ex-4,y-6),Size(8f,if(face==RobotFace.SURPRISE) 14f else 11f),CornerRadius(4f))
        }
    }
    val mx=if(side) x+5 else x
    when(face) {
        RobotFace.SURPRISE -> drawCircle(light,3f,Offset(mx,y+14),style=Stroke(2f))
        RobotFace.SLEEP -> Unit
        else -> {
            val p=Path().apply { moveTo(mx-5,y+12);quadraticTo(mx,y+if(face==RobotFace.SAD) 6f else 19f,mx+5,y+12) }
            drawPath(p,light,style=Stroke(2f,cap=StrokeCap.Round))
        }
    }
}
private fun DrawScope.robotFront(p:RobotPose,a:Color,e:RobotEquipment,f:RobotFace,blink:Boolean,tier:RobotTier) {
    val style=RobotChassisCatalog.of(tier)
    backGear(e.equipped(RobotSlot.BACK),false,a)
    val boots=e.equipped(RobotSlot.FEET)
    leg(85f,133f,p.farLeg*0.12f,false,false,a,boots,tier)
    leg(115f,133f,p.nearLeg*0.12f,false,false,a,boots,tier)
    // Fixed shoulder mounts overlap the torso shell; the arm pivots stay at their centers.
    panel(63f,87f,22f,13f,5f,joint)
    panel(115f,87f,22f,13f,5f,joint)
    panel(74f,78f,52f,60f,style.bodyRadius,style.shell)
    chassisBody(tier,false,a)
    panel(80f,126f,40f,9f,4f,shade)
    arm(65f,94f,RobotMotion.frontArmAngle(p.farArm,false),false,a,e.equipped(RobotSlot.RIGHT_HAND),tier)
    arm(135f,94f,RobotMotion.frontArmAngle(p.nearArm,true),false,a,e.equipped(RobotSlot.LEFT_HAND),tier)
    // Stationary bearing caps sit over the rotating arm roots.
    drawCircle(ink,9f,Offset(65f,94f));drawCircle(shade,7.5f,Offset(65f,94f))
    drawCircle(ink,9f,Offset(135f,94f));drawCircle(shade,7.5f,Offset(135f,94f))
    bolt(65f,94f,4f);bolt(135f,94f,4f)
    drawCircle(ink,13f,Offset(100f,104f));drawCircle(a,10f,Offset(100f,104f));drawCircle(glow,6f,Offset(100f,104f))
    if(tier==RobotTier.OMEGA)omegaCore(false,a,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
    chestGear(e.equipped(RobotSlot.CHEST),false,a)
    panel(94f,70f,12f,13f,4f,joint)
    panel(56f,42f,12f,27f,5f,amber);panel(132f,42f,12f,27f,5f,amber)
    panel(65f,30f,70f,49f,style.headRadius,style.shell)
    panel(72f,39f,56f,33f,if(tier==RobotTier.MK1)12f else style.headRadius.coerceAtLeast(4f),screen)
    chassisHead(tier,false,a)
    face(100f,50f,false,f,blink)
    panel(92f,29f,16f,5f,2f,a)
    drawLine(joint,Offset(100f,29f),Offset(104f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(104f,14f));drawCircle(glow,3.5f,Offset(104f,14f))
    if(tier==RobotTier.OMEGA)omegaCrown(false,a)
    headGear(e.equipped(RobotSlot.HEAD),false,a)
}
private fun DrawScope.robotSide(p:RobotPose,a:Color,e:RobotEquipment,view:RobotView,f:RobotFace,blink:Boolean,tier:RobotTier) {
    val style=RobotChassisCatalog.of(tier)
    backGear(e.equipped(RobotSlot.BACK),true,a)
    val boots=e.equipped(RobotSlot.FEET)
    // True right profile: far limbs first, then depth-specific torso and near limbs.
    leg(94f,132f,p.farLeg,true,true,a,boots,tier)
    arm(94f,91f,p.farArm,true,a,e.equipped(view.farHand()),tier)
    leg(102f,133f,p.nearLeg,true,false,a,boots,tier)
    panel(76f,82f,46f,54f,if(tier==RobotTier.MK1)13f else style.bodyRadius,style.shell)
    chassisBody(tier,true,a)
    panel(70f,91f,10f,34f,4f,shade)
    panel(117f,95f,8f,22f,3f,a)
    drawRoundRect(glow,Offset(121f,100f),Size(3f,12f),CornerRadius(1.5f))
    if(tier==RobotTier.OMEGA)omegaCore(true,a,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
    chestGear(e.equipped(RobotSlot.CHEST),true,a)
    panel(92f,72f,13f,13f,4f,joint)
    panel(67f,30f,65f,49f,style.headRadius,style.shell)
    panel(112f,39f,23f,33f,if(tier==RobotTier.MK1)9f else style.headRadius.coerceAtMost(9f),screen)
    chassisHead(tier,true,a)
    face(121f,50f,true,f,blink)
    drawCircle(ink,12f,Offset(87f,54f));drawCircle(amber,10f,Offset(87f,54f));drawCircle(a,5f,Offset(87f,54f))
    panel(89f,29f,13f,5f,2f,a)
    drawLine(joint,Offset(95f,29f),Offset(92f,16f),4f,StrokeCap.Round)
    drawCircle(ink,5f,Offset(92f,14f));drawCircle(glow,3.5f,Offset(92f,14f))
    arm(99f,92f,p.nearArm,false,a,e.equipped(view.nearHand()),tier)
    if(tier==RobotTier.OMEGA)omegaCrown(true,a)
    headGear(e.equipped(RobotSlot.HEAD),true,a)
}
private fun DrawScope.omegaCore(side:Boolean,a:Color,upgraded:Boolean) {
    if(side) {
        omegaPlate(shell,listOf(Offset(114f,88f),Offset(126f,94f),Offset(132f,104f),Offset(126f,116f),Offset(114f,121f)))
        drawCircle(ink,7f,Offset(125f,104f))
        drawCircle(if(upgraded)amber else a,5f,Offset(125f,104f))
        drawCircle(glow,3f,Offset(126f,104f))
    } else {
        omegaPlate(shell,listOf(Offset(77f,84f),Offset(94f,91f),Offset(100f,96f),Offset(106f,91f),Offset(123f,84f),Offset(123f,112f),Offset(100f,127f),Offset(77f,112f)))
        omegaPlate(joint,listOf(Offset(81f,91f),Offset(91f,96f),Offset(91f,111f),Offset(81f,106f)))
        omegaPlate(joint,listOf(Offset(119f,91f),Offset(109f,96f),Offset(109f,111f),Offset(119f,106f)))
        drawCircle(ink,16f,Offset(100f,104f))
        drawCircle(if(upgraded)amber else a,14f,Offset(100f,104f))
        drawCircle(glow,11f,Offset(100f,104f),style=Stroke(3f))
        drawCircle(screen,8f,Offset(100f,104f))
        drawCircle(glow,6f,Offset(100f,104f))
        drawCircle(Color.White,3f,Offset(100f,104f))
    }
}
private fun DrawScope.omegaCrown(side:Boolean,a:Color) {
    if(side) {
        omegaPlate(shell,listOf(Offset(71f,37f),Offset(49f,17f),Offset(75f,25f),Offset(91f,36f)))
        omegaPlate(a,listOf(Offset(70f,34f),Offset(59f,23f),Offset(81f,33f)))
        drawCircle(glow,5f,Offset(87f,54f),style=Stroke(2f))
    } else {
        for(sign in listOf(-1f,1f)) {
            omegaPlate(shell,listOf(Offset(100f+25f*sign,37f),Offset(100f+47f*sign,15f),Offset(100f+38f*sign,38f),Offset(100f+31f*sign,43f)))
            omegaPlate(a,listOf(Offset(100f+31f*sign,36f),Offset(100f+41f*sign,23f),Offset(100f+35f*sign,38f)))
        }
        omegaPlate(amber,listOf(Offset(93f,31f),Offset(107f,31f),Offset(104f,40f),Offset(100f,43f),Offset(96f,40f)))
    }
}
private fun DrawScope.omegaWings(side:Boolean,a:Color,phase:Float,action:RobotAction,light:Color=glow) {
    val pulse=(0.5f+0.5f*sin(phase*2f*PI.toFloat())).coerceIn(0f,1f)
    val power=if(action==RobotAction.SLEEP).22f else .52f+.28f*pulse
    if(side) {
        drawCircle(ink,24f,Offset(69f,86f))
        drawCircle(joint,21f,Offset(69f,86f))
        drawCircle(a,18f,Offset(69f,86f),style=Stroke(5f))
        drawCircle(light.copy(alpha=power),18f,Offset(69f,86f),style=Stroke(2f))
        for(i in 0..2) {
            rotate((-12f+i*34f)+(pulse-0.5f)*6f,Offset(66f,86f)) {
                omegaPlate(joint,listOf(Offset(68f,76f),Offset(37f,47f),Offset(11f,40f),Offset(27f,68f),Offset(66f,91f)))
                omegaPlate(light.copy(alpha=power),listOf(Offset(61f,79f),Offset(19f,45f),Offset(31f,66f),Offset(62f,87f)))
                drawLine(light.copy(alpha=power),Offset(24f,48f),Offset(61f,83f),1.4f)
            }
        }
    } else {
        drawCircle(joint,37f,Offset(100f,83f))
        drawCircle(a,33f,Offset(100f,83f),style=Stroke(5f))
        drawCircle(light.copy(alpha=power),33f,Offset(100f,83f),style=Stroke(2f))
        for(sign in listOf(-1f,1f)) {
            val root=Offset(100f+26f*sign,87f)
            for(i in 0..2) {
                rotate(sign*(i*29f+(pulse-.5f)*6f),root) {
                    omegaPlate(joint,listOf(Offset(root.x,root.y-8f),Offset(100f+63f*sign,47f),Offset(100f+88f*sign,35f),Offset(100f+74f*sign,65f),Offset(root.x,root.y+8f)))
                    omegaPlate(light.copy(alpha=power),listOf(Offset(root.x+5f*sign,root.y),Offset(100f+81f*sign,42f),Offset(100f+67f*sign,64f)))
                    drawLine(light.copy(alpha=power),Offset(100f+76f*sign,46f),Offset(root.x+7f*sign,root.y-2f),1.5f)
                }
            }
        }
    }
}
private fun DrawScope.chassisBody(tier:RobotTier,side:Boolean,a:Color) {
    if(tier==RobotTier.MK1)return
    val style=RobotChassisCatalog.of(tier)
    if(side) {
        panel(80f,85f,18f,8f,2f,style.shade)
        panel(80f,122f,32f,10f,3f,style.shade)
        if(tier>=RobotTier.MK3) {
            panel(72f,87f,12f,38f,3f,style.shade)
            for(y in listOf(94f,104f,114f))drawLine(a,Offset(76f,y),Offset(80f,y),2f)
        }
        if(tier==RobotTier.MK4) panel(102f,83f,16f,9f,2f,a)
    } else {
        panel(80f,80f,40f,9f,3f,style.shade)
        panel(84f,122f,32f,10f,3f,style.shade)
        if(tier>=RobotTier.MK3) {
            for(x in listOf(77f,117f)) {
                panel(x,94f,6f,29f,2f,style.shade)
                drawLine(a,Offset(x+3,98f),Offset(x+3,118f),2f)
            }
        }
        if(tier==RobotTier.MK4) {
            panel(79f,82f,14f,9f,2f,a)
            panel(107f,82f,14f,9f,2f,a)
            drawLine(glow,Offset(90f,118f),Offset(110f,118f),2f)
        }
    }
}
private fun DrawScope.chassisHead(tier:RobotTier,side:Boolean,a:Color) {
    if(tier==RobotTier.MK1)return
    val style=RobotChassisCatalog.of(tier)
    if(side) {
        panel(70f,32f,35f,7f,2f,style.shade)
        if(tier>=RobotTier.MK3) {
            panel(65f,41f,12f,25f,3f,style.shade)
            drawLine(a,Offset(69f,46f),Offset(69f,61f),2f)
        }
        if(tier==RobotTier.MK4) {
            panel(65f,69f,43f,8f,2f,style.shade)
            drawLine(glow,Offset(74f,73f),Offset(99f,73f),2f)
        }
    } else {
        panel(69f,31f,20f,7f,2f,style.shade)
        panel(111f,31f,20f,7f,2f,style.shade)
        if(tier>=RobotTier.MK3) {
            panel(61f,46f,10f,25f,3f,style.shade)
            panel(129f,46f,10f,25f,3f,style.shade)
            drawLine(a,Offset(66f,50f),Offset(66f,65f),2f)
            drawLine(a,Offset(134f,50f),Offset(134f,65f),2f)
        }
        if(tier==RobotTier.MK4) {
            panel(72f,73f,56f,6f,2f,style.shade)
            drawLine(glow,Offset(84f,75f),Offset(116f,75f),2f)
        }
    }
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
private val companionShell=Color(0xFFF5F0FA)
private val companionShade=Color(0xFF9C8FAF)
private val companionLight=Color(0xFFFFA9E6)

private fun DrawScope.companionShape(fill:Color,path:Path) {
    drawPath(path,fill)
    drawPath(path,ink,style=Stroke(2.1f,join=androidx.compose.ui.graphics.StrokeJoin.Round))
}
private fun DrawScope.companionHeart(x:Float,y:Float,r:Float,a:Color,upgraded:Boolean) {
    val heart=Path().apply {
        moveTo(x,y+r)
        cubicTo(x-r*1.5f,y-r*.05f,x-r*1.2f,y-r*1.15f,x,y-r*.48f)
        cubicTo(x+r*1.2f,y-r*1.15f,x+r*1.5f,y-r*.05f,x,y+r)
        close()
    }
    companionShape(if(upgraded)companionLight else a,heart)
    drawPath(heart,companionLight,style=Stroke(1.4f))
}
private fun DrawScope.companionHair(tier:RobotTier,side:Boolean,a:Color,phase:Float,action:RobotAction) {
    val d=CompanionCatalog.of(tier)
    if(d.hairLength==0f)return
    val sway=sin(phase*2f*PI.toFloat())*(if(action==RobotAction.RUN)9f else if(action==RobotAction.SLEEP)0f else 3f)
    for(sign in if(side)listOf(-1f) else listOf(-1f,1f)) {
        val x=if(side)76f else 100f+31f*sign
        val y=39f
        rotate(sign*sway,Offset(x,y)) {
            val len=d.hairLength
            val hair=Path().apply {
                moveTo(x,y)
                cubicTo(x+sign*22f,y-8f,x+sign*(if(side)38f else 26f),y+len*.6f,x+sign*24f,y+len)
                quadraticTo(x+sign*10f,y+len*.78f,x+sign*9f,y+len*.42f)
                quadraticTo(x+sign*4f,y+8f,x,y)
                close()
            }
            companionMetal(hair)
            val tip=Path().apply {
                moveTo(x+sign*14f,y+len*.52f)
                quadraticTo(x+sign*30f,y+len*.74f,x+sign*24f,y+len)
                quadraticTo(x+sign*15f,y+len*.84f,x+sign*14f,y+len*.52f);close()
            }
            companionShape(a,tip)
            val stripe=Path().apply {
                moveTo(x+sign*9f,y+7f)
                quadraticTo(x+sign*23f,y+len*.45f,x+sign*24f,y+len-4f)
            }
            drawPath(stripe,a,style=Stroke(4f,cap=StrokeCap.Round))
            if(tier>=RobotTier.MK4)drawPath(stripe,companionLight,style=Stroke(1.7f,cap=StrokeCap.Round))
            drawCircle(ink,9f,Offset(x,y))
            drawCircle(a,7f,Offset(x,y))
            drawCircle(companionShell,4f,Offset(x,y))
        }
    }
}
// Companion has its own sculpted parts, not scaled Classic arms/legs.
private fun DrawScope.companionMetal(path:Path,far:Boolean=false) {
    drawPath(path,Brush.linearGradient(listOf(if(far)companionShade else Color.White,
        if(far)Color(0xFF655F80) else companionShade),Offset(65f,30f),Offset(155f,175f)))
    drawPath(path,ink,style=Stroke(1.6f,join=androidx.compose.ui.graphics.StrokeJoin.Round))
}
private fun DrawScope.companionJoint(x:Float,y:Float,r:Float,a:Color) {
    drawCircle(ink,r+1.3f,Offset(x,y))
    drawCircle(joint,r,Offset(x,y))
    drawCircle(companionShade,r*.66f,Offset(x,y))
    drawCircle(a,r*.33f,Offset(x,y))
}
private fun DrawScope.companionHead(tier:RobotTier,side:Boolean,a:Color,f:RobotFace,blink:Boolean,e:RobotEquipment) {
    val small=tier==RobotTier.MK1
    val advanced=tier>=RobotTier.MK3
    if(side) {
        val cap=Path().apply {
            moveTo(65f,47f);cubicTo(63f,22f,106f,18f,124f,32f)
            quadraticTo(141f,47f,135f,66f);quadraticTo(130f,78f,110f,81f)
            quadraticTo(76f,85f,66f,65f);close()
        }
        companionMetal(cap)
        val visor=Path().apply {
            moveTo(111f,37f);quadraticTo(135f,37f,138f,49f)
            quadraticTo(139f,65f,128f,72f);lineTo(112f,74f)
            quadraticTo(105f,60f,111f,37f);close()
        }
        companionShape(screen,visor)
        face(123f,50f,true,f,blink,companionLight)
        drawCircle(ink,14f,Offset(87f,54f))
        drawCircle(a,12f,Offset(87f,54f))
        drawCircle(joint,8f,Offset(87f,54f))
        drawCircle(companionLight,5f,Offset(87f,54f),style=Stroke(2f))
        if(small) {
            omegaPlate(companionShell,listOf(Offset(72f,36f),Offset(64f,14f),Offset(92f,31f)))
            omegaPlate(a,listOf(Offset(72f,29f),Offset(70f,19f),Offset(84f,30f)))
        } else {
            val sweep=Path().apply {moveTo(82f,28f);quadraticTo(114f,19f,129f,35f);quadraticTo(114f,33f,108f,45f);quadraticTo(99f,32f,82f,28f);close()}
            companionMetal(sweep)
            if(advanced)omegaPlate(a,listOf(Offset(66f,36f),Offset(50f,20f),Offset(85f,29f),Offset(91f,35f)))
        }
        drawLine(a,Offset(78f,29f),Offset(96f,27f),2.5f,StrokeCap.Round)
    } else {
        for(sign in listOf(-1f,1f)) {
            val x=100f+34f*sign
            if(small) {
                val ear=Path().apply {moveTo(x-10f,35f);quadraticTo(x-11f,13f,x,15f);quadraticTo(x+10f,17f,x+10f,35f);close()}
                companionMetal(ear)
                omegaPlate(a,listOf(Offset(x-5f,29f),Offset(x,20f),Offset(x+5f,29f)))
            }
            drawCircle(ink,13f,Offset(x,55f));drawCircle(a,11f,Offset(x,55f))
            drawCircle(joint,8f,Offset(x,55f));drawCircle(companionLight,5f,Offset(x,55f),style=Stroke(1.5f))
        }
        val cap=Path().apply {
            moveTo(62f,49f);cubicTo(62f,16f,138f,16f,138f,49f)
            quadraticTo(144f,77f,116f,82f);quadraticTo(100f,85f,84f,82f)
            quadraticTo(56f,77f,62f,49f);close()
        }
        companionMetal(cap)
        val visor=Path().apply {
            moveTo(70f,49f);quadraticTo(70f,36f,85f,38f)
            quadraticTo(100f,42f,115f,38f);quadraticTo(130f,36f,130f,49f)
            quadraticTo(134f,75f,100f,76f);quadraticTo(66f,75f,70f,49f);close()
        }
        companionShape(screen,visor)
        val shine=Path().apply {moveTo(75f,47f);quadraticTo(78f,42f,88f,44f)}
        drawPath(shine,Color(0xFF77617E),style=Stroke(1.8f,cap=StrokeCap.Round))
        face(100f,51f,false,f,blink,companionLight)
        if(!blink && f!=RobotFace.SLEEP && f!=RobotFace.ANGRY) {
            for(x in listOf(79f,121f))drawLine(a.copy(alpha=.9f),Offset(x-2f,62f),Offset(x+2f,62f),2f,StrokeCap.Round)
        }
        val fringe=Path().apply {
            moveTo(67f,40f);quadraticTo(70f,26f,90f,25f)
            quadraticTo(99f,27f,108f,38f);quadraticTo(119f,35f,132f,39f)
            quadraticTo(112f,47f,103f,40f);quadraticTo(92f,34f,84f,38f)
            quadraticTo(74f,43f,67f,40f);close()
        }
        companionMetal(fringe)
        drawLine(a,Offset(93f,29f),Offset(102f,30f),2.6f,StrokeCap.Round)
        if(advanced)for(sign in listOf(-1f,1f)) {
            omegaPlate(companionShell,listOf(Offset(100f+29f*sign,33f),Offset(100f+43f*sign,21f),Offset(100f+39f*sign,43f),Offset(100f+33f*sign,47f)))
            drawLine(a,Offset(100f+38f*sign,28f),Offset(100f+35f*sign,39f),2f)
        }
    }
    if(tier==RobotTier.OMEGA) {
        val cx=if(side)98f else 100f
        drawOval(a.copy(alpha=.8f),Offset(cx-34f,7f),Size(68f,13f),style=Stroke(4f))
        drawOval(companionLight,Offset(cx-34f,7f),Size(68f,13f),style=Stroke(1.5f))
    }
    headGear(e.equipped(RobotSlot.HEAD),side,a)
}
private fun DrawScope.companionBody(tier:RobotTier,side:Boolean,a:Color,e:RobotEquipment) {
    val d=CompanionCatalog.of(tier)
    if(side) {
        val spine=Path().apply {
            moveTo(84f,83f);quadraticTo(111f,78f,122f,89f)
            quadraticTo(130f,99f,110f,112f);lineTo(115f,126f)
            quadraticTo(112f,140f,81f,136f);lineTo(84f,114f)
            quadraticTo(73f,98f,84f,83f);close()
        }
        companionShape(joint,spine)
        val chest=Path().apply {moveTo(84f,85f);quadraticTo(110f,77f,122f,90f);quadraticTo(127f,99f,113f,108f);lineTo(94f,112f);quadraticTo(78f,101f,84f,85f);close()}
        companionMetal(chest)
        val hip=Path().apply {moveTo(86f,119f);quadraticTo(101f,115f,116f,125f);lineTo(119f,137f);quadraticTo(95f,144f,80f,135f);close()}
        companionMetal(hip)
        drawLine(a,Offset(118f,91f),Offset(120f,102f),4f,StrokeCap.Round)
        if(d.armor>=1f)omegaPlate(companionShell,listOf(Offset(84f,121f),Offset(99f,122f),Offset(100f,134f),Offset(73f,143f)))
        if(d.armor>=3f)drawLine(companionLight,Offset(83f,125f),Offset(78f,137f),2f)
        chestGear(e.equipped(RobotSlot.CHEST),true,a)
    } else {
        panel(65f,87f,22f,12f,5f,joint);panel(113f,87f,22f,12f,5f,joint)
        val frame=Path().apply {
            moveTo(78f,84f);quadraticTo(100f,76f,122f,84f)
            quadraticTo(130f,96f,116f,111f);lineTo(111f,118f)
            quadraticTo(125f,126f,122f,135f);lineTo(78f,135f)
            quadraticTo(75f,126f,89f,118f);lineTo(84f,111f)
            quadraticTo(70f,96f,78f,84f);close()
        }
        companionShape(joint,frame)
        val chest=Path().apply {
            moveTo(78f,84f);quadraticTo(90f,78f,100f,85f);quadraticTo(110f,78f,122f,84f)
            quadraticTo(129f,94f,117f,104f);lineTo(100f,113f);lineTo(83f,104f);quadraticTo(71f,94f,78f,84f);close()
        }
        companionMetal(chest)
        val hip=Path().apply {moveTo(88f,119f);lineTo(100f,124f);lineTo(112f,119f);quadraticTo(125f,123f,122f,134f);lineTo(100f,142f);lineTo(78f,134f);quadraticTo(75f,123f,88f,119f);close()}
        companionMetal(hip)
        if(d.armor>=1f)for(sign in listOf(-1f,1f)) {
            omegaPlate(companionShell,listOf(Offset(100f+13f*sign,119f),Offset(100f+25f*sign,118f),Offset(100f+29f*sign,133f),Offset(100f+20f*sign,142f),Offset(100f+14f*sign,135f)))
            drawLine(a,Offset(100f+20f*sign,124f),Offset(100f+23f*sign,134f),3f)
            if(d.armor>=3f)drawLine(companionLight,Offset(100f+19f*sign,125f),Offset(100f+22f*sign,132f),1.2f)
        }
        chestGear(e.equipped(RobotSlot.CHEST),false,a)
        companionHeart(100f,96f,if(tier==RobotTier.OMEGA)13f else 10f,a,e.equipped(RobotSlot.CHEST)==RobotGear.REACTOR)
        if(d.armor>=2f) {
            drawLine(a,Offset(91f,111f),Offset(94f,119f),2f,StrokeCap.Round)
            drawLine(a,Offset(109f,111f),Offset(106f,119f),2f,StrokeCap.Round)
        }
    }
}
private fun DrawScope.companionArm(x:Float,y:Float,angle:Float,far:Boolean,a:Color,gear:RobotGear?,tier:RobotTier) {
    val d=CompanionCatalog.of(tier)
    val w=6f+d.armor*.65f
    rotate(angle,Offset(x,y)) {
        companionJoint(x,y,6.5f,a)
        val upper=Path().apply {moveTo(x-w,y+2f);quadraticTo(x-w-4f,y+9f,x-4f,y+18f);lineTo(x+4f,y+18f);quadraticTo(x+w+4f,y+9f,x+w,y+2f);close()}
        companionMetal(upper,far)
        if(d.armor>=1f)omegaPlate(a,listOf(Offset(x-w,y+4f),Offset(x+w,y+4f),Offset(x+3f,y+12f),Offset(x-3f,y+12f)))
        companionJoint(x,y+20f,4.7f,a)
        val cuff=Path().apply {moveTo(x-5f,y+22f);quadraticTo(x-w-5f,y+28f,x-w,y+36f);lineTo(x-4f,y+40f);lineTo(x+4f,y+40f);quadraticTo(x+w+5f,y+30f,x+5f,y+22f);close()}
        companionMetal(cuff,far)
        drawLine(a,Offset(x,y+26f),Offset(x,y+35f),3.6f,StrokeCap.Round)
        if(d.armor>=3f)drawLine(companionLight,Offset(x,y+27f),Offset(x,y+34f),1.4f)
        panel(x-5f,y+39f,10f,8f,3f,joint)
        for(i in -1..1)drawLine(companionShade,Offset(x+i*3f,y+46f),Offset(x+i*3f,y+49f),2f,StrokeCap.Round)
        if(gear==RobotGear.SHIELD_L||gear==RobotGear.SHIELD_R) {
            panel(x-14f,y+19f,28f,29f,7f,a);companionHeart(x,y+32f,6f,a,true)
        }
        if(gear==RobotGear.TOOL_L||gear==RobotGear.TOOL_R) {
            panel(x-5f,y+45f,10f,16f,3f,companionShade);panel(x-9f,y+58f,18f,6f,2f,amber)
        }
        if(gear==RobotGear.BLASTER_L||gear==RobotGear.BLASTER_R) {
            panel(x-11f,y+27f,22f,24f,5f,joint);panel(x-8f,y+48f,16f,12f,3f,a)
            drawLine(companionLight,Offset(x-5f,y+55f),Offset(x+5f,y+55f),3f)
        }
    }
}
private fun DrawScope.companionLeg(x:Float,y:Float,angle:Float,side:Boolean,far:Boolean,a:Color,gear:RobotGear?,tier:RobotTier) {
    val d=CompanionCatalog.of(tier)
    val w=7f+d.armor*.7f
    rotate(angle,Offset(x,y)) {
        companionJoint(x,y+2f,6f,a)
        val thigh=Path().apply {moveTo(x-w,y);quadraticTo(x-w-2f,y+8f,x-5f,y+18f);lineTo(x+5f,y+18f);quadraticTo(x+w+2f,y+8f,x+w,y);close()}
        companionMetal(thigh,far)
        companionJoint(x,y+18f,5f,a)
        val shin=Path().apply {moveTo(x-5f,y+19f);quadraticTo(x-w-4f,y+24f,x-w-1f,y+36f);lineTo(x-5f,y+40f);lineTo(x+5f,y+40f);quadraticTo(x+w+4f,y+27f,x+5f,y+19f);close()}
        companionMetal(shin,far)
        omegaPlate(a,listOf(Offset(x-4f,y+20f),Offset(x+4f,y+20f),Offset(x+5f,y+27f),Offset(x,y+32f),Offset(x-5f,y+27f)))
        if(d.armor>=2f)drawLine(companionLight,Offset(x,y+23f),Offset(x,y+28f),2f,StrokeCap.Round)
        val toe=if(side)16f else 10f
        val foot=Path().apply {moveTo(x-8f,y+36f);lineTo(x+7f,y+36f);quadraticTo(x+toe,y+40f,x+toe,y+46f);quadraticTo(x+toe-3f,y+49f,x-10f,y+47f);lineTo(x-10f,y+43f);close()}
        companionMetal(foot,far)
        drawLine(a,Offset(x-7f,y+45f),Offset(x+toe-2f,y+45f),2.6f,StrokeCap.Round)
        if(gear==RobotGear.BOOTS||gear==RobotGear.SPEED_BOOTS) {
            panel(x-12f,y+35f,if(side)31f else 24f,12f,4f,joint)
            drawLine(a,Offset(x-8f,y+40f),Offset(x+toe-1f,y+40f),4f,StrokeCap.Round)
            if(gear==RobotGear.SPEED_BOOTS)drawLine(companionLight,Offset(x-8f,y+45f),Offset(x+toe-1f,y+45f),1.5f)
        }
    }
}
private fun DrawScope.companionRobot(p:RobotPose,view:RobotView,a:Color,e:RobotEquipment,f:RobotFace,blink:Boolean,tier:RobotTier,phase:Float,action:RobotAction) {
    val side=view!=RobotView.FRONT
    companionHair(tier,side,a,phase,action)
    backGear(e.equipped(RobotSlot.BACK),side,a)
    val boots=e.equipped(RobotSlot.FEET)
    if(side) {
        companionLeg(94f,132f,p.farLeg,true,true,a,boots,tier)
        companionArm(94f,91f,p.farArm,true,a,e.equipped(view.farHand()),tier)
        companionLeg(102f,133f,p.nearLeg,true,false,a,boots,tier)
        companionBody(tier,true,a,e)
        panel(94f,74f,11f,11f,4f,joint)
        companionHead(tier,true,a,f,blink,e)
        companionArm(99f,92f,p.nearArm,false,a,e.equipped(view.nearHand()),tier)
        companionJoint(99f,92f,6.5f,a)
    } else {
        companionLeg(85f,133f,p.farLeg*.12f,false,false,a,boots,tier)
        companionLeg(115f,133f,p.nearLeg*.12f,false,false,a,boots,tier)
        companionBody(tier,false,a,e)
        companionArm(68f,94f,RobotMotion.frontArmAngle(p.farArm,false),false,a,e.equipped(RobotSlot.RIGHT_HAND),tier)
        companionArm(132f,94f,RobotMotion.frontArmAngle(p.nearArm,true),false,a,e.equipped(RobotSlot.LEFT_HAND),tier)
        companionJoint(68f,94f,6.5f,a);companionJoint(132f,94f,6.5f,a)
        panel(95f,74f,10f,11f,4f,joint)
        companionHead(tier,false,a,f,blink,e)
    }
}
@Composable
internal fun RobotPreview(view:RobotView, action:RobotAction, face:RobotFace, loadout:RobotLoadout,
                          phase:Float, showSockets:Boolean, modifier:Modifier=Modifier) {
    val p=RobotMotion.pose(action,phase)
    Canvas(modifier) {
        val omega=loadout.tier==RobotTier.OMEGA
        val companion=loadout.series==RobotSeries.COMPANION
        val growth=if(companion)CompanionCatalog.of(loadout.tier).scale else 1f
        val s=min(size.width/(if(omega)236f else 200f),size.height/(if(omega)270f else if(companion)235f else 215f))
        val dx=(size.width-200*s)/2; val dy=(size.height-215*s)/2+(if(omega)18*s else if(companion)5*s else 0f)
        translate(dx,dy) {
            scale(s,s,Offset.Zero) {
                drawOval(ink.copy(alpha=.35f),Offset(63f,181f),Size(74f,8f))
                translate(top=p.lift) {
                    scale(growth,growth,Offset(100f,180f)) {
                    scale(if(view==RobotView.LEFT) -1f else 1f,1f,Offset(100f,100f)) {
                        val actualFace=if(action==RobotAction.SLEEP) RobotFace.SLEEP else face
                        val blink=phase>.94f && action==RobotAction.IDLE
                        if(loadout.tier==RobotTier.OMEGA) omegaWings(view!=RobotView.FRONT,loadout.accent,phase,action,if(companion)companionLight else glow)
                        if(companion) companionRobot(p,view,loadout.accent,loadout.equipment,actualFace,blink,loadout.tier,phase,action)
                        else if(view==RobotView.FRONT) robotFront(p,loadout.accent,loadout.equipment,actualFace,blink,loadout.tier)
                        else robotSide(p,loadout.accent,loadout.equipment,view,actualFace,blink,loadout.tier)
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
    var classicSkin by remember { mutableStateOf(initial.second) }
    var companionSkin by remember(context) { mutableStateOf(RobotLabStorage.loadCompanionSkin(context)) }
    var series by remember { mutableStateOf(RobotSeries.CLASSIC) }
    val skin=if(series==RobotSeries.COMPANION)companionSkin else classicSkin
    var selectedSlot by remember { mutableStateOf(RobotSlot.HEAD) }
    var progress by remember(context) { mutableStateOf(RobotProgressStorage.load(context)) }
    LaunchedEffect(progress){RobotProgressStorage.save(context,progress)}
    var skills by remember(context){mutableStateOf(RobotSkillsStorage.load(context))}
    LaunchedEffect(skills){RobotSkillsStorage.save(context,skills)}
    var tier by remember { mutableStateOf(RobotTier.MK1) }
    val stats=remember(equipment,progress.level,skills,tier){RobotBalance.total(tier,equipment,progress.level,skills.bonuses())}
    val baseStats=remember(progress.level,tier){RobotBalance.total(tier,RobotEquipment(),progress.level)}
    LaunchedEffect(equipment,classicSkin){RobotLabStorage.save(context,equipment,classicSkin)}
    LaunchedEffect(companionSkin){RobotLabStorage.saveCompanionSkin(context,companionSkin)}
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
            Text("Robot Lab · ${tier.title}",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick=onBack){Text("Назад")}
        }
        Text("Незалежний тестовий стенд. Не змінює збереження гри.")
        Text("Серія робота")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            RobotSeries.entries.forEach { item ->
                FilterChip(selected=series==item,onClick={series=item},label={Text(item.title)})
            }
        }
        Text("Еволюції · вільний перегляд")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            RobotTier.entries.forEach { t ->
                FilterChip(selected=tier==t,onClick={tier=t},label={Text(t.title)})
            }
        }
        val chassis=RobotChassisCatalog.of(tier)
        Text(if(series==RobotSeries.COMPANION)CompanionCatalog.of(tier).title+" · Companion" else chassis.title+" · "+chassis.description,style=MaterialTheme.typography.bodyMedium)
        Text(when {
            tier==RobotTier.OMEGA -> "Секретна форма · лише особливі досягнення (умови ще не визначені)"
            RobotChassisCatalog.unlocked(tier,progress.level)->"Доступний за рівнем"
            else->"Тестовий перегляд · відкриття з рівня ${tier.requiredLevel}"
        },style=MaterialTheme.typography.bodySmall)
        Card { RobotPreview(view,action,face,RobotLoadout(skin.accent,equipment,tier,series),phase,sockets,
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
        RobotStatsPanel(tier,stats,baseStats,progress,onTrain={progress=progress.gain(25)},onReset={progress=RobotProgress()})
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
                FilterChip(selected=equipment.equipped(selectedSlot)==g,enabled=RobotBalance.compatible(g,tier),onClick={equipment=equipment.equip(g)},label={Text(g.title)})
            }
        }
        equipment.equipped(selectedSlot)?.let { gear ->
            Text(RobotBalance.spec(gear).delta.summary(),style=MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick={equipment=RobotEquipment()}){Text("Зняти все")}
        Text("Скін · колір акцентів")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            RobotSkin.entries.forEach { s ->
                FilterChip(selected=skin==s,onClick={if(series==RobotSeries.COMPANION)companionSkin=s else classicSkin=s},label={Text(s.title)})
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Точки кріплення (у нейтральній позі)",Modifier.weight(1f))
            Switch(checked=sockets,onCheckedChange={sockets=it})
        }
        Text("Еволюції доступні для тестового перегляду. Справжні мініігри та автономний пошук ще не підключені.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RobotStatsPanel(tier:RobotTier,stats:RobotStats,base:RobotStats,progress:RobotProgress,onTrain:()->Unit,onReset:()->Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text("Характеристики · ${tier.title}",style=MaterialTheme.typography.titleMedium)
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
            Text("Доступний за рівнем ранг: ${progress.nextTier.title}. У лабораторії всі корпуси доступні для перегляду незалежно від рівня.",style=MaterialTheme.typography.bodySmall)
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
