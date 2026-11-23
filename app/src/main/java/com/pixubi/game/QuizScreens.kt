package com.pixubi.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val quizCard = Color(0xFF233551)
private val headingColor = Color(0xFFFFD987)
private val good = Color(0xFF96E8C5)

@Composable
fun GamesHubScreen(
    petName: String,
    progress: QuizProgress,
    onStars: () -> Unit,
    onQuiz: (QuizTopic) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🎮 Ігровий світ", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Грайся разом із " + petName, color = Color(0xFFC3D8EE))
            }
            OutlinedButton(onClick = onBack) { Text("← Назад") }
        }
        Card(onClick = onStars, modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF384E6F))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("✨ Лови іскорки", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Наша перша мінігра: злови п’ять іскорок і порадуй звірятко.",
                    color = Color(0xFFDAE6F4), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("🧠 Вікторини PIXUBI", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text("Шість тем • 72 різні запитання • 5 випадкових на одну гру. " +
            "Для перемоги потрібно 3 правильні відповіді!",
            color = Color(0xFFC3D8EE), style = MaterialTheme.typography.bodySmall)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF192941))) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🎯 Раунди: ${progress.rounds}", color = good)
                Text("🏆 Перемоги: ${progress.victories}", color = headingColor)
            }
        }
        QuizTopic.entries.forEach { topic ->
            Card(onClick = { onQuiz(topic) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = quizCard)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.icon, fontSize = 29.sp)
                    Spacer(Modifier.padding(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(topic.description, style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C8E1))
                        Text("Питань: ${QuizBank.total(topic)}  •  Перемог: ${progress.winsFor(topic)}",
                            style = MaterialTheme.typography.labelSmall, color = good)
                    }
                    Text("›", fontSize = 30.sp, color = headingColor)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun QuizGameScreen(
    topic: QuizTopic,
    attempt: Int,
    onAnswer: (Boolean) -> Unit,
    onComplete: (Int, Int) -> Unit,
    onExit: () -> Unit
) {
    val questions = remember(topic, attempt) { QuizBank.newRound(topic) }
    var number by remember(topic, attempt) { mutableIntStateOf(0) }
    var selected by remember(topic, attempt) { mutableIntStateOf(-1) }
    var score by remember(topic, attempt) { mutableIntStateOf(0) }
    var completed by remember(topic, attempt) { mutableIntStateOf(0) }

    val question = questions[number]
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(topic.icon + " " + topic.title,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Питання ${number + 1} із ${questions.size}",
                    color = Color(0xFFC4D2EC))
            }
            OutlinedButton(onClick = onExit) { Text("✕ Вийти") }
        }
        LinearProgressIndicator(
            progress = { (number + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth().height(9.dp),
            color = good, trackColor = quizCard
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🏆 Правильних: $score", color = good)
            Text("Завдання без повторів", style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB7CBE1))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF364460))) {
            Text(question.text, modifier = Modifier.fillMaxWidth().padding(20.dp),
                textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        question.answers.forEachIndexed { index, answer ->
            val answered = selected >= 0
            val correctAnswer = index == question.correctIndex
            val color = when {
                answered && correctAnswer -> Color(0xFF25564B)
                answered && index == selected -> Color(0xFF6B303E)
                else -> quizCard
            }
            Card(
                onClick = {
                    if (selected == -1) {
                        selected = index
                        if (index == question.correctIndex) score += 1
                        onAnswer(index == question.correctIndex)
                    }
                },
                enabled = !answered,
                colors = CardDefaults.cardColors(
                    containerColor = color,
                    disabledContainerColor = color
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("${'А' + index}.", fontWeight = FontWeight.Bold)
                    Text(answer, modifier = Modifier.weight(1f))
                    if (answered && correctAnswer) Text("✓", color = good)
                    if (answered && index == selected && !correctAnswer)
                        Text("✕", color = Color(0xFFFFACAE))
                }
            }
        }
        if (selected >= 0) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3446))) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (selected == question.correctIndex) "✅ Правильно!"
                    else "💡 Наступного разу вийде!", fontWeight = FontWeight.Bold,
                        color = if (selected == question.correctIndex) good else headingColor)
                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = {
                if (number < questions.lastIndex) {
                    number += 1
                    selected = -1
                } else if (completed == 0) {
                    completed = 1
                    onComplete(score, questions.size)
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (number == questions.lastIndex) "🏁 Підбити підсумки" else "Наступне питання →")
            }
        } else {
            Text("Обери одну правильну відповідь.",
                color = Color(0xFFB9CBE0), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun QuizResultsScreen(
    topic: QuizTopic,
    score: Int,
    total: Int,
    onAgain: () -> Unit,
    onHub: () -> Unit
) {
    val won = score >= 3 && total == 5
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(36.dp))
        Text(if (won) "🏆" else "🌱", fontSize = 72.sp)
        Text(if (won) "Піксубі пишається тобою!" else "Спробуємо ще раз?",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text(topic.icon + " " + topic.title, color = Color(0xFFD4DAEC))
        Text("$score / $total", fontSize = 52.sp, fontWeight = FontWeight.Black,
            color = if (won) good else headingColor)
        Text(if (won) "💖 +18 настрою Піксубі! Ти виграв вікторину."
            else "Для нагороди потрібно правильно відповісти щонайменше на 3 питання.",
            textAlign = TextAlign.Center)
        Text("Усі питання зберігаються в грі й працюють офлайн.",
            color = Color(0xFFC5D3EA), style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center)
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) {
            Text("🔄 Нова вікторина")
        }
        OutlinedButton(onClick = onHub, modifier = Modifier.fillMaxWidth()) {
            Text("← До списку ігор")
        }
    }
}
