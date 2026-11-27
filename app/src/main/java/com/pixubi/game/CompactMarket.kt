package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal fun navSymbol(id:String)=when(id){"home"->"⌂";"missions"->"◎";"adventure"->"◇";"shop"->"▣";"inventory"->"▤";"construction"->"⬡";"stats"->"↗";"archive"->"▥";else->"◉"}
internal enum class MarketKind(val title:String,val symbol:String){
    ALL("Усі","▦"),WEAPON("Зброя","⚔"),HEAD("Шоломи","◈"),CHEST("Броня","⬡"),HAND("Руки","◧"),FEET("Ноги","⌁"),BACK("Модулі","▣"),SUPPLY("Ресурси","⚡");
    companion object{
        fun of(g:RobotGear)=if(WorldGearCatalog.category(g)==WorldCategory.WEAPONS)WEAPON else when(g.slot){
            RobotSlot.HEAD->HEAD;RobotSlot.CHEST->CHEST;RobotSlot.LEFT_HAND,RobotSlot.RIGHT_HAND->HAND;RobotSlot.FEET->FEET;RobotSlot.BACK->BACK
        }
    }
}
internal object MarketCatalog{
    fun shortStats(g:RobotGear):String{val stats=RobotBalance.spec(g).delta;val labels=listOf("HP","EN","ATK","DEF","SPD","INT","TEMP");return RobotStat.entries.filter{stats[it]!=0}.joinToString(" · "){s->val v=stats[s];labels[s.ordinal]+" "+(if(v>0)"+" else "")+v}.ifEmpty{"Без змін"}}
    fun groups(p:WorldPet,inventory:Boolean,kind:MarketKind,compatible:Boolean,available:Boolean,query:String):List<List<WorldOffer>> =
        WorldGearCatalog.groupOffers(WorldRules.shop.filter{o->
            val g=o.gear
            (!inventory||g in p.owned)&&(kind==MarketKind.ALL||kind==MarketKind.of(g))&&
            (!compatible||GearAccess.reason(p,g)==null||p.equipment.equipped(g.slot)==g)&&
            (!available||p.level>=o.level)&&(query.isBlank()||GearAccess.title(p.series,g).contains(query.trim(),ignoreCase=true))
        })
}
@Composable private fun ItemGlyph(gear:RobotGear?=null,supply:WorldSupply?=null){
    Canvas(Modifier.size(44.dp)){
        val w=size.width;val h=size.height
        val c=when{gear?.name?.contains("SHADOW")==true->Color(0xFFB4A0F7);gear?.name?.contains("TITAN")==true||gear==RobotGear.HEAVY_ARMOR->Color(0xFFF2BE80);gear?.name?.contains("PRISM")==true->Color(0xFFD6A3F0);else->Color(0xFF74D9DF)}
        val metal=Color(0xFF69849A);val shade=Color(0xFF172B3C)
        fun poly(vararg xy:Float){val p=Path().apply{moveTo(xy[0]*w,xy[1]*h);for(i in 2 until xy.size step 2)lineTo(xy[i]*w,xy[i+1]*h);close()};drawPath(p,androidx.compose.ui.graphics.Brush.linearGradient(listOf(metal,c.copy(alpha=.5f),Color(0xFF243B53)),Offset.Zero,Offset(w,h)));drawPath(p,c,style=Stroke(1.6.dp.toPx()))}
        fun line(x:Float,y:Float,x2:Float,y2:Float){drawLine(c,Offset(x*w,y*h),Offset(x2*w,y2*h),2.dp.toPx(),StrokeCap.Round)}
        drawRoundRect(shade,cornerRadius=CornerRadius(w*.18f))
        if(gear!=null){drawGearModel(gear,Color(0xFF74D9DF));return@Canvas}
        val family=gear?.name?.removeSuffix("_L")?.removeSuffix("_R").orEmpty()
        when{
            supply?.isAmmo==true->{for(i in 0..2){val x=.21f+i*.21f;poly(x,.29f,x+.07f,.16f,x+.14f,.29f,x+.14f,.79f,x,.79f)}}
            family=="BLADE"->{poly(.22f,.69f,.69f,.16f,.84f,.12f,.78f,.28f,.34f,.78f);line(.17f,.63f,.42f,.87f);line(.28f,.75f,.13f,.89f)}
            family=="SHIELD"->{poly(.20f,.22f,.50f,.12f,.80f,.22f,.76f,.62f,.50f,.88f,.24f,.62f);line(.50f,.27f,.50f,.69f)}
            family in setOf("BLASTER","RIFLE","CANNON","ARC")->{val end=if(family=="BLASTER").68f else .89f;poly(.12f,.30f,end,.30f,end,.48f,.48f,.48f,.39f,.79f,.22f,.79f,.30f,.48f,.12f,.48f);line(.40f,.37f,end-.06f,.37f);if(family=="CANNON"||family=="ARC")line(.45f,.23f,.78f,.23f)}
            gear==RobotGear.SCANNER->{poly(.14f,.34f,.32f,.24f,.80f,.24f,.88f,.47f,.70f,.65f,.24f,.65f);drawCircle(shade,w*.16f,Offset(w*.64f,h*.45f));drawCircle(c,w*.10f,Offset(w*.64f,h*.45f));line(.23f,.76f,.79f,.76f)}
            gear==RobotGear.VISOR->{poly(.12f,.32f,.88f,.32f,.76f,.64f,.55f,.70f,.45f,.70f,.24f,.64f);line(.24f,.43f,.75f,.43f)}
            gear==RobotGear.HELMET_SHADOW->{poly(.16f,.70f,.12f,.35f,.34f,.12f,.72f,.17f,.88f,.43f,.78f,.76f,.62f,.87f,.34f,.87f);poly(.23f,.39f,.78f,.39f,.70f,.57f,.30f,.57f);line(.31f,.48f,.68f,.48f)}
            gear==RobotGear.HELMET_TITAN->{poly(.12f,.27f,.29f,.12f,.71f,.12f,.88f,.27f,.88f,.76f,.65f,.84f,.65f,.57f,.35f,.57f,.35f,.84f,.12f,.76f);line(.27f,.38f,.73f,.38f)}
            gear?.slot==RobotSlot.HEAD->{poly(.18f,.69f,.13f,.36f,.30f,.16f,.70f,.16f,.87f,.36f,.82f,.69f,.64f,.78f,.64f,.52f,.36f,.52f,.36f,.78f);line(.29f,.43f,.71f,.43f);if(family.endsWith("PRISM"))poly(.42f,.17f,.50f,.05f,.58f,.17f,.50f,.32f)}
            gear?.slot==RobotSlot.CHEST&&gear!=RobotGear.REACTOR->{poly(.16f,.29f,.35f,.18f,.50f,.32f,.65f,.18f,.84f,.29f,.71f,.77f,.50f,.87f,.29f,.77f);line(.50f,.36f,.50f,.70f);if(gear==RobotGear.HEAVY_ARMOR){line(.12f,.26f,.25f,.19f);line(.75f,.19f,.88f,.26f)}}
            gear?.slot==RobotSlot.FEET->{for(x in listOf(.15f,.51f))poly(x,.19f,x+.20f,.19f,x+.20f,.56f,x+.31f,.64f,x+.31f,.80f,x,.80f)}
            gear==RobotGear.REACTOR||gear==RobotGear.SCANNER||supply==WorldSupply.SYNC->{drawCircle(metal,w*.31f,Offset(w*.5f,h*.5f));drawCircle(c,w*.24f,Offset(w*.5f,h*.5f),style=Stroke(2.dp.toPx()));drawCircle(c,w*.09f,Offset(w*.5f,h*.5f))}
            family.contains("REPAIR")||family=="TOOL"||supply?.category==WorldCategory.REPAIR->{poly(.15f,.28f,.40f,.13f,.55f,.30f,.43f,.43f,.83f,.76f,.67f,.90f,.28f,.51f,.13f,.52f);line(.35f,.32f,.67f,.65f)}
            else->{poly(.26f,.24f,.74f,.24f,.74f,.82f,.26f,.82f);line(.40f,.14f,.60f,.14f);line(.50f,.36f,.40f,.54f);line(.40f,.54f,.60f,.54f);line(.60f,.54f,.50f,.70f)}
        }
    }
}
@Composable internal fun CompactMarket(p:WorldPet,inventory:Boolean,onChange:(WorldPet)->Unit,onSound:(PixubiSound)->Unit){
    var kind by remember(inventory){mutableStateOf(MarketKind.ALL)}
    var compatible by remember(inventory){mutableStateOf(false)}
    var available by remember(inventory){mutableStateOf(false)}
    var query by remember(inventory){mutableStateOf("")}
    var selected by remember(inventory){mutableStateOf<RobotGear?>(null)}
    var supplySelected by remember(inventory){mutableStateOf<WorldSupply?>(null)}
    var basicSelected by remember(inventory){mutableStateOf<Int?>(null)}
    val groups=MarketCatalog.groups(p,inventory,kind,compatible,available,query)
    val supplies=WorldSupply.entries.filter{(kind==MarketKind.ALL||kind==MarketKind.SUPPLY)&&(!inventory||p.stock(it)>0)&&(!available||p.level>=it.level)&&it.title.contains(query.trim(),true)}
    val basics=listOf("Енергокомірки","Ремонтні набори","Модулі синхронізації")
    val basicIds=basics.indices.filter{(kind==MarketKind.ALL||kind==MarketKind.SUPPLY)&&basics[it].contains(query.trim(),true)&&(!inventory||when(it){0->p.cells;1->p.repair;else->p.snacks}>0)}
    Row(verticalAlignment=Alignment.CenterVertically){Text(if(inventory)"Інвентар" else "Магазин",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);Text("${p.coins} 🪙",color=MaterialTheme.colorScheme.secondary)}
    OutlinedTextField(query,{query=it},singleLine=true,placeholder={Text("Пошук")},modifier=Modifier.fillMaxWidth())
    MarketKind.entries.chunked(4).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
        row.forEach{k->Card(onClick={kind=k},modifier=Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=if(kind==k)Color(0xFF244959) else Color(0xFF1B293C))){
            Column(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)){PixubiGlyph(k.name,kind==k);Text(k.title,style=MaterialTheme.typography.labelSmall,maxLines=1)}
        }}
    }}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        FilterChip(compatible,{compatible=!compatible},label={Text("Сумісні")})
        FilterChip(available,{available=!available},label={Text("Доступні")})
    }
    if(inventory&&(kind==MarketKind.ALL||kind==MarketKind.SUPPLY))MaterialStock(p)
    if(p.message.isNotBlank())Text(p.message,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
    groups.forEach{group->
        val g=group.first().gear
        val owned=group.count{it.gear in p.owned}
        val worn=group.count{p.equipment.equipped(it.gear.slot)==it.gear}
        val status=when{worn>0->"Одягнуто";group.all{GearAccess.reason(p,it.gear)!=null}->"Несумісно";owned==group.size->"У власності";else->"Рівень ${group.minOf{it.level}}"}
        Card(onClick={selected=g},modifier=Modifier.fillMaxWidth()){
            Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ItemGlyph(gear=g)
                Column(Modifier.weight(1f)){
                    Text(GearAccess.title(p.series,g),style=MaterialTheme.typography.titleSmall,maxLines=2)
                    Text(MarketKind.of(g).title+" · "+status,style=MaterialTheme.typography.labelSmall,color=if(worn>0)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MarketCatalog.shortStats(g),style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Text(if(owned==group.size)"›" else "${group.minOf{it.price}} 🪙",style=MaterialTheme.typography.labelLarge)
            }
        }
    }
    supplies.forEach{s->Card(onClick={supplySelected=s},modifier=Modifier.fillMaxWidth()){
        Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            ItemGlyph(supply=s)
            Column(Modifier.weight(1f)){Text(s.title,style=MaterialTheme.typography.titleSmall);Text(s.category.title+" · запас "+p.stock(s),style=MaterialTheme.typography.labelSmall)}
            Text(if(inventory)"›" else "${s.price} 🪙",style=MaterialTheme.typography.labelLarge)
        }
    }}
    basicIds.forEach{i->Card(onClick={basicSelected=i},modifier=Modifier.fillMaxWidth()){
        Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            ItemGlyph(supply=listOf(WorldSupply.CELL_SMALL,WorldSupply.PATCH,WorldSupply.SYNC)[i])
            Column(Modifier.weight(1f)){Text(basics[i],style=MaterialTheme.typography.titleSmall);Text("Запас · "+when(i){0->p.cells;1->p.repair;else->p.snacks},style=MaterialTheme.typography.labelSmall)}
            Text(if(inventory)"›" else "25 🪙")
        }
    }}
    if(groups.isEmpty()&&supplies.isEmpty()&&basicIds.isEmpty())Text("Нічого не знайдено")
    selected?.let{g->
        val offers=WorldGearCatalog.groupOffers(WorldRules.shop).first{group->group.any{it.gear==g}}.filter{!inventory||it.gear in p.owned}
        var preview by remember(g){mutableStateOf(g)}
        val e=p.equipment.equip(preview)
        AlertDialog(onDismissRequest={selected=null},title={Text(GearAccess.title(p.series,g))},text={
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text("Примірка · не змінює спорядження",style=MaterialTheme.typography.labelSmall)
                var side by remember(g){mutableStateOf(false)}
                Row(verticalAlignment=Alignment.CenterVertically){ItemGlyph(gear=preview);Text(preview.title,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);TextButton(onClick={side=!side}){Text(if(side)"Спереду" else "Збоку")}}
                RobotPreview(if(side)RobotView.RIGHT else RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(p.skin.accent,e,p.tier,p.series),0f,false,Modifier.fillMaxWidth().height(160.dp))
                Text(RobotBalance.spec(g).delta.summary());Text(WorldGearCatalog.description(g),style=MaterialTheme.typography.bodySmall)
                offers.forEach{o->
                    val item=o.gear;val worn=p.equipment.equipped(item.slot)==item;val owned=item in p.owned;val reason=GearAccess.reason(p,item)
                    HorizontalDivider()
                    Row(verticalAlignment=Alignment.CenterVertically){Text(WorldCatalog.slots(p.series,item.slot),Modifier.weight(1f));TextButton(onClick={preview=item}){Text("Примірка")}}
                    if(reason!=null)Text(reason,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelSmall)
                    Text("Рівень ${o.level} · ${o.price} 🪙",style=MaterialTheme.typography.labelSmall)
                    if(owned)OutlinedButton(onClick={onChange(WorldRules.equip(p,item))},enabled=p.mission==null&&p.adventure.battle==null&&(worn||reason==null),modifier=Modifier.fillMaxWidth()){Text(if(worn)"Зняти" else "Одягнути")}
                    else Button(onClick={onChange(WorldRules.buy(p,item))},enabled=p.level>=o.level&&p.coins>=o.price,modifier=Modifier.fillMaxWidth()){Text("Купити")}
                }
            }
        },confirmButton={TextButton(onClick={selected=null}){Text("Закрити")}})
    }
    supplySelected?.let{s->AlertDialog(onDismissRequest={supplySelected=null},title={Text(s.title)},text={Text(s.description+"\nЗапас: "+p.stock(s)+" · рівень "+s.level)},confirmButton={
        if(!inventory)Button(onClick={onChange(WorldRules.buySupply(p,s))},enabled=p.level>=s.level&&p.coins>=s.price&&p.stock(s)+s.pack<=99){Text("+${s.pack} · ${s.price} 🪙")}
        else if(!s.isAmmo)Button(onClick={onChange(WorldRules.useSupply(p,s));onSound(PixubiSound.HAPPY)},enabled=p.mode==WorldMode.AWAKE&&p.mission==null&&p.stock(s)>0){Text("Використати")}
    },dismissButton={TextButton(onClick={supplySelected=null}){Text("Закрити")}})}
    basicSelected?.let{i->val stock=when(i){0->p.cells;1->p.repair;else->p.snacks};AlertDialog(onDismissRequest={basicSelected=null},title={Text(basics[i])},text={Text("Запас $stock/30")},confirmButton={
        if(inventory)Button(onClick={onChange(WorldRules.care(p,i))},enabled=stock>0&&p.mode==WorldMode.AWAKE&&p.mission==null){Text("Використати")}
        else Button(onClick={onChange(WorldRules.supplies(p,i))},enabled=p.coins>=25&&stock<=27){Text("+3 · 25 🪙")}
    },dismissButton={TextButton(onClick={basicSelected=null}){Text("Закрити")}})}
}
