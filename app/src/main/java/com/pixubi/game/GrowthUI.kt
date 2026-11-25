package com.pixubi.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun GrowthProgressCard(pet: Pet, onGallery: () -> Unit) {
    val stage = PetGrowth.stage(pet)
    val level = PetGrowth.level(pet.xp)
    val next = PetGrowth.xpForLevel((level + 1).coerceAtMost(PetGrowth.MAX_LEVEL))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243550)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${stage.icon} ${stage.title}  ·  рівень $level",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Досвід: ${pet.xp} XP" +
                        if (level < PetGrowth.MAX_LEVEL) " • до нового рівня ще ${PetGrowth.xpToNextLevel(pet)}" else " • максимум",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB7D2E8))
                }
                TextButton(onClick = onGallery) { Text("Етапи ›") }
            }
            LinearProgressIndicator(
                progress = { PetGrowth.progressInLevel(pet) },
                modifier = Modifier.fillMaxWidth().height(9.dp),
                color = Color(0xFF94E9C8),
                trackColor = Color(0xFF40516B)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🗓️ Вік: ${pet.daysOld} дн.", style = MaterialTheme.typography.bodySmall)
                Text("💖 Днів догляду: ${pet.careDays}", style = MaterialTheme.typography.bodySmall)
            }
            Text("🔥 Серія: ${pet.careStreak} дн.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD38B))
            Text(PetGrowth.stageHint(pet),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBCECF6))
            if (pet.mode == Mode.HIBERNATION) {
                Text("❄️ Сплячка: ріст, дні й потреби на паузі.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB5DDF2))
            }
        }
    }
}

/** Never render undiscovered elder forms; babies are the public teaser. */
@Composable
fun GrowthGalleryDialog(
    pet: Pet,
    collection: PlayerCollection,
    onClose: () -> Unit
) {
    var selected by remember(pet.bornAt) { mutableStateOf(PetGrowth.stage(pet)) }
    val revealed = CollectionRules.canPreviewCurrent(collection, pet, selected)
    val opened = CollectionRules.hasForm(
        collection, pet.species, pet.sex, pet.colorIndex, selected
    )
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🧬 Таємниці еволюції", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Малюка видно наперед, а старші форми сховані. Рости й відкривай нові образи!",
                    style = MaterialTheme.typography.bodySmall)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    GrowthStage.playable.forEach { stage ->
                        val known = CollectionRules.canPreviewCurrent(collection, pet, stage)
                        FilterChip(
                            selected = stage == selected,
                            onClick = { selected = stage },
                            label = { Text(if (known) stage.icon + " " + stage.title else "🔒 ???") }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (revealed) {
                        Text(selected.title, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Box(Modifier.size(175.dp)
                            .background(Color(0xFF13213A), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("???", fontSize = 44.sp, color = Color(0xFF61738C))
                        }
                    }
                }
                Text(if (revealed) selected.icon + " " + selected.title else "🔒 Таємна форма",
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !revealed -> "Цей образ засекречений. Вирощуй Піксубі й відкривай нові стадії."
                        opened -> "✅ Відкрито назавжди! Форма збереглася в твоїй колекції."
                        selected == GrowthStage.BABY -> "Початкова стадія розвитку."
                        else -> "Продовжуй турбуватися про Піксубі."
                    },
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
                if (revealed) {
                    val timeRequired = if (selected.minMinutes < 60L)
                        "${selected.minMinutes} хв"
                    else "${selected.minMinutes / 1_440L} активних дн."
                    Text("Для відкриття: $timeRequired • рівень ${selected.minLevel}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA6D9E5))
                }
                Text("Відкриття не губляться при появі нового яйця.",
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Повернутися") } }
    )
}
