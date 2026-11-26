package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import kotlinx.coroutines.delay

@Composable
internal fun AdventureScreen(p:WorldPet,onChange:(WorldPet)->Unit,onBack:()->Unit,onSound:(PixubiSound)->Unit){
    val a=p.adventure;val b=a.battle
    var retreat by remember{mutableStateOf(false)}
    var weapon by remember{mutableStateOf<RobotGear?>(null)}
    var moving by remember{mutableStateOf(false)}
    var attack by remember{mutableStateOf(false)}
    var direction by remember{mutableStateOf(RobotView.RIGHT)}
    val now={System.currentTimeMillis()}
    val loop=rememberInfiniteTransition(label="adventure")
    val phase by loop.animateFloat(0f,1f,infiniteRepeatable(tween(1000,easing=LinearEasing)),label="actors")
    LaunchedEffect(a.pos,b?.pos){if(moving){delay(300);moving=false}}
    LaunchedEffect(attack){if(attack){delay(350);attack=false}}
    fun back(){if(b!=null)retreat=true else onBack()}
    BackHandler{back()}
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text(if(b==null)"PIXUBI · FRONTIER" else "ТАКТИЧНА АРЕНА",style=MaterialTheme.typography.headlineSmall)
                Text(if(b==null)"Сектор ${a.stage} · дослідження" else "Раунд ${b.turn} · твій хід")
            }
            OutlinedButton(onClick={back()}){Text(if(b==null)"База" else "Відступ")}
        }
        Text("${p.name} · ${p.series.title} · ${p.tier.title}")
        Text("⚡ ${p.energy}%  ·  🪙 ${p.coins}  ·  🛠 ${p.repair}")
        if(b==null){
            Text("Натискай сусідні клітинки. Туман відкривається під час руху. Ворожий бот запускає бій.")
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val cell=maxWidth/AdventureRules.MAP_W
                Box(Modifier.fillMaxWidth().height(cell*AdventureRules.MAP_H)){
                    Column{
                        for(y in 0 until AdventureRules.MAP_H)Row{
                            for(x in 0 until AdventureRules.MAP_W){
                                val q=GridPoint(x,y);val visible=AdventureRules.visible(a,q);val tile=AdventureRules.tile(a,q)
                                val can=AdventureRules.canExplore(p)&&a.pos.distance(q)==1&&tile!=AdventureTile.WALL
                                val bg=if(!visible)Color(0xFF101621)else when(tile){
                                    AdventureTile.WALL->Color(0xFF39465A);AdventureTile.ENEMY->Color(0xFF63353F)
                                    AdventureTile.BASE->Color(0xFF2F746C);AdventureTile.EMPTY->if((x+y)%2==0)Color(0xFF29464B)else Color(0xFF253E43)
                                    else->Color(0xFF465646)
                                }
                                Box(Modifier.size(cell).background(bg).border(0.7.dp,if(can)Color(0xFF91DDC1)else Color(0xFF1C2835))
                                    .clickable(enabled=can&&!moving){
                                        direction=if(q.x<a.pos.x)RobotView.LEFT else RobotView.RIGHT
                                        val next=AdventureRules.move(p,q,now());if(next.adventure.pos!=a.pos)moving=true
                                        onChange(next);onSound(PixubiSound.TAP)
                                    },contentAlignment=Alignment.Center){
                                    if(visible&&tile==AdventureTile.ENEMY){
                                        RobotPreview(RobotView.LEFT,RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),RobotTier.MK1,AdventureRules.enemySeries(a,q)),phase,false,Modifier.fillMaxSize().padding(2.dp))
                                    }else if(visible)Text(tile.symbol,style=MaterialTheme.typography.bodyMedium)
                                    else Text("·",color=Color(0xFF374255))
                                }
                            }
                        }
                    }
                    val px by animateDpAsState(cell*a.pos.x,tween(260),label="map x")
                    val py by animateDpAsState(cell*a.pos.y,tween(260),label="map y")
                    Box(Modifier.offset(px,py).size(cell).border(1.5.dp,Color(0xFFB9F8DB),RoundedCornerShape(5.dp))){
                        RobotPreview(direction,if(moving)RobotAction.WALK else RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),phase,false,Modifier.fillMaxSize())
                    }
                }
            }
            Text("⌂ База · 🪙 Монети · ⚡ Заряд · 🛠 Ремонт · ▣ Боєприпаси · ▧ Перешкода",style=MaterialTheme.typography.bodySmall)
            Text("Переможено ботів: ${AdventureRules.encounters.count{AdventureRules.id(it) in a.cleared}}/4")
            Text(p.message)
            if(!AdventureRules.canExplore(p))Text("Повернись на базу: потрібен заряд, ремонт або завершення місії.",color=MaterialTheme.colorScheme.error)
            if(AdventureRules.complete(a))Button(onClick={onChange(AdventureRules.advanceMap(p))},enabled=AdventureRules.canExplore(p)&&a.stage<100,modifier=Modifier.fillMaxWidth()){Text("Наступний сектор")}
        }else{
            val selected=weapon?.takeIf{it in AdventureRules.weapons(p)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Твій робот · HP");Text("${b.hp}/${b.maxHp}");LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.fillMaxWidth())
                }}
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Вороги · HP");b.enemies.forEach{Text("${it.series.title} ${it.hp}/${it.maxHp}",style=MaterialTheme.typography.bodySmall)}
                }}
            }
            Text("Зелені клітинки — доступні для ходу. Червона рамка — ворог у радіусі атаки. Після твоєї дії ходять усі живі боти.",style=MaterialTheme.typography.bodySmall)
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val cell=maxWidth/AdventureRules.ARENA_W
                Box(Modifier.fillMaxWidth().height(cell*AdventureRules.ARENA_H)){
                    Column{
                        for(y in 0 until AdventureRules.ARENA_H)Row{
                            for(x in 0 until AdventureRules.ARENA_W){
                                val q=GridPoint(x,y);val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                                val hit=i>=0&&AdventureRules.canHit(p,b,b.enemies[i],selected)
                                val can=AdventureRules.reachable(p,b,q)
                                val color=when{hit->Color(0xFF783D48);can->Color(0xFF355F53);i>=0->Color(0xFF49343F);else->if((x+y)%2==0)Color(0xFF243749)else Color(0xFF203143)}
                                Box(Modifier.size(cell).background(color).border(if(hit||can)1.dp else .5.dp,if(hit)Color(0xFFFF7B86)else if(can)Color(0xFF83D9AF)else Color(0xFF536276))
                                    .clickable(enabled=(can||hit)&&!moving&&!attack){
                                        if(hit){attack=true;onChange(AdventureRules.strike(p,i,selected,now()))}
                                        else{direction=if(q.x<b.pos.x)RobotView.LEFT else RobotView.RIGHT;moving=true;onChange(AdventureRules.moveArena(p,q,now()))}
                                        onSound(PixubiSound.TAP)
                                    },contentAlignment=Alignment.Center){if(can)Text("·",color=Color(0xFF91D3AC))}
                            }
                        }
                    }
                    val px by animateDpAsState(cell*b.pos.x,tween(240),label="battle x")
                    val py by animateDpAsState(cell*b.pos.y,tween(240),label="battle y")
                    Box(Modifier.offset(px,py).size(cell)){
                        RobotPreview(direction,if(attack)RobotAction.ATTACK else if(moving)RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),phase,false,Modifier.fillMaxSize())
                        LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp))
                    }
                    b.enemies.forEachIndexed{i,e->key(i){
                        val ex by animateDpAsState(cell*e.pos.x,tween(240),label="bot x")
                        val ey by animateDpAsState(cell*e.pos.y,tween(240),label="bot y")
                        if(e.hp>0)Box(Modifier.offset(ex,ey).size(cell)){
                            RobotPreview(RobotView.LEFT,RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),e.tier,e.series),phase,false,Modifier.fillMaxSize())
                            LinearProgressIndicator(progress={e.hp.toFloat()/e.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp),color=Color(0xFFF07586))
                        }
                    }}
                }
            }
            Text(b.log)
            Text("Зброя · рух до ${AdventureRules.movement(p)} клітинок")
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(selected==null,{weapon=null},label={Text("Ближній удар · 1")})
                AdventureRules.weapons(p).forEach{g->
                    val ammo=AdventureRules.attackCost(g)
                    FilterChip(selected==g,{weapon=g},label={Text(g.title+" · "+AdventureRules.range(g)+(ammo?.let{" · ${p.stock(it)} зар."}?:""))})
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={onChange(AdventureRules.guard(p,now()));onSound(PixubiSound.TAP)},enabled=!moving&&!attack,modifier=Modifier.weight(1f)){Text("Захист")}
                OutlinedButton(onClick={onChange(AdventureRules.repair(p,now()));onSound(PixubiSound.HAPPY)},enabled=p.repair>0&&b.hp<b.maxHp&&!moving&&!attack,modifier=Modifier.weight(1f)){Text("Ремонт")}
            }
        }
    }
    if(retreat)AlertDialog(onDismissRequest={retreat=false},title={Text("Відступити?")},text={Text("Отримані пошкодження та витрачені заряди збережуться. Вороги залишаться на карті.")},
        confirmButton={TextButton(onClick={retreat=false;onChange(AdventureRules.retreat(p,now()))}){Text("Відступити")}},dismissButton={TextButton(onClick={retreat=false}){Text("Продовжити бій")}})
}
