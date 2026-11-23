package com.pixubi.game

import android.content.Context

/** Local, independent quiz statistics; no account, analytics or internet. */
data class QuizProgress(
    val rounds: Int = 0,
    val victories: Int = 0,
    val correctAnswers: Int = 0,
    val topicWins: Map<QuizTopic, Int> = emptyMap()
) {
    fun winsFor(topic: QuizTopic): Int = topicWins[topic] ?: 0
}

class QuizProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_quiz_stats", Context.MODE_PRIVATE)

    fun load(): QuizProgress = QuizProgress(
        rounds = prefs.getInt("rounds", 0).coerceAtLeast(0),
        victories = prefs.getInt("victories", 0).coerceAtLeast(0),
        correctAnswers = prefs.getInt("correct", 0).coerceAtLeast(0),
        topicWins = QuizTopic.entries.associateWith {
            prefs.getInt("wins_" + it.name, 0).coerceAtLeast(0)
        }
    )

    fun record(topic: QuizTopic, correct: Int, total: Int): QuizProgress {
        val prev = load()
        val won = correct >= 3 && total == 5
        prefs.edit()
            .putInt("rounds", prev.rounds + 1)
            .putInt("victories", prev.victories + if (won) 1 else 0)
            .putInt("correct", prev.correctAnswers + correct.coerceIn(0,total))
            .putInt("wins_" + topic.name, prev.winsFor(topic) + if (won) 1 else 0)
            .apply()
        return load()
    }
}
