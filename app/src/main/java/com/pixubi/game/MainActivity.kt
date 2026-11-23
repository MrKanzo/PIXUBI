package com.pixubi.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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
            var temporaryEmotion by remember { mutableStateOf<PetEmotion?>(null) }
            var reactionVersion by remember { mutableIntStateOf(0) }
            fun react(emotion: PetEmotion?) { temporaryEmotion = emotion; reactionVersion++ }
            LaunchedEffect(reactionVersion) {
                if (temporaryEmotion != null) {
                    delay(1800L)
                    if (temporaryEmotion == PetEmotion.EATING || temporaryEmotion == PetEmotion.DRINKING) {
                        temporaryEmotion = PetEmotion.HAPPY
                        delay(1400L)
                    }
                    temporaryEmotion = null
                }
            }
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
                                react(PetEmotion.HAPPY)
                            },
                            onBack = { showGame = false }
                        )
                        else -> HomeScreen(
                            pet = current,
                            reaction = temporaryEmotion,
                            onAction = { action ->
                                store(GameEngine.act(current, action, System.currentTimeMillis()))
                                react(when (action) {
                                    Action.FEED -> PetEmotion.EATING
                                    Action.WATER -> PetEmotion.DRINKING
                                    Action.WAKE -> PetEmotion.HAPPY
                                    Action.PLAY_WIN -> PetEmotion.HAPPY
                                    Action.SLEEP, Action.HIBERNATE -> null
                                })
                            },
                            onPetTap = { if (current.mode == Mode.AWAKE) react(PetEmotion.SURPRISED) },
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
private fun HomeScreen(
    pet: Pet,
    reaction: PetEmotion?,
    onAction: (Action) -> Unit,
    onPetTap: () -> Unit,
    onPlay: () -> Unit
) {
    val emotion = EmotionResolver.resolve(pet, reaction)
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
        WorldScene(pet, reaction, onPetTap)
        Card(colors = CardDefaults.cardColors(containerColor = navy)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💬 " + EmotionResolver.label(emotion), color = Color(0xFFFFD58A), fontWeight = FontWeight.Bold)
                Text(EmotionResolver.caption(pet, emotion), style = MaterialTheme.typography.bodyMedium)
            }
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
private fun WorldScene(pet: Pet, reaction: PetEmotion?, onPetTap: () -> Unit) {
    val emotion = EmotionResolver.resolve(pet, reaction)
    val landscape = when (pet.region) {
        Region.FOREST -> Color(0xFF61BA9D) to Color(0xFF245C56)
        Region.MOUNTAINS -> Color(0xFF7997CD) to Color(0xFF394E78)
        Region.LAKES -> Color(0xFF79CEEA) to Color(0xFF315E9A)
        Region.DESERT -> Color(0xFFFFD294) to Color(0xFFB96F59)
        Region.SKY -> Color(0xFFA2C8FF) to Color(0xFF7476BF)
    }
    val loop = rememberInfiniteTransition(label = "pixubi-life")
    val bob by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse), label = "bob"
    )
    val blinkCycle by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3300, easing = LinearEasing)), label = "blink"
    )
    val sway by loop.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "sway"
    )
    val sleeping = emotion == PetEmotion.SLEEPING || emotion == PetEmotion.HIBERNATING
    val eyeBlink = !sleeping && blinkCycle > 0.93f && blinkCycle < 0.985f
    val travelY = when (emotion) {
        PetEmotion.HAPPY -> -12f * bob
        PetEmotion.EATING, PetEmotion.DRINKING -> 3f * bob
        PetEmotion.SLEEPING, PetEmotion.HIBERNATING -> 4f * bob
        PetEmotion.SAD, PetEmotion.TIRED -> 3f
        else -> -4f * bob
    }
    val speech = EmotionResolver.bubble(pet, emotion)
    val frame = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().height(286.dp).clip(frame)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(landscape.first, landscape.second)))
            val sunColor = if (sleeping) Color(0xFFE8EDFF) else Color(0xFFFFE6A1)
            drawCircle(sunColor.copy(alpha = .88f), size.minDimension * .105f,
                Offset(size.width * .83f, size.height * .18f))
            drawCircle(Color.White.copy(alpha = .12f), size.minDimension * .33f,
                Offset(size.width * .5f, size.height * .62f))
            when (pet.region) {
                Region.FOREST -> {
                    drawRect(Color(0xFF28654E), Offset(0f, size.height * .76f),
                        Size(size.width, size.height * .24f))
                    for (i in 0..4) {
                        val x = size.width * (.04f + i * .23f)
                        drawRect(Color(0xFF624735), Offset(x, size.height * .47f),
                            Size(13f, size.height * .30f))
                        drawCircle(Color(0xFF22644B), size.width * .10f,
                            Offset(x + 7f, size.height * .48f))
                        drawCircle(Color(0xFF3F9872), size.width * .065f,
                            Offset(x - 10f, size.height * .43f))
                    }
                }
                Region.MOUNTAINS -> {
                    for (i in 0..2) {
                        val left = size.width * (i * .39f - .10f)
                        val peak = Path().apply {
                            moveTo(left, size.height * .79f)
                            lineTo(left + size.width * .26f, size.height * (.23f + i * .06f))
                            lineTo(left + size.width * .59f, size.height * .79f)
                            close()
                        }
                        drawPath(peak, Color(0xFF475B7E).copy(alpha = .7f))
                    }
                    drawRect(Color(0xFF52688E), Offset(0f, size.height * .78f),
                        Size(size.width, size.height * .22f))
                }
                Region.LAKES -> {
                    drawRect(Color(0xFF3C95B9), Offset(0f, size.height * .74f),
                        Size(size.width, size.height * .26f))
                    for (i in 0..5) {
                        val x = size.width * (i + .3f) / 6f
                        drawRect(Color(0xFFBDEEF3).copy(alpha = .55f),
                            Offset(x, size.height * (.81f + (i % 2) * .07f)),
                            Size(size.width * .085f, 4f))
                    }
                }
                Region.DESERT -> {
                    drawOval(Color(0xFFF9C781), Offset(-size.width * .15f, size.height * .68f),
                        Size(size.width * .85f, size.height * .45f))
                    drawOval(Color(0xFFDC9D65), Offset(size.width * .42f, size.height * .72f),
                        Size(size.width * .85f, size.height * .38f))
                }
                Region.SKY -> {
                    for (i in 0..4) {
                        val x = size.width * (i * .26f - .06f)
                        val y = size.height * (.70f + (i % 2) * .10f)
                        drawCircle(Color.White.copy(alpha = .70f), size.width * .085f, Offset(x,y))
                        drawCircle(Color.White.copy(alpha = .62f), size.width * .07f,
                            Offset(x + size.width * .09f,y))
                    }
                }
            }
            if (sleeping) drawRect(Color(0xFF101A37).copy(alpha = .24f))
            for (i in 0..12) {
                val x = size.width * ((i * 37 + 17) % 97) / 97f
                val y = size.height * ((i * 29 + 11) % 73) / 100f
                drawRect(Color.White.copy(alpha = .22f), Offset(x, y), Size(4f, 4f))
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = Color(0xFF15253B).copy(alpha = .7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(pet.region.icon + " " + pet.region.title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Box(
            Modifier.align(Alignment.BottomCenter).offset(y = (-35).dp)
                .size(width = 112.dp, height = 16.dp)
                .clip(CircleShape).background(Color(0xFF142237).copy(alpha = .25f))
        )
        PetSprite(
            pet.species, pet.colorIndex,
            modifier = Modifier.align(Alignment.Center)
                .offset(y = (17f + travelY).dp)
                .size(175.dp)
                .graphicsLayer {
                    rotationZ = if (emotion == PetEmotion.HAPPY) sway * 2.5f else sway
                    scaleY = if (sleeping) 0.93f + bob * .035f else 1f - bob * .025f
                }
                .clickable(enabled = pet.mode == Mode.AWAKE) { onPetTap() },
            emotion = emotion, blink = eyeBlink, chewOpen = bob > .45f
        )
        if (speech != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 49.dp)
                    .offset(y = (-5f * bob).dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFDF9F1),
                shadowElevation = 5.dp
            ) {
                Text(speech, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color(0xFF253149), fontWeight = FontWeight.Bold)
            }
        }
        Text(
            if (sleeping) "Тихенько, відпочиваю…" else "Торкнись мене ✨",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = .90f)
        )
    }
}

