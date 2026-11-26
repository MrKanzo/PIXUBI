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
import kotlinx.coroutines.delay

@Composable
internal fun AdventureScreen(p:WorldPet,onChange:(WorldPet)->Unit,onBack:()->Unit,onSound:(PixubiSound)->Unit){
    val a=p.adventure;val b=a.battle
    var retreat by remember{mutableStateOf(false)}
    var weapon by remember{mutableStateOf<RobotGear?>(null)}
    var moving by remember{mutableStateOf(false)}
    var attack by remember{mutableStateOf(false)}
    var direction by remember{mutableStateOf(RobotView.RIGHT)}
    var actorChoice by remember{mutableIntStateOf(-1)}
    val context=LocalContext.current
    val settings=remember(context){context.getSharedPreferences("pixubi_arena_settings",android.content.Context.MODE_PRIVATE)}
    var speed by remember{mutableStateOf(settings.getFloat("speed",1f).takeIf{it in listOf(.5f,1f,2f,3f)}?:1f)}
    val latestPet by rememberUpdatedState(p)
    val latestChange by rememberUpdatedState(onChange)
    val latestSound by rememberUpdatedState(onSound)
    var visualTarget by remember{mutableStateOf<GridPoint?>(null)}
    var visualSource by remember{mutableStateOf<GridPoint?>(null)}
    var visualKind by remember{mutableStateOf("")}
    var melee by remember{mutableStateOf(false)}
    var damageLabels by remember{mutableStateOf<List<Pair<GridPoint,Int>>>(emptyList())}
    val response=remember{Animatable(0f)}
    var queued by remember{mutableStateOf<(() -> WorldPet)?>(null)}
    val fx=remember{Animatable(0f)}
    fun duration(ms:Int)=(ms/speed).toInt().coerceAtLeast(1)
    val now={System.currentTimeMillis()}
    val loop=rememberInfiniteTransition(label="adventure")
    val phase by loop.animateFloat(0f,1f,infiniteRepeatable(tween(duration(1000),easing=LinearEasing)),label="actors")
    val busy=queued!=null||moving||attack
    fun perform(kind:String,source:GridPoint,target:GridPoint,closeCombat:Boolean=false,action:()->WorldPet){
        if(queued!=null||moving||attack)return
        visualKind=kind;visualSource=source;visualTarget=target;melee=closeCombat
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
            }else{
                fx.animateTo(1f,tween(duration(520),easing=LinearEasing))
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
            moving=false;attack=false;visualTarget=null;visualSource=null;visualKind="";damageLabels=emptyList();queued=null
        }
    }
    fun back(){if(busy)return;if(b!=null)retreat=true else onBack()}
    BackHandler{back()}
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text(if(b==null)"PIXUBI · ЕКСПЕДИЦІЯ" else "ТАКТИЧНА АРЕНА",style=MaterialTheme.typography.headlineSmall)
                Text(if(b==null)"Сектор ${a.stage} · дослідження" else "Раунд ${b.turn} · "+(if(busy)"Виконується дія" else "Твій хід"))
            }
            OutlinedButton(onClick={back()}){Text(if(b==null)"База" else "Відступ")}
        }
        Text("${p.name} · ${p.series.title} · ${p.tier.title}")
        Text("⚡ ${p.energy}%  ·  🪙 ${p.coins}  ·  🛠 ${p.repair}")
        if(b==null){
            Text("Торкнися дороги, контейнера або ворога. Робот прокладе маршрут через відкриту місцевість.",style=MaterialTheme.typography.bodySmall)
            FrontierMap(p,onChange,onSound)
            SquadPanel(p,onChange)
            Text("База · монети · енергокомірки · ремонт · боєприпаси",style=MaterialTheme.typography.bodySmall)
            Text("Переможено ботів: ${AdventureRules.encounters(a).count{AdventureRules.id(it) in a.cleared}}/${AdventureRules.encounters(a).size}")
            Text(p.message)
            if(!AdventureRules.canExplore(p))Text("Повернись на базу: потрібен заряд, ремонт або завершення місії.",color=MaterialTheme.colorScheme.error)
            if(AdventureRules.canAdvance(a))Button(onClick={onChange(AdventureRules.advanceMap(p))},enabled=AdventureRules.canExplore(p)&&a.stage<100,modifier=Modifier.fillMaxWidth()){Text("Наступний сектор")}
        }else{
            val selected=weapon?.takeIf{it in AdventureRules.weapons(p)}
            val available=SquadRules.available(b)
            val actor=actorChoice.takeIf{it in available}?:available.firstOrNull()?:-1
            val source=SquadRules.position(b,actor)
            val ally=b.allies.getOrNull(actor)
            fun canHit(enemy:ArenaBot)=if(actor==-1)AdventureRules.canHit(p,b,enemy,selected)else SquadRules.canHit(b,actor,enemy)
            fun reachable(q:GridPoint)=SquadRules.reachable(p,b,actor,q)
            Text("Загін · обери юніта для ходу",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(actor==-1,{actorChoice=-1},enabled=!busy&&-1 in available,label={Text("Командир "+if(-1 in b.acted)"✓" else "${b.hp} HP")})
                b.allies.forEachIndexed{i,u->FilterChip(actor==i,{actorChoice=i},enabled=!busy&&i in available,label={Text(u.kind.title+" ×${u.count} "+if(u.hp==0)"×" else if(i in b.acted)"✓" else "${u.hp} HP")})}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                Text("Швидкість",style=MaterialTheme.typography.labelMedium)
                listOf(.5f,1f,2f,3f).forEach{v->FilterChip(speed==v,{speed=v;settings.edit().putFloat("speed",v).apply()},enabled=!busy,label={Text(if(v==.5f)"0,5×" else "${v.toInt()}×")})}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Твій робот · HP");Text("${b.hp}/${b.maxHp}");LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.fillMaxWidth())
                }}
                Card(Modifier.weight(1f)){Column(Modifier.padding(8.dp)){
                    Text("Вороги · HP");b.enemies.forEach{Text("${it.series.title} ${it.hp}/${it.maxHp}",style=MaterialTheme.typography.bodySmall)}
                }}
            }
            Text("Блакитні шестикутники — доступні для ходу. Червона рамка — ворог у радіусі атаки. Після ходу всіх живих юнітів відповідають боти.",style=MaterialTheme.typography.bodySmall)
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val cell=maxWidth/(AdventureRules.ARENA_W+.5f)
                val hexH=cell*1.1547f
                val rowStep=hexH*.75f
                fun xOf(q:GridPoint)=cell*(q.x+if(q.y%2==1).5f else 0f)
                fun yOf(q:GridPoint)=rowStep*q.y
                Box(Modifier.fillMaxWidth().height(rowStep*(AdventureRules.ARENA_H-1)+hexH).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF263A4D),Color(0xFF10202B),Color(0xFF182A36))))){
                    Canvas(Modifier.fillMaxSize().pointerInput(p,b,selected,busy,speed,actor){
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
                            if(q!=null&&!busy){
                                val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                                val friend=b.allies.indexOfFirst{it.hp>0&&it.pos==q}
                                if(q==b.pos&&-1 in available){actorChoice=-1}
                                else if(friend>=0&&friend in available){actorChoice=friend}
                                else if(i>=0&&canHit(b.enemies[i])){
                                    direction=if(q.x<source.x)RobotView.LEFT else RobotView.RIGHT
                                    perform("strike",source,q,if(ally!=null)ally.kind.range==1 else selected==null||AdventureRules.range(selected)==1){
                                        latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.strike(latestPet,i,selected,now())else AdventureRules.allyStrike(latestPet,actor,i,now())
                                    }
                                }else if(reachable(q)){
                                    direction=if(q.x<source.x)RobotView.LEFT else RobotView.RIGHT
                                    perform("move",source,q){latestSound(PixubiSound.TAP);if(actor==-1)AdventureRules.moveArena(latestPet,q,now())else AdventureRules.allyMove(latestPet,actor,q,now())}
                                }
                            }
                        }
                    }){
                        val w=size.width/(AdventureRules.ARENA_W+.5f)
                        val h=w*1.1547f
                        // Dark industrial deck, illuminated channels and a warm enemy side.
                        drawRect(Brush.radialGradient(listOf(Color(0xFF3D7880).copy(alpha=.25f),Color.Transparent),Offset(size.width*.17f,size.height*.52f),size.width*.62f))
                        drawRect(Brush.radialGradient(listOf(Color(0xFF9F4C48).copy(alpha=.22f),Color.Transparent),Offset(size.width*.85f,size.height*.52f),size.width*.55f))
                        for(k in 0..12){
                            val yy=k*size.height/12f
                            drawLine(Color(0xFF91A7A4).copy(alpha=.09f),Offset(0f,yy),Offset(size.width,yy),1f)
                        }
                        for(y in 0 until AdventureRules.ARENA_H)for(x in 0 until AdventureRules.ARENA_W){
                            val q=GridPoint(x,y)
                            val i=b.enemies.indexOfFirst{it.hp>0&&it.pos==q}
                            val hit=i>=0&&canHit(b.enemies[i])
                            val can=reachable(q)
                            val cx=w*(x+.5f+if(y%2==1).5f else 0f);val cy=h*(.5f+y*.75f)
                            val outline=Path().apply{
                                moveTo(cx,cy-h*.48f);lineTo(cx+w*.48f,cy-h*.24f)
                                lineTo(cx+w*.48f,cy+h*.24f);lineTo(cx,cy+h*.48f)
                                lineTo(cx-w*.48f,cy+h*.24f);lineTo(cx-w*.48f,cy-h*.24f);close()
                            }
                            val fill=when{hit->Color(0xFF743844);can->Color(0xFF285D5A);i>=0->Color(0xFF523846);q==source->Color(0xFF246B83);q==b.pos||b.allies.any{it.hp>0&&it.pos==q}->Color(0xFF244C66);else->if((x+y)%2==0)Color(0xFF34454B)else Color(0xFF293A43)}
                            drawPath(outline,Brush.verticalGradient(listOf(Color(0xFF8FACA6).copy(alpha=.30f),fill,Color(0xFF11232C)),startY=cy-h*.5f,endY=cy+h*.5f))
                            drawPath(outline,when{hit->Color(0xFFFF8490);can->Color(0xFF65DBE8);else->Color(0xFF59647E)},style=Stroke(if(hit||can)2f else 1f))
                            for(j in 0..5){
                                val dx=((x*17+y*9+j*13)%21-10)*w/38f
                                val dy=((x*9+y*19+j*7)%17-8)*h/38f
                                drawRect(Color.White.copy(alpha=.06f),Offset(cx+dx,cy+dy),androidx.compose.ui.geometry.Size(w*.08f,h*.035f))
                            }
                            if(can&&!busy){drawCircle(Color(0xFF71CAD4).copy(alpha=.16f),w*.19f,Offset(cx,cy));drawCircle(Color(0xFF71CAD4),w*.035f,Offset(cx,cy))}
                        }
                    }
                    val px by animateDpAsState(xOf(b.pos),tween(duration(300)),label="battle x")
                    val py by animateDpAsState(yOf(b.pos)+hexH*.08f,tween(duration(300)),label="battle y")
                    Box(Modifier.offset(px,py).size(cell)){
                        RobotPreview(direction,if(attack&&visualSource==b.pos)RobotAction.ATTACK else if(moving&&visualTarget==b.pos)RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),phase,false,Modifier.fillMaxSize())
                        LinearProgressIndicator(progress={b.hp.toFloat()/b.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp))
                    }
                    b.allies.forEachIndexed{i,u->key("ally"+i){
                        val ax by animateDpAsState(xOf(u.pos),tween(duration(300)),label="ally x")
                        val ay by animateDpAsState(yOf(u.pos)+hexH*.08f,tween(duration(300)),label="ally y")
                        if(u.hp>0)Box(Modifier.offset(ax,ay).size(cell)){
                            Canvas(Modifier.fillMaxSize()){
                                drawOval(Color(0xFF70E6CD).copy(alpha=if(actor==i).65f else .25f),Offset(size.width*.1f,size.height*.76f),Size(size.width*.8f,size.height*.20f),style=Stroke(2f))
                            }
                            val activeAction=when{
                                attack&&visualSource==u.pos->RobotAction.ATTACK
                                moving&&visualTarget==u.pos->if(u.kind==SquadKind.AERIAL)RobotAction.BIO_FLIGHT else RobotAction.WALK
                                busy&&visualKind=="repair"&&visualSource==u.pos->RobotAction.CHARGE
                                else->RobotAction.IDLE
                            }
                            RobotPreview(if(activeAction==RobotAction.IDLE)RobotView.RIGHT else direction,activeAction,RobotFace.NEUTRAL,u.kind.loadout(),phase,false,Modifier.fillMaxSize())
                            LinearProgressIndicator(progress={u.hp.toFloat()/u.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp),color=Color(0xFF77DCC5))
                            Text("×${u.count}",Modifier.align(Alignment.BottomEnd).background(Color(0xFF142D3C),RoundedCornerShape(4.dp)).padding(horizontal=3.dp),color=Color(0xFFB9F8DB),style=MaterialTheme.typography.labelSmall)
                        }
                    }}
                    b.enemies.forEachIndexed{i,e->key(i){
                        val ex by animateDpAsState(xOf(e.pos),tween(duration(300)),label="bot x")
                        val ey by animateDpAsState(yOf(e.pos)+hexH*.08f,tween(duration(300)),label="bot y")
                        if(e.hp>0)Box(Modifier.offset(ex,ey).size(cell)){
                            RobotPreview(RobotView.LEFT,if(busy&&visualKind=="move")RobotAction.WALK else RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),e.tier,e.series),phase,false,Modifier.fillMaxSize())
                            LinearProgressIndicator(progress={e.hp.toFloat()/e.maxHp},modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp),color=Color(0xFFF07586))
                        }
                    }}
                    // Effects above sprites. Their progress is presentation-only.
                    if(busy)Canvas(Modifier.fillMaxSize()){
                        val src=visualSource;val dst=visualTarget
                        if(src!=null&&dst!=null){
                            val w=size.width/(AdventureRules.ARENA_W+.5f);val h=w*1.1547f
                            fun center(q:GridPoint)=Offset(w*(q.x+.5f+if(q.y%2==1).5f else 0f),h*(.5f+q.y*.75f))
                            val from=center(src);val to=center(dst)
                            val t=fx.value
                            when(visualKind){
                                "strike"->{
                                    val point=from+(to-from)*(t*1.8f).coerceIn(0f,1f)
                                    if(t<.62f&&melee){
                                        val sweep=Offset(to.x-w*.34f,to.y-w*.34f)
                                        drawArc(Color(0xFFE2FAFF).copy(alpha=1f-t),-135f,190f*t/.62f,false,sweep,Size(w*.68f,w*.68f),style=Stroke(w*.10f,cap=StrokeCap.Round))
                                    }else if(t<.62f){
                                        drawLine(Color(0xFF61E9FF).copy(alpha=.24f),from,point,w*.20f,StrokeCap.Round)
                                        drawLine(Color(0xFFB5FFFF),from,point,w*.052f,StrokeCap.Round)
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
                    damageLabels.forEach{(point,amount)->
                        val t=response.value
                        Box(Modifier.offset(xOf(point),yOf(point)-cell*(.15f+t*.4f)).width(cell),contentAlignment=Alignment.Center){
                            Text(if(amount>0)"−$amount" else "+${-amount}",color=(if(amount>0)Color(0xFFFFA285)else Color(0xFF9EFFB6)).copy(alpha=(1f-t*.65f).coerceIn(0f,1f)),style=MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            if(busy)Text(when(visualKind){"strike"->"⚡ Атака";"guard"->"🛡 Щит";"repair"->"✚ Ремонт";else->"Переміщення"},color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.titleMedium)
            Text(b.log)
            Text(if(ally==null)"Командир · рух до ${AdventureRules.movement(p)} клітинок" else "${ally.kind.title} ×${ally.count} · ATK ${ally.attack} · рух ${ally.move} · дальність ${ally.range}"+if(ally.kind.charges>0)" · заряди ${ally.charges}" else "")
            if(ally==null){
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(selected==null,{weapon=null},label={Text("Ближній удар · 1")})
                AdventureRules.weapons(p).forEach{g->
                    val ammo=AdventureRules.attackCost(g)
                    FilterChip(selected==g,{weapon=g},label={Text(g.title+" · "+AdventureRules.range(g)+(ammo?.let{" · ${p.stock(it)} зар."}?:""))})
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
    }
    if(retreat)AlertDialog(onDismissRequest={retreat=false},title={Text("Відступити?")},text={Text("Отримані пошкодження та витрачені заряди збережуться. Вороги залишаться на карті.")},
        confirmButton={TextButton(onClick={retreat=false;onChange(AdventureRules.retreat(p,now()))}){Text("Відступити")}},dismissButton={TextButton(onClick={retreat=false}){Text("Продовжити бій")}})
}

@Composable private fun SquadPanel(p:WorldPet,onChange:(WorldPet)->Unit){
    var expanded by remember{mutableStateOf(false)}
    var dismiss by remember{mutableStateOf<SquadKind?>(null)}
    val atBase=SquadRules.atBase(p)
    val roster=p.adventure.squad
    val loop=rememberInfiniteTransition(label="squad")
    val phase by loop.animateFloat(0f,1f,infiniteRepeatable(tween(2800,easing=LinearEasing)),label="squad preview")
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
                Text(if(atBase)"До 5 бійців одного класу в клітинці. Втрата бійця зменшує силу загону. Заряди поповнюються перед боєм." else "Найм, ремонт і покращення доступні на стартовій базі.")
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
                            Button(onClick={onChange(SquadRules.recruit(p,k))},enabled=atBase&&p.adventure.barracks>=k.requiredBarracks&&(hp!=null||roster.size<SquadRules.slots(p.adventure))&&count<SquadRules.MAX_STACK&&p.coins>=k.price){Text("+1 боєць · ${k.price} 🪙 · $count/5")}
                            if(hp!=null){
                                val rank=SquadRules.rank(p.adventure,k)
                                if(rank<3)OutlinedButton(onClick={onChange(SquadRules.upgrade(p,k))},enabled=atBase&&rank<p.adventure.barracks&&p.coins>=SquadRules.upgradePrice(p.adventure,k)){Text("Ранг ${rank+1} · ${SquadRules.upgradePrice(p.adventure,k)} 🪙")}
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    TextButton(onClick={onChange(SquadRules.restore(p,k))},enabled=atBase&&hp<SquadRules.maxStackHp(p.adventure,k)&&p.coins>=15){Text("Ремонт 15")}
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
