package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PixubiSettingsScreen(
    settings: SoundSettings,
    notificationSettings: PixubiNotificationSettings,
    onNotificationSettingsChange: (PixubiNotificationSettings) -> Unit,
    onTestNotification: () -> Unit,
    currentSeries: RobotSeries,
    previewSeries: RobotSeries?,
    onSettingsChange: (SoundSettings) -> Unit,
    onPreviewSeries: (RobotSeries) -> Unit,
    onTestSound: () -> Unit,
    onCheckUpdates: () -> Unit,
    isStableInstall: Boolean,
    checkingForUpdates: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }
    var page by rememberSaveable { mutableStateOf("home") }
    val goBack: () -> Unit = {
        if (page == "home") onBack() else page = "home"
    }
    BackHandler(onBack = goBack)
    if (page == "lab" && !BuildConfig.PLAY_DISTRIBUTION) {
        RobotLab { page = "home" }
        return
    }
    if (page == "about") {
        PixubiAboutScreen(version) { page = "home" }
        return
    }

    val heading = when (page) {
        "sound" -> "Звук і музика"
        "tracks" -> "Саундтреки"
        "notifications" -> "Сповіщення"
        "updates" -> "Оновлення"
        else -> "Налаштування"
    }
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(heading, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (page == "home") "PIXUBI WORLD · $version" else "Налаштування PIXUBI",
                    color = Color(0xFFB8C8E3), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = goBack) { Text("← Назад") }
        }
        key(page) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (page) {
                    "sound" -> {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSwitch(
                    title = "Фонова музика",
                    description = "Мелодійний інструментальний саундтрек: база, світ і битви",
                    checked = settings.music,
                    onChecked = { onSettingsChange(settings.copy(music = it)) }
                )
                Text("Гучність музики: ${(settings.musicVolume * 100).toInt()}%",
                    color = Color(0xFFE4F3FA))
                Slider(
                    value = settings.musicVolume,
                    onValueChange = { onSettingsChange(settings.copy(musicVolume = it)) },
                    enabled = settings.music,
                    valueRange = 0f..1f
                )
                HorizontalDivider(color = Color(0xFF41556D))
                SettingsSwitch(
                    title = "Звукові ефекти",
                    description = "Сигнали зарядки, ремонту, активації та місій",
                    checked = settings.effects,
                    onChecked = { onSettingsChange(settings.copy(effects = it)) }
                )
                Text("Гучність ефектів: ${(settings.effectsVolume * 100).toInt()}%",
                    color = Color(0xFFE4F3FA))
                Slider(
                    value = settings.effectsVolume,
                    onValueChange = { onSettingsChange(settings.copy(effectsVolume = it)) },
                    enabled = settings.effects,
                    valueRange = 0f..1f
                )
                OutlinedButton(onClick = onTestSound, enabled = settings.effects,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Перевірити звук")
                }
            }
        }

                        Text("Звук автоматично вимикається, коли гра у фоні.",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCFE9))
                    }
                    "tracks" -> {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Саундтрек відповідає серії роботика та режиму гри. " +
                    "Тут можна послухати інші середовища без зміни персонажа.",
                    color = Color(0xFFBFCFE9), style = MaterialTheme.typography.bodySmall)
                RobotSeries.entries.forEach { world ->
                    val selected = previewSeries == world ||
                        (previewSeries == null && currentSeries == world)
                    FilterChip(
                        selected = selected,
                        onClick = { onPreviewSeries(world) },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(world.title + " · " + HabitatCatalog.of(world).title +
                                (if (currentSeries == world) " · твоя серія" else ""))
                        }
                    )
                }
                if (!settings.music) {
                    Text("Увімкни фонову мелодію, щоб почути вибране середовище.",
                        color = Color(0xFFFFD58A), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

                    }
                    "notifications" -> {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSwitch(
                    title = "Сповіщення роботика",
                    description = "Енергія, стан корпусу, відпочинок та завершені місії",
                    checked = notificationSettings.enabled,
                    onChecked = { onNotificationSettingsChange(notificationSettings.copy(enabled = it)) }
                )
                if (notificationSettings.enabled) {
                    HorizontalDivider(color = Color(0xFF41556D))
                    SettingsSwitch(
                        title = "Тихі години 22:00–08:00",
                        description = "У цей час не турбуватиму тебе",
                        checked = notificationSettings.quietHours,
                        onChecked = { onNotificationSettingsChange(notificationSettings.copy(quietHours = it)) }
                    )
                    SettingsSwitch("Низька енергія", "Коли потрібна зарядка",
                        notificationSettings.food,
                        { onNotificationSettingsChange(notificationSettings.copy(food = it)) })
                    SettingsSwitch("Стан корпусу", "Коли потрібен ремонт",
                        notificationSettings.water,
                        { onNotificationSettingsChange(notificationSettings.copy(water = it)) })
                    SettingsSwitch("Режим відпочинку", "Коли робот довго активний і розряджений",
                        notificationSettings.sleep,
                        { onNotificationSettingsChange(notificationSettings.copy(sleep = it)) })
                    SettingsSwitch("Місії та синхронізація", "Завершені місії та низький настрій",
                        notificationSettings.play,
                        { onNotificationSettingsChange(notificationSettings.copy(play = it)) })
                    OutlinedButton(onClick = onTestNotification, modifier = Modifier.fillMaxWidth()) {
                        Text("Тестове сповіщення")
                    }
                    Text("Перевірка приблизно кожні 30 хвилин, залежно від Android. " +
                        "Не частіше одного нагадування за 4 години. Сплячка зупиняє нагадування.",
                        color = Color(0xFFBFCFE9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

                    }
                    "updates" -> {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Поточна версія: $version" +
                    (if (isStableInstall) " • стабільний випуск" else " • тестова збірка"),
                    color = Color(0xFFCAD9EE), style = MaterialTheme.typography.bodySmall)
                if(BuildConfig.PLAY_DISTRIBUTION){
                    Text("Оновлення через Google Play.",style=MaterialTheme.typography.bodySmall)
                }else{
                OutlinedButton(onClick = onCheckUpdates,
                    enabled = !checkingForUpdates, modifier = Modifier.fillMaxWidth()) {
                    Text(if (checkingForUpdates) "⏳ Перевіряю GitHub…" else "Перевірити оновлення")
                }
                Text(if (isStableInstall)
                    "Перевіряємо підписані GitHub Releases. При новій версії PIXUBI сама запропонує завантаження й відкриє Android-інсталятор. Дані лишаються, якщо підпис збігається."
                else
                    "Це тестова збірка. Оновлення поверх зі збереженням прогресу працюватиме тільки зі стабільним APK, підписаним постійним ключем.",
                    color = Color(0xFFFFDA97), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

                    }
                    else -> {
                        val musicStatus = if (settings.music) "${(settings.musicVolume * 100).toInt()}%" else "вимк."
                        val effectsStatus = if (settings.effects) "${(settings.effectsVolume * 100).toInt()}%" else "вимк."
                        SettingsMenuRow("♫", "Звук і музика", "Музика: $musicStatus · Ефекти: $effectsStatus") { page = "sound" }
                        SettingsMenuRow("◈", "Саундтреки", "Середовище: ${(previewSeries ?: currentSeries).title}") { page = "tracks" }
                        SettingsMenuRow("🔔", "Сповіщення",
                            if (!notificationSettings.enabled) "Вимкнено" else if (notificationSettings.quietHours) "Увімкнено · тихі години 22:00–08:00" else "Увімкнено") { page = "notifications" }
                        SettingsMenuRow("↻", "Оновлення", if (BuildConfig.PLAY_DISTRIBUTION) "Google Play · $version" else "Версія $version · " + if (isStableInstall) "стабільна" else "тестова") { page = "updates" }
                        SettingsMenuRow("ⓘ", "Про гру", "Світ · серії · автори") { page = "about" }
                        if (!BuildConfig.PLAY_DISTRIBUTION) {
                            Text("РОЗРОБНИК", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C8E3),
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                            SettingsMenuRow("⚙", "Robot Lab", "Моделі та анімації") { page = "lab" }
                        }
                        Text("Зміни зберігаються автоматично.", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF98E6CD), modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(symbol: String, title: String, summary: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(symbol, color = Color(0xFF98E6CD), style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFCFE9))
            }
            Text("›", color = Color(0xFF98E6CD), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFFBFCFE9),
                style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
