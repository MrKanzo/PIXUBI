package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class QuizBankTest {
    @Test fun bankHasSixTopicsWithTwelveDifferentQuestionsEach() {
        assertEquals(6, QuizTopic.entries.size)
        val all = QuizTopic.entries.flatMap { QuizBank.questionsFor(it) }
        assertEquals(72, all.size)
        assertEquals(72, all.map { it.id }.toSet().size)
        assertEquals(72, all.map { it.text }.toSet().size)
        QuizTopic.entries.forEach { assertEquals(12, QuizBank.total(it)) }
    }

    @Test fun eachRoundHasFiveDistinctQuestionsAndFourUniqueOptions() {
        QuizTopic.entries.forEach { topic ->
            repeat(15) { seed ->
                val questions = QuizBank.newRound(topic, random = Random(seed))
                assertEquals(5, questions.size)
                assertEquals(5, questions.map { it.id }.toSet().size)
                questions.forEach { question ->
                    assertEquals(topic, question.topic)
                    assertEquals(4, question.answers.size)
                    assertEquals(4, question.answers.toSet().size)
                    assertTrue(question.correctIndex in 0..3)
                    assertTrue(question.explanation.isNotBlank())
                }
            }
        }
    }

    @Test fun optionShufflingPreservesTheActualCorrectAnswer() {
        QuizTopic.entries.forEach { topic ->
            val original = QuizBank.questionsFor(topic).associateBy { it.id }
            repeat(15) { n ->
                QuizBank.newRound(topic, random = Random(n)).forEach { shuffled ->
                    val q = original.getValue(shuffled.id)
                    assertEquals(q.answers[q.correctIndex], shuffled.answers[shuffled.correctIndex])
                }
            }
        }
    }

    @Test fun consecutiveRoundsUseDifferentQuestionSets() {
        val first = QuizBank.newRound(QuizTopic.CYBER, random = Random(1)).map { it.id }
        val second = QuizBank.newRound(QuizTopic.CYBER, random = Random(2)).map { it.id }
        assertNotEquals(first, second)
    }
}
