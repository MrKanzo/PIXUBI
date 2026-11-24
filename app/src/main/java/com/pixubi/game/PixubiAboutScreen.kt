package com.pixubi.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val loreMint=Color(0xFF98E6CD)
private val loreMuted=Color(0xFFCCD9ED)
private val loreGold=Color(0xFFFFD58A)
private val loreCard=Color(0xFF22344F)

@Composable
internal fun PixubiAboutScreen(version:String,onBack:()->Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PIXUBI",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black,color=loreMint)
                Text("Версія $version",style=MaterialTheme.typography.labelMedium,color=loreMuted)
            }
            OutlinedButton(onClick=onBack){Text("← Назад")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("🌍 Світ","🐾 Види","💜 Автори").forEachIndexed{i,title->
                FilterChip(selected=section==i,onClick={section=i},label={Text(title)},modifier=Modifier.weight(1f))
            }
        }
        key(section) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                when(section) {
                    0 -> {
                        LorePanel(PixubiLore.tagline,PixubiLore.description)
                        LorePanel("✨ Легенда про Велику Тишу",PixubiLore.origin)
                        LorePanel("💚 Життя твого Піксу",PixubiLore.care)
                        LorePanel("🔭 Майбутнє світу",PixubiLore.future)
                    }
                    1 -> Species.entries.forEach{s->
                        val story=PixubiLore.story(s)
                        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
                            colors=CardDefaults.cardColors(containerColor=loreCard)) {
                            Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    IllustratedYoungPixu(s,Sex.BOY,0,GrowthStage.BABY,
                                        modifier=Modifier.size(86.dp))
                                    Column(Modifier.weight(1f).padding(start=8.dp)){
                                        Text(s.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=loreMint)
                                        Text(story.epithet,color=loreGold,style=MaterialTheme.typography.bodySmall)
                                        Text(s.native.icon+" "+s.native.title,color=loreMuted,style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text("Характер",fontWeight=FontWeight.Bold)
                                Text(story.personality,color=loreMuted)
                                Text("Що вміє у грі",fontWeight=FontWeight.Bold)
                                Text(story.talent,color=loreMuted)
                                HorizontalDivider(color=Color(0xFF41556D))
                                Text("Легенда походження",fontWeight=FontWeight.Bold,color=loreGold)
                                Text(story.legend,color=loreMuted)
                                Text(story.detail,color=loreMuted,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    else -> {
                        LorePanel("🎬 Творці PIXUBI","Незалежний ігровий проєкт. Ідея, творчий напрям і перевірка гри — від людини; спільна робота над кодом, текстами та графікою — за допомогою ШІ.")
                        LorePanel(PixubiLore.creator,PixubiLore.creatorRole)
                        LorePanel(PixubiLore.assistant,PixubiLore.assistantRole)
                        LorePanel("🎨 Художній напрям","Кібернетичний піксель-арт, дизайн світу та персонажів — спільна концепція Миколи й Аліси. Ілюстрації створені за допомогою генеративного ШІ та відібрані під час розробки.")
                        LorePanel("🛠 Технології","Android • Kotlin • Jetpack Compose. Власні локальні ігрові системи, синтезовані звуки й покадрові художні ресурси.")
                        LorePanel("Особлива подяка",PixubiLore.dedication)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
@Composable
private fun LorePanel(title:String,body:String) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=loreCard)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=loreMint)
            Text(body,color=loreMuted,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
