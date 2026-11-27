package com.pixubi.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
internal fun WorldShowcase(p:WorldPet,action:RobotAction,face:RobotFace,fx:WorldFx,event:Int,
    modifier:Modifier=Modifier,roam:Boolean=false,onTap:(()->Unit)?=null){
    val series=p.series
    val accent=p.skin.accent
    val foreground=LocalPixubiForeground.current
    val ambient by rememberGamePhase(12000,RenderBudget.SCENERY_FPS,p.mode!=WorldMode.HIBERNATE)
    val roomX=remember(series){Animatable(.5f)}
    var patrolAction by remember(series){mutableStateOf(RobotAction.IDLE)}
    var patrolView by remember(series){mutableStateOf(RobotView.FRONT)}
    val walkingMission=KageGait.isLocomotion(action)
    val canRoam=foreground&&roam&&p.mode==WorldMode.AWAKE&&
        (walkingMission||(action==RobotAction.IDLE&&p.energy>=15&&p.integrity>=20))
    LaunchedEffect(series,canRoam,action){
        if(!canRoam)return@LaunchedEffect
        // The position survives interruptions: care, sleep and page interaction do not teleport the robot.
        patrolAction=RobotAction.IDLE;patrolView=RobotView.FRONT
        while(true){
            delay(HomeRoam.pause(Random.Default,walkingMission))
            val from=roomX.value
            val next=HomeRoam.destination(from,Random.Default)
            patrolView=HomeRoam.direction(from,next)
            patrolAction=if(action==RobotAction.RUN)RobotAction.RUN else RobotAction.WALK
            roomX.animateTo(next,tween(HomeRoam.duration(series,from,next,patrolAction==RobotAction.RUN),easing=LinearEasing))
            patrolAction=RobotAction.IDLE
            // Briefly hold the arrival-facing pose before looking toward the player.
            delay(350L)
            patrolView=RobotView.FRONT
        }
    }
    val displayedAction=if(canRoam)patrolAction else if(roam&&walkingMission)RobotAction.IDLE else action
    val period=WorldMotion.period(series,displayedAction)
    val phase by rememberGamePhase(period,if(displayedAction==RobotAction.SLEEP)RenderBudget.SCENERY_FPS else RenderBudget.ACTOR_FPS,p.mode!=WorldMode.HIBERNATE)
    val flash=remember{Animatable(1f)}
    LaunchedEffect(event,fx){if(fx!=WorldFx.NONE){flash.snapTo(0f);flash.animateTo(1f,tween(if(fx==WorldFx.EVOLVE)2300 else 1400,easing=LinearEasing))}else flash.snapTo(1f)}
    val view=if(canRoam)patrolView else if(KageGait.isLocomotion(displayedAction)||displayedAction==RobotAction.ATTACK)RobotView.RIGHT else RobotView.FRONT
    Box(modifier.clip(RoundedCornerShape(androidx.compose.ui.unit.Dp(16f))).then(if(onTap!=null)Modifier.clickable(onClick=onTap)else Modifier)){
        HabitatScene(series,ambient,Modifier.fillMaxSize()){
            // One common transform for the robot, ground shadow and local effects.
            // The floor stays fixed; the feet remain anchored to its perspective.
            Box(Modifier.fillMaxSize().graphicsLayer{
                if(roam){
                    scaleX=HomeRoam.SCALE;scaleY=HomeRoam.SCALE
                    transformOrigin=TransformOrigin(.5f,.89f)
                    translationX=(roomX.value-.5f)*size.width
                }
            }){
            Canvas(Modifier.fillMaxSize()){
                val w=size.width;val h=size.height;val c=Offset(w*.5f,h*.55f)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha=.17f),Color.Transparent),c,min(w,h)*.52f),min(w,h)*.52f,c)
                val g=h*.89f
                val spread=w*(if(series==RobotSeries.SYMBIONT||series==RobotSeries.MYCELIA).26f else .15f)
                drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha=.44f),Color.Transparent),Offset(w*.5f,g),spread),
                    Offset(w*.5f-spread,g-h*.036f),Size(spread*2,h*.072f))
                drawOval(accent.copy(alpha=.10f),Offset(w*.5f-spread,g-h*.035f),Size(spread*2,h*.07f),style=Stroke(1f))
            }
            RobotPreview(view,displayedAction,face,RobotLoadout(accent,p.equipment,p.tier,series),phase,false,Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()){
                val h=size.height;val w=size.width;val unit=min(w,h)/100f
                scale(w/100f,h/100f,Offset.Zero){
                    if(view==RobotView.LEFT&&KageGait.isLocomotion(displayedAction)){
                        scale(-1f,1f,Offset(50f,50f)){worldActionFx(displayedAction,phase,accent)}
                    }else worldActionFx(displayedAction,phase,accent)
                    if(fx!=WorldFx.NONE&&flash.value<1f)eventFx(fx,flash.value,accent)
                }
            }
            }
            Canvas(Modifier.fillMaxSize()){
                val w=size.width;val h=size.height
                scale(w/100f,h/100f,Offset.Zero){worldForeground(series,ambient,HabitatCatalog.of(series).light)}
                // Darken corners softly, keeping the actor readable.
                drawRect(Brush.radialGradient(listOf(Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.32f)),Offset(w/2,h*.48f),max(w,h)*.67f))
                if(p.mode==WorldMode.HIBERNATE)drawRect(Color(0xFF10192D).copy(alpha=.19f))
            }
        }
    }
}
private fun DrawScope.worldForeground(series:RobotSeries,phase:Float,a:Color){
    for(i in 0..15){
        val t=phase*2f*PI.toFloat()+i*.81f
        val left=i%2==0
        val x=(if(left)5f else 95f)+sin(t)*(3f+(i%4)*2f)
        val y=12f+(i*17%73)+cos(t*.999f)*3f
        val alpha=.09f+.19f*(.5f+.5f*sin(t))
        when(series){
            RobotSeries.CLASSIC->{drawLine(a.copy(alpha=alpha),Offset(x,y),Offset(x+1.1f,y-2.5f),.25f)}
            RobotSeries.COMPANION,RobotSeries.VALKYRIE->{drawOval(a.copy(alpha=alpha),Offset(x,y),Size(1.3f,.65f))}
            RobotSeries.KAGE->{drawOval(a.copy(alpha=alpha*.28f),Offset(x-10f,y+18f),Size(20f,5f))}
            RobotSeries.GLITCH,RobotSeries.NEXUS->{drawRect(a.copy(alpha=alpha),Offset(x,y),Size(1.1f,.6f));drawLine(a.copy(alpha=alpha*.65f),Offset(x,y),Offset(x,y-3f),.20f)}
            RobotSeries.SYMBIONT,RobotSeries.MYCELIA->{drawCircle(a.copy(alpha=alpha*.3f),1.6f,Offset(x,y));drawCircle(a.copy(alpha=alpha),.42f,Offset(x,y))}
        }
    }
}
private fun DrawScope.worldActionFx(action:RobotAction,t:Float,a:Color){
    val fade=sin(t*PI.toFloat()).coerceAtLeast(0f)
    when(action){
        RobotAction.CHARGE->{for(i in 0..2){val u=(t+i/3f)%1f;drawOval(a.copy(alpha=(1f-u)*.48f),Offset(27f-u*8f,80f-u*30f),Size(46f+u*16f,8f),style=Stroke(.5f))}}
        RobotAction.SCAN->{val x=52f+t*32f;drawLine(a.copy(alpha=.45f*fade),Offset(x,27f),Offset(x,82f),.5f);drawRect(a.copy(alpha=.05f*fade),Offset(51f,27f),Size(x-51f,55f))}
        RobotAction.ATTACK->{if(t>.22f&&t<.65f){val u=(t-.22f)/.43f;drawArc(a.copy(alpha=sin(u*PI.toFloat())*.8f),-95f+u*80f,75f,false,Offset(50f,25f),Size(37f,52f),style=Stroke(1.2f));drawArc(Color.White.copy(alpha=fade*.5f),-90f+u*80f,55f,false,Offset(52f,28f),Size(34f,47f),style=Stroke(.4f))}}
        RobotAction.WALK,RobotAction.RUN->{for(i in 0..4){val u=(t+i*.2f)%1f;drawCircle(a.copy(alpha=(1f-u)*.20f),.5f+u,Offset(41f-u*12f,88f-u*2f))}}
        RobotAction.SLEEP->{for(i in 0..2){val u=(t+i/3f)%1f;drawCircle(a.copy(alpha=(1f-u)*.35f),.6f+u*.7f,Offset(59f+u*10f,33f-u*12f),style=Stroke(.25f))}}
        else->Unit
    }
}
private fun DrawScope.eventFx(fx:WorldFx,t:Float,a:Color){
    val alpha=(1f-t).coerceAtLeast(0f)
    val c=when(fx){WorldFx.REPAIR->Color(0xFF7CF5AF);WorldFx.CHARGE->Color(0xFF7FDDFF);WorldFx.EVOLVE->Color(0xFFFFD88A);else->a}
    val center=Offset(50f,55f)
    if(fx==WorldFx.REPAIR){
        for(i in 0..11){val ang=i*2.399f;val start=center+Offset(cos(ang)*(12f+t*23f),sin(ang)*(10f+t*19f))
            drawLine(c.copy(alpha=alpha*.8f),start,start+Offset(cos(ang)*2f,sin(ang)*2f),.5f)}
        drawLine(c.copy(alpha=alpha*.6f),Offset(32f,32f+t*45f),Offset(68f,32f+t*45f),.6f)
    }else{
        val count=if(fx==WorldFx.EVOLVE)3 else 1
        for(i in 0 until count){val u=(t+i*.24f)%1f;val r=5f+u*34f
            drawOval(c.copy(alpha=(1f-u)*alpha*.75f),Offset(50f-r,55f-r*.75f),Size(r*2,r*1.5f),style=Stroke(.8f))}
        for(i in 0..15){val ang=i*2.399f;val r=9f+t*34f;drawCircle(c.copy(alpha=alpha*.65f),.55f,center+Offset(cos(ang)*r,sin(ang)*r*.75f))}
    }
}
