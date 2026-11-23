package com.pixubi.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.min

private val palette = listOf(
    Color(0xFF7EDBC0), Color(0xFFFFAF7A), Color(0xFFB3A1FF),
    Color(0xFFFF8FBE), Color(0xFF83C8FF), Color(0xFFFFD879)
)
private val night = Color(0xFF101A2E)
private val navy = Color(0xFF19253C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val storage = remember { PetStorage(applicationContext) }
            var pet by remember { mutableStateOf(storage.load()?.let { GameEngine.advance(it, System.currentTimeMillis()) }) }
            var showGame by remember { mutableStateOf(false) }
            fun store(next: Pet) { pet = next; storage.save(next) }
            val latestPet by rememberUpdatedState(pet)
            fun refresh() {
                val current = latestPet ?: return
                val next = GameEngine.advance(current, System.currentTimeMillis())
                if (next != current) store(next)
            }
            val lifecycle = LocalLifecycleOwner.current
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) refresh()
                }
                lifecycle.lifecycle.addObserver(observer)
                onDispose { lifecycle.lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(pet?.mode, pet?.alive) {
                while (true) {
                    delay(30_000L)
                    refresh()
                }
            }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF98E6CD), secondary = Color(0xFFFFD58A),
                background = night, surface = navy, onBackground = Color.White,
                onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = night) {
                    val current = pet
                    when {
                        current == null -> HatchScreen { store(it) }
                        !current.alive -> FarewellScreen(current) {
                            storage.erase(); pet = null; showGame = false
                        }
                        showGame -> CatchStarsScreen(
                            onFinish = {
                                store(GameEngine.act(current, Action.PLAY_WIN, System.currentTimeMillis()))
                                showGame = false
                            },
                            onBack = { showGame = false }
                        )
                        else -> HomeScreen(
                            pet = current,
                            onAction = { action -> store(GameEngine.act(current, action, System.currentTimeMillis())) },
                            onPlay = { showGame = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HatchScreen(onHatch: (Pet) -> Unit) {
    var species by remember { mutableStateOf(Species.LISYK) }
    var sex by remember { mutableStateOf(Sex.GIRL) }
    var region by remember { mutableStateOf(Region.FOREST) }
    var petName by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("PIXUBI 🐣", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("Знайди свого першого цифрового друга", color = Color(0xFFC5D2E9))
        Text("1. Обери різновид", style = MaterialTheme.typography.titleMedium)
        Species.entries.forEach { choice ->
            val selected = species == choice
            Card(
                onClick = { species = choice; region = choice.native },
                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF36515A) else navy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PetSprite(choice, 0, Modifier.size(63.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(choice.title, fontWeight = FontWeight.Bold)
                        Text(choice.personality, style = MaterialTheme.typography.bodySmall)
                        Text("Рідний дім: ${choice.native.title}", color = Color(0xFFB5D2E8), style = MaterialTheme.typography.labelSmall)
                    }
                    if (selected) Text("✓", fontSize = 26.sp, color = Color(0xFF98E6CD))
                }
            }
        }
        Text("2. Хлопчик чи дівчинка?", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { option ->
                FilterChip(selected = sex == option, onClick = { sex = option }, label = { Text(option.title) })
            }
        }
        Text("3. Де житиме твій Піксубі?", style = MaterialTheme.typography.titleMedium)
        Text("Можна обрати будь-який регіон. У кожного різновиду є рідний край.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Region.entries.forEach { option ->
            FilterChip(
                selected = region == option,
                onClick = { region = option },
                label = { Text("${option.icon} ${option.title}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text("4. Дай ім'я", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = petName, onValueChange = { petName = it.take(24) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Ім'я (необов'язково)") },
            singleLine = true
        )
        Text("🎲 Забарвлення при народженні визначиться випадково. Його не можна обрати вручну.", color = Color(0xFFD4DBE8), style = MaterialTheme.typography.bodySmall)
        Text("⚠️ Піксубі може померти без їжі та води. Коли йдеш надовго — обов’язково вмикай сплячку, вона зупиняє час.", color = Color(0xFFFFD58A), style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { onHatch(GameEngine.hatch(species, sex, region, petName, System.currentTimeMillis())) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("🥚 Вилупити Піксубі") }
        Text("Без інтернету • Без реклами • Збереження на телефоні", modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HomeScreen(pet: Pet, onAction: (Action) -> Unit, onPlay: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${pet.species.title} • ${pet.sex.title} • ${pet.daysOld}-й день життя", color = Color(0xFFBFD0E7))
            }
            Text(if (pet.mode == Mode.HIBERNATION) "❄️" else "💜", fontSize = 30.sp)
        }
        WorldScene(pet)
        Card(colors = CardDefaults.cardColors(containerColor = navy)) {
            Text(pet.message, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
        }
        if (pet.hunger < 25.0 || pet.thirst < 25.0) {
            Text("⚠️ Піксубі потребує турботи. Якщо довго не вистачає їжі або води, він може загинути.",
                color = Color(0xFFFFD28B), style = MaterialTheme.typography.bodySmall)
        }
        Text("Потреби", style = MaterialTheme.typography.titleLarge)
        StatLine("🍓 Їжа", pet.hunger)
        StatLine("💧 Вода", pet.thirst)
        StatLine("💤 Енергія", pet.energy)
        StatLine("💖 Настрій", pet.happiness)
        when (pet.mode) {
            Mode.HIBERNATION -> {
                Text("❄️ Сплячка: час, вік і всі потреби поставлені на паузу.", color = Color(0xFFACD9F9))
                Button(onClick = { onAction(Action.WAKE) }, modifier = Modifier.fillMaxWidth()) { Text("☀️ Розбудити Піксубі") }
            }
            Mode.SLEEP -> {
                Text("🌙 Звичайний сон відновлює сили, але спрага й голод зростають.", color = Color(0xFFACD9F9))
                Button(onClick = { onAction(Action.WAKE) }, modifier = Modifier.fillMaxWidth()) { Text("☀️ Розбудити") }
                OutlinedButton(onClick = { onAction(Action.HIBERNATE) }, modifier = Modifier.fillMaxWidth()) { Text("❄️ Перевести в сплячку") }
            }
            Mode.AWAKE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onAction(Action.FEED) }, modifier = Modifier.weight(1f)) { Text("🍓 Годувати") }
                    Button(onClick = { onAction(Action.WATER) }, modifier = Modifier.weight(1f)) { Text("💧 Напоїти") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onAction(Action.SLEEP) }, modifier = Modifier.weight(1f)) { Text("🌙 Сон") }
                    OutlinedButton(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("✨ Мінігра") }
                }
                OutlinedButton(onClick = { onAction(Action.HIBERNATE) }, modifier = Modifier.fillMaxWidth()) { Text("❄️ Сплячка: я йду у справах") }
            }
        }
        HorizontalDivider(color = Color(0xFF35455D))
        Text("🌍 Регіон: ${pet.region.title}")
        Text("🏡 Рідний край: ${pet.species.native.title}  •  🎲 Колір №${pet.colorIndex + 1}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text("🧬 Приховані гени кольору збережені для майбутньої системи розмноження.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text("🏆 Мініігор виграно: ${pet.miniGameWins}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatLine(name: String, value: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${value.toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { (value / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(6.dp)),
            color = if (value < 25.0) Color(0xFFF4A1A1) else Color(0xFF9DEAD1),
            trackColor = Color(0xFF39485F)
        )
    }
}

@Composable
private fun WorldScene(pet: Pet) {
    val colors = when (pet.region) {
        Region.FOREST -> Color(0xFF4D957B) to Color(0xFF277858)
        Region.MOUNTAINS -> Color(0xFF7088B3) to Color(0xFF637186)
        Region.LAKES -> Color(0xFF67C5DA) to Color(0xFF397EB3)
        Region.DESERT -> Color(0xFFEFBF79) to Color(0xFFBA8255)
        Region.SKY -> Color(0xFF9BC9F8) to Color(0xFF9CA7DC)
    }
    val animation = rememberInfiniteTransition(label = "breath")
    val bob by animation.animateFloat(-4f, 4f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "bob")
    Box(Modifier.fillMaxWidth().height(245.dp).clip(RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.first)
            drawRect(colors.second, Offset(0f, size.height * .74f), Size(size.width, size.height * .26f))
            drawCircle(Color(0xFFFFF3B0), radius = size.minDimension * .12f,
                center = Offset(size.width * .83f, size.height * .20f))
            // Colorful pixels as simple original background decoration.
            for (i in 0..8) {
                val x = size.width * (i / 9f) + 5f
                val y = size.height * (0.78f + (i % 3) * .045f)
                drawRect(Color.White.copy(alpha = .2f), Offset(x, y), Size(18f, 12f))
            }
            if (pet.region == Region.FOREST) {
                for (i in 0..3) {
                    val x = size.width * (.05f + i * .26f)
                    drawRect(Color(0xFF73533E), Offset(x, size.height * .51f), Size(13f, size.height * .24f))
                    drawCircle(Color(0xFF2D6B4E), size.width * .095f, Offset(x + 6f, size.height * .50f))
                }
            }
            if (pet.region == Region.SKY) {
                for (i in 0..3) drawCircle(Color.White.copy(alpha = .65f),
                    size.width * .08f, Offset(size.width * (.1f + i * .25f), size.height * .7f))
            }
        }
        Text("${pet.region.icon} ${pet.region.title}", modifier = Modifier.align(Alignment.TopStart).padding(12.dp), color = Color.White)
        PetSprite(
            pet.species, pet.colorIndex,
            modifier = Modifier.align(Alignment.Center).offset(y = if (pet.mode == Mode.AWAKE) bob.dp else 0.dp).size(150.dp)
        )
        if (pet.mode != Mode.AWAKE) {
            Text(if (pet.mode == Mode.HIBERNATION) "❄️ ПАУЗА" else "💤 Zzz",
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color.White,
                fontWeight = FontWeight.Bold)
        }
    }
}

/** Pixel patterns are original lightweight placeholders; every species has a distinct silhouette. */
@Composable
private fun PetSprite(species: Species, coatIndex: Int, modifier: Modifier = Modifier) {
    val pattern = when (species) {
        Species.LISYK -> listOf(
            "..DD.......DD..", "..DAA.....AAD..", "..DAAA...AAAD..", "...DAAAAAAAD...",
            "...DAAAAAAAD...", "..DAAAAAAAAAD..", "..DAEAAAAEAAD..", "..DAAAAAAAAAD..",
            "..DAAAANAAAAD..", "...DAAAAAAAD...", "....DAAAAAD....", "....DAAAAAD....",
            "...DDAAADDAAD..", "...DDDDDDDDDD..."
        )
        Species.ISKRYK -> listOf(
            ".......DD......", "......DAAD.....", ".....DAAAD.....", "..DDDAAAADD....",
            "..DAAAAAAAD....", "...DAAAAAAAD...", "...DAEAAEAAD...", "..DAAAAAAAAD...",
            "..DAAAANAAAD...", "...DAAAAAAD....", "....DAAAAD.....", "...DDAAAADD....",
            "...DD.DD.DD....", "....D....D....."
        )
        Species.KRAPLYK -> listOf(
            "...............", "..DD.......DD..", ".DAAD.....DAAD.", ".DAAADDDDAAAAD.",
            "..DAAAAAAAAAD..", "..DAAAAAAAAAD..", "..DAEAAAAEAAD..", "..DAAAAAAAAAD..",
            "..DAAAANAAAAD..", "...DAAAAAAAD...", "....DAAAAAD....", "...DDAAAADD....",
            "..DD.DDDDD.DD..", "..............."
        )
        Species.PISHCHANYK -> listOf(
            ".DDD.......DDD.", ".DAAD.....DAAD.", ".DAAAD...DAAAD.", "..DAAAD.DAAAD..",
            "...DAAAAAAAD...", "..DAAAAAAAAAD..", "..DAEAAAAEAAD..", "..DAAAAAAAAAD..",
            "..DAAAANAAAAD..", "...DAAAAAAAD...", "....DAAAAAD....", "...DDAAAADD....",
            "...DD.DD.DD....", "..............."
        )
        Species.KHMARYK -> listOf(
            "...............", "....DD...DD....", "...DAAD.DAAD...", "..DAAAAAAAAAD..",
            ".DAAAAAAAAAAAD.", ".DAAAAAAAAAAAD.", ".DAAEAAAAEAAD..", ".DAAAAAAAAAAAD.",
            "..DAAAANAAAAD..", "..DAAAAAAAAAD..", "...DAAAAAAAD...", "....DAAAAD.....",
            "...DDAAAADD....", "...DD....DD...."
        )
    }
    val body = palette[coatIndex.coerceIn(0, 5)]
    val shadow = Color(0xFF26334B)
    Canvas(modifier) {
        val w = pattern.maxOf { it.length }
        val unit = min(size.width / w, size.height / pattern.size)
        val originX = (size.width - unit * w) / 2f
        val originY = (size.height - unit * pattern.size) / 2f
        pattern.forEachIndexed { y, row ->
            row.forEachIndexed { x, character ->
                val color = when (character) {
                    'A' -> body
                    'D' -> shadow
                    'E' -> Color.White
                    'N' -> Color(0xFFFF668D)
                    else -> null
                }
                if (color != null) drawRect(color, Offset(originX + x * unit, originY + y * unit), Size(unit + .2f, unit + .2f))
            }
        }
    }
}

@Composable
private fun CatchStarsScreen(onFinish: () -> Unit, onBack: () -> Unit) {
    var caught by remember { mutableIntStateOf(0) }
    val places = listOf(10.dp to 15.dp, 164.dp to 83.dp, 68.dp to 155.dp, 204.dp to 27.dp, 119.dp to 134.dp)
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("✨ Полювання на іскорки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Торкнися 5 іскорок! За перемогу Піксубі отримає радість.")
        Text("Спіймано: $caught / 5", color = Color(0xFFFFD58A), fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF283C63))) {
            for (i in 0..14) {
                Text("·", Modifier.align(Alignment.TopStart).offset(x = ((i * 47) % 270).dp, y = ((i * 39) % 200).dp), color = Color.White.copy(alpha = .6f))
            }
            val (x, y) = places[caught.coerceIn(0, 4)]
            Box(
                Modifier.align(Alignment.TopStart).offset(x = x, y = y).size(57.dp)
                    .clip(CircleShape).background(Color(0xFF5276A4))
                    .clickable {
                        if (caught >= 4) onFinish() else caught += 1
                    },
                contentAlignment = Alignment.Center
            ) { Text("✨", fontSize = 32.sp) }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Повернутися додому") }
    }
}

@Composable
private fun FarewellScreen(pet: Pet, onNewPet: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🕯️", fontSize = 74.sp)
        Spacer(Modifier.height(12.dp))
        Text("Пам'яті ${pet.name}", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("${pet.species.title} • ${pet.daysOld} днів активного життя", textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(pet.deathReason, color = Color.LightGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text("Ваші пригоди залишаться теплим спогадом 💜", textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        Button(onClick = onNewPet, modifier = Modifier.fillMaxWidth()) { Text("🥚 Виростити нового Піксубі") }
        Text("У версії 0.1 можна доглядати лише одного вихованця; новий замінить попереднє збереження.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.LightGray)
    }
}
