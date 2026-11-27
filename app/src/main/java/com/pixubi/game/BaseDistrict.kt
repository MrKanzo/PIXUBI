package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable private fun BuildingIcon(kind:BaseBuilding,built:Boolean){
    Canvas(Modifier.size(62.dp)){
        val w=size.width;val h=size.height
        val c=when(kind){BaseBuilding.BARRACKS->Color(0xFF69E4D1);BaseBuilding.FOUNDRY->Color(0xFFF5AD69);BaseBuilding.NEURAL->Color(0xFFAB9CFF);BaseBuilding.SERVICE->Color(0xFF8EE7A4);BaseBuilding.WORKSHOP->Color(0xFF67C9FF)}
        fun poly(col:Color,vararg xy:Float){
            val path=Path().apply{moveTo(xy[0]*w,xy[1]*h);for(i in 2 until xy.size step 2)lineTo(xy[i]*w,xy[i+1]*h);close()}
            drawPath(path,col);drawPath(path,Color(0xFF102238),style=Stroke(1.dp.toPx()))
        }
        fun block(x:Float,y:Float,dx:Float,dy:Float){
            val d=.13f
            poly(Color(0xFF53758D),x,y,x+d,y-d,x+dx+d,y-d,x+dx,y)
            poly(Color(0xFF294258),x,y,x+dx,y,x+dx,y+dy,x,y+dy)
            poly(Color(0xFF203347),x+dx,y,x+dx+d,y-d,x+dx+d,y+dy-d,x+dx,y+dy)
            drawLine(c,Offset((x+.025f)*w,(y+.03f)*h),Offset((x+dx-.025f)*w,(y+.03f)*h),2.dp.toPx())
        }
        drawOval(Color(0xFF0B1524),Offset(w*.05f,h*.77f),Size(w*.90f,h*.18f))
        when(kind){
            BaseBuilding.BARRACKS->{block(.09f,.42f,.67f,.38f);block(.28f,.30f,.32f,.12f);drawRect(Color(0xFF101F30),Offset(w*.34f,h*.59f),Size(w*.18f,h*.21f));for(i in 0..2)drawRect(c,Offset(w*(.16f+i*.22f),h*.51f),Size(w*.09f,h*.05f))}
            BaseBuilding.FOUNDRY->{block(.10f,.45f,.65f,.36f);block(.14f,.20f,.13f,.26f);block(.57f,.29f,.13f,.17f);drawCircle(c,w*.13f,Offset(w*.43f,h*.63f),style=Stroke(3.dp.toPx()));drawCircle(Color(0xFF122536),w*.07f,Offset(w*.43f,h*.63f))}
            BaseBuilding.NEURAL->{block(.25f,.73f,.44f,.10f);block(.43f,.50f,.10f,.23f);poly(Color(0xFF253B64),.12f,.22f,.80f,.14f,.80f,.51f,.12f,.59f);for(i in 0..2)drawLine(c,Offset(w*.22f,h*(.32f+i*.07f)),Offset(w*(.68f-i*.08f),h*(.26f+i*.07f)),2.dp.toPx())}
            BaseBuilding.WORKSHOP->{block(.12f,.42f,.66f,.38f);block(.32f,.26f,.30f,.17f);drawCircle(c,w*.15f,Offset(w*.45f,h*.61f),style=Stroke(2.5.dp.toPx()));drawLine(c,Offset(w*.45f,h*.50f),Offset(w*.45f,h*.72f),2.dp.toPx())}
            BaseBuilding.SERVICE->{block(.12f,.40f,.66f,.41f);block(.30f,.28f,.31f,.13f);drawLine(c,Offset(w*.45f,h*.48f),Offset(w*.45f,h*.71f),4.dp.toPx());drawLine(c,Offset(w*.31f,h*.595f),Offset(w*.59f,h*.595f),4.dp.toPx())}
        }
        if(built)drawCircle(Color(0xFF6DF6AE),w*.055f,Offset(w*.88f,h*.13f))
    }
}
@Composable internal fun BaseLocationPanel(p:WorldPet,onReturn:()->Unit,onWake:()->Unit){
    val at=p.adventure.pos==AdventureRules.base
    val route=remember(p.adventure){AdventureRules.homeRoute(p.adventure)}
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF1B3043))){
        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(if(at)"⌂ Робот на базі" else "◇ Робот в експедиції · сектор ${p.adventure.stage}",style=MaterialTheme.typography.titleSmall,color=Color(0xFF89E8DE))
            if(p.mode!=WorldMode.AWAKE)TextButton(onClick=onWake){Text("Розбудити робота")}
            else if(!at){
                val can=AdventureRules.canExplore(p)
                Text(if(route.isNotEmpty())"Відкритий маршрут · ${route.size} кроків" else "Безпечний маршрут ще не відкрито",style=MaterialTheme.typography.labelSmall)
                Button(onClick=onReturn,enabled=can,modifier=Modifier.fillMaxWidth()){Text(if(route.isNotEmpty())"Повернутися на базу" else "Відкрити карту")}
                if(!can)Text("Потрібен заряд, ремонт або завершення місії",style=MaterialTheme.typography.labelSmall)
            }
            else if(p.mission!=null)Text("Будівництво після завершення місії",style=MaterialTheme.typography.labelSmall)
        }
    }
}
@Composable internal fun BaseDistrict(p:WorldPet,onOpen:(BaseBuilding)->Unit,onShop:()->Unit){
    Text("МІСТО PIXUBI · ${p.buildings.size}/${BaseBuilding.entries.size}",style=MaterialTheme.typography.titleMedium)
    BaseBuilding.entries.chunked(2).forEach{row->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            row.forEach{b->Card(onClick={if(b in p.buildings)onOpen(b)else onShop()},modifier=Modifier.weight(1f)){
                Column(Modifier.padding(8.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                    BuildingIcon(b,b in p.buildings);Text(b.title,style=MaterialTheme.typography.labelLarge)
                    Text(if(b in p.buildings)"Відкрити" else "Будівництво · ${b.price} 🪙",style=MaterialTheme.typography.labelSmall)
                }
            }}
        }
    }
    OutlinedButton(onClick=onShop,modifier=Modifier.fillMaxWidth()){Text("Магазин будівель")}
}
@Composable internal fun BaseConstructionShop(p:WorldPet,onBuy:(BaseBuilding)->Unit,onOpen:(BaseBuilding)->Unit){
    Text("Будівництво · ${p.coins} 🪙",style=MaterialTheme.typography.titleLarge)
    val location=BaseRules.locationReason(p)
    BaseBuilding.entries.forEach{b->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF1B293C))){
        Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            BuildingIcon(b,b in p.buildings)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                Text(b.title,style=MaterialTheme.typography.titleSmall)
                Text(b.code+" · рів. "+b.level,color=Color(0xFF8AD9E6),style=MaterialTheme.typography.labelSmall)
                Text(b.description,style=MaterialTheme.typography.bodySmall)
                if(b in p.buildings)OutlinedButton(onClick={onOpen(b)}){Text("Відкрити")}
                else{
                    val reason=BaseRules.buyReason(p,b)
                    if(reason!=null&&reason!=location)Text(reason,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error)
                    Button(onClick={onBuy(b)},enabled=reason==null,contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)){Text("Побудувати · ${b.price} 🪙",style=MaterialTheme.typography.labelMedium)}
                }
            }
        }
    }}
}
@Composable internal fun BaseBuildingDialog(p:WorldPet,kind:BaseBuilding,onChange:(WorldPet)->Unit,onClose:()->Unit){
    var counts by remember(kind){mutableStateOf<Map<SquadKind,Int>>(emptyMap())}
    AlertDialog(onDismissRequest=onClose,title={Text(kind.code+" · "+kind.title)},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("Баланс: ${p.coins} 🪙");Text(p.message,style=MaterialTheme.typography.bodySmall)
            when(kind){
                BaseBuilding.BARRACKS,BaseBuilding.FOUNDRY->{
                    Text("Казарми: ${p.adventure.barracks} · загони ${p.adventure.squad.size}/${SquadRules.slots(p.adventure)}")
                    if(kind==BaseBuilding.BARRACKS&&p.adventure.barracks<3)OutlinedButton(onClick={onChange(SquadRules.expand(p))},enabled=BaseRules.available(p)&&p.coins>=SquadRules.barracksPrice(p.adventure)){Text("Розширити · ${SquadRules.barracksPrice(p.adventure)} 🪙")}
                    SquadKind.entries.filter{BaseRules.buildingFor(it)==kind}.forEach{k->
                        val locked=SquadDisclosure.locked(p.adventure,k)
                        if(locked!=null){Text(locked.title+" · "+SquadResearch.progress(p.adventure,k),style=MaterialTheme.typography.bodySmall)}
                        else{
                            HorizontalDivider()
                            val max=BaseRules.recruitLimit(p,k);val count=(counts[k]?:1).coerceIn(1,max.coerceAtLeast(1))
                            Row(verticalAlignment=Alignment.CenterVertically){
                                RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,k.loadout(),0f,false,Modifier.size(60.dp))
                                Column(Modifier.weight(1f)){Text(k.title);Text("У загоні ${SquadRules.stackCount(p.adventure,k)}/5 · ${k.price} 🪙 / шт.",style=MaterialTheme.typography.labelSmall)}
                            }
                            if(k.requiredBarracks>p.adventure.barracks)Text("Потрібні казарми ${k.requiredBarracks}")
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                OutlinedButton(onClick={counts=counts+(k to count-1)},enabled=count>1){Text("−")}
                                Text("$count")
                                OutlinedButton(onClick={counts=counts+(k to count+1)},enabled=count<max){Text("+")}
                            }
                            Button(onClick={onChange(BaseRules.recruit(p,k,count))},enabled=max>=count,modifier=Modifier.fillMaxWidth()){Text("Створити ×$count · ${k.price*count} 🪙")}
                            if(k in p.adventure.squad){
                                val rank=SquadRules.rank(p.adventure,k)
                                if(rank<p.adventure.barracks&&rank<3)OutlinedButton(onClick={onChange(SquadRules.upgrade(p,k))},enabled=BaseRules.available(p)&&p.coins>=SquadRules.upgradePrice(p.adventure,k)){Text("Ранг ${rank+1} · ${SquadRules.upgradePrice(p.adventure,k)} 🪙")}
                                OutlinedButton(onClick={onChange(SquadRules.restore(p,k))},enabled=BaseRules.available(p)&&SquadRules.restorePrice(p.adventure,k)>0&&p.coins>=SquadRules.restorePrice(p.adventure,k)){Text("Ремонт · ${SquadRules.restorePrice(p.adventure,k)} 🪙")}
                            }
                        }
                    }
                }
                BaseBuilding.NEURAL->{
                    Text(ArenaAttackStyle.of(p.series).title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.secondary)
                    Text("Бойовий модуль · ${p.neuralRank}/3")
                    LinearProgressIndicator(progress={p.neuralRank/3f},modifier=Modifier.fillMaxWidth())
                    Text("Активна навичка командира: +${50+p.neuralRank*15}% базової шкоди, один раз за бій.")
                    val reason=BaseRules.neuralReason(p)
                    if(reason!=null)Text(reason)
                    if(p.neuralRank<3)Button(onClick={onChange(BaseRules.buyNeural(p))},enabled=reason==null){Text("Купити модуль ${p.neuralRank+1} · ${BaseRules.neuralPrice(p)} 🪙")}
                }
                BaseBuilding.WORKSHOP->CraftWorkshop(p,onChange)
                BaseBuilding.SERVICE->listOf("Енергокомірки","Ремонтні набори","Синхромодулі").forEachIndexed{i,title->
                    val stock=when(i){0->p.cells;1->p.repair;else->p.snacks}
                    Text("$title · $stock/30")
                    Button(onClick={onChange(WorldRules.supplies(p,i))},enabled=BaseRules.available(p)&&p.coins>=25&&stock<=27){Text("+3 · 25 🪙")}
                }
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("До бази")}})
}
