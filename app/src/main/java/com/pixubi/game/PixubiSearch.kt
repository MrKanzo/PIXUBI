package com.pixubi.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Pure, deterministic motion: only the selected species decides how it searches. */
data class SearchPose(
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val submerged: Float = 0f
)

object PixubiSearch {
    fun pose(species: Species, cycle: Float): SearchPose {
        val t = ((cycle % 1f) + 1f) % 1f
        val side = sin(t * 2.0 * PI).toFloat()
        val hop = abs(sin(t * 4.0 * PI)).toFloat()
        return when (species) {
            Species.LISYK -> SearchPose(
                xDp = 46f * side, yDp = -11f * hop,
                rotation = 5f * side
            )
            Species.ISKRYK -> SearchPose(
                xDp = 64f * side, yDp = -34f * hop,
                rotation = 12f * side, scale = 1f + .06f * hop
            )
            Species.KRAPLYK -> {
                val dive = (1f + side) / 2f
                SearchPose(
                    xDp = 35f * side, yDp = 27f * dive,
                    rotation = -5f * side,
                    scale = 1f - .14f * dive,
                    submerged = .68f * dive
                )
            }
            Species.PISHCHANYK -> {
                // Enter the sand, travel beneath it, then pop out.
                val enter = ((t - .13f) / .13f).coerceIn(0f, 1f)
                val exit = ((.83f - t) / .13f).coerceIn(0f, 1f)
                val buried = minOf(enter, exit)
                SearchPose(
                    xDp = 51f * side, yDp = 67f * buried - 9f * hop * (1f - buried),
                    rotation = 6f * side * (1f - buried),
                    scale = 1f - .23f * buried,
                    submerged = buried
                )
            }
            Species.KHMARYK -> SearchPose(
                xDp = 52f * side, yDp = -27f - 24f * hop,
                rotation = 9f * side, scale = 1f + .035f * hop
            )
        }
    }

    fun icon(species: Species): String = when (species) {
        Species.LISYK -> "🍃"
        Species.ISKRYK -> "⚡"
        Species.KRAPLYK -> "💧"
        Species.PISHCHANYK -> "🏜️"
        Species.KHMARYK -> "☁️"
    }

    fun activity(species: Species): String = when (species) {
        Species.LISYK -> "Нюхаю сліди й шукаю під листям"
        Species.ISKRYK -> "Стрибаю між камінням"
        Species.KRAPLYK -> "Пірнаю за знахідками"
        Species.PISHCHANYK -> "Рию підземний тунель"
        Species.KHMARYK -> "Досліджую хмаринки"
    }

    fun returnMessage(species: Species): String = when (species) {
        Species.LISYK -> "Знайшов схованку серед листя!"
        Species.ISKRYK -> "Відшукав скарб біля скель!"
        Species.KRAPLYK -> "Дістав знахідку з води!"
        Species.PISHCHANYK -> "Викопав щось цікаве!"
        Species.KHMARYK -> "Приніс подарунок із неба!"
    }

    /** Rare discoveries depend on saved pet genes and expedition count, never on rendering frames. */
    fun foundRareTreasure(pet: Pet): Boolean =
        pet.expeditionsDone > 0 &&
            (pet.geneA * 17 + pet.geneB * 7 + pet.expeditionsDone * 13) % 9 == 0
}
