package com.pixubi.game
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
internal fun PixubiAboutScreen(version:String,onBack:()->Unit){
    var tab by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("PIXUBI WORLD",style=MaterialTheme.typography.headlineMedium);Text("Версія $version")}
            OutlinedButton(onClick=onBack){Text("Назад")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("Світ","Серії","Автори").forEachIndexed{i,label->FilterChip(tab==i,{tab=i},label={Text(label)})}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            when(tab){
                0->{WorldInfo("GENESIS","Після Великої Тиші в цифрових руїнах прокинулися автономні механічні та біосинтетичні істоти. Вісім серій утворили власні середовища.")
                    WorldInfo("Життя роботика","Заряджай ядро, відновлюй корпус, синхронізуй настрій, виконуй місії та встановлюй спорядження. Розвиток відкриває п’ять форм від MK-1 до OMEGA.")
                    WorldInfo("Локальний світ","Гра працює офлайн без реклами. Інтернет використовується для перевірки підписаних оновлень. Сплячка зупиняє витрату ресурсів.")}
                1->RobotSeries.entries.forEach{s->val info=WorldCatalog.info(s);WorldInfo(s.title+" · "+info.role,info.description+"\n"+HabitatCatalog.of(s).title+"\n"+info.origin)}
                else->{WorldInfo(PixubiLore.creator,PixubiLore.creatorRole);WorldInfo(PixubiLore.assistant,PixubiLore.assistantRole);WorldInfo("Технології","Kotlin, Jetpack Compose, процедурна графіка, анімації та локальний синтез звуку.");WorldInfo("Подяка",PixubiLore.dedication)}
            }
        }
    }
}
@Composable private fun WorldInfo(title:String,body:String){
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,style=MaterialTheme.typography.titleMedium);Text(body)
    }}
}
