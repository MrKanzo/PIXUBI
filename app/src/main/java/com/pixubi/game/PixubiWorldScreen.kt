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
internal fun PixubiWorldScreen(onSettings:()->Unit,onSeries:(RobotSeries)->Unit,onSound:(PixubiSound)->Unit,onMusicScene:(MusicScene)->Unit={}){
    val context=LocalContext.current
    var pet by remember(context){mutableStateOf(WorldStorage.load(context)?.let{WorldRules.tick(it,System.currentTimeMillis())})}
    var page by remember{mutableStateOf("home")}
    var guideProgress by remember(context){mutableStateOf(GuideStore.load(context))}
    var guideOpen by remember{mutableStateOf(false)}
    fun updateGuide(next:GuideProgress){guideProgress=next;GuideStore.save(context,next)}
    var tab by remember{mutableStateOf<RobotSlot?>(null)}
    var category by remember{mutableStateOf<WorldCategory?>(null)}
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    var careAnimation by remember{mutableStateOf<RobotAction?>(null)}
    var careEvent by remember{mutableIntStateOf(0)}
    var fx by remember{mutableStateOf(WorldFx.NONE)}
    var fxEvent by remember{mutableIntStateOf(0)}
    fun effect(next:WorldFx){fx=next;fxEvent++}
    var archiveSeries by remember{mutableStateOf(RobotSeries.CLASSIC)}
    var archiveTier by remember{mutableStateOf(RobotTier.MK1)}
    var profileName by remember{mutableStateOf(WorldProfile.load(context))}
    fun save(p:WorldPet){val before=pet;WorldStorage.save(context,p);pet=p;onSeries(p.series);if(before!=null&&p.tier!=before.tier){effect(WorldFx.EVOLVE);onSound(PixubiSound.HAPPY)}}
    val foreground=LocalPixubiForeground.current
    LaunchedEffect(foreground,pet?.mission?.until,pet?.mode,pet?.adventure?.battle!=null){
        if(!foreground)return@LaunchedEffect
        while(true){
            val time=System.currentTimeMillis()
            if(pet?.mission!=null)now=time
            pet?.let{val q=WorldRules.tick(it,time);if(q!=it)save(q)}
            delay(if(pet?.mission!=null)1000L else 15000L)
        }
    }
    LaunchedEffect(careEvent){if(careAnimation!=null){delay(1600);careAnimation=null}}
    BackHandler(enabled=page!="home"){page="home"}
    val p=pet
    val musicScene=when{p?.adventure?.battle!=null->MusicScene.BATTLE;page=="adventure"&&p!=null->MusicScene.EXPEDITION;else->MusicScene.BASE}
    LaunchedEffect(musicScene){onMusicScene(musicScene)}
    if(p==null){
        GenesisLab(onBack=onSettings,onAdopt={ newborn->
            if(WorldStorage.load(context)==null){
                WorldRules.adoptGenesis(newborn,System.currentTimeMillis())?.let{
                    save(it)
                    if(!guideProgress.dismissed&&!guideProgress.finished)guideOpen=true
                    GenesisStorage.save(context,null,world=true)
                    onSound(PixubiSound.HAPPY)
                }
            }
        },world=true,onSound=onSound)
        return
    }
    if(guideOpen)PixubiGuide(guideProgress,onProgress={updateGuide(it)},onClose={guideOpen=false},onOpen={destination->
        guideOpen=false
        if(p.adventure.battle==null){page=destination;tab=null;category=null}
    },initialLesson=when{p.adventure.battle!=null->GuideLesson.BATTLE;page=="adventure"->GuideLesson.MAP;else->null})
    if(page=="adventure"||p.adventure.battle!=null){
        AdventureScreen(p,onChange={save(it)},onBack={page="home"},onSound=onSound,onGuide={guideOpen=true})
        return
    }
    val action=when{p.mode!=WorldMode.AWAKE->RobotAction.SLEEP;p.mission!=null->p.mission.task.action;else->careAnimation?:RobotAction.IDLE}
    val period=WorldMotion.period(p.series,RobotAction.IDLE)
    val phase by rememberGamePhase(period,RenderBudget.SCENERY_FPS,enabled=page=="archive")
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("PIXUBI · WORLD",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
            TextButton(onClick={guideOpen=true}){Text("Академія")}
            OutlinedButton(onClick=onSettings){Text("⚙")}
        }
            val prog=RobotProgress(p.xp)
            Text("${p.name} · ${p.series.title}",style=MaterialTheme.typography.titleLarge)
            Text("${p.tier.title} · Рівень ${p.level} · 🪙 ${p.coins} · ${p.trait.title}")
            LinearProgressIndicator(progress={prog.fraction},modifier=Modifier.fillMaxWidth())
            Text(if(prog.remaining==0)"Максимальний рівень · OMEGA: 10 перемог і 10 експедицій" else "${p.xp} XP · До рівня ${prog.remaining} XP",style=MaterialTheme.typography.bodySmall)
            val navigation=listOf(listOf("home" to "База","missions" to "Місії","adventure" to "Експедиція","shop" to "Магазин"),
                listOf("inventory" to "Інвентар","stats" to "Розвиток","archive" to "Архів","profile" to "Профіль"))
            navigation.forEach{group->Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                group.forEach{(id,label)->FilterChip(page==id,{page=id;if(id=="archive"){archiveSeries=p.series;archiveTier=p.tier}},label={Text(label)})}
            }}
            if(page=="home"){
                if(!guideProgress.finished&&!guideProgress.dismissed)Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("Академія PIXUBI · ${guideProgress.completed.size}/${GuideLesson.entries.size}",style=MaterialTheme.typography.titleMedium)
                        Text("Наступний урок: ${guideProgress.next?.title.orEmpty()}")
                        Button(onClick={guideOpen=true}){Text("Продовжити навчання")}
                    }
                }
                Text(HabitatCatalog.of(p.series).title,style=MaterialTheme.typography.titleMedium)
                val face=when{p.mode!=WorldMode.AWAKE->RobotFace.SLEEP;p.integrity<30||p.mood<25->RobotFace.SAD;action==RobotAction.ATTACK->RobotFace.ANGRY;p.mood>75->RobotFace.HAPPY;else->RobotFace.NEUTRAL}
                WorldShowcase(p,action,if(fx==WorldFx.CONTACT&&careAnimation!=null)RobotFace.HAPPY else face,fx,fxEvent,
                    Modifier.fillMaxWidth().height(320.dp),roam=true,onTap={
                        if(p.mode==WorldMode.AWAKE&&p.mission==null){
                            careAnimation=RobotAction.WAVE;careEvent++;effect(WorldFx.CONTACT);onSound(PixubiSound.TAP)
                        }
                    })
                WorldMeter("Енергія",p.energy);WorldMeter("Міцність",p.integrity);WorldMeter("Настрій",p.mood)
                Text(p.message,style=MaterialTheme.typography.bodyMedium)
                val m=p.mission
                if(m!=null){
                    Text("${m.task.title} · ${((m.until-now+999)/1000).coerceAtLeast(0)} с")
                    LinearProgressIndicator(progress={1f-((m.until-now).coerceAtLeast(0L).toFloat()/(m.task.seconds*1000f)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                }
                if(p.mode==WorldMode.AWAKE){
                    Text("Технічне обслуговування")
                    val extras=WorldSupply.entries.filter{!it.isAmmo&&p.stock(it)>0}
                    if(extras.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        extras.forEach{kind->OutlinedButton(onClick={val q=WorldRules.useSupply(p,kind);save(q);if(q.stock(kind)<p.stock(kind)){careAnimation=if(kind.category==WorldCategory.ENERGY)RobotAction.CHARGE else RobotAction.WAVE;careEvent++;effect(when(kind.category){WorldCategory.ENERGY->WorldFx.CHARGE;WorldCategory.REPAIR->WorldFx.REPAIR;else->WorldFx.SYNC});onSound(PixubiSound.HAPPY)}},enabled=m==null){Text(kind.title+" "+p.stock(kind))}}
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        listOf("Заряд ${p.cells}","Ремонт ${p.repair}","Синхронізація ${p.snacks}").forEachIndexed{i,label->
                            OutlinedButton(onClick={val q=WorldRules.care(p,i);save(q);if(q.xp!=p.xp||q.mood!=p.mood){careAnimation=if(i==0)RobotAction.CHARGE else RobotAction.WAVE;careEvent++;effect(when(i){0->WorldFx.CHARGE;1->WorldFx.REPAIR;else->WorldFx.SYNC});onSound(if(i==0)PixubiSound.HAPPY else PixubiSound.TAP)}},enabled=m==null){Text(label)}
                        }
                    }
                    Button(onClick={page="adventure"},modifier=Modifier.fillMaxWidth()){Text("🗺 Експедиція · дослідити світ")}
                    OutlinedButton(onClick={page="missions"},modifier=Modifier.fillMaxWidth()){Text("Відкрити центр місій")}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(onClick={save(WorldRules.mode(p,WorldMode.SLEEP,System.currentTimeMillis()))},enabled=m==null){Text("Сон")}
                        OutlinedButton(onClick={save(WorldRules.mode(p,WorldMode.HIBERNATE,System.currentTimeMillis()))},enabled=m==null){Text("Сплячка")}
                    }
                }else Button(onClick={save(WorldRules.mode(p,WorldMode.AWAKE,System.currentTimeMillis()))}){Text("Розбудити")}
            }else if(page=="missions"){
                Text("Центр місій",style=MaterialTheme.typography.titleLarge)
                Text(p.message)
                val m=p.mission
                if(m!=null){
                    Text("${m.task.title} · ${((m.until-now+999)/1000).coerceAtLeast(0)} с")
                    LinearProgressIndicator(progress={1f-((m.until-now).coerceAtLeast(0L).toFloat()/(m.task.seconds*1000f)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                    WorldShowcase(p,action,if(action==RobotAction.ATTACK)RobotFace.ANGRY else RobotFace.NEUTRAL,fx,fxEvent,Modifier.fillMaxWidth().height(265.dp),roam=true)
                }
                val ammoCost=WorldGearCatalog.ammoCost(p.equipment)
                if(ammoCost.isNotEmpty())Text("Арена · "+ammoCost.entries.joinToString(" · "){(kind,n)->kind.title+" "+p.stock(kind)+"/"+n})
                WorldTask.entries.forEach{task->
                    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(task.title,style=MaterialTheme.typography.titleMedium)
                        Text("${task.seconds} с · -${AdventureBalance.taskEnergy(p,task)} ⚡ · до ${task.xp} XP · ${task.coins} монет")
                        Button(onClick={save(WorldRules.start(p,task,System.currentTimeMillis()));onSound(PixubiSound.TAP)},
                            enabled=m==null&&p.mode==WorldMode.AWAKE&&p.energy>=AdventureBalance.taskEnergy(p,task)&&p.integrity>=20,modifier=Modifier.fillMaxWidth()){Text("Розпочати")}
                    }}
                }
            }else if(page=="archive"){
                Text("Архів серій · 8 серій / 40 форм",style=MaterialTheme.typography.titleLarge)
                Text("Твоя активована серія: ${p.series.title}. Інші серії доступні як довідник.")
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    RobotSeries.entries.forEach{s->FilterChip(archiveSeries==s,{archiveSeries=s;archiveTier=RobotTier.MK1},label={Text(s.title)})}
                }
                val info=WorldCatalog.info(archiveSeries)
                Text(info.role,style=MaterialTheme.typography.titleMedium);Text(info.description);Text(info.origin)
                RobotTier.entries.forEach{tier->
                    val unlocked=archiveSeries==p.series&&WorldCatalog.unlocked(p,tier)
                    OutlinedButton(onClick={archiveTier=tier},enabled=unlocked||tier==RobotTier.MK1,modifier=Modifier.fillMaxWidth()){
                        Text(tier.title+" · рівень "+tier.requiredLevel+if(unlocked)" ✓" else if(tier==RobotTier.MK1)" · довідник" else " · заблоковано")
                    }
                }
                HabitatScene(archiveSeries,phase,Modifier.fillMaxWidth().height(250.dp)){
                    RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,
                        RobotLoadout(if(archiveSeries==p.series)p.skin.accent else RobotSkin.CYAN.accent,RobotEquipment(),archiveTier,archiveSeries),phase,false,Modifier.fillMaxSize())
                }
                Text(HabitatCatalog.of(archiveSeries).title)
            }else if(page=="profile"){
                Text("Профіль оператора",style=MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick={guideOpen=true}){Text("Академія · ${guideProgress.completed.size}/${GuideLesson.entries.size}")}
                TextButton(onClick={updateGuide(GuideProgress());guideOpen=true}){Text("Повторити навчання з початку")}
                OutlinedTextField(profileName,{profileName=it.take(24);WorldProfile.save(context,profileName)},label={Text("Ім’я оператора")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Text("Активний роботик: ${p.name} · ${p.series.title}")
                Text("Характер: ${p.trait.title} · колір: ${p.skin.title}")
                Text("Рівень ${p.level} · ${p.xp} XP")
                Text("Перемоги: ${p.wins} · Експедиції: ${p.expeditions}")
                Text("Відкриті форми: ${p.tier.ordinal+1}/5 · Предмети: ${p.owned.size}")
                Text("Спеціалізація: "+p.skills.specialties.joinToString(", "){it.title}.ifBlank{"ще не визначена"})
                OutlinedButton(onClick={page="archive";archiveSeries=p.series;archiveTier=p.tier}){Text("Відкрити архів")}
                OutlinedButton(onClick={page="stats"}){Text("Характеристики")}
            }else if(page=="shop"||page=="inventory"){
                Text(p.message)
                if(page=="shop"){
                    Text("Витратні матеріали · 25 монет за 3 шт.")
                    listOf("Енергокомірки","Ремонтні набори","Модулі синхронізації").forEachIndexed{i,label->
                        OutlinedButton(onClick={save(WorldRules.supplies(p,i))},enabled=p.coins>=25,modifier=Modifier.fillMaxWidth()){Text("$label +3 · 25 🪙")}
                    }
                }
                Text(if(page=="shop")"Модулі для ${p.series.title}" else "Інвентар · ${p.series.title}")
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    FilterChip(category==null,{category=null;tab=null},label={Text("Усі")})
                    WorldCategory.entries.forEach{c->FilterChip(category==c,{category=c;tab=null},label={Text(c.title)})}
                }
                val consumables=WorldSupply.entries.filter{(category==null||category==it.category)&&(page=="shop"||p.stock(it)>0)}
                if(consumables.isNotEmpty())Text("Боєприпаси й витратні модулі",style=MaterialTheme.typography.titleMedium)
                consumables.forEach{kind->
                    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(kind.title+" · "+p.stock(kind)+" шт.",style=MaterialTheme.typography.titleMedium)
                        Text(kind.description)
                        if(page=="shop")Button(onClick={save(WorldRules.buySupply(p,kind))},enabled=p.coins>=kind.price&&p.level>=kind.level&&p.stock(kind)+kind.pack<=99){
                            Text("+${kind.pack} шт. · ${kind.price} 🪙 · рівень ${kind.level}")
                        }
                        else if(!kind.isAmmo)OutlinedButton(onClick={save(WorldRules.useSupply(p,kind));onSound(PixubiSound.HAPPY)},enabled=p.mode==WorldMode.AWAKE&&p.mission==null){Text("Використати")}
                    }}
                }
                WorldShowcase(p,RobotAction.IDLE,RobotFace.NEUTRAL,WorldFx.NONE,0,Modifier.fillMaxWidth().height(230.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    FilterChip(tab==null,{tab=null},label={Text("Усі слоти")})
                    RobotSlot.entries.forEach{slot->FilterChip(tab==slot,{tab=slot},label={Text(WorldCatalog.slots(p.series,slot))})}
                }
                val offers=WorldRules.shop.filter{(tab==null||it.gear.slot==tab)&&(category==null||WorldGearCatalog.category(it.gear)==category)&&(page=="shop"||it.gear in p.owned)}
                if(offers.isEmpty())Text("Поки порожньо.")
                offers.forEach{o->
                    val g=o.gear
                    val owned=g in p.owned
                    val equipped=p.equipment.equipped(g.slot)==g
                    Card(Modifier.fillMaxWidth()){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Text(g.title,style=MaterialTheme.typography.titleMedium)
                            Text(RobotBalance.spec(g).delta.summary(),style=MaterialTheme.typography.bodySmall)
                            Text(WorldCatalog.slots(p.series,g.slot)+" · "+WorldGearCatalog.description(g),style=MaterialTheme.typography.bodySmall)
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
        Spacer(Modifier.height(12.dp))
    }
}
@Composable private fun WorldMeter(label:String,value:Int){
    Text("$label · $value%",style=MaterialTheme.typography.bodySmall)
    LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
}

internal object WorldProfile{
    fun load(c:android.content.Context)=c.getSharedPreferences("pixubi_world_profile_v1",android.content.Context.MODE_PRIVATE).getString("operator","").orEmpty()
    fun save(c:android.content.Context,name:String){c.getSharedPreferences("pixubi_world_profile_v1",android.content.Context.MODE_PRIVATE).edit().putString("operator",name.take(24)).apply()}
}
