package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

internal data class Habitat(val title:String,val description:String,val sky:Color,val horizon:Color,val ground:Color,val light:Color)
internal object HabitatCatalog {
    fun of(series:RobotSeries):Habitat=when(series){
        RobotSeries.CLASSIC->Habitat("01 · Механічна майстерня","Ремонтні модулі, метал і тепле світло",Color(0xFF101923),Color(0xFF344B59),Color(0xFF202C36),Color(0xFFF0C17E))
        RobotSeries.COMPANION->Habitat("02 · Неонова оранжерея","Скляний купол і живі цифрові рослини",Color(0xFF18192E),Color(0xFF3A4E50),Color(0xFF263C3F),Color(0xFFFFB9D9))
        RobotSeries.KAGE->Habitat("03 · Храм тіней","Місячна ніч, дах пагоди та ліхтарі",Color(0xFF0B1329),Color(0xFF303D5B),Color(0xFF202536),Color(0xFFFFB17D))
        RobotSeries.GLITCH->Habitat("04 · Мертве місто","Дощ, антени й покинуті неонові вежі",Color(0xFF140F28),Color(0xFF302948),Color(0xFF211D31),Color(0xFF77F0E9))
        RobotSeries.VALKYRIE->Habitat("05 · Небесна цитадель","Плавучі арки над хмарами",Color(0xFF182C4A),Color(0xFF65778F),Color(0xFF39465D),Color(0xFFFFDDA0))
        RobotSeries.NEXUS->Habitat("06 · Квантове ядро","Цифровий простір і потоки даних",Color(0xFF080F22),Color(0xFF1D3049),Color(0xFF101C2F),Color(0xFF79D9FF))
        RobotSeries.SYMBIONT->Habitat("07 · Безодня симбіонтів","Органічні ребра, спори й темний панцир",Color(0xFF0C1013),Color(0xFF37323C),Color(0xFF211E27),Color(0xFFA3D38F))
        RobotSeries.MYCELIA->Habitat("08 · Грибний ліс","Світні гриби, туман і спорові колонії",Color(0xFF0B2228),Color(0xFF315354),Color(0xFF1D3639),Color(0xFF9BE7BB))
    }
}

