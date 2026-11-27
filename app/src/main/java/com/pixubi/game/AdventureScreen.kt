package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.pm.ActivityInfo
import kotlinx.coroutines.delay

@Composable
internal fun AdventureScreen(p:WorldPet,onChange:(WorldPet)->Unit,onBack:()->Unit,onSound:(PixubiSound)->Unit,onGuide:()->Unit={},returnHome:Boolean=false,speech:RobotSpeech?=null,speechEnabled:Boolean=true,onSpeechToggle:()->Unit={}){
    val a=p.adventure;val b=a.battle
    var retreat by remember{mutableStateOf(false)}
    var tacticsPanel by remember{mutableStateOf(false)}
    var skillsPanel by remember{mutableStateOf(false)}
    var showGrid by remember{mutableStateOf(true)}
    var artifactsOpen by remember{mutableStateOf(false)}
    var deployPicked by remember(b?.encounter){mutableStateOf(false)}
    var weapon by remember{mutableStateOf<RobotGear?>(null)}
    var moving by remember{mutableStateOf(false)}
    var attack by remember{mutableStateOf(false)}
    var direction by remember{mutableStateOf(RobotView.RIGHT)}
    var actorChoice by remember{mutableIntStateOf(-1)}
    var targetChoice by remember{mutableIntStateOf(-1)}
    var enemyVisual by remember{mutableIntStateOf(-1)}
    val context=LocalContext.current
    val settings=remember(context){context.getSharedPreferences("pixubi_arena_settings",android.content.Context.MODE_PRIVATE)}
    var speed by remember{mutableStateOf(settings.getFloat("speed",1f).takeIf{it in listOf(.5f,1f,2f,3f)}?:1f)}
    var landscapeMode by remember{mutableStateOf(settings.getBoolean("wide_arena",true))}
    val configuration=LocalConfiguration.current
    val wide=b!=null&&configuration.screenWidthDp>configuration.screenHeightDp
    val activity=context as? Activity
    DisposableEffect(activity,b!=null,landscapeMode){
        val previous=activity?.requestedOrientation
        if(b!=null&&activity!=null)activity.requestedOrientation=if(landscapeMode)ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        onDispose{if(b!=null&&activity!=null&&previous!=null)activity.requestedOrientation=previous}
    }
    val latestPet by rememberUpdatedState(p)
    val latestChange by rememberUpdatedState(onChange)
    val latestSound by rememberUpdatedState(onSound)
    var visualTarget by remember{mutableStateOf<GridPoint?>(null)}
    var visualSource by remember{mutableStateOf<GridPoint?>(null)}
    var visualKind by remember{mutableStateOf("")}
    var skillVisual by remember{mutableStateOf<ArenaSkillFx?>(null)}
    var skillCells by remember{mutableStateOf<List<GridPoint>>(emptyList())}
    var melee by remember{mutableStateOf(false)}
    var attackStyle by remember{mutableStateOf(ArenaAttackStyle.IMPACT)}
    var damageLabels by remember{mutableStateOf<List<Pair<GridPoint,Int>>>(emptyList())}
    val response=remember{Animatable(0f)}
    var queued by remember{mutableStateOf<(() -> WorldPet)?>(null)}
    val fx=remember{Animatable(0f)}
    fun duration(ms:Int)=(ms/speed).toInt().coerceAtLeast(1)
    val now={System.currentTimeMillis()}
    val phase by rememberGamePhase(duration(1000),enabled=b!=null)
    val busy=queued!=null||moving||attack||b?.enemyPending==true
    fun perform(kind:String,source:GridPoint,target:GridPoint,closeCombat:Boolean=false,skill:ArenaSkillFx?=null,cells:List<GridPoint> = emptyList(),style:ArenaAttackStyle=ArenaAttackStyle.IMPACT,action:()->WorldPet){
        if(queued!=null||moving||attack)return
        targetChoice=-1
        visualKind=kind;visualSource=source;visualTarget=target;melee=closeCombat;skillVisual=skill;skillCells=cells;attackStyle=style
        attack=kind=="strike";moving=kind=="move"
        queued=action
    }
    LaunchedEffect(queued){
        val action=queued?:return@LaunchedEffect
        try{
            val oldBattle=latestPet.adventure.battle
            val result:WorldPet
            fx.snapTo(0f)
            response.snapTo(0f)
            if(visualKind=="move"){
                result=action();latestChange(result)
                fx.animateTo(1f,tween(duration(460),easing=LinearEasing))
            }else if(visualKind=="strike"&&attackStyle.lunge){
                fx.animateTo(.52f,tween(duration(420),easing=LinearEasing))
                result=action()
                if(result.adventure.battle!=null)latestChange(result)
                fx.animateTo(1f,tween(duration(380),easing=LinearEasing))
                if(result.adventure.battle==null)latestChange(result)
            }else{
                fx.animateTo(1f,tween(duration(skillVisual?.millis?:520),easing=LinearEasing))
                result=action();latestChange(result)
            }
            val updated=result.adventure.battle
            if(oldBattle!=null&&updated!=null){
                damageLabels=buildList{
                    val taken=oldBattle.hp-updated.hp
                    if(taken!=0)add(updated.pos to taken)
                    oldBattle.allies.zip(updated.allies).forEach{(before,after)->if(before.hp!=after.hp)add(after.pos to (before.hp-after.hp))}
                    oldBattle.enemies.zip(updated.enemies).forEach{(before,after)->
                        if(before.hp>after.hp)add(after.pos to (before.hp-after.hp))
                    }
                }
            }
            response.animateTo(1f,tween(duration(520),easing=LinearEasing))
        }finally{
            moving=false;attack=false;enemyVisual=-1;visualTarget=null;visualSource=null;visualKind="";skillVisual=null;skillCells=emptyList();damageLabels=emptyList();queued=null
        }
    }
    val foreground=LocalPixubiForeground.current
    LaunchedEffect(b?.enemyPending,queued,foreground){
        if(foreground&&queued==null&&b?.enemyPending==true){
            val plan=AdventureRules.enemyPlan(b)?:return@LaunchedEffect
            delay(duration(180).toLong())
            enemyVisual=plan.index
            val style=ArenaAttackStyle.of(b.enemies[plan.index].series)
            perform(if(plan.target!=null)"strike" else "move",plan.from,plan.to,style.closeCombat,style=style){latestSound(PixubiSound.TAP);AdventureRules.enemyTurn(latestPet,now())}
        }
    }
    fun back(){if(busy)return;if(a.contact!=null)onChange(EncounterRules.leave(p)) else if(b!=null)retreat=true else onBack()}
    BackHandler{back()}
    Column(Modifier.fillMaxSize().safeDrawingPadding().then(if(wide)Modifier else Modifier.verticalScroll(rememberScrollState())).padding(if(wide)3.dp else 8.dp),verticalArrangement=Arrangement.spacedBy(if(wide)2.dp else 6.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text(if(b==null)"PIXUBI · ЕКСПЕДИЦІЯ" else if(FrontierSites.isBoss(a,GridPoint(b.encounter%AdventureRules.MAP_W,b.encounter/AdventureRules.MAP_W)))"БОС · ВАРТОВИЙ ЯДРА" else if(FrontierSites.isVault(a,GridPoint(b.encounter%AdventureRules.MAP_W,b.encounter/AdventureRules.MAP_W)))"ОХОРОНА БУНКЕРА" else "ТАКТИЧНА АРЕНА",style=if(wide)MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall)
                Text(if(b==null)"Сектор ${a.stage} · дослідження" else if(b.deploying)"Розстановка · до початку бою" else "Раунд ${b.turn} · "+(if(enemyVisual>=0||b.enemyPending)"Хід ворога" else if(busy)"Твоя дія" else "Твій хід"))
            }
            if(b!=null)TextButton(onClick={landscapeMode=!landscapeMode;settings.edit().putBoolean("wide_arena",landscapeMode).apply()},enabled=!busy){Text(if(landscapeMode)"Вертикально" else "Широкий екран")}
            TextButton(onClick=onGuide,enabled=!busy){Text("?")}
            OutlinedButton(onClick={back()},enabled=!busy){Text(if(b==null)"Меню" else "Відступ")}
        }
        if(a.contact==null)RobotSpeechBubble(speech,speechEnabled,onSpeechToggle,compact=wide)
        if(!wide&&b==null){Text("${p.name} · ${p.series.title} · ${p.tier.title}")
        Text("⚡ ${p.energy}%  ·  🪙 ${p.coins}  ·  🛠 ${p.repair}")}
        if(b==null){
            Text("Торкнися дороги, контейнера або ворога. Робот прокладе маршрут через відкриту місцевість.",style=MaterialTheme.typography.bodySmall)
            FrontierMap(p,onChange,onSound,returnHome)
            MiningPanel(p,onChange)
            if(a.contact!=null)EncounterDialog(p,onChange,onSound,speechEnabled)
            SquadPanel(p,onChange)
            Text("База · шахти · родовища · лабораторії · бункери",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Знахідки сектора: ${FrontierSites.found(a)}/${FrontierSites.sites(a.stage).size}",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick={artifactsOpen=true}){Text("Артефакти ${a.artifacts.size}/${FrontierArtifact.entries.size}")}
            }
            if(artifactsOpen)AlertDialog(onDismissRequest={artifactsOpen=false},title={Text("Колекція артефактів")},text={
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text("Артефакти діють постійно, навіть після переходу до іншого сектора.")
                    FrontierArtifact.entries.forEach{artifact->
                        Text(if(artifact in a.artifacts)artifact.title else "Невідомий артефакт",style=MaterialTheme.typography.titleSmall)
                        Text(if(artifact in a.artifacts)artifact.description else "Знайди під час експедиції.",style=MaterialTheme.typography.bodySmall)
                    }
                }
            },confirmButton={TextButton(onClick={artifactsOpen=false}){Text("Закрити")}})
            Text("Пройдено зустрічей: ${AdventureRules.encounters(a).count{AdventureRules.id(it) in a.cleared}}/${AdventureRules.encounters(a).size}")
            Text(p.message)
            if(a.contact==null&&!AdventureRules.canExplore(p))Text("Потрібен заряд, ремонт або завершення місії.",color=MaterialTheme.colorScheme.error)
            if(AdventureBalance.needsRecovery(p))OutlinedButton(onClick={onChange(AdventureBalance.recover(p))},modifier=Modifier.fillMaxWidth()){
                Text(if(a.pos==AdventureRules.base)"Аварійний сервіс · безкоштовно" else "Аварійна евакуація на базу")
            }
            if(AdventureRules.canAdvance(a))Button(onClick={onChange(AdventureRules.advanceMap(p))},enabled=AdventureRules.canExplore(p)&&a.stage<100,modifier=Modifier.fillMaxWidth()){Text("Наступний сектор")}
        }else{
            val selected=weapon?.takeIf{p.series!=RobotSeries.KAGE&&it in AdventureRules.weapons(p)}
            val combos=SquadTactics.active(b)
            val available=if(b.deploying)listOf(-1)+b.allies.indices.filter{b.allies[it].hp>0} else SquadRules.available(b)
            val actor=actorChoice.takeIf{it in available}?:available.firstOrNull()?:-1
            val source=SquadRules.position(b,actor)
            val ally=b.allies.getOrNull(actor)
            val chosenStyle=if(ally==null)ArenaAttackStyle.of(p.series,selected) else ArenaAttackStyle.of(ally.kind.series,ally.kind.gear)
            val chosenSkillStyle=if(AdventureRules.isNinja(p,b,actor))ArenaAttackStyle.KATANA else chosenStyle
            val chosenEnemy=b.enemies.getOrNull(targetChoice)?.takeIf{it.hp>0}
            val damagePreview=AdventureRules.previewDamage(p,b,actor,targetChoice,selected)
            val nextEnemy=AdventureRules.nextEnemy(b)
            fun canHit(enemy:ArenaBot)=if(actor==-1)AdventureRules.canHit(p,b,enemy,selected)else SquadRules.canHit(b,actor,enemy)
            fun reachable(q:GridPoint)=if(b.deploying)AdventureRules.deploymentCell(q) else SquadRules.reachable(p,b,actor,q)
            val controls: @Composable ()->Unit = {
                Text(if(b.enemyPending)"Відповідає: "+(nextEnemy?.series?.title?:"—") else "Наступний ворог: "+(nextEnemy?.series?.title?:"—"),color=Color(0xFFE9B5AE),style=MaterialTheme.typography.labelMedium)
                if(combos.isNotEmpty())Text("Активні зв’язки: "+combos.joinToString(" · "){it.title},color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.labelSmall)
            Text(if(wide)"Загін" else "Загін · обери юніта для ходу",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(actor==-1,{actorChoice=-1},enabled=!busy&&-1 in available,label={Text("Командир "+if(-1 in b.acted)"✓" else "${b.hp} HP")})
                b.allies.forEachIndexed{i,u->FilterChip(actor==i,{actorChoice=i},enabled=!busy&&i in available,label={Text(u.kind.title+" ×${u.count} "+if(u.hp==0)"×" else if(i in b.acted)"✓" else "${u.hp} HP")})}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                Text("Швидкість",style=MaterialTheme.typography.labelMedium)
                listOf(.5f,1f,2f,3f).forEach{v->FilterChip(speed==v,{speed=v;settings.edit().putFloat("speed",v).apply()},enabled=!busy,label={Text(if(v==.5f)"0,5×" else "${v.toInt()}×")})}
            }
            if(!wide)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Твій робот · HP");Text("${b.hp}/${b.maxHp}");LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.fillMaxWidth())
                }}
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Вороги · HP");b.enemies.forEach{Text("${it.series.title} ${it.hp}/${it.maxHp}",style=MaterialTheme.typography.bodySmall)}
                }}
            }
            if(!wide)Text("Блакитне — рух · червоне — атака. Торкнись цілі для прогнозу.",style=MaterialTheme.typography.labelSmall)
            }
            val actions: @Composable ()->Unit = {
            if(chosenEnemy!=null&&!busy){
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("Ціль: ${chosenEnemy.series.title}",style=MaterialTheme.typography.titleMedium)
                        Text("HP ${chosenEnemy.hp}/${chosenEnemy.maxHp} · захист ${chosenEnemy.defense} · відстань ${AdventureRules.arenaDistance(source,chosenEnemy.pos)}")
                        if(damagePreview!=null){
                            Text("Прогноз: −${minOf(damagePreview,chosenEnemy.hp)} HP · "+if(damagePreview>=chosenEnemy.hp)"знищення" else "залишиться ${chosenEnemy.hp-damagePreview} HP",color=MaterialTheme.colorScheme.secondary)
                            Button(onClick={
                                val i=targetChoice;val dest=chosenEnemy.pos
                                direction=if(dest.x<source.x)RobotView.LEFT else RobotView.RIGHT
                                perform("strike",source,dest,chosenStyle.closeCombat,style=chosenStyle){
                                    latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.strike(latestPet,i,selected,now())else AdventureRules.allyStrike(latestPet,actor,i,now())
                                }
                            },modifier=Modifier.fillMaxWidth()){Text("Підтвердити атаку")}
                        }else Text("Ціль недоступна для цієї атаки: перевір дальність, заряди та чергу ходу.",style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={targetChoice=-1}){Text("Скасувати вибір")}
                    }
                }
            }
            if(busy)Text(when(visualKind){"strike"->"⚡ Атака";"guard"->"🛡 Щит";"repair"->"✚ Ремонт";else->"Переміщення"},color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.titleMedium)
            Text(b.log)
            Text(if(ally==null)"Командир · рух до ${AdventureRules.movement(p)} клітинок" else "${ally.kind.title} ×${ally.count} · ATK ${ally.attack+SquadTactics.attackBonus(b,ally.kind)} · рух ${ally.move} · дальність ${ally.range}"+if(ally.kind.charges>0)" · заряди ${ally.charges}" else "")
            if(ally==null){
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(selected==null,{weapon=null},label={Text(ArenaAttackStyle.of(p.series).title+" · "+AdventureRules.commanderRange(p,null))})
                AdventureRules.weapons(p).filter{p.series!=RobotSeries.KAGE}.forEach{g->
                    val ammo=AdventureRules.attackCost(g)
                    FilterChip(selected==g,{weapon=g},label={Text(g.title+" · "+AdventureRules.commanderRange(p,g)+(ammo?.let{" · ${p.stock(it)} зар."}?:""))})
                }
            }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={perform("guard",source,source){latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.guard(latestPet,now())else AdventureRules.allyGuard(latestPet,actor,now())}},enabled=!busy&&actor in available,modifier=Modifier.weight(1f)){Text("Захист / пропуск")}
                val repairTarget=if(actor==-1)null else SquadRules.repairTarget(b,actor)
                if(actor==-1||ally?.kind==SquadKind.MEDIC)OutlinedButton(onClick={
                    val target=repairTarget?.let{SquadRules.position(b,it)}?:source
                    perform("repair",source,target){latestSound(PixubiSound.HAPPY);if(actor==-1)AdventureRules.repair(latestPet,now())else AdventureRules.allyRepair(latestPet,actor,now())}
                },enabled=!busy&&actor in available&&(if(actor==-1)p.repair>0&&b.hp<b.maxHp else repairTarget!=null),modifier=Modifier.weight(1f)){Text("Ремонт")}
            }
            }
            val battlefield: @Composable (Modifier,Dp)->Unit = { modifier,boardHeight ->
            BoxWithConstraints(modifier.height(boardHeight)){
                val projection=remember(maxWidth,boardHeight){ArenaProjection(maxWidth.value,boardHeight.value)}
                val cell=projection.cell.dp
                val occupied=remember(b.pos,b.allies,b.enemies){buildList{add(b.pos);b.allies.filter{it.hp>0}.forEach{add(it.pos)};b.enemies.filter{it.hp>0}.forEach{add(it.pos)}}}
                val fixedEnvelope=projection.envelope(b.pos,emptyList())
                fun spriteW(q:GridPoint)=fixedEnvelope.width.dp
                fun spriteH(q:GridPoint)=fixedEnvelope.height.dp
                fun spriteX(q:GridPoint)=projection.foot(q).x.dp-spriteW(q)/2
                fun spriteY(q:GridPoint)=projection.foot(q).y.dp-spriteH(q)*.89f
                fun lungeOffset(q:GridPoint):Offset{
                    val target=visualTarget?:return Offset.Zero
                    if(!attack||!attackStyle.lunge||visualSource!=q)return Offset.Zero
                    val d=projection.foot(target)-projection.foot(q)
                    val length=d.getDistance().coerceAtLeast(1f)
                    val stop=(projection.cell*.26f/length).coerceAtMost(.45f)
                    return d*((1f-stop)*ArenaAttackStyle.travel(fx.value))
                }
                fun attackAction():RobotAction=if(attackStyle==ArenaAttackStyle.KATANA&&(fx.value<.38f||fx.value>.63f))RobotAction.NINJA_DASH else RobotAction.ATTACK
                fun xOf(q:GridPoint)=projection.center(q).x.dp-cell/2
                fun yOf(q:GridPoint)=projection.center(q).y.dp
                Box(Modifier.fillMaxWidth().height(boardHeight).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF263A4D),Color(0xFF10202B),Color(0xFF182A36))))){
                    Box(Modifier.fillMaxSize().then(remember(a.stage){Modifier.cachedScene{arenaLandscape(ArenaProjection(size.width,size.height),a.stage)}}))
                    Canvas(Modifier.fillMaxSize().pointerInput(p,b,selected,busy,speed,actor,targetChoice,deployPicked){
                        detectTapGestures{tap->
                            val plane=ArenaProjection(size.width.toFloat(),size.height.toFloat())
                            val ground=plane.hit(tap)
                            // A tap near an actor's feet always selects its cell; body taps
                            // work in the empty horizon too, without stealing legal ground moves.
                            val body=if(ground==null)occupied.minByOrNull{q->
                                val f=plane.foot(q);val h=plane.envelope(q,occupied).height
                                (tap-Offset(f.x,f.y-h*.42f)).getDistance()
                            }?.takeIf{q->
                                val f=plane.foot(q);val e=plane.envelope(q,occupied)
                                abs(tap.x-f.x)<=e.width*.30f&&tap.y in (f.y-e.height*.89f)..f.y
                            } else null
                            val q=ground?:body
                            if(q!=null&&!busy){
                                if(b.deploying){
                                    val friendly=if(q==b.pos)-1 else b.allies.indexOfFirst{it.hp>0&&it.pos==q}.takeIf{it>=0}
                                    if(friendly!=null&&(!deployPicked||friendly==actor)){
                                        actorChoice=friendly;deployPicked=true
                                    }else if(AdventureRules.deploymentCell(q)){
                                        latestChange(AdventureRules.deployUnit(latestPet,actor,q));deployPicked=false
                                    }
                                    return@detectTapGestures
                                }
                                val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                                val friend=b.allies.indexOfFirst{it.hp>0&&it.pos==q}
                                if(q==b.pos&&-1 in available){actorChoice=-1;targetChoice=-1}
                                else if(friend>=0&&friend in available){actorChoice=friend;targetChoice=-1}
                                else if(i>=0){
                                    targetChoice=i
                                    latestSound(PixubiSound.TAP)
                                }else if(reachable(q)){
                                    direction=if(q.x<source.x)RobotView.LEFT else RobotView.RIGHT
                                    perform("move",source,q){latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.moveArena(latestPet,q,now())else AdventureRules.allyMove(latestPet,actor,q,now())}
                                }
                            }
                        }
                    }){
                        val plane=ArenaProjection(size.width,size.height)
                        val w=plane.cell;val h=plane.row
                        fun surface(points:List<Offset>)=Path().apply{moveTo(points[0].x,points[0].y);points.drop(1).forEach{lineTo(it.x,it.y)};close()}
                        for(q in AdventureRules.arenaCells){
                            val x=q.x;val y=q.y
                            val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                            val hit=i>=0&&canHit(b.enemies[i])
                            val can=reachable(q)
                            val center=plane.center(q);val cx=center.x;val cy=center.y
                            val corners=plane.vertices(q).map{center+(it-center)*.97f}
                            val outline=surface(corners)
                            // Heroes-like overlays: terrain remains visible and only tactical cells are highlighted.
                            val fill=when{
                                q==source->Color(0xFF72DFD0).copy(alpha=.24f)
                                hit->Color(0xFFFF6F79).copy(alpha=.23f)
                                can&&!busy->Color(0xFF071D18).copy(alpha=.38f)
                                i>=0&&i==targetChoice->Color(0xFFFF9A78).copy(alpha=.23f)
                                else->Color.Transparent
                            }
                            if(fill!=Color.Transparent)drawPath(outline,fill)
                            if(showGrid||q==source||hit||can&&!busy){
                                val edge=when{q==source->Color(0xFF99F6DF);hit->Color(0xFFFF8790);can&&!busy->Color(0xFF9BD3BA).copy(alpha=.55f);else->Color.White.copy(alpha=.18f)}
                                drawPath(outline,edge,style=Stroke(if(q==source||hit)2f else 1f))
                            }
                            if(i>=0&&i==targetChoice&&!busy)drawPath(outline,Color(0xFFFFE1A5),style=Stroke(2.5f))

                        }
                    }
                    val px by animateDpAsState(spriteX(b.pos),tween(duration(300)),label="battle x")
                    val py by animateDpAsState(spriteY(b.pos),tween(duration(300)),label="battle y")
                    Box(Modifier.offset(px+(if(enemyVisual<0)lungeOffset(b.pos).x.dp else 0.dp),py+(if(enemyVisual<0)lungeOffset(b.pos).y.dp else 0.dp)).size(spriteW(b.pos),spriteH(b.pos)).zIndex(projection.depth(b.pos)+1f)){
                        ArenaGroundShadow(false,actor==-1&&!busy)
                        RobotPreview(direction,if(enemyVisual<0&&attack&&visualSource==b.pos)attackAction() else if(enemyVisual<0&&moving&&visualTarget==b.pos)RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(p.skin.accent,ArenaAttackStyle.combatEquipment(p.series,p.equipment,attack&&attackStyle==ArenaAttackStyle.KATANA&&enemyVisual<0&&visualSource==b.pos),p.tier,p.series,arena=true),if(busy&&enemyVisual<0&&(visualSource==b.pos||moving&&visualTarget==b.pos))fx.value else phase,false,Modifier.fillMaxSize())

                    }
                    b.allies.forEachIndexed{i,u->key("ally"+i){
                        val ax by animateDpAsState(spriteX(u.pos),tween(duration(300)),label="ally x")
                        val ay by animateDpAsState(spriteY(u.pos),tween(duration(300)),label="ally y")
                        if(u.hp>0)Box(Modifier.offset(ax+(if(enemyVisual<0)lungeOffset(u.pos).x.dp else 0.dp),ay+(if(enemyVisual<0)lungeOffset(u.pos).y.dp else 0.dp)).size(spriteW(u.pos),spriteH(u.pos)).zIndex(projection.depth(u.pos)+1f)){
                            ArenaGroundShadow(false,actor==i&&!busy)
                            val activeAction=when{
                                enemyVisual<0&&skillVisual==ArenaSkillFx.DOME&&visualSource==u.pos->RobotAction.CHARGE
                                enemyVisual<0&&attack&&visualSource==u.pos->attackAction()
                                enemyVisual<0&&moving&&visualTarget==u.pos->if(u.kind==SquadKind.AERIAL)RobotAction.BIO_FLIGHT else RobotAction.WALK
                                busy&&visualKind=="repair"&&visualSource==u.pos->RobotAction.CHARGE
                                else->RobotAction.IDLE
                            }
                            RobotPreview(if(activeAction==RobotAction.IDLE)RobotView.RIGHT else direction,activeAction,RobotFace.NEUTRAL,u.kind.loadout().let{it.copy(arena=true,equipment=ArenaAttackStyle.combatEquipment(it.series,it.equipment,attack&&attackStyle==ArenaAttackStyle.KATANA&&enemyVisual<0&&visualSource==u.pos))},if(activeAction==RobotAction.IDLE)phase else fx.value,false,Modifier.fillMaxSize())

                        }
                    }}
                    b.enemies.forEachIndexed{i,e->key(i){
                        val ex by animateDpAsState(spriteX(e.pos),tween(duration(300)),label="bot x")
                        val ey by animateDpAsState(spriteY(e.pos),tween(duration(300)),label="bot y")
                        if(e.hp>0)Box(Modifier.offset(ex+(if(enemyVisual==i)lungeOffset(e.pos).x.dp else 0.dp),ey+(if(enemyVisual==i)lungeOffset(e.pos).y.dp else 0.dp)).size(spriteW(e.pos),spriteH(e.pos)).zIndex(projection.depth(e.pos)+1f)){
                            ArenaGroundShadow(true,targetChoice==i)
                            RobotPreview(if(enemyVisual==i&&visualTarget!=null&&visualSource!=null&&visualTarget!!.x>visualSource!!.x)RobotView.RIGHT else RobotView.LEFT,if(enemyVisual==i&&attack)attackAction() else if(enemyVisual==i&&moving)RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(RobotSkin.RED.accent,ArenaAttackStyle.combatEquipment(e.series,RobotEquipment()),e.tier,e.series,arena=true),if(enemyVisual==i)fx.value else phase,false,Modifier.fillMaxSize())

                        }
                    }}
                    // Numbers are on the ground, separate from the model, never a box covering its body.
                    val hpAt: @Composable (GridPoint,Int,Int,String,Boolean,Boolean)->Unit = {q,hp,max,count,enemy,active->
                        val foot=projection.foot(q)
                        Box(Modifier.offset(foot.x.dp-cell/2,foot.y.dp+1.dp).width(cell).zIndex(24f),contentAlignment=Alignment.TopCenter){
                            ArenaUnitBadge(hp,max,count,enemy,active)
                        }
                    }
                    hpAt(b.pos,b.hp,b.maxHp,"",false,actor==-1&&!busy)
                    b.allies.filter{it.hp>0}.forEach{u->hpAt(u.pos,u.hp,u.maxHp,"×${u.count}",false,false)}
                    b.enemies.filter{it.hp>0}.forEach{e->hpAt(e.pos,e.hp,e.maxHp,"",true,false)}
                    // Effects above sprites. Their progress is presentation-only.
                    if(busy)Canvas(Modifier.fillMaxSize().zIndex(30f)){
                        val src=visualSource;val dst=visualTarget
                        if(src!=null&&dst!=null){
                            val plane=ArenaProjection(size.width,size.height);val w=plane.cell
                            val raise=if(visualKind=="strike")Offset(0f,-w*.42f) else Offset.Zero
                            val from=plane.foot(src)+raise;val to=plane.foot(dst)+raise
                            val t=fx.value
                            val special=skillVisual
                            if(special!=null&&!(visualKind=="strike"&&attackStyle.lunge))drawSkillFx(special,plane,src,dst,skillCells,t) else when(visualKind){
                                "strike"->{
                                    val point=from+(to-from)*(t*1.8f).coerceIn(0f,1f)
                                    if(t>=.35f&&t<=.68f&&melee){
                                        val sweep=Offset(to.x-w*.34f,to.y-w*.34f)
                                        drawArc(attackStyle.tint.copy(alpha=1f-t),-135f,190f*((t-.35f)/.33f).coerceIn(0f,1f),false,sweep,Size(w*.68f,w*.68f),style=Stroke(w*.10f,cap=StrokeCap.Round))
                                    }else if(t<.62f&&!melee){
                                        drawLine(attackStyle.tint.copy(alpha=.24f),from,point,w*.20f,StrokeCap.Round)
                                        drawLine(attackStyle.tint,from,point,w*.052f,StrokeCap.Round)
                                        drawCircle(Color.White,w*.06f,point)
                                    }
                                    if(t>.38f){
                                        val z=((t-.38f)/.62f).coerceIn(0f,1f)
                                        drawCircle(Color(0xFFFFBB72).copy(alpha=(1-z)*.40f),w*(.10f+z*.47f),to)
                                        drawCircle(Color(0xFFFFE2AA).copy(alpha=1-z),w*(.12f+z*.46f),to,style=Stroke(w*.045f))
                                        repeat(12){i->
                                            val a=i*6.283f/12f;val end=to+Offset(cos(a),sin(a))*w*(.10f+z*.52f)
                                            drawLine(Color(0xFFFFC480).copy(alpha=1-z),to+(end-to)*.75f,end,w*.035f,StrokeCap.Round)
                                        }
                                    }
                                }
                                "guard","repair"->{
                                    val col=if(visualKind=="guard")Color(0xFF8CDFFF)else Color(0xFF8BFFB1)
                                    val radius=w*(.22f+.30f*sin(t*3.14159f))
                                    drawCircle(col.copy(alpha=.18f),radius,from)
                                    drawCircle(col.copy(alpha=1-t*.7f),radius,from,style=Stroke(w*.045f))
                                    repeat(6){i->val a=i*1.0472f+t*3f;drawCircle(col,w*.035f,from+Offset(cos(a),sin(a))*radius)}
                                }
                                "move"->{
                                    val point=from+(to-from)*t
                                    drawLine(Color(0xFF9BE3DF).copy(alpha=.25f*(1-t)),from,point,w*.10f,StrokeCap.Round)
                                }
                            }
                        }
                    }
                    skillVisual?.let{special->
                        Text(special.title,modifier=Modifier.align(Alignment.TopCenter).padding(top=5.dp).zIndex(32f),color=Color(0xFFE1FFFF),style=MaterialTheme.typography.titleMedium.copy(shadow=Shadow(Color.Black,Offset(1f,2f),5f)))
                    }
                    damageLabels.forEach{(point,amount)->
                        val t=response.value
                        Box(Modifier.offset(xOf(point),yOf(point)-cell*(.15f+t*.4f)).width(cell).zIndex(31f),contentAlignment=Alignment.Center){
                            Text(if(amount>0)"−$amount" else "+${-amount}",color=(if(amount>0)Color(0xFFFFA285)else Color(0xFF9EFFB6)).copy(alpha=(1f-t*.65f).coerceIn(0f,1f)),style=MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            }
            val actionStrip:@Composable ()->Unit={
                if(b.deploying){
                    Text(if(deployPicked)"Обери позицію · союзники можуть помінятися місцями" else "Торкнись свого юніта або обери його нижче",style=MaterialTheme.typography.labelSmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        FilterChip(actor==-1,{actorChoice=-1;deployPicked=true},label={Text("Командир")})
                        b.allies.forEachIndexed{i,u->if(u.hp>0)FilterChip(actor==i,{actorChoice=i;deployPicked=true},label={Text(u.kind.title)})}
                    }
                    Button(onClick={latestChange(AdventureRules.startBattle(latestPet));targetChoice=-1},modifier=Modifier.fillMaxWidth()){Text("Почати бій")}
                }else{
                    val repairTarget=if(actor==-1)null else SquadRules.repairTarget(b,actor)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        FilterChip(actor==-1,{actorChoice=-1;targetChoice=-1},enabled=!busy&&-1 in available,label={Text("Командир · ${b.hp}")})
                        b.allies.forEachIndexed{i,u->if(u.hp>0)FilterChip(actor==i,{actorChoice=i;targetChoice=-1},enabled=!busy&&i in available,label={Text(u.kind.title+" · ${u.hp}")})}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        Button(onClick={
                            val dest=chosenEnemy?.pos?:return@Button
                            val i=targetChoice
                            direction=if(dest.x<source.x)RobotView.LEFT else RobotView.RIGHT
                            perform("strike",source,dest,chosenStyle.closeCombat,style=chosenStyle){
                                latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.strike(latestPet,i,selected,now())else AdventureRules.allyStrike(latestPet,actor,i,now())
                            }
                        },enabled=!busy&&damagePreview!=null,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)){Text("Атака")}
                        OutlinedButton(onClick={perform("guard",source,source){latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.guard(latestPet,now())else AdventureRules.allyGuard(latestPet,actor,now())}},enabled=!busy&&actor in available,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)){Text("Захист")}
                        OutlinedButton(onClick={skillsPanel=true},enabled=!busy&&actor in available,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)){Text("⚡ Навички")}
                        if(actor==-1||ally?.kind==SquadKind.MEDIC)OutlinedButton(onClick={
                            val dest=repairTarget?.let{SquadRules.position(b,it)}?:source
                            perform("repair",source,dest){latestSound(PixubiSound.HAPPY);if(actor==-1)AdventureRules.repair(latestPet,now())else AdventureRules.allyRepair(latestPet,actor,now())}
                        },enabled=!busy&&actor in available&&(if(actor==-1)p.repair>0&&b.hp<b.maxHp else repairTarget!=null),modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)){Text("Ремонт")}
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(if(chosenEnemy!=null)"Ціль: ${chosenEnemy.series.title} · ${damagePreview?.let{"−$it HP"}?:"поза дальністю"}" else b.log,modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,maxLines=1)
                        TextButton(onClick={showGrid=!showGrid}){Text(if(showGrid)"Сітка ✓" else "Сітка")}
                        TextButton(onClick={tacticsPanel=true},enabled=!busy){Text("Тактика · ${speed}×")}
                    }
                }
            }
            if(wide){
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)){
                    battlefield(Modifier.fillMaxWidth(),maxHeight)
                }
                actionStrip()
            }else{
                BoxWithConstraints(Modifier.fillMaxWidth()){
                    battlefield(Modifier.fillMaxWidth(),maxWidth*.76f)
                }
                actionStrip()
                // All controls are now directly under the battlefield or inside the tactics sheet.
            }
            if(tacticsPanel&&!b.deploying)AlertDialog(onDismissRequest={tacticsPanel=false},title={Text("Тактика та спорядження")},text={
                Column(Modifier.heightIn(max=220.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)){controls();actions()}
            },confirmButton={TextButton(onClick={tacticsPanel=false}){Text("До бою")}})
            if(skillsPanel&&!b.deploying){
                val hit=AdventureRules.skillDamage(p,b,actor,targetChoice,selected)
                val support=AdventureRules.skillSupport(b,actor)
                val supportReady=AdventureRules.skillSupportReady(b,actor)
                AlertDialog(onDismissRequest={skillsPanel=false},containerColor=Color(0xFF102332),titleContentColor=Color(0xFF8DECF5),
                    title={Text("⌬ NEURAL LINK · Навички")},text={
                    Column(Modifier.fillMaxWidth().border(1.dp,Color(0xFF397F91),RoundedCornerShape(12.dp)).background(Color(0xFF152D3C),RoundedCornerShape(12.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("БОЙОВИЙ ІНТЕРФЕЙС  /  СИНХРОНІЗАЦІЯ",color=Color(0xFF8DECF5),style=MaterialTheme.typography.labelSmall)
                        Text(AdventureRules.skillName(b,actor,p.series),style=MaterialTheme.typography.titleMedium)
                        Text(AdventureRules.skillDescription(b,actor,p.series))
                        if(actor==-1)Text("NEURAL LINK · модуль ${p.neuralRank}/3 · додаткові ${p.neuralRank*15}% базової шкоди",color=Color(0xFF8DECF5),style=MaterialTheme.typography.bodySmall)
                        Text("Один раз за бій. Витрачає хід юніта та заряд, якщо потрібен.",style=MaterialTheme.typography.bodySmall)
                        Text(if(actor in b.skillUsed)"Навичку вже використано" else if(support)if(supportReady)"Готово до застосування" else "Немає доступної цілі або заряду" else if(hit==null)if(chosenSkillStyle==ArenaAttackStyle.KATANA)"Обери ворога до 2 клітинок" else "Обери ворога в радіусі атаки" else "Прогноз: −$hit HP")
                        if(ally?.kind==SquadKind.MEDIC)Text("Ремонт: відновлює союзника в радіусі двох клітинок.")
                    }
                },confirmButton={Button(onClick={
                    if(support){
                        skillsPanel=false
                        perform(if(ally?.kind==SquadKind.GUARD)"guard" else "repair",source,source,skill=ArenaSkillFx.forActor(ally?.kind),cells=skillFxCells(b,actor,-1)){latestSound(PixubiSound.HAPPY);AdventureRules.useSupportSkill(latestPet,actor,now())}
                        return@Button
                    }
                    val dest=chosenEnemy?.pos?:return@Button
                    val i=targetChoice;skillsPanel=false
                    direction=if(dest.x<source.x)RobotView.LEFT else RobotView.RIGHT
                    perform("strike",source,dest,chosenSkillStyle.closeCombat,skill=ArenaSkillFx.forActor(ally?.kind),cells=skillFxCells(b,actor,i),style=chosenSkillStyle){
                        latestSound(PixubiSound.TAP);AdventureRules.skillStrike(latestPet,actor,i,selected,now())
                    }
                },enabled=(hit!=null||supportReady)&&!busy){Text("Застосувати")}},dismissButton={TextButton(onClick={skillsPanel=false}){Text("Закрити")}})
            }

        }
    }
    if(retreat)AlertDialog(onDismissRequest={retreat=false},title={Text("Відступити?")},text={Text("Отримані пошкодження та витрачені заряди збережуться. Вороги залишаться на карті.")},
        confirmButton={TextButton(onClick={retreat=false;onChange(AdventureRules.retreat(p,now()))}){Text("Відступити")}},dismissButton={TextButton(onClick={retreat=false}){Text("Продовжити бій")}})
}

