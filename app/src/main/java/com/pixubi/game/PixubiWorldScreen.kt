package com.pixubi.game
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun PixubiWorldScreen(onSettings:()->Unit,onLegacy:()->Unit){
    val context=LocalContext.current
    var pet by remember(context){mutableStateOf(WorldStorage.load(context)?.let{WorldRules.tick(it,System.currentTimeMillis())})}
    var selected by remember{mutableStateOf(RobotSeries.CLASSIC)}
    var name by remember{mutableStateOf("")}
    var page by remember{mutableStateOf("home")}
    var tab by remember{mutableStateOf(RobotSlot.HEAD)}
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    var careAnimation by remember{mutableStateOf<RobotAction?>(null)}
    var careEvent by remember{mutableIntStateOf(0)}
    var legacyConfirm by remember{mutableStateOf(false)}
    fun save(p:WorldPet){WorldStorage.save(context,p);pet=p}
    LaunchedEffect(Unit){pet?.let{save(it)};while(true){delay(1000L);now=System.currentTimeMillis();pet?.let{val q=WorldRules.tick(it,now);if(q!=it)save(q)}}}
    LaunchedEffect(careEvent){if(careAnimation!=null){delay(1600);careAnimation=null}}
    BackHandler(enabled=page!="home"){page="home"}
    val p=pet
    val loop=rememberInfiniteTransition(label="world")
    val action=if(p==null)RobotAction.IDLE else when{p.mode!=WorldMode.AWAKE->RobotAction.SLEEP;p.mission!=null->p.mission.task.action;else->careAnimation?:RobotAction.IDLE}
    val period=when(action){RobotAction.WALK->950;RobotAction.IDLE->3200;else->1600}
    val phase by loop.animateFloat(0f,1f,infiniteRepeatable(tween(period,easing=LinearEasing)),label="world motion")
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("PIXUBI · WORLD",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick=onSettings){Text("⚙")}
        }
        if(p==null){
            Text("Обери свого цифрового компаньйона",style=MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                RobotSeries.entries.forEach{s->FilterChip(selected==s,onClick={selected=s},label={Text(s.title)})}
            }
            Text(HabitatCatalog.of(selected).title)
            HabitatScene(selected,phase,Modifier.fillMaxWidth().height(275.dp)){
                RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(series=selected),phase,false,Modifier.fillMaxSize())
            }
            OutlinedTextField(name,{name=it.take(24)},label={Text("Ім’я")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("Колір і характер випадкові. Початкова форма MK-1.")
            Button(onClick={save(WorldRules.hatch(selected,name,System.currentTimeMillis()))},modifier=Modifier.fillMaxWidth()){Text("Почати життя")}
            OutlinedButton(onClick={legacyConfirm=true}){Text("Попередня гра / збереження")}
        }else{
            val prog=RobotProgress(p.xp)
            Text("${p.name} · ${p.series.title}",style=MaterialTheme.typography.titleLarge)
            Text("${p.tier.title} · Рівень ${p.level} · 🪙 ${p.coins} · ${p.trait.title}")
            LinearProgressIndicator(progress={prog.fraction},modifier=Modifier.fillMaxWidth())
            Text(if(prog.remaining==0)"Максимальний рівень · OMEGA: 10 перемог і 10 експедицій" else "${p.xp} XP · До рівня ${prog.remaining} XP",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                listOf("home" to "Дім","shop" to "Магазин","inventory" to "Інвентар","stats" to "Розвиток").forEach{(id,label)->FilterChip(page==id,{page=id},label={Text(label)})}
            }
            if(page=="home"){
                Text(HabitatCatalog.of(p.series).title,style=MaterialTheme.typography.titleMedium)
                val face=when{p.mode!=WorldMode.AWAKE->RobotFace.SLEEP;p.integrity<30||p.mood<25->RobotFace.SAD;action==RobotAction.ATTACK->RobotFace.ANGRY;p.mood>75->RobotFace.HAPPY;else->RobotFace.NEUTRAL}
                HabitatScene(p.series,phase,Modifier.fillMaxWidth().height(285.dp)){
                    RobotPreview(if(action==RobotAction.WALK||action==RobotAction.ATTACK)RobotView.RIGHT else RobotView.FRONT,action,face,
                        RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),phase,false,Modifier.fillMaxSize())
                }
                WorldMeter("Енергія",p.energy);WorldMeter("Міцність",p.integrity);WorldMeter("Настрій",p.mood)
                Text(p.message,style=MaterialTheme.typography.bodyMedium)
                val m=p.mission
                if(m!=null){
                    Text("${m.task.title} · ${((m.until-now+999)/1000).coerceAtLeast(0)} с")
                    LinearProgressIndicator(progress={1f-((m.until-now).coerceAtLeast(0L).toFloat()/(m.task.seconds*1000f)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                }
                if(p.mode==WorldMode.AWAKE){
                    Text("Догляд")
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        listOf("Заряд ${p.cells}","Ремонт ${p.repair}","Пожива ${p.snacks}").forEachIndexed{i,label->
                            OutlinedButton(onClick={val q=WorldRules.care(p,i);save(q);if(q.xp!=p.xp||q.mood!=p.mood){careAnimation=if(i==0)RobotAction.CHARGE else RobotAction.WAVE;careEvent++}},enabled=m==null){Text(label)}
                        }
                    }
                    Text("Завдання · нагорода після завершення")
                    WorldTask.entries.forEach{task->
                        OutlinedButton(onClick={save(WorldRules.start(p,task,System.currentTimeMillis()))},enabled=m==null&&p.energy>=task.energy&&p.integrity>=20,modifier=Modifier.fillMaxWidth()){
                            Text("${task.title} · ${task.seconds} с · -${task.energy} ⚡ · +${task.xp} XP")
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(onClick={save(WorldRules.mode(p,WorldMode.SLEEP,System.currentTimeMillis()))},enabled=m==null){Text("Сон")}
                        OutlinedButton(onClick={save(WorldRules.mode(p,WorldMode.HIBERNATE,System.currentTimeMillis()))},enabled=m==null){Text("Сплячка")}
                    }
                }else Button(onClick={save(WorldRules.mode(p,WorldMode.AWAKE,System.currentTimeMillis()))}){Text("Розбудити")}
            }else if(page=="shop"||page=="inventory"){
                Text(p.message)
                if(page=="shop"){
                    Text("Витратні матеріали · 25 монет за 3 шт.")
                    listOf("Енергокомірки","Ремонтні набори","Поживні модулі").forEachIndexed{i,label->
                        OutlinedButton(onClick={save(WorldRules.supplies(p,i))},enabled=p.coins>=25,modifier=Modifier.fillMaxWidth()){Text("$label +3 · 25 🪙")}
                    }
                }
                Text(if(page=="shop")"Спорядження для всіх серій" else "Куплене спорядження")
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    RobotSlot.entries.forEach{slot->FilterChip(tab==slot,{tab=slot},label={Text(slot.title)})}
                }
                val offers=WorldRules.shop.filter{it.gear.slot==tab&&(page=="shop"||it.gear in p.owned)}
                if(offers.isEmpty())Text("Поки порожньо.")
                offers.forEach{o->
                    val g=o.gear
                    val owned=g in p.owned
                    val equipped=p.equipment.equipped(g.slot)==g
                    Card(Modifier.fillMaxWidth()){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Text(g.title,style=MaterialTheme.typography.titleMedium)
                            Text(RobotBalance.spec(g).delta.summary(),style=MaterialTheme.typography.bodySmall)
                            if(owned)OutlinedButton(onClick={save(WorldRules.equip(p,g))},enabled=p.mission==null){Text(if(equipped)"Зняти" else "Одягнути")}
                            else Button(onClick={save(WorldRules.buy(p,g))},enabled=p.level>=o.level&&p.coins>=o.price){Text("${o.price} 🪙 · рівень ${o.level}")}
                        }
                    }
                }
            }else{
                Text("Характеристики · ${p.tier.title}",style=MaterialTheme.typography.titleMedium)
                RobotStat.entries.forEach{stat->Row(Modifier.fillMaxWidth()){Text(stat.title,Modifier.weight(1f));Text(p.stats[stat].toString())}}
                Text("Еволюції")
                RobotTier.entries.forEach{tier->Text("${tier.title} · рівень ${tier.requiredLevel}"+if(p.tier==tier)" ✓" else "")}
                Text("OMEGA: 30 рівень · перемоги ${p.wins}/10 · експедиції ${p.expeditions}/10")
                Text("Навички")
                RobotSkill.entries.forEach{k->Text("${k.title} · ${p.skills.level(k)}/10");LinearProgressIndicator(progress={p.skills.fraction(k)},modifier=Modifier.fillMaxWidth())}
            }
            OutlinedButton(onClick={legacyConfirm=true}){Text("Попередня гра / збереження")}
        }
        Spacer(Modifier.height(12.dp))
    }
    if(legacyConfirm)AlertDialog(onDismissRequest={legacyConfirm=false},title={Text("Попередній світ?")},text={Text("Збереження нового й попереднього світу окремі. Повернутися можна через Налаштування → Світ PIXUBI.")},confirmButton={TextButton(onClick={legacyConfirm=false;onLegacy()}){Text("Перейти")}},dismissButton={TextButton(onClick={legacyConfirm=false}){Text("Скасувати")}})
}
@Composable private fun WorldMeter(label:String,value:Int){
    Text("$label · $value%",style=MaterialTheme.typography.bodySmall)
    LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
}
