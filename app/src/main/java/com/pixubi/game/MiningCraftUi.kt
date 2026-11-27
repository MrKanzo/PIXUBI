package com.pixubi.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private fun materialColor(k:CraftMaterial)=when(k){
    CraftMaterial.IRON->Color(0xFFB0C6DF);CraftMaterial.COPPER->Color(0xFFF4AA72);CraftMaterial.SILICON->Color(0xFF99E2B5)
    CraftMaterial.CRYSTAL->Color(0xFF63EEFF);CraftMaterial.TITANIUM->Color(0xFFE0A9FF);CraftMaterial.CORE->Color(0xFFFFD775)
}
internal fun DrawScope.drawMiningSite(n:MineralSite,phase:Float){
    val c=materialColor(n.material)
    drawOval(Color(0xFF18242F).copy(alpha=.65f),Offset(-34f,-10f),Size(68f,25f))
    fun crystal(x:Float,h:Float,w:Float){
        val path=Path().apply{moveTo(x-w,3f);lineTo(x-w,-h*.55f);lineTo(x,-h);lineTo(x+w,-h*.46f);lineTo(x+w,3f);close()}
        drawPath(path,c.copy(alpha=.8f));drawPath(path,Color(0xFF22364D),style=Stroke(2f))
        drawLine(Color.White.copy(alpha=.65f),Offset(x,-h+3),Offset(x,0f),1.5f)
    }
    if(n.mine){
        val p=Path().apply{moveTo(-36f,7f);lineTo(-34f,-26f);lineTo(-18f,-48f);lineTo(18f,-48f);lineTo(34f,-26f);lineTo(36f,7f);close()}
        drawPath(p,Color(0xFF475A70));drawPath(p,c,style=Stroke(3f))
        drawRect(Color(0xFF111C2B),Offset(-18f,-26f),Size(36f,33f))
        drawLine(c,Offset(-25f,-30f),Offset(25f,-30f),3f)
        crystal(-25f,16f,7f);crystal(26f,22f,8f)
        drawCircle(c.copy(alpha=.65f+.25f*sin(phase*6.28f)),4f,Offset(0f,-40f))
    }else{crystal(-17f,22f,10f);crystal(0f,38f,12f);crystal(17f,26f,9f)}
}
@Composable internal fun MaterialStock(p:WorldPet){
    Text("Матеріали",style=MaterialTheme.typography.titleSmall)
    CraftMaterial.entries.chunked(3).forEach{group->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            group.forEach{k->Surface(modifier=Modifier.weight(1f),color=MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.small){
                Column(Modifier.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(k.code+" · "+p.material(k),color=materialColor(k),style=MaterialTheme.typography.labelLarge)
                    Text(k.title,style=MaterialTheme.typography.labelSmall,maxLines=1)
                }
            }}
        }
    }
}
@Composable internal fun MiningPanel(p:WorldPet,onChange:(WorldPet)->Unit){
    val a=p.adventure;val n=MiningRules.node(a,a.pos)
    Text("Копалини · ${MiningRules.sites(a.stage).keys.count{AdventureRules.id(it) in a.cleared}}/${MiningRules.sites(a.stage).size} видобуто",style=MaterialTheme.typography.labelMedium)
    if(n!=null){
        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(n.title,color=materialColor(n.material),style=MaterialTheme.typography.titleSmall)
                Text("+${n.amount} ${n.material.code} · −${n.energy}% енергії",style=MaterialTheme.typography.bodySmall)
                val reason=MiningRules.reason(p)
                Button(onClick={onChange(MiningRules.harvest(p))},enabled=reason==null,modifier=Modifier.fillMaxWidth()){Text("Видобути")}
                if(reason!=null)Text(reason,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
    if(p.materials.isNotEmpty())MaterialStock(p)
}
@Composable internal fun CraftWorkshop(p:WorldPet,onChange:(WorldPet)->Unit){
    MaterialStock(p)
    Text("Крафт · рецепти",style=MaterialTheme.typography.titleMedium)
    CraftRecipe.entries.forEach{r->
        HorizontalDivider()
        val reason=CraftRules.reason(p,r)
        Text(r.title,style=MaterialTheme.typography.titleSmall)
        Text(r.cost.entries.joinToString(" · "){(k,n)->k.code+" "+p.material(k)+"/"+n},style=MaterialTheme.typography.bodySmall)
        if(reason!=null)Text(reason,style=MaterialTheme.typography.labelSmall)
        Button(onClick={onChange(CraftRules.craft(p,r))},enabled=reason==null,modifier=Modifier.fillMaxWidth()){Text("Створити")}
    }
}