@Composable private fun SquadPanel(p:WorldPet,onChange:(WorldPet)->Unit){
    var expanded by remember{mutableStateOf(false)}
    var dismiss by remember{mutableStateOf<SquadKind?>(null)}
    val atBase=SquadRules.atBase(p)&&BaseBuilding.BARRACKS in p.buildings
    val roster=p.adventure.squad
    val phase by rememberGamePhase(2800,RenderBudget.SCENERY_FPS)
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text("Казарми · рівень ${p.adventure.barracks}",style=MaterialTheme.typography.titleMedium);Text("Командир + ${roster.size}/${SquadRules.slots(p.adventure)} загонів · ${roster.keys.sumOf{SquadRules.stackCount(p.adventure,it)}} бійців")}
                OutlinedButton(onClick={expanded=!expanded}){Text(if(expanded)"Згорнути" else "Керувати")}
            }
            if(roster.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                roster.forEach{(kind,hp)->Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(82.dp)){
                    RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,kind.loadout(),phase,false,Modifier.size(72.dp))
                    Text(kind.title+" ×${SquadRules.stackCount(p.adventure,kind)}",style=MaterialTheme.typography.labelSmall);Text("$hp/${SquadRules.maxStackHp(p.adventure,kind)} HP",style=MaterialTheme.typography.labelSmall)
                }}
            }
            if(expanded){
                val active=SquadTactics.active(roster.keys)
                Text("Тактичні зв’язки",style=MaterialTheme.typography.titleMedium)
                SquadCombo.entries.filter{combo->combo.kinds.all{SquadResearch.unlocked(p.adventure,it)}}.forEach{combo->
                    Text((if(combo in active)"✓ " else "○ ")+combo.title+" · "+combo.detail,style=MaterialTheme.typography.bodySmall,color=if(combo in active)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if(atBase)"До 5 бійців одного класу в клітинці. Втрата бійця зменшує силу загону. Заряди поповнюються перед боєм." else "Повернися на базу та побудуй казарми для найму й покращення.")
                if(p.adventure.barracks<3)Button(onClick={onChange(SquadRules.expand(p))},enabled=atBase&&p.coins>=SquadRules.barracksPrice(p.adventure),modifier=Modifier.fillMaxWidth()){Text("Розширити казарми · ${SquadRules.barracksPrice(p.adventure)} 🪙")}
                Text("Рівень 2: четвертий загін. Рівень 3: елітні війська. Нові класи відкриваються перемогами в експедиції.",style=MaterialTheme.typography.bodySmall)
                val opened=SquadKind.entries.count{SquadResearch.unlocked(p.adventure,it)}
                Text("Технології · $opened/${SquadKind.entries.size} · перемоги на карті ${SquadResearch.victories(p.adventure)}",style=MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress={opened.toFloat()/SquadKind.entries.size},modifier=Modifier.fillMaxWidth())
                SquadKind.entries.forEach{k->
                    val locked=SquadDisclosure.locked(p.adventure,k)
                    if(locked!=null){
                        ClassifiedUnitCard(locked,SquadResearch.progress(p.adventure,k),phase)
                    }else{
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,k.loadout(),phase,false,Modifier.size(68.dp))
                        Column(Modifier.weight(1f)){
                            Text(k.title+" · ранг ${SquadRules.rank(p.adventure,k)}",style=MaterialTheme.typography.titleSmall)
                            Text("Технологію відкрито",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                            Text("HP ${SquadRules.unitHp(k,SquadRules.rank(p.adventure,k))} · ATK ${k.attack+4*(SquadRules.rank(p.adventure,k)-1)} · DEF ${k.defense+5*(SquadRules.rank(p.adventure,k)-1)} · рух ${k.move} · дальність ${k.range}",style=MaterialTheme.typography.bodySmall)
                            if(k.requiredBarracks>p.adventure.barracks)Text("Потрібні казарми рівня ${k.requiredBarracks}",style=MaterialTheme.typography.labelSmall)
                            if(k==SquadKind.MEDIC)Text("3 ремонти в радіусі 2 клітинок",style=MaterialTheme.typography.labelSmall)
                            if(k==SquadKind.RANGER)Text("8 власних зарядів на бій",style=MaterialTheme.typography.labelSmall)
                            val hp=roster[k]
                            val count=SquadRules.stackCount(p.adventure,k)
                            Button(onClick={onChange(SquadRules.recruit(p,k))},enabled=atBase&&BaseRules.buildingFor(k) in p.buildings&&p.adventure.barracks>=k.requiredBarracks&&(hp!=null||roster.size<SquadRules.slots(p.adventure))&&count<SquadRules.MAX_STACK&&p.coins>=k.price){Text("+1 боєць · ${k.price} 🪙 · $count/5")}
                            if(hp!=null){
                                val rank=SquadRules.rank(p.adventure,k)
                                if(rank<3)OutlinedButton(onClick={onChange(SquadRules.upgrade(p,k))},enabled=atBase&&rank<p.adventure.barracks&&p.coins>=SquadRules.upgradePrice(p.adventure,k)){Text("Ранг ${rank+1} · ${SquadRules.upgradePrice(p.adventure,k)} 🪙")}
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    TextButton(onClick={onChange(SquadRules.restore(p,k))},enabled=atBase&&hp<SquadRules.maxStackHp(p.adventure,k)&&p.coins>=SquadRules.restorePrice(p.adventure,k)){Text("Ремонт ${SquadRules.restorePrice(p.adventure,k)}")}
                                    TextButton(onClick={dismiss=k},enabled=atBase){Text("Вивести")}
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
    dismiss?.let{k->AlertDialog(onDismissRequest={dismiss=null},title={Text("Вивести юніта?")},text={Text("Увесь загін цього класу буде розформовано без повернення монет.")},confirmButton={TextButton(onClick={onChange(SquadRules.dismiss(p,k));dismiss=null}){Text("Вивести")}},dismissButton={TextButton(onClick={dismiss=null}){Text("Скасувати")}})}
}

@Composable private fun ClassifiedUnitCard(info:SquadClassified,progress:String,phase:Float){
    val accent=if(info.secret)Color(0xFFB29AEF)else Color(0xFF69BFCB)
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF172234))){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0D1523)),contentAlignment=Alignment.Center){
                // Deliberately identical abstract vault, never an actual silhouette or RobotPreview.
                Canvas(Modifier.fillMaxSize()){
                    val c=Offset(size.width/2,size.height/2)
                    val radius=size.minDimension*.35f
                    val outline=Path().apply{
                        for(i in 0..5){
                            val a=(i*60-90)*PI.toFloat()/180f
                            val p=c+Offset(cos(a),sin(a))*radius
                            if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)
                        };close()
                    }
                    drawPath(outline,accent.copy(alpha=.10f))
                    drawPath(outline,accent.copy(alpha=.65f),style=Stroke(2f))
                    drawCircle(accent.copy(alpha=.10f+.10f*sin(phase*6.283f)),radius*.7f,c)
                    repeat(7){i->val y=size.height*(.12f+i*.12f)
                        drawLine(accent.copy(alpha=.07f),Offset(size.width*.1f,y),Offset(size.width*.9f,y),1f)}
                    val y=size.height*(.12f+.76f*phase)
                    drawLine(accent.copy(alpha=.4f),Offset(size.width*.12f,y),Offset(size.width*.88f,y),1.5f)
                }
                Text("?",color=accent,style=MaterialTheme.typography.headlineLarge)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(info.code+" · 🔒",color=accent,style=MaterialTheme.typography.labelSmall)
                Text(info.title,style=MaterialTheme.typography.titleSmall)
                Text("Модель та характеристики засекречено",style=MaterialTheme.typography.bodySmall)
                Text(progress,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun ArenaGroundShadow(enemy:Boolean,selected:Boolean){
    Canvas(Modifier.fillMaxSize()){
        val col=if(enemy)Color(0xFFEF8390)else Color(0xFF7BEAD4)
        val w=size.width;val h=size.height
        drawOval(Color.Black.copy(alpha=.60f),Offset(w*.40f,h*.865f),Size(w*.20f,h*.05f))
        drawOval(col.copy(alpha=if(selected).80f else .28f),Offset(w*.385f,h*.852f),Size(w*.23f,h*.075f),style=Stroke(if(selected)2f else 1f))
    }
}

@Composable private fun ArenaUnitBadge(hp:Int,maxHp:Int,count:String,enemy:Boolean,selected:Boolean,modifier:Modifier=Modifier){
    val color=if(enemy)Color(0xFFFFA1AA)else Color(0xFFBEFFEF)
    Text(if(count.isBlank())"$hp" else "$count · $hp",modifier=modifier,color=color,maxLines=1,
        style=MaterialTheme.typography.labelLarge.copy(shadow=Shadow(Color.Black,Offset(1f,1f),4f)))
}
