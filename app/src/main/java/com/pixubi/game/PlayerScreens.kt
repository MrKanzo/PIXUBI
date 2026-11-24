package com.pixubi.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val profileBg = Color(0xFF22344F)
private val muted = Color(0xFFB8CBE5)
private val mint = Color(0xFF9BE7D4)
private val gold = Color(0xFFFFD18E)

@Composable
fun PlayerProfileScreen(
    collection: PlayerCollection,
    progress: QuizProgress,
    current: Pet?,
    onName: (String) -> Unit,
    onAvatar: (String) -> Unit,
    onCollection: () -> Unit,
    onBack: () -> Unit
) {
    val formattedDate = remember(collection.startedAt) {
        if (collection.startedAt <= 0L) "невідомо"
        else DateTimeFormatter.ofPattern("dd.MM.yyyy").format(
            Instant.ofEpochMilli(collection.startedAt).atZone(ZoneId.systemDefault()))
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👤 Профіль гравця", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text("← Назад") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = profileBg),
            shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(collection.avatar, fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                OutlinedTextField(
                    value = collection.nickname,
                    onValueChange = { onName(it.take(24)) },
                    label = { Text("Ім'я гравця") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Обери аватар", color = muted, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("🐾", "🦊", "⚡", "🦎", "🪽", "🌟").forEach { avatar ->
                        Box(
                            Modifier.size(43.dp)
                                .background(
                                    if (collection.avatar == avatar) Color(0xFF385E69) else Color(0xFF15243A),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onAvatar(avatar) },
                            contentAlignment = Alignment.Center
                        ) { Text(avatar, fontSize = 22.sp) }
                    }
                }
                Text("Грає з: $formattedDate", color = muted,
                    style = MaterialTheme.typography.labelSmall)
                Text("⭐ Рівень гравця: ${collection.playerLevel}", color = mint,
                    fontWeight = FontWeight.Bold)
            }
        }
        Text("📊 Твій шлях", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat("🥚", "Вирощено", "${collection.totalRaised}", Modifier.weight(1f))
            ProfileStat("🦊", "Дорослих", "${collection.grownAdults}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat("🧬", "Форм", "${collection.unlockedCount}/${CollectionRules.ALL_FORMS}", Modifier.weight(1f))
            ProfileStat("🔥", "Рекорд серії", "${collection.bestStreak} дн.", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat("✨", "Мініігор", "${collection.totalMiniGameWins}", Modifier.weight(1f))
            ProfileStat("🧠", "Вікторин", "${progress.victories}", Modifier.weight(1f))
        }
        Text("🌍 Відкрито видів: ${collection.knownSpecies} із ${Species.entries.size}",
            color = muted, style = MaterialTheme.typography.bodyMedium)
        Text("🕰️ Сумарно прожито: ${collection.lifeDays} активних днів",
            color = muted, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCollection, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("📖 Моя колекція Піксу")
        }
        Text("Усі відкриття залишаються в колекції навіть після появи нового яйця. Майбутні форми сховані.",
            style = MaterialTheme.typography.bodySmall, color = gold)
        if (collection.raised.isNotEmpty()) {
            Text("📜 Історія моїх Піксу", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            collection.raised.asReversed().take(20).forEach { record ->
                Card(colors = CardDefaults.cardColors(containerColor = profileBg)) {
                    Column(Modifier.fillMaxWidth().padding(11.dp)) {
                        Text("${record.species.icon} ${record.name} · ${record.sex.title}",
                            fontWeight = FontWeight.Bold)
                        Text("Відкрито: ${record.highest.title} · ${record.activeDays} дн. · ${record.xp} XP",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                        Text(if (current != null && CollectionRules.recordId(current) == record.id && current.alive)
                            "💚 Зараз із тобою" else if (record.alive) "💜 Мандрівник" else "🕯️ Залишився в колекції",
                            color = if (record.alive) mint else gold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun ProfileStat(icon: String, label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = profileBg),
        shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, fontSize = 21.sp)
            Text(value, color = mint, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

@Composable
fun PixuCollectionScreen(
    library: PlayerCollection,
    current: Pet?,
    onBack: () -> Unit
) {
    var chosen by remember { mutableStateOf<Species?>(null) }
    var sex by remember { mutableStateOf(Sex.GIRL) }
    var color by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("📖 Колекція Піксу", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("${library.unlockedCount} / ${CollectionRules.ALL_FORMS} відкритих форм",
                    style = MaterialTheme.typography.bodySmall, color = muted)
            }
            OutlinedButton(onClick = {
                if (chosen != null) chosen = null else onBack()
            }) { Text("← Назад") }
        }
        if (chosen == null) {
            Text("Доглядай Піксу, щоб зібрати незвичайні форми. Те, чого ще не відкривав, залишається таємницею.",
                color = gold, style = MaterialTheme.typography.bodySmall)
            Species.entries.forEach { species ->
                val found = library.raised.any { it.species == species }
                val count = library.discovered.count { it.startsWith(species.name + ":") }
                Card(
                    onClick = {
                        chosen = species
                        val first = library.raised.firstOrNull { it.species == species }
                        sex = first?.sex ?: Sex.GIRL
                        color = first?.colorIndex ?: 0
                    },
                    colors = CardDefaults.cardColors(containerColor = profileBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (found) species.icon else "🔒", fontSize = 31.sp)
                        Spacer(Modifier.padding(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (found) species.title else "??? · невідомий вид",
                                fontWeight = FontWeight.Bold)
                            Text(if (found) "Відкрито $count форм" else "Ще не знайдено",
                                color = muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", fontSize = 26.sp, color = mint)
                    }
                }
            }
        } else {
            val species = chosen!!
            val discovered = library.raised.any { it.species == species }
            Text(if (discovered) species.icon + " " + species.title else "🔒 Невідомий вид",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!discovered) {
                Text("Вирости цей вид, щоб відкрити його колекцію.",
                    color = gold, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Стать", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Sex.entries.forEach { s ->
                        FilterChip(selected = s == sex, onClick = {
                            sex = s
                            color = library.raised.firstOrNull {
                                it.species == species && it.sex == s
                            }?.colorIndex ?: 0
                        }, label = { Text(s.title) })
                    }
                }
                Text("Забарвлення", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in 0..5) {
                        val unlockedColor = library.raised.any {
                            it.species == species && it.sex == sex && it.colorIndex == i
                        }
                        FilterChip(
                            selected = color == i,
                            onClick = { color = i },
                            label = { Text(if (unlockedColor) "${i + 1}" else "🔒") }
                        )
                    }
                }
                val sample = Pet(
                    species=species,sex=sex,region=species.native,name=species.title,
                    geneA=color,geneB=0,bornAt=0L,lastUpdatedAt=0L
                )
                GrowthStage.playable.forEach { stage ->
                    val exact = CollectionRules.hasForm(library, species, sex, color, stage)
                    // A baby is shown as an early teaser only for the active pet's exact genes.
                    val teaser = !exact && stage == GrowthStage.BABY && current != null &&
                        current.species == species && current.sex == sex &&
                        current.colorIndex == color
                    val visible = exact || teaser
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (visible) profileBg else Color(0xFF18263D)
                    )) {
                        Row(Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (visible) {
                                GrowthSprite(
                                    pet=sample,previewStage=stage,
                                    modifier=Modifier.size(92.dp)
                                )
                            } else {
                                Box(Modifier.size(92.dp).background(
                                    Color(0xFF101A2E), RoundedCornerShape(14.dp)
                                ), contentAlignment = Alignment.Center) {
                                    Text("???", color = Color(0xFF71849F), fontSize = 25.sp)
                                }
                            }
                            Spacer(Modifier.padding(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (visible) stage.icon + " " + stage.title else "🔒 Таємна форма",
                                    fontWeight = FontWeight.Bold)
                                Text(when {
                                    exact -> "Відкрито назавжди ✓"
                                    teaser -> "Малюк — перше перетворення"
                                    else -> "Вирости цього Піксу, щоб дізнатись"
                                }, style = MaterialTheme.typography.bodySmall,
                                    color = if (visible) mint else muted)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
