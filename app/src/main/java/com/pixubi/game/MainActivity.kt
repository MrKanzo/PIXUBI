package com.pixubi.game

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
            val notificationStore = remember { PixubiNotificationSettingsStore(applicationContext) }
            var notificationSettings by remember { mutableStateOf(notificationStore.load()) }
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    val updated = notificationSettings.copy(enabled = true)
                    notificationSettings = updated
                    notificationStore.save(updated)
                    PixubiNeedsNotifier.sync(applicationContext, updated)
                } else {
                    Toast.makeText(applicationContext,
                        "Сповіщення заборонено в Android. Можна увімкнути у налаштуваннях телефону.",
                        Toast.LENGTH_LONG).show()
                }
            }
            fun saveNotifications(next: PixubiNotificationSettings) {
                if (next.enabled && !PixubiNeedsNotifier.permissionGranted(applicationContext)) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
                notificationSettings = next
                notificationStore.save(next)
                PixubiNeedsNotifier.sync(applicationContext, next)
            }
            LaunchedEffect(Unit) {
                PixubiNeedsNotifier.createChannel(applicationContext)
                PixubiNeedsNotifier.sync(applicationContext, notificationSettings)
            }
            val audio = remember { PixubiAudio(applicationContext) }
            var soundSettings by remember { mutableStateOf(audioStore.load()) }
            var showSettings by remember { mutableStateOf(false) }
            var auditionRegion by remember { mutableStateOf<Region?>(null) }
            var pet by remember { mutableStateOf(storage.load()?.let { GameEngine.restoreAfterAbsence(it, System.currentTimeMillis()) }) }
            val collectionStore = remember { PlayerCollectionStore(applicationContext) }
            var collection by remember { mutableStateOf(collectionStore.load()) }
            var showProfile by remember { mutableStateOf(false) }
            var showCollection by remember { mutableStateOf(false) }
            val progressStore = remember { QuizProgressStore(applicationContext) }
            var quizProgress by remember { mutableStateOf(progressStore.load()) }
            var gamePage by remember { mutableStateOf<GamePage?>(null) }
            var quizTopic by remember { mutableStateOf(QuizTopic.LOGIC) }
            var quizAttempt by remember { mutableIntStateOf(0) }
            var quizScore by remember { mutableIntStateOf(0) }
            val updater = remember { PixubiUpdater(this@MainActivity) }
            var updateDialog by remember { mutableStateOf<PixubiUpdateResult?>(null) }
            var checkingUpdates by remember { mutableStateOf(false) }
            fun showUpdateToast(message: String) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
            fun openUpdates() {
                if (checkingUpdates) return
                checkingUpdates = true
                updater.check { result ->
                    checkingUpdates = false
                    updateDialog = result
                }
            }
            LaunchedEffect(Unit) {
                updater.checkDaily { release ->
                    updateDialog = PixubiUpdateResult.NewRelease(release)
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
            fun saveCollection(next: PlayerCollection) {
                collection = next
                collectionStore.save(next)
            }
            fun store(next: Pet) {
                pet = next
                storage.save(next)
                collection = collectionStore.observe(next)
            }
            LaunchedEffect(pet?.bornAt) {
                pet?.let { collection = collectionStore.observe(it) }
            }
            BackHandler(enabled = showCollection || showProfile) {
                if (showCollection) showCollection = false else showProfile = false
            }
            val latestPet by rememberUpdatedState(pet)
            fun refresh(offline: Boolean = false) {
                val current = latestPet ?: return
                val now = System.currentTimeMillis()
                val next = if (offline) GameEngine.restoreAfterAbsence(current, now)
                    else GameEngine.advance(current, now)
                if (next != current) store(next)
            }
            val lifecycle = LocalLifecycleOwner.current
            DisposableEffect(lifecycle) {
                audio.setForeground(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            PixubiNeedsNotifier.markVisible(true)
                            PixubiNeedsNotifier.clear(applicationContext)
                            audio.setForeground(true)
                            refresh(offline = true)
                        }
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            PixubiNeedsNotifier.markVisit(applicationContext)
                            PixubiNeedsNotifier.markVisible(false)
                            refresh()
                            audio.setForeground(false)
                        }
                        else -> Unit
                    }
                }
                lifecycle.lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.lifecycle.removeObserver(observer)
                    PixubiNeedsNotifier.markVisible(false)
                    audio.close()
                }
            }
            LaunchedEffect(pet?.mode, pet?.alive) {
                while (true) {
                    delay(30_000L)
                    refresh()
                }
            }
            // Finish a real-world expedition exactly when its saved deadline arrives.
            LaunchedEffect(pet?.expeditionStartedAt) {
                val started = pet?.expeditionStartedAt ?: 0L
                if (started > 0L) {
                    val remaining = (started + GameEngine.EXPEDITION_MINUTES * 60_000L -
                        System.currentTimeMillis()).coerceAtLeast(0L)
                    delay(remaining + 300L)
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
                        showCollection -> PixuCollectionScreen(
                            library = collection,
                            current = current,
                            onBack = { showCollection = false }
                        )
                        showProfile -> PlayerProfileScreen(
                            collection = collection,
                            progress = quizProgress,
                            current = current,
                            onName = { saveCollection(collection.copy(nickname = it)) },
                            onAvatar = { saveCollection(collection.copy(avatar = it)) },
                            onCollection = { showCollection = true },
                            onBack = { showProfile = false }
                        )
                        showSettings -> PixubiSettingsScreen(
                            settings = soundSettings,
                            notificationSettings = notificationSettings,
                            onNotificationSettingsChange = { saveNotifications(it) },
                            onTestNotification = {
                                if (PixubiNeedsNotifier.permissionGranted(applicationContext)) {
                                    PixubiNeedsNotifier.sendTest(applicationContext, current)
                                } else {
                                    Toast.makeText(applicationContext,
                                        "Потрібно дозволити сповіщення PIXUBI в Android.",
                                        Toast.LENGTH_LONG).show()
                                }
                            },
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
                            isStableInstall = updater.isStableInstall,
                            checkingForUpdates = checkingUpdates,
                            onBack = { closeSettings() }
                        )
                        current == null -> HatchScreen(
                            onHatch = {
                                store(it)
                                audio.effect(PixubiSound.HAPPY)
                            },
                            onSettings = { goSettings() },
                            onProfile = { showProfile = true }
                        )
                        !current.alive -> FarewellScreen(current, onProfile = { showProfile = true }) {
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
                            collection = collection,
                            reaction = temporaryEmotion,
                            onAction = { action ->
                                store(GameEngine.act(current, action, System.currentTimeMillis()))
                                audio.effect(when (action) {
                                    Action.FEED -> PixubiSound.FEED
                                    Action.WATER -> PixubiSound.WATER
                                    Action.CHARGE -> PixubiSound.HAPPY
                                    Action.EXPLORE -> PixubiSound.STAR
                                    Action.SLEEP -> PixubiSound.SLEEP
                                    Action.WAKE -> PixubiSound.WAKE
                                    Action.HIBERNATE -> PixubiSound.FREEZE
                                    Action.PLAY_WIN -> PixubiSound.HAPPY
                                })
                                react(when (action) {
                                    Action.FEED -> PetEmotion.EATING
                                    Action.WATER -> PetEmotion.DRINKING
                                    Action.CHARGE -> PetEmotion.HAPPY
                                    Action.EXPLORE -> null
                                    Action.WAKE -> PetEmotion.HAPPY
                                    Action.PLAY_WIN -> PetEmotion.HAPPY
                                    Action.SLEEP, Action.HIBERNATE -> null
                                })
                            },
                            onBuy = { item ->
                                store(GameEngine.buy(current, item, System.currentTimeMillis()))
                                audio.effect(PixubiSound.TAP)
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
                            onSettings = { goSettings() },
                            onProfile = { showProfile = true }
                        )
                    }
                }
                val notice = updateDialog
                if (notice != null) {
                    val stable = updater.isStableInstall
                    AlertDialog(
                        onDismissRequest = { updateDialog = null },
                        title = {
                            Text(when (notice) {
                                is PixubiUpdateResult.NewRelease -> "🐣 Новий PIXUBI ${notice.release.version}"
                                is PixubiUpdateResult.UpToDate -> "✅ PIXUBI актуальний"
                                is PixubiUpdateResult.Unavailable -> "ℹ️ Оновлення"
                            })
                        },
                        text = {
                            Text(when (notice) {
                                is PixubiUpdateResult.NewRelease -> if (stable)
                                    "Є новіша підписана версія PIXUBI. Завантажити перевірений APK та оновити гру поверх зі збереженням вихованця? Підтвердження встановлення покаже Android."
                                else
                                    "Це тестова збірка PIXUBI. Стабільна версія встановлюється окремо. Оновлення поверх запрацюють після встановлення першого підписаного релізу."
                                is PixubiUpdateResult.UpToDate ->
                                    "Встановлено PIXUBI ${notice.version}. Нових підписаних випусків наразі немає."
                                is PixubiUpdateResult.Unavailable -> notice.message
                            })
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                updateDialog = null
                                if (notice is PixubiUpdateResult.NewRelease) {
                                    if (stable) {
                                        updater.downloadAndInstall(notice.release) { msg ->
                                            showUpdateToast(msg)
                                        }
                                    } else {
                                        runCatching {
                                            startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/MrKanzo/PIXUBI/releases"))
                                            )
                                        }.onFailure { showUpdateToast("Браузер недоступний") }
                                    }
                                }
                            }) {
                                Text(when {
                                    notice is PixubiUpdateResult.NewRelease && stable -> "Завантажити й оновити"
                                    notice is PixubiUpdateResult.NewRelease -> "Відкрити релізи"
                                    else -> "Гаразд"
                                })
                            }
                        },
                        dismissButton = {
                            if (notice is PixubiUpdateResult.NewRelease) {
                                TextButton(onClick = { updateDialog = null }) { Text("Пізніше") }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HatchScreen(onHatch: (Pet) -> Unit, onSettings: () -> Unit,
                        onProfile: () -> Unit) {
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
            OutlinedButton(onClick = onProfile) { Text("👤") }
            Spacer(Modifier.width(5.dp))
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
        Text("🐾 Твій Піксубі починає життя малюком, потім стає юним, дорослим і відкриває ультра-форму. Зростання потребує активних днів та XP.", color = Color(0xFFD4DBE8), style = MaterialTheme.typography.bodySmall)
        Text("🎲 Забарвлення при народженні визначиться випадково. Його не можна обрати вручну.", color = Color(0xFFD4DBE8), style = MaterialTheme.typography.bodySmall)
        Text("⚠️ Піксубі може померти без їжі та води. Коли йдеш надовго — обов’язково вмикай сплячку, вона зупиняє час.", color = Color(0xFFFFD58A), style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { onHatch(GameEngine.hatch(species, sex, region, petName, System.currentTimeMillis())) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("🐾 Створити Піксубі") }
        Text("Без інтернету • Без реклами • Збереження на телефоні", modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HomeScreen(
    pet: Pet,
    collection: PlayerCollection,
    reaction: PetEmotion?,
    onAction: (Action) -> Unit,
    onBuy: (ShopItem) -> Unit,
    onPetTap: () -> Unit,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit
) {
    val emotion = EmotionResolver.resolve(pet, reaction)
    var showGrowthGallery by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
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
            OutlinedButton(onClick = onProfile) { Text("👤") }
            Spacer(Modifier.width(5.dp))
            OutlinedButton(onClick = onSettings) { Text("⚙️") }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🪙 ${pet.coins}  •  🎒 Запаси",
                modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD58A))
            OutlinedButton(onClick = { showShop = true }) { Text("🛍️ Крамничка") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = navy)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("🍓 Їжа: ${pet.foodStock}     💧 Вода: ${pet.waterStock}     🔋 Заряд: ${pet.chargeStock}",
                    style = MaterialTheme.typography.bodyMedium)
                Text("🏡 ${if (pet.hasHome) "Є" else "Немає"}  •  🍽️ ${if (pet.hasBowl) "Є" else "Немає"}  •  🥤 ${if (pet.hasCup) "Є" else "Немає"}  •  ⚡ ${if (pet.hasCharger) "Є" else "Немає"}",
                    style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
        }
        PetStatusCard(pet, reaction, onPetTap)
        if (pet.expeditionStartedAt > 0L && pet.mode == Mode.AWAKE) {
            SearchProgressCard(pet)
        }
        GrowthProgressCard(pet, onGallery = { showGrowthGallery = true })
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
                    Button(onClick = { onAction(Action.FEED) }, enabled = pet.foodStock > 0, modifier = Modifier.weight(1f)) { Text("🍓 Їжа (${pet.foodStock})") }
                    Button(onClick = { onAction(Action.WATER) }, enabled = pet.waterStock > 0, modifier = Modifier.weight(1f)) { Text("💧 Вода (${pet.waterStock})") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onAction(Action.SLEEP) }, modifier = Modifier.weight(1f)) { Text("🌙 Сон") }
                    OutlinedButton(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("🎮 Ігри") }
                }
                OutlinedButton(onClick = { onAction(Action.CHARGE) },
                    enabled = pet.chargeStock > 0, modifier = Modifier.fillMaxWidth()) {
                    Text("⚡ Зарядити Пікса (${pet.chargeStock})")
                }
                val today = PetGrowth.careDay(System.currentTimeMillis())
                val used = if (pet.expeditionDay == today) pet.expeditionToday else 0
                Button(onClick = { onAction(Action.EXPLORE) },
                    enabled = pet.expeditionStartedAt == 0L && used < GameEngine.EXPEDITIONS_PER_DAY && pet.energy >= 15 && PetGrowth.stage(pet) !in listOf(GrowthStage.EGG, GrowthStage.COCOON),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (pet.expeditionStartedAt > 0L) "🧭 Пікс шукає скарби (20 хв)"
                    else "🧭 Пошук монет і ресурсів (${used}/4)")
                }
                Text("🪙 Ігри: до 30 монет/день. Прогулянки: 4 за день, знахідка через 20 хвилин.",
                    style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                OutlinedButton(onClick = { onAction(Action.HIBERNATE) },
                    modifier = Modifier.fillMaxWidth()) { Text("❄️ Сплячка: я йду у справах") }
            }
        }
        HorizontalDivider(color = Color(0xFF35455D))
        Text("🌍 Регіон: ${pet.region.title}")
        Text("🏡 Рідний край: ${pet.species.native.title}  •  🎲 Колір №${pet.colorIndex + 1}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text("🧬 Приховані гени кольору збережені для майбутньої системи розмноження.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text("🏆 Мініігор виграно: ${pet.miniGameWins}", style = MaterialTheme.typography.bodySmall)
        Text("⏳ Поки гра закрита, Пікс росте й витрачає потреби до 12 годин. У сплячці час зупинено.",
            style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Spacer(Modifier.height(24.dp))
    }
    if (showGrowthGallery) {
        GrowthGalleryDialog(pet, collection, onClose = { showGrowthGallery = false })
    }
    if (showShop) {
        PixubiShopDialog(pet, onBuy = onBuy, onClose = { showShop = false })
    }
}

