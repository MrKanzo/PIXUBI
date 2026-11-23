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

/** Visual-only search states. Rewards and the expedition clock remain in GameEngine. */
enum class SearchPhase { WANDERING, HIDING, HIDDEN, EMERGING }

data class SearchFrame(
    val phase: SearchPhase,
    val pose: SearchPose,
    val alpha: Float,
    val concealment: Float,
    val entryXDp: Float,
    val exitXDp: Float,
    val coverXDp: Float,
    val rustleDp: Float,
    val pulse: Float
)

object PixubiSearch {
    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun fade(from: Float, to: Float, current: Float) =
        smooth((current - from) / (to - from))

    /**
     * Four different 12-second scenes loop seamlessly. A Pixubi approaches a hiding spot,
     * disappears behind/under the scenery, moves to the next spot while out of sight,
     * and emerges. Search cycle is cosmetic and never advances the saved expedition.
     */
    fun frame(species: Species, timeline: Float, seed: Int = 0): SearchFrame {
        val loop = ((timeline % 4f) + 4f) % 4f
        val cycle = loop.toInt()
        val t = loop - cycle
        val direction = if ((seed + cycle) % 2 == 0) 1f else -1f
        val distance = when (species) {
            Species.LISYK -> 48f
            Species.ISKRYK -> 51f
            Species.KRAPLYK -> 39f
            Species.PISHCHANYK -> 47f
            Species.KHMARYK -> 44f
        }
        val entry = direction * distance
        val exit = -entry
        val hideStart = .28f + (seed + cycle).mod(3) * .025f
        val hiddenStart = hideStart + .105f
        val emergeStart = .64f + (seed + cycle).mod(2) * .03f
        val emergeEnd = emergeStart + .105f
        val phase = when {
            t < hideStart -> SearchPhase.WANDERING
            t < hiddenStart -> SearchPhase.HIDING
            t < emergeStart -> SearchPhase.HIDDEN
            t < emergeEnd -> SearchPhase.EMERGING
            else -> SearchPhase.WANDERING
        }
        val alpha = when (phase) {
            SearchPhase.HIDING -> 1f - fade(hideStart, hiddenStart, t)
            SearchPhase.HIDDEN -> 0f
            SearchPhase.EMERGING -> fade(emergeStart, emergeEnd, t)
            SearchPhase.WANDERING -> 1f
        }
        val x = when {
            t < hideStart -> entry * fade(0f, hideStart, t)
            t < hiddenStart -> entry
            t < emergeStart -> entry + (exit - entry) * fade(hiddenStart, emergeStart, t)
            t < emergeEnd -> exit
            else -> exit * (1f - fade(emergeEnd, 1f, t))
        }
        val concealment = 1f - alpha
        val old = pose(species, t)
        val y = when (species) {
            Species.PISHCHANYK -> old.yDp * alpha + 67f * concealment
            Species.KRAPLYK -> old.yDp * alpha + 45f * concealment
            Species.KHMARYK -> -22f - 24f * kotlin.math.abs(
                sin(t * 2.0 * PI).toFloat()) - 14f * concealment
            else -> old.yDp * alpha + 7f * concealment
        }
        val pulse = (.5f + .5f * sin(t * 30.0 * PI).toFloat()).coerceIn(0f, 1f)
        return SearchFrame(
            phase = phase,
            pose = old.copy(
                xDp = x,
                yDp = y,
                scale = old.scale * (1f - .12f * concealment),
                rotation = old.rotation * alpha
            ),
            alpha = alpha,
            concealment = concealment,
            entryXDp = entry,
            exitXDp = exit,
            coverXDp = x,
            rustleDp = sin(t * 42.0 * PI).toFloat() * (2.3f + 3f * concealment),
            pulse = pulse
        )
    }

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