/** Pixel patterns are original lightweight placeholders; every species has a distinct silhouette. */
@Composable
private fun PetSprite(
    species: Species,
    coatIndex: Int,
    modifier: Modifier = Modifier,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    chewOpen: Boolean = false
) {
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
    val shadow = Color(0xFF25324A)
    val blush = Color(0xFFFF7EA8)
    Canvas(modifier) {
        val w = pattern.maxOf { it.length }
        val unit = min(size.width / w, size.height / pattern.size)
        val originX = (size.width - unit * w) / 2f
        val originY = (size.height - unit * pattern.size) / 2f
        val eyes = mutableListOf<Pair<Int, Int>>()
        var mouth = 7 to 8
        fun cell(x: Float, y: Float, width: Float, height: Float, color: Color) {
            drawRect(color, Offset(originX + x * unit, originY + y * unit),
                Size(width * unit, height * unit))
        }
        fun stroke(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = shadow) {
            drawLine(color, Offset(originX + x1 * unit, originY + y1 * unit),
                Offset(originX + x2 * unit, originY + y2 * unit),
                strokeWidth = unit * .24f)
        }
        pattern.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = when (ch) {
                    'A', 'E', 'N' -> body
                    'D' -> shadow
                    else -> null
                }
                if (color != null) cell(x.toFloat(), y.toFloat(), 1.02f, 1.02f, color)
                if (ch == 'E') eyes.add(x to y)
                if (ch == 'N') mouth = x to y
            }
        }
        val closed = blink || emotion == PetEmotion.SLEEPING || emotion == PetEmotion.HIBERNATING
        eyes.forEach { (x, y) ->
            val fx = x.toFloat()
            val fy = y.toFloat()
            when {
                closed -> stroke(fx + .04f, fy + .53f, fx + .94f, fy + .48f)
                emotion == PetEmotion.HAPPY -> {
                    stroke(fx + .05f, fy + .72f, fx + .50f, fy + .20f)
                    stroke(fx + .50f, fy + .20f, fx + .97f, fy + .72f)
                }
                emotion == PetEmotion.SAD || emotion == PetEmotion.TIRED -> {
                    cell(fx + .07f, fy + .23f, .83f, .68f, Color.White)
                    cell(fx + .37f, fy + .48f, .35f, .38f, shadow)
                    stroke(fx, fy + .09f, fx + .85f, fy + .28f)
                }
                else -> {
                    cell(fx + .05f, fy + .02f, .88f, .94f, Color.White)
                    cell(fx + .35f, fy + .35f, .40f, .60f, shadow)
                    cell(fx + .38f, fy + .40f, .13f, .15f, Color.White)
                }
            }
        }
        val (mx, my) = mouth
        val x = mx.toFloat()
        val y = my.toFloat()
        when (emotion) {
            PetEmotion.SAD, PetEmotion.TIRED -> {
                stroke(x - .48f, y + .70f, x, y + .32f)
                stroke(x, y + .32f, x + .48f, y + .70f)
                if (emotion == PetEmotion.SAD && eyes.isNotEmpty()) {
                    val (ex, ey) = eyes.last()
                    cell(ex + .86f, ey + 1.12f, .27f, .55f, Color(0xFF77DAFF))
                }
            }
            PetEmotion.SLEEPING, PetEmotion.HIBERNATING -> {
                stroke(x - .35f, y + .44f, x + .34f, y + .44f)
            }
            PetEmotion.EATING, PetEmotion.DRINKING -> {
                if (chewOpen) {
                    drawCircle(shadow, unit * .45f,
                        Offset(originX + (x + .2f) * unit, originY + (y + .45f) * unit))
                    cell(x - .04f, y + .53f, .42f, .21f, blush)
                } else stroke(x - .3f, y + .38f, x + .42f, y + .40f)
            }
            PetEmotion.SURPRISED -> {
                drawCircle(shadow, unit * .52f,
                    Offset(originX + (x + .15f) * unit, originY + (y + .5f) * unit))
                drawCircle(blush, unit * .20f,
                    Offset(originX + (x + .13f) * unit, originY + (y + .61f) * unit))
            }
            PetEmotion.HAPPY -> {
                stroke(x - .53f, y + .15f, x, y + .66f)
                stroke(x, y + .66f, x + .58f, y + .15f)
                cell(x - 1.8f, y - 1.45f, .56f, .35f, blush.copy(alpha = .85f))
                cell(x + 1.25f, y - 1.45f, .56f, .35f, blush.copy(alpha = .85f))
            }
            PetEmotion.IDLE -> {
                stroke(x - .34f, y + .40f, x, y + .65f)
                stroke(x, y + .65f, x + .38f, y + .40f)
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
        Text("У версії 0.2 можна доглядати лише одного вихованця; новий замінить попереднє збереження.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.LightGray)
    }
}
