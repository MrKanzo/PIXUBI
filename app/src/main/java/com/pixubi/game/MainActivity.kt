package com.pixubi.game

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
private enum class GamePage { HUB, STARS, QUIZ, RESULTS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val storage = remember { PetStorage(applicationContext) }
            val audioStore = remember { SoundSettingsStore(applicationContext) }
            val audio = remember { PixubiAudio(applicationContext) }
            var soundSettings by remember { mutableStateOf(audioStore.load()) }
            var showSettings by remember { mutableStateOf(false) }
            var auditionRegion by remember { mutableStateOf<Region?>(null) }
            var pet by remember { mutableStateOf(storage.load()?.let { GameEngine.advance(it, System.currentTimeMillis()) }) }
            val progressStore = remember { QuizProgressStore(applicationContext) }
            var quizProgress by remember { mutableStateOf(progressStore.load()) }
            var gamePage by remember { mutableStateOf<GamePage?>(null) }
            var quizTopic by remember { mutableStateOf(QuizTopic.LOGIC) }
            var quizAttempt by remember { mutableIntStateOf(0) }
            var quizScore by remember { mutableIntStateOf(0) }
            fun openUpdates() {
                val url = "https://github.com/MrKanzo/PIXUBI/actions"
                runCatching {
                    this@MainActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    Toast.makeText(applicationContext, "Не вдалося відкрити браузер", Toast.LENGTH_LONG).show()
                }
            }
            LaunchedEffect(soundSettings, pet?.region, auditionRegion) {
                audio.configure(soundSettings, auditionRegion ?: pet?.region ?: Region.FOREST)
            }
            fun saveSound(next: SoundSettings) {
                soundSettings = next
                audioStore.save(next)
                audio.configure(next, auditionRegion ?: pet?.region ?: Region.FOREST)
            }
            fun goSettings() {
                auditionRegion = null
                showSettings = true
                audio.effect(PixubiSound.TAP)
            }
            fun closeSettings() {
                auditionRegion = null
                showSettings = false
            }
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
                audio.setForeground(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            audio.setForeground(true)
                            refresh()
                        }
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP ->
                            audio.setForeground(false)
                        else -> Unit
                    }
                }
                lifecycle.lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.lifecycle.removeObserver(observer)
                    audio.close()
                }
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
                        showSettings -> PixubiSettingsScreen(
                            settings = soundSettings,
                            currentRegion = current?.region ?: Region.FOREST,
                            previewRegion = auditionRegion,
                            onSettingsChange = { saveSound(it) },
                            onPreviewRegion = {
                                auditionRegion = it
                                audio.configure(soundSettings, it)
                                audio.effect(PixubiSound.STAR)
                            },
                            onTestSound = { audio.effect(PixubiSound.HAPPY) },
                            onCheckUpdates = { openUpdates() },
                            onBack = { closeSettings() }
                        )
                        current == null -> HatchScreen(
                            onHatch = {
                                store(it)
                                audio.effect(PixubiSound.HAPPY)
                            },
                            onSettings = { goSettings() }
                        )
                        !current.alive -> FarewellScreen(current) {
                            storage.erase()
                            pet = null
                            gamePage = null
                        }
                        gamePage == GamePage.HUB -> GamesHubScreen(
                            petName = current.name,
                            progress = quizProgress,
                            onStars = { gamePage = GamePage.STARS },
                            onQuiz = { topic ->
                                quizTopic = topic
                                quizAttempt++
                                gamePage = GamePage.QUIZ
                                audio.effect(PixubiSound.TAP)
                            },
                            onBack = { gamePage = null }
                        )
                        gamePage == GamePage.STARS -> CatchStarsScreen(
                            onFinish = {
                                store(GameEngine.act(current, Action.PLAY_WIN, System.currentTimeMillis()))
                                gamePage = GamePage.HUB
                                react(PetEmotion.HAPPY)
                                audio.effect(PixubiSound.HAPPY)
                            },
                            onBack = { gamePage = GamePage.HUB },
                            onStar = { audio.effect(PixubiSound.STAR) }
                        )
                        gamePage == GamePage.QUIZ -> QuizGameScreen(
                            topic = quizTopic,
                            attempt = quizAttempt,
                            onAnswer = { correct ->
                                audio.effect(if (correct) PixubiSound.QUIZ_CORRECT else PixubiSound.QUIZ_WRONG)
                            },
                            onComplete = { score, total ->
                                quizScore = score
                                quizProgress = progressStore.record(quizTopic, score, total)
                                if (score >= 3 && total == 5) {
                                    store(GameEngine.act(current, Action.PLAY_WIN, System.currentTimeMillis()))
                                    react(PetEmotion.HAPPY)
                                    audio.effect(PixubiSound.HAPPY)
                                }
                                gamePage = GamePage.RESULTS
                            },
                            onExit = { gamePage = GamePage.HUB }
                        )
                        gamePage == GamePage.RESULTS -> QuizResultsScreen(
                            topic = quizTopic,
                            score = quizScore,
                            total = 5,
                            onAgain = {
                                quizAttempt++
                                gamePage = GamePage.QUIZ
                            },
                            onHub = { gamePage = GamePage.HUB }
                        )
                        else -> HomeScreen(
                            pet = current,
                            reaction = temporaryEmotion,
                            onAction = { action ->
                                store(GameEngine.act(current, action, System.currentTimeMillis()))
                                audio.effect(when (action) {
                                    Action.FEED -> PixubiSound.FEED
                                    Action.WATER -> PixubiSound.WATER
                                    Action.SLEEP -> PixubiSound.SLEEP
                                    Action.WAKE -> PixubiSound.WAKE
                                    Action.HIBERNATE -> PixubiSound.FREEZE
                                    Action.PLAY_WIN -> PixubiSound.HAPPY
                                })
                                react(when (action) {
                                    Action.FEED -> PetEmotion.EATING
                                    Action.WATER -> PetEmotion.DRINKING
                                    Action.WAKE -> PetEmotion.HAPPY
                                    Action.PLAY_WIN -> PetEmotion.HAPPY
                                    Action.SLEEP, Action.HIBERNATE -> null
                                })
                            },
                            onPetTap = {
                                if (current.mode == Mode.AWAKE) {
                                    react(PetEmotion.SURPRISED)
                                    audio.effect(PixubiSound.TAP)
                                }
                            },
                            onPlay = {
                                gamePage = GamePage.HUB
                                audio.effect(PixubiSound.TAP)
                            },
                            onSettings = { goSettings() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HatchScreen(onHatch: (Pet) -> Unit, onSettings: () -> Unit) {
    var species by remember { mutableStateOf(Species.LISYK) }
    var sex by remember { mutableStateOf(Sex.GIRL) }
    var region by remember { mutableStateOf(Region.FOREST) }
    var petName by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PIXUBI 🐣", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onSettings) { Text("⚙️") }
        }
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
                    PetSprite(choice, 0, Modifier.size(63.dp), sex = sex)
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
    onPlay: () -> Unit,
    onSettings: () -> Unit
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
            Text(if (pet.mode == Mode.HIBERNATION) "❄️" else "💜", fontSize = 27.sp)
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = onSettings) { Text("⚙️") }
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
                    OutlinedButton(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("🎮 Ігри") }
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
                .size(194.dp)
                .graphicsLayer {
                    rotationZ = if (emotion == PetEmotion.HAPPY) sway * 2.5f else sway
                    scaleY = if (sleeping) 0.93f + bob * .035f else 1f - bob * .025f
                }
                .clickable(enabled = pet.mode == Mode.AWAKE) { onPetTap() },
            emotion = emotion, blink = eyeBlink, chewOpen = bob > .45f, sex = pet.sex
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

/** PIXUBI v0.4 draws the ten original cyber pets from a bundled sprite atlas. */
@Composable
private fun PetSprite(
    species: Species,
    coatIndex: Int,
    modifier: Modifier = Modifier,
    emotion: PetEmotion = PetEmotion.IDLE,
    blink: Boolean = false,
    chewOpen: Boolean = false,
    sex: Sex = Sex.BOY
) {
    PixelPetSprite(
        species = species, sex = sex, coatIndex = coatIndex,
        modifier = modifier, emotion = emotion, blink = blink, chewOpen = chewOpen
    )
}

@Composable
private fun CatchStarsScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onStar: () -> Unit
) {
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
                        onStar()
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
        Text("У версії 0.5 можна доглядати лише одного вихованця; новий замінить попереднє збереження.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.LightGray)
    }
}
