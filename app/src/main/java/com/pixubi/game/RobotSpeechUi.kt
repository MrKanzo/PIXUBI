package com.pixubi.game

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

internal class RobotSpeechState {
    private val deck=SpeechDeck()
    var line by mutableStateOf<RobotSpeech?>(null)
        private set
    private var last=0L
    fun clear(){line=null}
    fun say(p:WorldPet,cue:SpeechCue,force:Boolean=false){
        val now=SystemClock.elapsedRealtime()
        val wait=if(cue.urgent)2300L else if(cue.event==SpeechEvent.WALK)13000L else 9000L
        if(!force&&last!=0L&&now-last<wait)return
        val series=cue.series?:p.series
        line=RobotSpeech(cue.speaker?:p.name,deck.pick(SpeechLibrary.choices(series,cue.event)),cue.enemy)
        last=now
    }
}
@Composable internal fun rememberRobotSpeech(p:WorldPet,page:String,foreground:Boolean,enabled:Boolean,tap:Int):RobotSpeechState{
    val state=remember{RobotSpeechState()}
    var previous by remember{mutableStateOf(p)}
    var previousPage by remember{mutableStateOf("")}
    var previousTap by remember{mutableIntStateOf(tap)}
    val latest by rememberUpdatedState(p)
    LaunchedEffect(p,page,foreground,enabled,tap){
        val event=SpeechEvents.between(previous,p)
        val changed=previousPage!=page
        val tapped=tap!=previousTap
        previous=p;previousPage=page;previousTap=tap
        if(!enabled||!foreground||page=="contact"){state.clear();return@LaunchedEffect}
        if(p.mode!=WorldMode.AWAKE){
            if(event?.event==SpeechEvent.SLEEP)state.say(p,event,true)
            else if(changed)state.clear()
            return@LaunchedEffect
        }
        if(event!=null)state.say(p,event,event.event in setOf(SpeechEvent.VICTORY,SpeechEvent.DEFEAT,SpeechEvent.RETREAT,SpeechEvent.WAKE))
        else if(changed)state.say(p,SpeechCue(SpeechEvents.scene(page)),true)
        else if(tapped)state.say(p,SpeechCue(SpeechEvent.TAP))
        else if(state.line==null)state.say(p,SpeechCue(SpeechEvents.scene(page)),true)
    }
    LaunchedEffect(page,foreground,enabled,p.mode){
        if(!foreground||!enabled||page=="contact"||p.mode!=WorldMode.AWAKE)return@LaunchedEffect
        while(true){
            delay(Random.nextLong(22000L,35001L))
            val q=latest
            if(q.adventure.contact==null&&q.mode==WorldMode.AWAKE)state.say(q,SpeechCue(SpeechEvents.idle(q,page)))
        }
    }
    LaunchedEffect(state.line){
        if(state.line!=null){delay(13000L);state.clear()}
    }
    return state
}
@Composable internal fun RobotSpeechBubble(line:RobotSpeech?,enabled:Boolean,onToggle:()->Unit,compact:Boolean=false){
    if(!enabled){
        Text("◉ Репліки вимкнено · увімкнути",Modifier.clickable(onClick=onToggle).padding(vertical=3.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if(line==null){
        Text("◉ Репліки · тихий режим",Modifier.clickable(onClick=onToggle).padding(vertical=2.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val tint=if(line.enemy)Color(0xFFFFAB9D)else Color(0xFF8BE8DB)
    Surface(color=Color(0xFF182C3C),shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()){
        Row(Modifier.padding(horizontal=7.dp,vertical=if(compact)2.dp else 5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
            Text(if(line.enemy)"◇" else "◉",color=tint,style=MaterialTheme.typography.labelMedium)
            Text(line.speaker+": "+line.text,modifier=Modifier.weight(1f),color=tint,style=MaterialTheme.typography.labelSmall,maxLines=if(compact)1 else 3,overflow=TextOverflow.Ellipsis)
            Text("×",modifier=Modifier.clickable(onClick=onToggle).padding(horizontal=5.dp,vertical=3.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium)
        }
    }
}
