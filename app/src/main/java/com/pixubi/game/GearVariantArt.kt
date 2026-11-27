package com.pixubi.game
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

private fun DrawScope.gearPlate(c:Color,a:Color,vararg xy:Float){
    val path=Path().apply{moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()}
    val b=path.getBounds()
    drawPath(path,Brush.linearGradient(listOf(c,Color(0xFF182336)),Offset(b.left,b.top),Offset(b.right,b.bottom)))
    drawPath(path,Color(0xFF101523),style=Stroke(1.4f))
    drawLine(a,Offset(xy[0],xy[1]),Offset(xy[2],xy[3]),1.5f)
}
internal fun DrawScope.drawVariantHelmet(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val width=if(animal).83f else 1f+grade*.035f
    scale(width,if(animal).85f else 1f,Offset.Zero){
        when(g){
            RobotGear.VISOR->gearPlate(Color(0xFF405F79),a,-26f,-26f,25f,-26f,32f,-18f,23f,-13f,-23f,-13f)
            RobotGear.HELMET_SHADOW->{gearPlate(Color(0xFF363B54),a,-32f,-18f,-24f,-40f,7f,-44f,31f,-30f,24f,-24f,-18f,-27f);gearPlate(Color(0xFF40495D),a,-32f,-18f,-22f,-24f,-17f,2f,-27f,12f)}
            RobotGear.HELMET_TITAN->{gearPlate(Color(0xFF8795A7),a,-34f,-20f,-29f,-43f,24f,-43f,34f,-23f,29f,-17f,-24f,-17f);gearPlate(Color(0xFF586779),a,-34f,-20f,-23f,-17f,-20f,13f,-33f,16f);if(!side)scale(-1f,1f,Offset.Zero){gearPlate(Color(0xFF586779),a,-34f,-20f,-23f,-17f,-20f,13f,-33f,16f)}}
            RobotGear.HELMET_PRISM->{gearPlate(Color(0xFFB2ACDA),a,-27f,-26f,0f,-51f,27f,-26f,19f,-20f,0f,-37f,-19f,-20f);drawCircle(a,4f,Offset(0f,-35f))}
            else->Unit
        }
    }
}
internal fun DrawScope.drawVariantArmor(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val w=if(animal)1.5f else if(side).8f else 1f+grade*.035f
    scale(w,if(animal).72f else 1f,Offset.Zero){
        translate(0f,if(animal)-16f else 0f){
            when(g){
                RobotGear.LIGHT_ARMOR,RobotGear.ARMOR_SHADOW->{
                    val c=if(g==RobotGear.ARMOR_SHADOW)Color(0xFF454961)else Color(0xFF8696A6)
                    gearPlate(c,a,-29f,-10f,-18f,-13f,-10f,17f,-21f,31f,-31f,8f)
                    if(!side||animal)scale(-1f,1f,Offset.Zero){gearPlate(c,a,-29f,-10f,-18f,-13f,-10f,17f,-21f,31f,-31f,8f)}
                }
                RobotGear.HEAVY_ARMOR->{for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){gearPlate(Color(0xFF8494A3),a,13f,-12f,35f,-18f,41f,1f,31f,31f,13f,23f);gearPlate(Color(0xFF4C596B),a,17f,25f,31f,31f,26f,43f,12f,38f)}}
                RobotGear.ARMOR_PRISM->{gearPlate(Color(0xFFB1A6D2),a,-28f,-8f,0f,3f,28f,-8f,25f,11f,0f,28f,-25f,11f);drawCircle(a,5f,Offset(0f,13f))}
                else->Unit
            }
        }
    }
}
