package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
                Text(if(b==null)"PIXUBI · ЕКСПЕДИЦІЯ" else "ТАКТИЧНА АРЕНА",style=MaterialTheme.typography.headlineSmall)
                Text(if(b==null)"Сектор ${a.stage} · дослідження" else "Раунд ${b.turn} · твій хід")
            }
            OutlinedButton(onClick={back()}){Text(if(b==null)"База" else "Відступ")}
        }
        Text("${p.name} · ${p.series.title} · ${p.tier.title}")
        Text("⚡ ${p.energy}%  ·  🪙 ${p.coins}  ·  🛠 ${p.repair}")
        if(b==null){
            Text("Торкнися дороги, контейнера або ворога. Робот прокладе маршрут через відкриту місцевість.",style=MaterialTheme.typography.bodySmall)
            FrontierMap(p,onChange,onSound)
            Text("База · монети · енергокомірки · ремонт · боєприпаси",style=MaterialTheme.typography.bodySmall)
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
            Text("Блакитні шестикутники — доступні для ходу. Червона рамка — ворог у радіусі атаки. Після твоєї дії ходять усі живі боти.",style=MaterialTheme.typography.bodySmall)
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val cell=maxWidth/(AdventureRules.ARENA_W+.5f)
                val hexH=cell*1.1547f
                val rowStep=hexH*.75f
                fun xOf(q:GridPoint)=cell*(q.x+if(q.y%2==1).5f else 0f)
                fun yOf(q:GridPoint)=rowStep*q.y
                Box(Modifier.fillMaxWidth().height(rowStep*(AdventureRules.ARENA_H-1)+hexH).background(Color(0xFF162237))){
                    Canvas(Modifier.fillMaxSize().pointerInput(p,b,selected,moving,attack){
                        detectTapGestures{tap->
                            val w=size.width/(AdventureRules.ARENA_W+.5f)
                            val h=w*1.1547f
                            var hitCell:GridPoint?=null
                            for(y in 0 until AdventureRules.ARENA_H)for(x in 0 until AdventureRules.ARENA_W){
                                val cx=w*(x+.5f+if(y%2==1).5f else 0f)
                                val cy=h*(.5f+y*.75f)
                                val dx=kotlin.math.abs(tap.x-cx)
                                val dy=kotlin.math.abs(tap.y-cy)
                                if(dx<=w/2f&&dy<=h/2f-dx*h/(2f*w))hitCell=GridPoint(x,y)
                            }
                            val q=hitCell
                            if(q!=null&&!moving&&!attack){
                                val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                                if(i>=0&&AdventureRules.canHit(p,b,b.enemies[i],selected)){
                                    attack=true;onChange(AdventureRules.strike(p,i,selected,now()));onSound(PixubiSound.TAP)
                                }else if(AdventureRules.reachable(p,b,q)){
                                    direction=if(q.x<b.pos.x)RobotView.LEFT else RobotView.RIGHT
                                    moving=true;onChange(AdventureRules.moveArena(p,q,now()));onSound(PixubiSound.TAP)
                                }
                            }
                        }
                    }){
                        val w=size.width/(AdventureRules.ARENA_W+.5f)
                        val h=w*1.1547f
                        for(y in 0 until AdventureRules.ARENA_H)for(x in 0 until AdventureRules.ARENA_W){
                            val q=GridPoint(x,y)
                            val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                            val hit=i>=0&&AdventureRules.canHit(p,b,b.enemies[i],selected)
                            val can=AdventureRules.reachable(p,b,q)
                            val cx=w*(x+.5f+if(y%2==1).5f else 0f);val cy=h*(.5f+y*.75f)
                            val outline=Path().apply{
                                moveTo(cx,cy-h*.48f);lineTo(cx+w*.48f,cy-h*.24f)
                                lineTo(cx+w*.48f,cy+h*.24f);lineTo(cx,cy+h*.48f)
                                lineTo(cx-w*.48f,cy+h*.24f);lineTo(cx-w*.48f,cy-h*.24f);close()
                            }
                            val fill=when{hit->Color(0xFF743844);can->Color(0xFF254F58);i>=0->Color(0xFF523846);q==b.pos->Color(0xFF244C66);else->if((x+y)%2==0)Color(0xFF343D53)else Color(0xFF30394D)}
                            drawPath(outline,fill)
                            drawPath(outline,when{hit->Color(0xFFFF8490);can->Color(0xFF65DBE8);else->Color(0xFF59647E)},style=Stroke(if(hit||can)2f else 1f))
                            for(j in 0..5){
                                val dx=((x*17+y*9+j*13)%21-10)*w/38f
                                val dy=((x*9+y*19+j*7)%17-8)*h/38f
                                drawRect(Color.White.copy(alpha=.06f),Offset(cx+dx,cy+dy),androidx.compose.ui.geometry.Size(w*.08f,h*.035f))
                            }
                            if(can)drawCircle(Color(0xFF71CAD4),w*.035f,Offset(cx,cy))
                        }
                    }
                    val px by animateDpAsState(xOf(b.pos),tween(240),label="battle x")
                    val py by animateDpAsState(yOf(b.pos)+hexH*.08f,tween(240),label="battle y")
                    Box(Modifier.offset(px,py).size(cell)){
                        RobotPreview(direction,if(attack)RobotAction.ATTACK else if(moving)RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),phase,false,Modifier.fillMaxSize())
                        LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp))
                    }
                    b.enemies.forEachIndexed{i,e->key(i){
                        val ex by animateDpAsState(xOf(e.pos),tween(240),label="bot x")
                        val ey by animateDpAsState(yOf(e.pos)+hexH*.08f,tween(240),label="bot y")
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
