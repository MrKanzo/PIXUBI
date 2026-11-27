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
    Canvas(Modifier.size(64.dp)){
        val c=if(built)Color(0xFF74E5DB)else Color(0xFF667A92)
        val w=size.width;val h=size.height
        fun block(x:Float,y:Float,dx:Float,dy:Float){drawRect(Color(0xFF24364B),Offset(w*x,h*y),Size(w*dx,h*dy));drawRect(c,Offset(w*x,h*y),Size(w*dx,h*dy),style=Stroke(2f))}
        drawOval(Color.Black.copy(alpha=.25f),Offset(w*.08f,h*.78f),Size(w*.84f,h*.15f))
        when(kind){
            BaseBuilding.BARRACKS->{block(.13f,.31f,.74f,.49f);block(.39f,.53f,.22f,.27f);for(i in 0..2)drawRect(c,Offset(w*(.24f+i*.22f),h*.40f),Size(w*.10f,h*.06f))}
            BaseBuilding.FOUNDRY->{block(.12f,.40f,.76f,.40f);block(.19f,.15f,.13f,.30f);block(.63f,.22f,.14f,.20f);drawCircle(c,w*.13f,Offset(w*.50f,h*.58f),style=Stroke(3f))}
            BaseBuilding.NEURAL->{block(.18f,.14f,.64f,.48f);block(.42f,.62f,.16f,.19f);drawLine(c,Offset(w*.28f,h*.32f),Offset(w*.72f,h*.32f),3f);drawLine(c,Offset(w*.28f,h*.43f),Offset(w*.58f,h*.43f),3f)}
            BaseBuilding.WORKSHOP->{block(.12f,.28f,.76f,.52f);block(.30f,.12f,.40f,.22f);drawCircle(c,w*.15f,Offset(w*.50f,h*.53f),style=Stroke(3f));drawLine(c,Offset(w*.50f,h*.38f),Offset(w*.50f,h*.68f),3f)}
            BaseBuilding.SERVICE->{block(.16f,.28f,.68f,.52f);drawLine(c,Offset(w*.50f,h*.39f),Offset(w*.50f,h*.68f),6f);drawLine(c,Offset(w*.35f,h*.535f),Offset(w*.65f,h*.535f),6f)}
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
    Text(p.message)
    BaseBuilding.entries.forEach{b->Card(Modifier.fillMaxWidth()){
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            BuildingIcon(b,b in p.buildings)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(b.title,style=MaterialTheme.typography.titleMedium);Text(b.code,color=MaterialTheme.colorScheme.secondary)
                Text(b.description,style=MaterialTheme.typography.bodySmall)
                if(b in p.buildings)OutlinedButton(onClick={onOpen(b)}){Text("Відкрити")}
                else{
                    val reason=BaseRules.buyReason(p,b)
                    if(reason!=null)Text(reason,style=MaterialTheme.typography.labelSmall)
                    Button(onClick={onBuy(b)},enabled=reason==null){Text("Побудувати · ${b.price} 🪙")}
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
