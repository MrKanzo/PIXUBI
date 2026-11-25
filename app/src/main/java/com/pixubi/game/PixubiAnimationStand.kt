package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Screen recording stand: all cells use the same selected synthetic identity and clock. */
@Composable
internal fun PixubiAnimationStand(spec:DevSpec,onSpec:(DevSpec)->Unit,onExit:()->Unit,recordingFirst:Boolean=false){
    val view=LocalView.current
    DisposableEffect(view){
        val previous=view.keepScreenOn
        view.keepScreenOn=true
        onDispose{view.keepScreenOn=previous}
    }
    var running by remember{mutableStateOf(!recordingFirst)}
    var speed by remember{mutableFloatStateOf(1f)}
    var seconds by remember{mutableFloatStateOf(0f)}
    var focused by remember{mutableIntStateOf(if(recordingFirst)0 else -1)}
    var automatic by remember{mutableStateOf(recordingFirst)}
    var finished by remember{mutableStateOf(false)}
    var left by remember{mutableStateOf(false)}
    val items=AnimationStandPlan.items
    BackHandler {
        if(focused>=0||automatic){focused=-1;automatic=false;seconds=0f}
        else onExit()
    }
    LaunchedEffect(running,speed,focused,automatic,spec){
        if(running){
            var previous=withFrameNanos{it}
            var accumulated=seconds
            val fps=if(focused>=0||automatic)32f else 16f
            while(true){
                val now=withFrameNanos{it}
                accumulated+=((now-previous)/1_000_000_000f).coerceIn(0f,.1f)*speed
                if(automatic&&accumulated>=AnimationStandPlan.duration){
                    seconds=AnimationStandPlan.duration-1f/32f
                    finished=true;running=false;break
                }
                if(!automatic)accumulated%=48f
                seconds=kotlin.math.floor(accumulated*fps)/fps
                previous=now
            }
        }
    }
    val chosen=if(automatic)AnimationStandPlan.position(seconds) else AnimationStandPlan.Position(focused,seconds)
    val context=LocalContext.current.applicationContext
    val form=remember(context,spec.species,spec.stage,spec.sex){
        ImportedPixuArt.form(context,spec.species,spec.stage,spec.sex)
    }
    val sceneSpec=spec.copy(bowl=true,cup=true,charger=true)
    val reset:()->Unit={seconds=0f;finished=false;running=!recordingFirst||!automatic}
    Column(Modifier.fillMaxSize().background(Color(0xFF101A2E)).padding(5.dp),
        verticalArrangement=Arrangement.spacedBy(3.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("🎬 PIXUBI • стенд",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold,
                fontSize=16.sp,color=Color.White)
            TextButton(onClick=onExit,contentPadding=PaddingValues(horizontal=5.dp)){Text("Назад")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
            StandChoice(Species.entries.toList(),spec.species,{it.title},{onSpec(spec.copy(species=it,region=it.native));reset()},Modifier.weight(1.25f))
            StandChoice(GrowthStage.playable,spec.stage,{it.title},{onSpec(spec.copy(stage=it));reset()},Modifier.weight(1f))
            StandChoice(Sex.entries.toList(),spec.sex,{if(it==Sex.BOY)"Хлоп." else "Дів."},{onSpec(spec.copy(sex=it));reset()},Modifier.weight(.8f))
            StandChoice((0..5).toList(),spec.color,{"№${it+1}"},{onSpec(spec.copy(color=it));reset()},Modifier.weight(.65f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
            StandButton(if(running)"Ⅱ" else "▶",{if(finished){seconds=0f;finished=false};running=!running},Modifier.weight(.6f))
            StandButton("↺",{running=false;finished=false;seconds=0f},Modifier.weight(.6f))
            StandButton("+1/16",{running=false;seconds=(seconds+1f/16f)%(if(automatic)AnimationStandPlan.duration else 48f)},Modifier.weight(1f))
            StandButton("${speed}×",{speed=when(speed){1f->.5f;.5f->.25f;.25f->2f;else->1f}},Modifier.weight(.9f))
            StandButton(if(left)"←" else "→",{left=!left},Modifier.weight(.6f))
            StandButton(if(automatic||focused>=0)"Сітка" else "Автопоказ",{
                if(automatic||focused>=0){automatic=false;focused=-1}
                else {automatic=true;focused=0}
                reset()
            },Modifier.weight(1.4f))
        }
        if(automatic){
            Button(onClick={seconds=0f;finished=false;running=true},modifier=Modifier.fillMaxWidth()){
                Text(if(finished)"↺ Повторити всі рухи" else "▶ ПОЧАТИ ПОКАЗ УСІХ РУХІВ")
            }
            Text(if(finished)"Готово: усі 20 сцен показано. Можна зупинити запис."
                else if(!running)"Увімкни запис екрана, потім натисни «Почати показ»."
                else "Записуй екран • дії змінюються автоматично",
                color=Color(0xFF98E6CD),fontSize=12.sp)
        }
        Text("${spec.species.title} • ${spec.sex.title} • ${spec.stage.title} • колір ${spec.color+1} • ${if(form!=null)"новий арт" else "старий арт"}",
            color=Color(0xFFBFD0E7),fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        if(automatic||focused>=0){
            val item=items[chosen.index.coerceIn(items.indices)]
            Text("${chosen.index+1}/${items.size}  ${item.title}   ${(chosen.seconds*10).toInt()/10f} с",
                fontWeight=FontWeight.Bold,color=Color.White,fontSize=18.sp)
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
                StandScene(sceneSpec,item,chosen.seconds,left,form,Modifier.fillMaxSize())
            }
            val note=if(item.action==null)
                if(form!=null&&ImportedSleepCandidate.available(spec.species,spec.stage,spec.sex))"Окремий тестовий цикл; не перехід у звичайній грі" else "Окремі кадри переходу відсутні"
                else if(!AnimationStandPlan.supported(spec.species,item))"Ця дія не властива виду"
                else if(form!=null)ImportedActionAudit.describe(form,spec.species,spec.stage,spec.sex,item.action,item.action==DevAction.WALK)
                else "Старий графічний набір"
            Text(note,color=Color(0xFFFFD58A),fontSize=12.sp)
            if(automatic)LinearProgressIndicator(progress={if(finished)1f else seconds/AnimationStandPlan.duration},modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                StandButton("‹ Попередня",{
                    focused=(chosen.index-1+items.size)%items.size;automatic=false;reset()
                },Modifier.weight(1f))
                StandButton("Наступна ›",{
                    focused=(chosen.index+1)%items.size;automatic=false;reset()
                },Modifier.weight(1f))
            }
        }else{
            Text("Усі сцени одночасно • натисни плитку для збільшення",color=Color(0xFFBFD0E7),fontSize=11.sp)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
                val columns=if(maxWidth>maxHeight)5 else 4
                val rows=(items.size+columns-1)/columns
                Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(3.dp)){
                    repeat(rows){row->
                        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
                            repeat(columns){col->
                                val index=row*columns+col
                                if(index>=items.size)Spacer(Modifier.weight(1f))
                                else key(index){
                                    val item=items[index]
                                    val supported=AnimationStandPlan.supported(spec.species,item)
                                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF19253C)).border(1.dp,Color(0xFF40536C),RoundedCornerShape(5.dp))){
                                        Text("${index+1}. ${item.title}",fontSize=10.sp,lineHeight=11.sp,
                                            color=if(supported)Color.White else Color(0xFFFFD58A),
                                            maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=3.dp,vertical=2.dp))
                                        Box(Modifier.weight(1f).fillMaxWidth()){
                                            StandScene(sceneSpec,item,seconds,left,form,Modifier.fillMaxSize())
                                            Box(Modifier.matchParentSize().clickable{focused=index;automatic=false;reset()})
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("ТЕСТ • прогрес не зберігається • ${if(running)"відтворення" else "пауза"}",
            color=Color(0xFF98E6CD),fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
    }
}

@Composable
private fun StandButton(title:String,onClick:()->Unit,modifier:Modifier){
    OutlinedButton(onClick=onClick,modifier=modifier.height(36.dp),
        contentPadding=PaddingValues(horizontal=2.dp)){
        Text(title,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Clip)
    }
}
@Composable
private fun <T> StandChoice(values:List<T>,selected:T,label:(T)->String,onSelect:(T)->Unit,modifier:Modifier){
    var open by remember{mutableStateOf(false)}
    Box(modifier){
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().height(36.dp),
            contentPadding=PaddingValues(horizontal=3.dp)){
            Text(label(selected)+" ▾",fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded=open,onDismissRequest={open=false}){
            values.forEach{v->DropdownMenuItem(text={Text(label(v))},onClick={open=false;onSelect(v)})}
        }
    }
}
@Composable
private fun StandScene(spec:DevSpec,item:AnimationStandPlan.Item,seconds:Float,left:Boolean,
    form:ImportedPixuArt.Form?,modifier:Modifier){
    if(item.action==null){
        Box(modifier,contentAlignment=Alignment.Center){
            if(form!=null&&ImportedSleepCandidate.available(spec.species,spec.stage,spec.sex))
                SleepCandidatePreview(spec.species,seconds,Modifier.fillMaxSize(),stage=spec.stage,sex=spec.sex,gene=spec.color)
            else Text("Немає кадрів переходу",fontSize=10.sp,color=Color(0xFFFFD58A),textAlign=TextAlign.Center)
        }
        return
    }
    val action=item.action
    val pet=remember(spec,action){spec.pet(action)}
    // Render the exact world at its normal logical size, then scale the whole scene.
    // This keeps props, shadows, offsets and cover synchronized at every tile size.
    Layout(modifier=modifier.clip(RoundedCornerShape(3.dp)),content={
        WorldScene(pet,action.emotion,onPetTap={},developer=DevScene(action,seconds,left,gaitPreview=true),reviewClean=true)
    }){measurables,constraints->
        val w=constraints.maxWidth.coerceAtLeast(1)
        val h=constraints.maxHeight.coerceAtLeast(1)
        val logicalW=320.dp.roundToPx()
        val logicalH=286.dp.roundToPx()
        val child=measurables.single().measure(Constraints.fixed(logicalW,logicalH))
        val scale=minOf(w.toFloat()/logicalW,h.toFloat()/logicalH)
        layout(w,h){
            child.placeWithLayer(((w-logicalW*scale)/2).toInt(),((h-logicalH*scale)/2).toInt()){
                transformOrigin=TransformOrigin(0f,0f)
                scaleX=scale;scaleY=scale
            }
        }
    }
}