@Composable
internal fun HabitatScene(series:RobotSeries,phase:Float,modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit){
    Box(modifier.clipToBounds()){
        Canvas(Modifier.fillMaxSize()){
            val h=HabitatCatalog.of(series)
            drawRect(Brush.verticalGradient(listOf(h.sky,h.horizon,h.ground),endY=size.height))
            // All geometry is in normalized coordinates; model renderer keeps its own scale.
            scale(size.width/100f,size.height/100f,Offset.Zero){
                clipRect(0f,0f,100f,100f){
                    atmosphere(h,phase)
                    when(series){
                        RobotSeries.CLASSIC->workshop(h,phase)
                        RobotSeries.COMPANION->garden(h,phase)
                        RobotSeries.KAGE->temple(h,phase)
                        RobotSeries.GLITCH->city(h,phase)
                        RobotSeries.VALKYRIE->citadel(h,phase)
                        RobotSeries.NEXUS->quantum(h,phase)
                        RobotSeries.SYMBIONT->abyss(h,phase)
                        RobotSeries.MYCELIA->forest(h,phase)
                    }
                    floor(h,series,phase)
                }
            }
        }
        content()
    }
}
private fun DrawScope.poly(color:Color,vararg xy:Float){
    val p=Path().apply{moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()}
    drawPath(p,color)
}
private fun DrawScope.stroke(c:Color,x:Float,y:Float,u:Float,v:Float,w:Float=.35f)=drawLine(c,Offset(x,y),Offset(u,v),w,StrokeCap.Round)
private fun DrawScope.atmosphere(h:Habitat,phase:Float){
    drawCircle(Brush.radialGradient(listOf(h.light.copy(alpha=.12f),Color.Transparent),Offset(50f,36f),48f),48f,Offset(50f,36f))
    for(i in 0..23){
        val x=(i*37%97+2).toFloat()
        val y=(i*19%64+3).toFloat()
        val pulse=.08f+.18f*(.5f+.5f*sin(phase*6.283f+i))
        drawCircle(h.light.copy(alpha=pulse),if(i%5==0).45f else .2f,Offset(x,y))
    }
}
private fun DrawScope.workshop(h:Habitat,phase:Float){
    val metal=Color(0xFF23333F)
    for(i in 0..4){
        val x=i*25f
        drawRect(metal,Offset(x-2f,0f),Size(3.5f,74f))
        stroke(h.light.copy(alpha=.24f),x+2f,3f,x+2f,68f)
    }
    drawRect(Color(0xFF14232E),Offset(31f,17f),Size(38f,42f))
    drawRect(h.light.copy(alpha=.25f),Offset(32f,18f),Size(36f,40f),style=Stroke(.45f))
    for(i in 0..4)stroke(h.light.copy(alpha=.18f),34f,23f+i*6f,65f,23f+i*6f)
    for(s in listOf(0f,83f)){
        drawRect(metal,Offset(s,44f),Size(17f,30f))
        for(i in 0..3){drawRect(h.sky,Offset(s+2f,47f+i*6f),Size(13f,4f));drawCircle(h.light.copy(alpha=.7f),.6f,Offset(s+4f,49f+i*6f))}
        drawRect(h.light.copy(alpha=.5f),Offset(s+2f,42f),Size(13f,1f))
    }
    for(i in 0..2)stroke(h.light.copy(alpha=.5f),38f+i*12f,6f,46f+i*12f,6f,1.1f)
}
private fun DrawScope.garden(h:Habitat,phase:Float){
    drawOval(h.light.copy(alpha=.16f),Offset(5f,-30f),Size(90f,106f),style=Stroke(.6f))
    for(i in 0..6)stroke(h.light.copy(alpha=.13f),50f,0f,i*16f,70f,.35f)
    drawOval(Color(0xFF9BDDD5).copy(alpha=.13f),Offset(9f,11f),Size(82f,45f))
    for(i in 0..7){
        val x=if(i<4)3f+i*5f else 81f+(i-4)*5f
        val top=25f+(i*13%25)
        val sway=sin(phase*6.283f+i)*1.4f
        stroke(Color(0xFF70AB8F),x,76f,x+sway,top,.75f)
        for(j in 0..3){
            val y=top+8f+j*7f
            drawOval(Color(0xFF509B87).copy(alpha=.8f),Offset(x-4f+(if(j%2==0)0f else 3f),y),Size(5f,2.8f))
        }
        drawCircle(h.light.copy(alpha=.18f),4f,Offset(x+sway,top))
        drawCircle(h.light,.9f,Offset(x+sway,top))
    }
    for(x in listOf(10f,90f))drawRect(h.ground,Offset(x-7f,68f),Size(14f,8f))
}
private fun DrawScope.temple(h:Habitat,phase:Float){
    drawCircle(Color(0xFFC5D7E9).copy(alpha=.82f),8f,Offset(78f,17f))
    poly(Color(0xFF202C47),0f,58f,12f,36f,28f,51f,40f,30f,60f,52f,75f,39f,100f,58f,100f,76f,0f,76f)
    for(x in listOf(8f,84f)){
        drawRect(Color(0xFF2A222E),Offset(x,29f),Size(8f,47f))
        poly(Color(0xFF101725),x-5f,30f,x+4f,18f,x+13f,30f,x+4f,27f)
        poly(Color(0xFF161B2D),x-7f,43f,x+4f,32f,x+15f,43f)
        drawRect(h.light.copy(alpha=.75f),Offset(x+2f,47f),Size(4f,7f))
        drawCircle(h.light.copy(alpha=.12f),8f,Offset(x+4f,50f))
    }
    for(x in listOf(29f,71f))drawRect(Color(0xFF663F43),Offset(x,37f),Size(2f,38f))
    drawRect(Color(0xFF663F43),Offset(23f,36f),Size(54f,2.4f))
    drawRect(Color(0xFF252238),Offset(20f,31f),Size(60f,3f))
}
private fun DrawScope.city(h:Habitat,phase:Float){
    for(i in 0..10){
        val x=i*10f-3f
        val top=16f+(i*17%35)
        drawRect(if(i%2==0)Color(0xFF242238)else Color(0xFF19182B),Offset(x,top),Size(8.5f,76f-top))
        stroke(h.light.copy(alpha=.24f),x+1f,top,x+1f,74f)
        for(j in 0..4)if((i+j)%3!=0)drawRect(if(i%3==0)Color(0xFFDE81C2).copy(alpha=.3f)else h.light.copy(alpha=.25f),Offset(x+2f,top+4f+j*5f),Size(4f,.9f))
        if(i%3==0)stroke(h.light.copy(alpha=.6f),x+4f,top,x+4f,top-6f)
    }
    for(i in 0..24){
        val x=(i*23%105).toFloat()
        val y=(i*17%60+phase*60f)%60f
        stroke(Color(0xFFB2D7ED).copy(alpha=.16f),x,y,x-1.4f,y+4f,.18f)
    }
}
private fun DrawScope.citadel(h:Habitat,phase:Float){
    for(i in 0..6){
        val x=i*17f-6f
        val y=42f+(i*13%18)+sin(phase*6.283f+i)*1.1f
        drawOval(Color(0xFFC3D5EA).copy(alpha=.13f),Offset(x,y),Size(28f,13f))
    }
    for(s in listOf(0f,86f)){
        poly(Color(0xFF63758B),s,73f,s+3f,19f,s+8f,7f,s+13f,19f,s+14f,73f)
        stroke(h.light.copy(alpha=.7f),s+7f,17f,s+7f,66f,.7f)
        drawOval(h.light.copy(alpha=.35f),Offset(s-2f,24f),Size(18f,8f),style=Stroke(.6f))
    }
    drawOval(h.light.copy(alpha=.3f),Offset(22f,4f),Size(56f,62f),style=Stroke(.65f))
    drawOval(h.light.copy(alpha=.13f),Offset(28f,10f),Size(44f,50f),style=Stroke(.35f))
}
private fun DrawScope.quantum(h:Habitat,phase:Float){
    for(i in 0..6){
        val x=i*16f
        stroke(h.light.copy(alpha=.17f),x,0f,x,73f,.4f)
        for(j in 0..5){
            val y=(i*11+j*13+phase*13f)%72f
            drawRect(h.light.copy(alpha=.35f),Offset(x+.6f,y),Size(1.5f,1.2f))
        }
    }
    for(r in listOf(13f,22f,31f)){
        drawCircle(h.light.copy(alpha=.12f),r,Offset(50f,34f),style=Stroke(.4f))
        drawArc(h.light.copy(alpha=.45f),phase*360f+r*4f,70f,false,Offset(50f-r,34f-r),Size(r*2,r*2),style=Stroke(.6f))
    }
    for(i in 0..5){
        val y=12f+i*10f
        stroke(h.light.copy(alpha=.3f),0f,y,14f,y)
        stroke(h.light.copy(alpha=.3f),86f,y,100f,y)
    }
}
private fun DrawScope.abyss(h:Habitat,phase:Float){
    val rib=Color(0xFF51434B)
    for(i in 0..4){
        val x=i*25f-5f
        val p=Path().apply{moveTo(x,0f);cubicTo(x+16f,18f,x-5f,48f,x+8f,76f)}
        drawPath(p,rib.copy(alpha=.6f),style=Stroke(4f,cap=StrokeCap.Round))
        drawPath(p,Color(0xFF79616A).copy(alpha=.25f),style=Stroke(.9f))
    }
    for(s in listOf(0f,87f)){
        val p=Path().apply{moveTo(s,0f);quadraticBezierTo(s+15f,15f,s+5f,37f);quadraticBezierTo(s+12f,58f,s+8f,76f);lineTo(s,76f);close()}
        drawPath(p,Color(0xFF19161F))
        for(i in 0..5){
            val y=10f+i*11f
            drawOval(h.light.copy(alpha=.16f),Offset(s+2f,y),Size(8f,4f))
            drawCircle(h.light.copy(alpha=.6f),.5f,Offset(s+5f,y+2f))
        }
    }
    for(i in 0..9){
        val x=(i*31%97+1).toFloat()
        val y=12f+(i*13%48)+sin(phase*6.283f+i)*1.5f
        drawCircle(h.light.copy(alpha=.12f),2f,Offset(x,y))
        drawCircle(h.light.copy(alpha=.4f),.3f,Offset(x,y))
    }
    poly(Color(0xFF23202B),0f,0f,13f,0f,8f,18f,4f,8f,0f,19f)
    poly(Color(0xFF23202B),81f,0f,100f,0f,100f,22f,93f,10f,88f,17f)
}
private fun DrawScope.forest(h:Habitat,phase:Float){
    for(i in 0..8){
        val x=i*13f-2f
        val top=12f+(i*17%31)
        stroke(Color(0xFF25464A),x,76f,x+2f,top,2.8f)
        drawOval(Color(0xFF385D5E),Offset(x-7f,top-6f),Size(17f,8f))
    }
    for(i in 0..7){
        val x=if(i<4)3f+i*5f else 81f+(i-4)*5f
        val y=54f-(i*7%19)
        val w=6f+(i%3)*2f
        stroke(Color(0xFF789B91),x,77f,x,y,1.4f)
        drawOval(h.light.copy(alpha=.12f),Offset(x-w,y-w*.6f),Size(w*2,w*1.2f))
        drawOval(Color(0xFF4D877E),Offset(x-w*.65f,y-2f),Size(w*1.3f,4f))
        stroke(h.light.copy(alpha=.7f),x-w*.5f,y+1f,x+w*.5f,y+1f,.5f)
    }
    for(i in 0..13)drawCircle(h.light.copy(alpha=.28f),.45f,Offset((i*29%99).toFloat(),(i*11+phase*15f)%63f))
}
private fun DrawScope.floor(h:Habitat,series:RobotSeries,phase:Float){
    val horizon=74f
    drawRect(Brush.verticalGradient(listOf(h.ground,h.sky),startY=horizon,endY=100f),Offset(0f,horizon),Size(100f,26f))
    stroke(h.light.copy(alpha=.3f),0f,horizon,100f,horizon,.45f)
    val organic=series==RobotSeries.SYMBIONT||series==RobotSeries.MYCELIA||series==RobotSeries.COMPANION||series==RobotSeries.KAGE
    if(organic){
        for(i in 0..19){
            val x=(i*37%100).toFloat()
            val y=77f+(i*13%22)
            val w=1f+(y-74f)*.1f
            drawOval(h.horizon.copy(alpha=.5f),Offset(x,y),Size(w*2,.8f))
        }
        for(i in 0..5){
            val x=if(i<3)i*6f else 83f+(i-3)*6f
            stroke(h.light.copy(alpha=.22f),x,98f,x+sin(phase*6.283f+i)*1.3f,91f,.5f)
        }
    }else{
        for(i in -3..3)stroke(h.light.copy(alpha=.15f),50f+i*8f,horizon,50f+i*27f,100f,.25f)
        for(y in listOf(77f,82f,89f,98f))stroke(h.light.copy(alpha=.15f),0f,y,100f,y,.25f)
    }
    drawOval(h.light.copy(alpha=.055f),Offset(19f,79f),Size(62f,17f))
    drawOval(h.light.copy(alpha=.10f),Offset(27f,83f),Size(46f,10f),style=Stroke(.25f))
}
