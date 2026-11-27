package com.pixubi.game

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class GuideLesson(val title:String,val description:String,val page:String,val question:String,val answers:List<String>,val correct:Int){
    CARE("Догляд за роботом","Енергія потрібна для місій і подорожей. Ремонт відновлює корпус, синхронізація — настрій. Сон відновлює заряд, а сплячка зупиняє витрати потреб.","home","Що обрати, якщо надовго залишаєш гру?",listOf("Сплячка","Бойова арена","Магазин"),0),
    MISSIONS("Перші місії","Прогулянки та місії дають XP і монети. Перед запуском перевір вартість енергії. Нагороди отримуєш після завершення.","missions","Коли нараховується нагорода?",listOf("При відкритті меню","Після завершення місії","При купівлі зброї"),1),
    GEAR("Спорядження","У магазині купуй модулі, а в інвентарі одягай їх. Зброя дальнього бою потребує боєприпасів; рівень відкриває нові предмети.","shop","Що потрібно для пострілу з гвинтівки?",listOf("Настрій 100%","Ремонтний набір","Імпульсні касети"),2),
    MAP("Експедиція","Торкнись видимої клітинки, щоб прокласти маршрут. Сканер відкриває територію поблизу. Контейнери та секретні об'єкти збираються один раз.","adventure","Що робити, якщо енергія або корпус критично низькі?",listOf("Скористатися аварійним поверненням або сервісом","Почати ще один бій","Купити нову серію"),0),
    SQUAD("Власний загін","На базі експедиції можна наймати, ремонтувати та покращувати союзників. Нові класи відкриваються після перемог і проходження секторів.","adventure","Де наймати союзних роботів?",listOf("У середині бою","На базі експедиції","У ворожому бункері"),1),
    BATTLE("Тактичний бій","Перед боєм розстав власних юнітів. Потім обери доступного бійця, рухайся або атакуй. Після твоєї дії відповідає один ворог. Особлива навичка доступна раз за бій.","adventure","Що відбувається після дії твого юніта?",listOf("Усі вороги атакують одночасно","Бій автоматично завершується","Один ворог отримує хід"),2),
    GROWTH("Розвиток та артефакти","XP відкриває форми, навички підсилюють характеристики, а артефакти дають постійні бонуси. Для OMEGA потрібні 30 рівень, 10 перемог і 10 експедицій.","stats","Чи зникають артефакти після переходу в новий сектор?",listOf("Ні, залишаються","Так, усі","Тільки якщо був бій"),0);
}
internal data class GuideProgress(val completed:Set<GuideLesson> = emptySet(),val dismissed:Boolean=false){
    val finished get()=completed.size==GuideLesson.entries.size
    val next get()=GuideLesson.entries.firstOrNull{it !in completed}
    fun answer(lesson:GuideLesson,index:Int)=if(index==lesson.correct)copy(completed=completed+lesson)else this
}
internal object GuideStore{
    private fun prefs(c:Context)=c.getSharedPreferences("pixubi_guide_v1",Context.MODE_PRIVATE)
    fun decode(raw:String,dismissed:Boolean)=GuideProgress(raw.split(",").mapNotNull{v->GuideLesson.entries.firstOrNull{it.name==v}}.toSet(),dismissed)
    fun load(c:Context)=decode(prefs(c).getString("completed","").orEmpty(),prefs(c).getBoolean("dismissed",false))
    fun save(c:Context,p:GuideProgress){prefs(c).edit().putString("completed",p.completed.joinToString(","){it.name}).putBoolean("dismissed",p.dismissed).apply()}
}
@Composable
internal fun PixubiGuide(progress:GuideProgress,onProgress:(GuideProgress)->Unit,onClose:()->Unit,onOpen:(String)->Unit){
    var lesson by remember{mutableStateOf(progress.next?:GuideLesson.CARE)}
    var feedback by remember(lesson){mutableStateOf("")}
    val current=lesson
    AlertDialog(onDismissRequest=onClose,title={Text("Академія PIXUBI · ${progress.completed.size}/${GuideLesson.entries.size}")},text={
        Column(Modifier.heightIn(max=390.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
            LinearProgressIndicator(progress={progress.completed.size.toFloat()/GuideLesson.entries.size},modifier=Modifier.fillMaxWidth())
            if(progress.finished)Text("Навчання завершено! Ти можеш повторити будь-який урок.")
            Text("${current.ordinal+1}. ${current.title}",style=MaterialTheme.typography.titleLarge)
            Text(current.description)
            Text(current.question,style=MaterialTheme.typography.titleSmall)
            current.answers.forEachIndexed{i,answer->
                OutlinedButton(onClick={
                    val next=progress.answer(current,i)
                    if(i==current.correct){onProgress(next);feedback="Правильно! Урок зараховано."}
                    else feedback="Не зовсім. Прочитай підказку й спробуй ще раз."
                },modifier=Modifier.fillMaxWidth()){Text(answer)}
            }
            if(feedback.isNotBlank())Text(feedback,color=MaterialTheme.colorScheme.secondary)
            OutlinedButton(onClick={onOpen(current.page)},modifier=Modifier.fillMaxWidth()){Text("Перейти до практики")}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                TextButton(onClick={lesson=GuideLesson.entries[(current.ordinal-1).coerceAtLeast(0)]},enabled=current.ordinal>0){Text("Назад")}
                TextButton(onClick={lesson=GuideLesson.entries[(current.ordinal+1).coerceAtMost(GuideLesson.entries.lastIndex)]},enabled=current.ordinal<GuideLesson.entries.lastIndex){Text("Наступний")}
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text(if(progress.finished)"Готово" else "Закрити")}},dismissButton={
        TextButton(onClick={onProgress(progress.copy(dismissed=true));onClose()}){Text("Пропустити")}
    })
}
