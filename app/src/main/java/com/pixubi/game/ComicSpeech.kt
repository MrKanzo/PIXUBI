package com.pixubi.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object ComicPlacement{
    fun left(anchor:Float,width:Float,viewport:Float)=(anchor-width/2f).coerceIn(0f,(viewport-width).coerceAtLeast(0f))
    fun top(anchor:Float,height:Float,viewport:Float,below:Boolean)=(if(below)anchor else anchor-height).coerceIn(0f,(viewport-height).coerceAtLeast(0f))
}
@Composable internal fun ComicBubble(line:RobotSpeech,modifier:Modifier=Modifier,compact:Boolean=false,tail:Float=.5f,below:Boolean=false,showName:Boolean=false){
    val fill=if(line.enemy)Color(0xFFFFDDD0)else Color(0xFFD8FFF1)
    val edge=if(line.enemy)Color(0xFFD47C68)else Color(0xFF70B8AE)
    val shape=RoundedCornerShape(if(line.thought)22.dp else 12.dp)
    val alpha=remember(line){Animatable(0f)}
    LaunchedEffect(line){alpha.animateTo(1f,tween(170))}
    @Composable fun Tail(){
        Canvas(Modifier.fillMaxWidth().height(if(line.thought)11.dp else 9.dp)){
            val x=size.width*tail.coerceIn(.13f,.87f);val h=size.height
            if(line.thought){
                drawCircle(edge,h*.29f,Offset(x,if(below)h*.70f else h*.28f));drawCircle(fill,h*.22f,Offset(x,if(below)h*.70f else h*.28f))
                drawCircle(edge,h*.17f,Offset(x+5.dp.toPx(),if(below)h*.16f else h*.83f));drawCircle(fill,h*.12f,Offset(x+5.dp.toPx(),if(below)h*.16f else h*.83f))
            }else{
                val base=if(below)h else 0f;val tip=if(below)0f else h
                val path=Path().apply{moveTo(x-7.dp.toPx(),base);lineTo(x+7.dp.toPx(),base);lineTo(x+2.dp.toPx(),tip);close()}
                drawPath(path,fill)
                drawLine(edge,Offset(x-7.dp.toPx(),base),Offset(x+2.dp.toPx(),tip),1.dp.toPx())
                drawLine(edge,Offset(x+7.dp.toPx(),base),Offset(x+2.dp.toPx(),tip),1.dp.toPx())
            }
        }
    }
    Column(modifier.graphicsLayer{this.alpha=alpha.value}){
        if(below)Tail()
        Surface(shape=shape,color=fill,shadowElevation=2.dp,modifier=Modifier.fillMaxWidth().border(1.dp,edge,shape)){
            Text(if(showName)line.speaker+": "+line.text else line.text,
                modifier=Modifier.padding(horizontal=9.dp,vertical=if(compact)4.dp else 6.dp),
                color=Color(0xFF172C3A),style=if(compact)MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                maxLines=if(showName)5 else if(compact)3 else 4,overflow=TextOverflow.Ellipsis)
        }
        if(!below)Tail()
    }
}
/** Overlay is measured in screen dp, not sprite scale; no pointer handlers, so the battlefield remains tappable. */
@Composable internal fun ComicAt(line:RobotSpeech,anchorX:Dp,anchorY:Dp,compact:Boolean=false,modifier:Modifier=Modifier){
    BoxWithConstraints(modifier.fillMaxSize()){
        val width=minOf(maxWidth,if(compact)168.dp else 205.dp)
        if(width<=0.dp||maxHeight<=0.dp)return@BoxWithConstraints
        val x=ComicPlacement.left(anchorX.value,width.value,maxWidth.value).dp
        val below=anchorY< (if(compact)56.dp else 76.dp)
        val tail=((anchorX-x)/width).coerceIn(.13f,.87f)
        Layout(content={ComicBubble(line,compact=compact,tail=tail,below=below)},modifier=Modifier.fillMaxSize()){measurables,constraints->
            val w=width.roundToPx().coerceIn(1,constraints.maxWidth.coerceAtLeast(1))
            val child=measurables.first().measure(Constraints(minWidth=w,maxWidth=w,maxHeight=constraints.maxHeight))
            val y=ComicPlacement.top(anchorY.toPx(),child.height.toFloat(),constraints.maxHeight.toFloat(),below).toInt()
            layout(constraints.maxWidth,constraints.maxHeight){child.placeRelative(x.roundToPx(),y)}
        }
    }
}
@Composable internal fun ComicPortrait(p:WorldPet,line:RobotSpeech?,reverse:Boolean=false){
    if(line==null)return
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        @Composable fun Portrait(){RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),0f,false,Modifier.size(44.dp))}
        if(!reverse)Portrait()
        ComicBubble(line,Modifier.weight(1f),showName=true)
        if(reverse)Portrait()
    }
}
