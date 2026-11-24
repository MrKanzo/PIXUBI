package com.pixubi.game

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** An isolated, disposable session. Leaving this screen discards all test changes. */
@Composable
internal fun PixubiDeveloperScreen(realPet:Pet?,onExit:()->Unit) {
    var spec by remember { mutableStateOf(DevSpec.from(realPet)) }
    var action by remember { mutableStateOf(DevAction.IDLE) }
    var running by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var seconds by remember { mutableFloatStateOf(0f) }
    var left by remember { mutableStateOf(false) }
    var standingPreview by remember { mutableStateOf(true) }
    LaunchedEffect(running,speed) {
        if(running) {
            var last=withFrameNanos{it}
            while(true) {
                val now=withFrameNanos{it}
                seconds=(seconds+((now-last)/1_000_000_000f).coerceIn(0f,.1f)*speed)%48f
                last=now
            }
        }
    }
    val pet=remember(spec,action){spec.pet(action)}
    val control=DevScene(action,seconds,left)
    val imported=ImportedPixuArt.form(LocalContext.current.applicationContext,spec.species,spec.stage,spec.sex)
    Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🛠 Режим розробника",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("ТЕСТ • зміни не зберігаються",color=Color(0xFFFFD58A),style=MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick=onExit){Text("Вимкнути")}
        }
        // Keep the actual game scene visible while scrolling the controls.
        WorldScene(pet,action.emotion,onPetTap={
            action=DevAction.HAPPY;seconds=0f
        },developer=control)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={running=!running},modifier=Modifier.weight(1f)){
                Text(if(running)"⏸ Пауза" else "▶ Старт")
            }
            OutlinedButton(onClick={running=false;seconds=(seconds+.125f)%48f},
                modifier=Modifier.weight(1f)){Text("+ Кадр")}
            OutlinedButton(onClick={seconds=0f},modifier=Modifier.weight(1f)){Text("↺ Спочатку")}
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Швидкість",fontWeight=FontWeight.Bold)
            DevChoices(listOf(.25f,.5f,1f,2f),speed,{speed=it}){ "${it}×" }
            Text("Час сцени: ${(seconds*10).toInt()/10f} с",style=MaterialTheme.typography.bodySmall)
            Slider(value=seconds,onValueChange={running=false;seconds=it},valueRange=0f..47.99f)
            if(action==DevAction.EXPLORE) {
                Text("Фаза пошуку: "+PixubiSearch.frame(spec.species,control.scoutCycle,spec.color).phase.name,
                    color=Color(0xFF98E6CD),style=MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("Підхід" to .12f,"Ховається" to .37f,"Схований" to .50f,"Виходить" to .70f).forEach{(label,t)->
                        OutlinedButton(onClick={running=false;seconds=t*12f},
                            contentPadding=PaddingValues(horizontal=5.dp),modifier=Modifier.weight(1f)){
                            Text(label,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if(imported!=null && ImportedWalkCandidate.available(spec.species,spec.stage,spec.sex)){
                Text("Кандидат ходьби • Лісик, хлопчик, малий",fontWeight=FontWeight.Bold)
                DevChoices(listOf(false,true),standingPreview,{standingPreview=it;seconds=0f}){if(it)"Стійка v2" else "Лапи v1"}
                WalkCandidatePreview(control.movementCycle,left,Modifier.fillMaxWidth().height(128.dp),standing=standingPreview)
                Text(if(standingPreview)"Стійка v2: корпус піднято, чотири лапи, спільна лінія опори. Тестова анімація без заміни ходи у світі." else "Лапи v1: корпус залишається сидячим. Попередній варіант для порівняння.",
                    color=Color(0xFFFFD58A),style=MaterialTheme.typography.bodySmall)
            }
            Text("Дії та емоції",fontWeight=FontWeight.Bold)
            DevAction.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    row.forEach { value ->
                        FilterChip(selected=action==value,onClick={action=value;seconds=0f},
                            label={Text(value.title,style=MaterialTheme.typography.labelSmall)},
                            modifier=Modifier.weight(1f))
                    }
                    repeat(3-row.size){Spacer(Modifier.weight(1f))}
                }
            }
            if(imported!=null)
                Text("Новий набір • тест. Міміка, рух лап, плавання й політ для відсутніх поз — тестові. Художніх кадрів: "+imported.frames.size+". Не готові: "+imported.missing.joinToString(", "),
                    color=Color(0xFFFFD58A),style=MaterialTheme.typography.bodySmall)
            else if(spec.species==Species.KRAPLYK && spec.stage==GrowthStage.BABY)
                Text("Краплик: 4 кадри плавання з оригінальної моделі. Дія «Пошук» перевіряє занурення; решта поз ще потребує художніх кадрів.",
                    color=Color(0xFF98E6CD),style=MaterialTheme.typography.bodySmall)
            else if(LisykStandardArtwork.matches(spec.species,spec.stage))
                Text("Новий піксельний набір: 16 поз • художня перевірка v0.33.",
                    color=Color(0xFF98E6CD),style=MaterialTheme.typography.bodySmall)
            else if(PetPosePack.forPet(spec.species,spec.stage)==null)
                Text("Для цієї форми деякі рухи ще не мають готових художніх кадрів. Режим покаже поточну реалізацію, а не вигадану анімацію.",
                    color=Color(0xFFFFD58A),style=MaterialTheme.typography.bodySmall)
            if(imported!=null && spec.stage==GrowthStage.ULTRA){
                OutlinedButton(onClick={action=DevAction.HAPPY;seconds=0f;running=true},
                    modifier=Modifier.fillMaxWidth()){Text("✨ "+ImportedUltraEffects.title(spec.species))}
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Рух ліворуч",modifier=Modifier.weight(1f))
                Switch(checked=left,onCheckedChange={left=it})
            }
            HorizontalDivider()
            Text("Усі персонажі та форми",fontWeight=FontWeight.Bold)
            DevChoices(Species.entries.toList(),spec.species,{spec=spec.copy(species=it,region=it.native);seconds=0f}){it.icon+" "+it.title}
            DevChoices(GrowthStage.playable.toList(),spec.stage,{spec=spec.copy(stage=it);seconds=0f}){it.icon+" "+it.title}
            DevChoices(Sex.entries.toList(),spec.sex,{spec=spec.copy(sex=it)}){it.title}
            Text("Забарвлення",style=MaterialTheme.typography.labelMedium)
            DevChoices((0..5).toList(),spec.color,{spec=spec.copy(color=it)}){"№${it+1}"}
            Text("Регіон",fontWeight=FontWeight.Bold)
            DevChoices(Region.entries.toList(),spec.region,{spec=spec.copy(region=it)}){it.icon+" "+it.title}
            HorizontalDivider()
            Text("Потреби тестового Піксу",fontWeight=FontWeight.Bold)
            Text("Вибери «Автономно», щоб перевірити природну реакцію на потреби.",style=MaterialTheme.typography.bodySmall)
            DevNeed("Їжа",spec.hunger){spec=spec.copy(hunger=it)}
            DevNeed("Вода",spec.thirst){spec=spec.copy(thirst=it)}
            DevNeed("Енергія",spec.energy){spec=spec.copy(energy=it)}
            DevNeed("Настрій",spec.happiness){spec=spec.copy(happiness=it)}
            Text("Предмети в сцені",fontWeight=FontWeight.Bold)
            listOf("Будиночок" to spec.home,"Тарілочка" to spec.bowl,
                "Стаканчик" to spec.cup,"Зарядна станція" to spec.charger).forEachIndexed{i,item->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(item.first,modifier=Modifier.weight(1f))
                    Switch(checked=item.second,onCheckedChange={checked->
                        spec=when(i){0->spec.copy(home=checked);1->spec.copy(bowl=checked)
                            2->spec.copy(cup=checked);else->spec.copy(charger=checked)}
                    })
                }
            }
            OutlinedButton(onClick={spec=DevSpec.from(realPet);action=DevAction.IDLE;seconds=0f;speed=1f;left=false;running=true},
                modifier=Modifier.fillMaxWidth()){Text("Скинути тестові зміни")}
            Text("Справжній вихованець живе за звичайним ігровим часом. Пауза стосується лише тестової анімації. Після вимкнення всі закриті форми, запаси й предмети залишаться такими, як у звичайній грі.",
                color=Color(0xFFBFCFE9),style=MaterialTheme.typography.bodySmall)
            Button(onClick=onExit,modifier=Modifier.fillMaxWidth()){Text("Вимкнути режим і повернутися до гри")}
            Spacer(Modifier.height(10.dp))
        }
    }
}
@Composable
private fun <T> DevChoices(values:List<T>,selected:T,onSelect:(T)->Unit,label:(T)->String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        values.forEach{value->FilterChip(selected=selected==value,onClick={onSelect(value)},label={Text(label(value))})}
    }
}
@Composable
private fun DevNeed(label:String,value:Int,onChange:(Int)->Unit) {
    Text("$label: $value%",style=MaterialTheme.typography.bodySmall)
    Slider(value=value.toFloat(),onValueChange={onChange(it.toInt())},valueRange=0f..100f)
}
