package com.pixubi.game
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
internal fun DrawScope.drawWorldWeapon(gear:RobotGear?,a:Color){
    val a=gearLight(gear,a)
    val dark=Color(0xFF202A38)
    val silver=Color(0xFFD7E1EA)
    when(gear){
        RobotGear.SHIELD_L,RobotGear.SHIELD_R->{val p=Path().apply{moveTo(-19f,-27f);lineTo(19f,-27f);lineTo(23f,-6f);lineTo(0f,15f);lineTo(-23f,-6f);close()};drawPath(p,dark);drawPath(p,silver,style=Stroke(2f));drawCircle(a,8f,Offset(0f,-9f),style=Stroke(3f));drawLine(a,Offset(-14f,-20f),Offset(14f,-20f),2f)}
        RobotGear.TOOL_L,RobotGear.TOOL_R->{drawRoundRect(dark,Offset(-6f,0f),Size(12f,31f),CornerRadius(3f));drawRect(silver,Offset(-4f,22f),Size(8f,15f));drawLine(a,Offset(-10f,37f),Offset(10f,37f),5f,StrokeCap.Round);drawCircle(a,3f,Offset(0f,10f))}
        RobotGear.BLASTER_L,RobotGear.BLASTER_R->{drawRoundRect(dark,Offset(-10f,-10f),Size(20f,32f),CornerRadius(4f));drawRoundRect(silver,Offset(-8f,-7f),Size(16f,23f),CornerRadius(3f),style=Stroke(2f));drawRect(silver,Offset(-7f,17f),Size(14f,9f));drawLine(a,Offset(-5f,6f),Offset(5f,6f),3f);drawLine(a,Offset(-5f,23f),Offset(5f,23f),2f)}
        RobotGear.BLADE_L,RobotGear.BLADE_R->{
            drawRoundRect(dark,Offset(-3f,2f),Size(6f,15f),CornerRadius(2f))
            drawRoundRect(silver,Offset(-10f,13f),Size(20f,5f),CornerRadius(2f))
            val blade=Path().apply{moveTo(-5f,18f);lineTo(5f,18f);lineTo(6f,39f);lineTo(0f,55f);lineTo(-6f,39f);close()}
            drawPath(blade,a.copy(alpha=.3f),style=Stroke(5f));drawPath(blade,a);drawPath(blade,silver,style=Stroke(1.2f))
            drawLine(Color.White,Offset(0f,21f),Offset(0f,45f),1.5f)
        }
        RobotGear.CANNON_L,RobotGear.CANNON_R->{
            drawRoundRect(dark,Offset(-12f,-9f),Size(24f,31f),CornerRadius(4f))
            drawRoundRect(silver,Offset(-9f,-6f),Size(18f,23f),CornerRadius(3f),style=Stroke(2f))
            for(x in listOf(-6f,6f)){
                drawRoundRect(dark,Offset(x-4f,17f),Size(8f,22f),CornerRadius(2f))
                drawLine(a,Offset(x,19f),Offset(x,34f),2f)
                drawRect(silver,Offset(x-4f,37f),Size(8f,4f))
            }
            drawCircle(a,4f,Offset(0f,4f))
        }
        RobotGear.RIFLE_L,RobotGear.RIFLE_R->{
            drawRoundRect(dark,Offset(-8f,-8f),Size(16f,30f),CornerRadius(3f))
            drawRect(silver,Offset(-5f,20f),Size(10f,31f))
            drawRect(dark,Offset(-3f,24f),Size(6f,26f))
            drawRect(a,Offset(-2f,26f),Size(4f,17f))
            drawRect(silver,Offset(-9f,2f),Size(18f,6f))
        }
        RobotGear.ARC_L,RobotGear.ARC_R->{
            drawRoundRect(dark,Offset(-13f,-5f),Size(26f,25f),CornerRadius(5f))
            drawCircle(a,7f,Offset(0f,6f))
            for(sign in listOf(-1f,1f)){
                drawLine(silver,Offset(sign*8f,15f),Offset(sign*13f,38f),5f,StrokeCap.Round)
                drawCircle(a,4f,Offset(sign*13f,38f))
            }
            val bolt=Path().apply{moveTo(-13f,38f);lineTo(-4f,32f);lineTo(2f,42f);lineTo(13f,38f)}
            drawPath(bolt,a,style=Stroke(2.5f))
        }
        RobotGear.REPAIR_L,RobotGear.REPAIR_R->{
            val green=Color(0xFF70E5A2)
            drawRoundRect(dark,Offset(-6f,-2f),Size(12f,27f),CornerRadius(3f))
            drawRect(silver,Offset(-3f,22f),Size(6f,17f))
            drawLine(silver,Offset(0f,38f),Offset(-9f,45f),5f,StrokeCap.Round)
            drawLine(silver,Offset(0f,38f),Offset(9f,45f),5f,StrokeCap.Round)
            drawCircle(green,3f,Offset(0f,7f))
        }
        else->Unit
    }
}
