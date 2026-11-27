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
        line=RobotSpeech(cue.speaker?:p.name,deck.pick(SpeechLibrary.choices(series,cue.event)),cue.enemy,SpeechEvents.isThought(cue.event),cue.actor)
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
        else if(tapped)state.say(p,SpeechCue(SpeechEvent.TAP),true)
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
        state.line?.let{delay((2600L+it.text.length*45L).coerceIn(3000L,6500L));state.clear()}
    }
    return state
}
