package com.pixubi.game
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
internal fun DrawScope.drawWorldWeapon(gear:RobotGear?,a:Color){
    val dark=Color(0xFF202A38)
    val silver=Color(0xFFD7E1EA)
    when(gear){
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
        else->Unit
    }
}
