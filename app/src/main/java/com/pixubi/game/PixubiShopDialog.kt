package com.pixubi.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One offline market shared by the home and future world screens. */
@Composable
fun PixubiShopDialog(pet: Pet, onBuy: (ShopItem) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🛍️ Крамничка Піксу") },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("🪙 На рахунку: ${pet.coins}", color = Color(0xFFFFD58A),
                    fontWeight = FontWeight.Bold)
                Text("Монетки заробляються в мінііграх та експедиціях. Усе працює офлайн.",
                    style = MaterialTheme.typography.bodySmall)
                ShopItem.entries.forEach { item ->
                    val owned = when (item) {
                        ShopItem.HOME -> pet.hasHome
                        ShopItem.BOWL -> pet.hasBowl
                        ShopItem.CUP -> pet.hasCup
                        ShopItem.CHARGER -> pet.hasCharger
                        else -> false
                    }
                    val full = when (item) {
                        ShopItem.FOOD -> pet.foodStock > 27
                        ShopItem.WATER -> pet.waterStock > 27
                        ShopItem.CELLS -> pet.chargeStock > 28
                        else -> false
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF243550))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${item.icon} ${item.title}", fontWeight = FontWeight.Bold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (owned) "Уже придбано" else if (full) "Запаси повні"
                                else "🪙 ${item.price}",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFFFD58A),
                                    style = MaterialTheme.typography.bodySmall)
                                Button(onClick = { onBuy(item) },
                                    enabled = !owned && !full && pet.coins >= item.price) {
                                    Text(if (owned) "Є ✓" else "Купити")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Закрити") } }
    )
}
