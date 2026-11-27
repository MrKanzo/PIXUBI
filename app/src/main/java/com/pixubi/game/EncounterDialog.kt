package com.pixubi.game
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable internal fun EncounterDialog(p:WorldPet,onChange:(WorldPet)->Unit,onSound:(PixubiSound)->Unit,speechEnabled:Boolean=true){
    val d=EncounterRules.assess(p)?:return
    val first=d.enemies.first()
    val phase by rememberGamePhase(2200,RenderBudget.SCENERY_FPS)
    AlertDialog(onDismissRequest={onChange(EncounterRules.leave(p))},containerColor=Color(0xFF152635),titleContentColor=Color(0xFF89DCE4),
        title={Text("⌬ ВХІДНИЙ СИГНАЛ")},text={
            Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),first.tier,first.series),phase,false,Modifier.size(95.dp))
                    Column(Modifier.weight(1f)){Text(first.series.title+" · "+first.tier.title,style=MaterialTheme.typography.titleMedium);Text(d.mood.title);Text("Ворожих ботів: ${d.enemies.size}",style=MaterialTheme.typography.labelSmall)}
                }
                Text("Оцінка загону: "+when{d.ratio>=1.8f->"твоя значна перевага";d.ratio>=1.15f->"ти сильніший";d.ratio>=.85f->"приблизно рівні сили";else->"ворог сильніший"},style=MaterialTheme.typography.labelMedium)
                HorizontalDivider()
                Text("«"+d.message+"»",style=MaterialTheme.typography.bodyLarge)
                if(speechEnabled){
                    val (enemy,own)=SpeechLibrary.dialogue(p,d)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        RobotPreview(RobotView.FRONT,RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),first.tier,first.series),phase,false,Modifier.size(46.dp))
                        ComicBubble(RobotSpeech(first.series.title,enemy,true),Modifier.weight(1f),showName=true)
                    }
                    ComicPortrait(p,RobotSpeech(p.name,own),reverse=true)

                }
                when(d.offer){
                    EncounterOffer.JOIN->Button(onClick={onChange(EncounterRules.accept(p,System.currentTimeMillis()));onSound(PixubiSound.HAPPY)},modifier=Modifier.fillMaxWidth()){Text("Прийняти · +1 "+d.recruit?.title.orEmpty())}
                    EncounterOffer.TOLL->Button(onClick={onChange(EncounterRules.accept(p,System.currentTimeMillis()))},enabled=p.coins>=d.price,modifier=Modifier.fillMaxWidth()){Text("Сплатити ${d.price} 🪙 · баланс ${p.coins}")}
                    EncounterOffer.FIGHT->Text("Мирна угода недоступна",style=MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick={onChange(EncounterRules.fight(p,System.currentTimeMillis()));onSound(PixubiSound.TAP)},enabled=p.energy>=AdventureBalance.BATTLE_ENERGY&&p.integrity>=AdventureBalance.MIN_INTEGRITY,modifier=Modifier.fillMaxWidth()){Text("⚔ Вступити в бій · −${AdventureBalance.BATTLE_ENERGY}% ⚡")}
            }
        },confirmButton={TextButton(onClick={onChange(EncounterRules.leave(p))}){Text("Відступити")}})
}