/** Countdown is visual only; the reward is settled by the saved timestamp in GameEngine. */
@Composable
private fun SearchProgressCard(pet: Pet) {
    var now by remember(pet.expeditionStartedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pet.expeditionStartedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val duration = GameEngine.EXPEDITION_MINUTES * 60_000L
    val elapsed = (now - pet.expeditionStartedAt).coerceIn(0L, duration)
    val secondsLeft = (duration - elapsed + 999L) / 1000L
    Card(colors = CardDefaults.cardColors(containerColor = navy)) {
        Column(Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(PixubiSearch.icon(pet.species) + " " + PixubiSearch.activity(pet.species),
                    modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall)
                Text(String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    color = Color(0xFFFFD58A), fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { elapsed.toFloat() / duration.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF98E6CD),
                trackColor = Color(0xFF39485F)
            )
            Text("Пошук іде за справжнім часом, навіть коли телефон заблокований.",
                style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        }
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
private fun PetStatusCard(pet: Pet, reaction: PetEmotion?, onPetTap: () -> Unit) {
    val searching = pet.expeditionStartedAt > 0L
    Card(colors = CardDefaults.cardColors(containerColor = navy)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(pet.region.title, style = MaterialTheme.typography.titleMedium)
            Text(pet.name, fontWeight = FontWeight.Bold)
            Text(EmotionResolver.label(EmotionResolver.resolve(pet, reaction)))
            Button(onClick = onPetTap, enabled = pet.mode == Mode.AWAKE && !searching) {
                Text("Погратися з вихованцем")
            }
        }
    }
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
private fun FarewellScreen(pet: Pet, onProfile: () -> Unit, onNewPet: () -> Unit) {
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
        Button(onClick = onProfile, modifier = Modifier.fillMaxWidth()) {
            Text("📖 Подивитися колекцію Піксу")
        }
        Button(onClick = onNewPet, modifier = Modifier.fillMaxWidth()) { Text("🥚 Виростити нового Піксубі") }
        Text("У версії 0.8 можна доглядати лише одного вихованця; новий замінить попереднє збереження.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.LightGray)
    }
}
