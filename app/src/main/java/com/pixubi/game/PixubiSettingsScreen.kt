package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
fun PixubiSettingsScreen(
    settings: SoundSettings,
    notificationSettings: PixubiNotificationSettings,
    onNotificationSettingsChange: (PixubiNotificationSettings) -> Unit,
    onTestNotification: () -> Unit,
    currentRegion: Region,
    previewRegion: Region?,
    onSettingsChange: (SoundSettings) -> Unit,
    onPreviewRegion: (Region) -> Unit,
    onTestSound: () -> Unit,
    onCheckUpdates: () -> Unit,
    isStableInstall: Boolean,
    checkingForUpdates: Boolean,
    onBack: () -> Unit
) {
    val context=LocalContext.current
    val version=remember(context){context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "?"}
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showRobotLab by rememberSaveable { mutableStateOf(false) }
    if(showRobotLab) {
        RobotLab { showRobotLab=false }
        return
    }
    BackHandler(enabled=showAbout){showAbout=false}
    if(showAbout) {
        PixubiAboutScreen(version){showAbout=false}
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⚙️ Налаштування", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Маленький світ — велика атмосфера", color = Color(0xFFB8C8E3))
            }
            OutlinedButton(onClick = onBack) { Text("← Назад") }
        }
        OutlinedButton(onClick={showRobotLab=true},modifier=Modifier.fillMaxWidth()) {
            Text("Robot Lab · MK-1")
        }
        OutlinedButton(onClick={showAbout=true},modifier=Modifier.fillMaxWidth()) {
            Text("📖 Про PIXUBI LAB · Світ, види та автори")
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔔 Піксубі на зв'язку", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                SettingsSwitch(
                    title = "🐣 Сповіщення від вихованця",
                    description = "Пікс нагадає, коли хоче їсти, пити, гратися або спати",
                    checked = notificationSettings.enabled,
                    onChecked = { onNotificationSettingsChange(notificationSettings.copy(enabled = it)) }
                )
                if (notificationSettings.enabled) {
                    HorizontalDivider(color = Color(0xFF41556D))
                    SettingsSwitch(
                        title = "🌙 Тихі години 22:00–08:00",
                        description = "У цей час не турбуватиму тебе",
                        checked = notificationSettings.quietHours,
                        onChecked = { onNotificationSettingsChange(notificationSettings.copy(quietHours = it)) }
                    )
                    SettingsSwitch("🍓 Про їжу", "Коли Пікс зголодніє",
                        notificationSettings.food,
                        { onNotificationSettingsChange(notificationSettings.copy(food = it)) })
                    SettingsSwitch("💧 Про воду", "Коли Пікс захоче пити",
                        notificationSettings.water,
                        { onNotificationSettingsChange(notificationSettings.copy(water = it)) })
                    SettingsSwitch("💤 Про сон", "Коли закінчується енергія",
                        notificationSettings.sleep,
                        { onNotificationSettingsChange(notificationSettings.copy(sleep = it)) })
                    SettingsSwitch("🎮 Про ігри", "Коли Піксу стане сумно",
                        notificationSettings.play,
                        { onNotificationSettingsChange(notificationSettings.copy(play = it)) })
                    OutlinedButton(onClick = onTestNotification, modifier = Modifier.fillMaxWidth()) {
                        Text("🔔 Надіслати тестове сповіщення")
                    }
                    Text("Перевірка приблизно кожні 30 хвилин, залежно від Android. " +
                        "Не частіше одного нагадування за 4 години. Сплячка зупиняє нагадування.",
                        color = Color(0xFFBFCFE9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSwitch(
                    title = "🎵 Фонова мелодія",
                    description = "Оригінальна цифрова музика залежно від регіону",
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
                    title = "🔊 Звукові ефекти",
                    description = "Біпи, їжа, вода, сон, радість і дотики",
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
                    Text("🎮 Перевірити цифровий звук")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🌍 Мелодії регіонів", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("У грі музика автоматично відповідає дому твого Піксубі. " +
                    "Тут можна послухати інші мелодії, не змінюючи регіон.",
                    color = Color(0xFFBFCFE9), style = MaterialTheme.typography.bodySmall)
                Region.entries.forEach { world ->
                    val selected = previewRegion == world ||
                        (previewRegion == null && currentRegion == world)
                    FilterChip(
                        selected = selected,
                        onClick = { onPreviewRegion(world) },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(world.icon + " " + world.title +
                                (if (currentRegion == world) "  •  твій регіон" else ""))
                        }
                    )
                }
                if (!settings.music) {
                    Text("Увімкни фонову мелодію, щоб почути вибраний регіон.",
                        color = Color(0xFFFFD58A), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24334F)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("🔄 Оновлення PIXUBI LAB", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Поточна версія: $version" +
                    (if (isStableInstall) " • стабільний випуск" else " • тестова збірка"),
                    color = Color(0xFFCAD9EE), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onCheckUpdates,
                    enabled = !checkingForUpdates, modifier = Modifier.fillMaxWidth()) {
                    Text(if (checkingForUpdates) "⏳ Перевіряю GitHub…" else "🔄 Перевірити оновлення")
                }
                Text(if (isStableInstall)
                    "Перевіряємо підписані GitHub Releases. При новій версії PIXUBI сама запропонує завантаження й відкриє Android-інсталятор. Дані лишаються, якщо підпис збігається."
                else
                    "Це тестова збірка. Оновлення поверх зі збереженням вихованця працюватиме тільки зі стабільним APK, підписаним постійним ключем.",
                    color = Color(0xFFFFDA97), style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17243B))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💜 Твій спокій важливий", fontWeight = FontWeight.Bold)
                Text("Гра автоматично замовкає, коли ти перемикаєшся на інший застосунок. " +
                    "Усі мелодії та ефекти синтезуються прямо в телефоні: " +
                    "без інтернету, реклами та завантажених пісень.",
                    color = Color(0xFFCCD5E9), style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("✅ Зберегти й повернутися")
        }
        Spacer(Modifier.height(24.dp))
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
