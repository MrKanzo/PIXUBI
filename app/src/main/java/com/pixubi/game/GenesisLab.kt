package com.pixubi.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.*

private val casing=Color(0xFF393D52)
private val edge=Color(0xFF121622)
private val highlight=Color(0xFFB5BDD0)
private fun DrawScope.shell(path:String,a:Color){
    val p=PathParser().parsePathString(path).toPath()
    drawPath(p,Brush.linearGradient(listOf(highlight,casing,edge),Offset(-64f,-65f),Offset(60f,66f)))
    drawPath(p,edge,style=Stroke(2f,join=StrokeJoin.Round))
    drawPath(p,a.copy(alpha=.55f),style=Stroke(.8f))
}
private fun DrawScope.circuit(a:Color,x:Float,y:Float,u:Float,v:Float){
    drawLine(a.copy(alpha=.25f),Offset(x,y),Offset(u,v),6f,StrokeCap.Round)
    drawLine(a,Offset(x,y),Offset(u,v),2f,StrokeCap.Round)
}
@Composable
private fun GenesisVessel(s:GenesisState,phase:Float,opening:Float,modifier:Modifier){
    val secret=s.mystery&&!s.born&&opening<.65f
    val a=if(secret)Color(0xFFB2A5EA)else s.skin.accent
    Canvas(modifier){
        val k=min(size.width/220f,size.height/220f)
        val pulse=.7f+.3f*sin(phase*2f*PI.toFloat())
        val strength=.18f+.8f*s.energy/100f
        val open=opening.coerceIn(0f,1f)
        translate(size.width/2,size.height/2){scale(k,k,Offset.Zero){
            drawOval(edge.copy(alpha=.45f),Offset(-57f,76f),Size(114f,13f))
            translate(0f,-5f*sin(phase*2f*PI.toFloat())){
                val energy=a.copy(alpha=strength*pulse)
                drawCircle(a.copy(alpha=.10f*strength),45f+open*25f,Offset.Zero)
                drawCircle(energy,21f+open*10f,Offset.Zero,style=Stroke(2f))
                drawCircle(energy,11f,Offset.Zero)
                if(secret){
                    for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*65f,0f){
                        shell("M 0 -57 L 47 -35 L 54 33 L 0 59 L 0 20 L 14 9 L 14 -11 L 0 -20 Z",a)
                        circuit(energy,13f,-38f,34f,-27f);circuit(energy,34f,17f,14f,32f)
                    }}
                    drawCircle(energy,4f,Offset(0f,10f))
                    drawArc(energy,-160f,240f,false,Offset(-9f,-18f),Size(18f,18f),style=Stroke(3f))
                }else when(s.series){
                    RobotSeries.CLASSIC->{
                        for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*53f,-open*12f){
                            shell("M 0 -65 L 54 -39 L 54 37 L 0 65 L 0 25 L 16 14 L 16 -15 L 0 -25 Z",a)
                            shell("M 0 -65 L 54 -39 L 16 -15 L 0 -25 Z",a)
                            circuit(energy,20f,-32f,42f,-22f);circuit(energy,42f,10f,21f,25f)
                        }}
                    }
                    RobotSeries.COMPANION->{
                        for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*48f,0f){rotate(side*open*28f,Offset.Zero){
                            shell("M 0 -60 Q 59 -58 61 0 Q 60 57 0 60 L 0 26 Q 25 23 25 0 Q 25 -23 0 -26 Z",a)
                            drawArc(energy,-72f,144f,false,Offset(-47f,-48f),Size(94f,96f),style=Stroke(3f))
                        }}}
                        val heart=Path().apply{moveTo(0f,14f);cubicTo(-29f,-5f,-13f,-23f,0f,-8f);cubicTo(13f,-23f,29f,-5f,0f,14f);close()}
                        drawPath(heart,energy)
                    }
                    RobotSeries.KAGE->{
                        for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*48f,0f){
                            shell("M 0 -76 L 29 -61 L 45 -36 L 45 43 L 27 70 L 0 77 L 0 32 L 18 20 L 18 -26 L 0 -38 Z",a)
                            circuit(energy,30f,-43f,34f,30f)
                            circuit(energy,10f,-59f,25f,-49f);circuit(energy,25f,49f,10f,59f)
                        }}
                        drawLine(energy,Offset(-12f,0f),Offset(12f,0f),4f)
                    }
                    RobotSeries.VALKYRIE->{
                        for(i in 0..5)rotate(i*60f,Offset.Zero){
                            translate(0f,-open*48f){
                                shell("M 0 -73 Q 35 -53 29 -27 L 0 -9 L -29 -27 Q -35 -53 0 -73 Z",a)
                                circuit(energy,0f,-62f,0f,-23f)
                            }
                        }
                        drawCircle(energy,18f,Offset.Zero,style=Stroke(3f))
                        drawCircle(energy,7f,Offset.Zero)
                    }
                    RobotSeries.NEXUS->{
                        for(i in 0..2)rotate(phase*360f+i*60f,Offset.Zero){
                            val r=48f+open*28f
                            drawOval(energy,Offset(-r,-r*.52f),Size(r*2,r*1.04f),style=Stroke(2.5f))
                            translate(r,0f){shell("M 0 -12 L 10 0 L 0 12 L -10 0 Z",a)}
                        }
                        shell("M 0 -22 L 22 0 L 0 22 L -22 0 Z",a)
                        drawCircle(energy,9f,Offset.Zero)
                    }
                    RobotSeries.MYCELIA->{
                        for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*55f,0f){
                            shell("M 0 -78 Q 40 -57 43 -12 Q 48 45 0 73 L 0 28 Q 23 12 17 -13 L 0 -28 Z",a)
                            for(i in 0..2)circuit(energy,8f,-48f+i*22f,28f,-35f+i*20f)
                        }}
                        for(i in 0..4)rotate(i*72f,Offset.Zero){
                            val p=Path().apply{moveTo(0f,0f);quadraticBezierTo(-15f,-15f,0f,-30f);quadraticBezierTo(15f,-15f,0f,0f);close()}
                            drawPath(p,energy.copy(alpha=.7f))
                        }
                        drawCircle(energy,7f,Offset.Zero)
                    }
                    RobotSeries.SYMBIONT->{
                        for(i in 0..4)rotate(i*72f+sin(phase*6.28f)*4f,Offset.Zero){
                            translate(0f,-open*48f){
                                shell("M 0 -78 Q 37 -53 29 -22 L 0 -7 L -29 -22 Q -37 -53 0 -78 Z",a)
                                circuit(energy,0f,-65f,0f,-19f)
                                for(j in 0..2)drawCircle(energy,3f,Offset(9f,-45f+j*9f))
                            }
                        }
                        drawCircle(energy,18f,Offset.Zero,style=Stroke(2f))
                        for(i in 0..5)drawCircle(energy,3f,Offset(cos(i*1.047f)*12f,sin(i*1.047f)*12f))
                    }
                    RobotSeries.GLITCH->{
                        for(side in listOf(-1f,1f))scale(side,1f,Offset.Zero){translate(open*55f,-open*18f){
                            shell("M 0 -77 L 42 -30 L 53 7 L 0 73 L 0 22 L 19 1 L 0 -29 Z",a)
                            circuit(energy,9f,-49f,32f,-24f);circuit(energy,33f,12f,10f,47f)
                        }}
                        for(i in 0..11){val t=i*1.7f+phase*6.28f;drawRect(energy,Offset(cos(t)*(50f+open*25f),sin(t)*(65f+open*16f)),Size(3f+(i%3),4f))}
                    }
                }
                if(open>0f)for(i in 0..15){
                    val t=i*2f+phase*6.28f;val r=20f+open*75f
                    if(s.series==RobotSeries.GLITCH)drawRect(a.copy(alpha=1f-open*.6f),Offset(cos(t)*r,sin(t)*r),Size(4f,4f))
                    else drawCircle(a.copy(alpha=1f-open*.65f),2f+(i%3),Offset(cos(t)*r,sin(t)*r))
                }
            }
        }}
    }
}
@Composable
internal fun GenesisLab(onBack:()->Unit,onAdopt:(GenesisState)->Unit,world:Boolean=false,onSound:(PixubiSound)->Unit={}){
    val context=LocalContext.current
    var state by remember(context){mutableStateOf(GenesisStorage.load(context,world))}
    var choose by remember{mutableStateOf(false)}
    var opening by remember{mutableStateOf(false)}
    var reset by remember{mutableStateOf(false)}
    val hatch=remember{Animatable(0f)}
    val loop=rememberInfiniteTransition(label="genesis")
    val phase by loop.animateFloat(0f,1f,infiniteRepeatable(tween(2300,easing=LinearEasing)),label="pulse")
    fun persist(s:GenesisState?){GenesisStorage.save(context,s,world);state=s}
    BackHandler{if(!opening){if(choose)choose=false else onBack()}}
    LaunchedEffect(opening){
        if(opening){
            hatch.snapTo(0f)
            hatch.animateTo(1f,tween(1900,easing=FastOutSlowInEasing))
            state?.let{persist(it.hatch());onSound(PixubiSound.HAPPY)}
            opening=false
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("GENESIS · MK-0",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick=onBack,enabled=!opening){Text(if(world)"⚙ Налаштування" else "Назад")}
        }
        Text(if(world)"Початок життя · обери ядро нового роботика" else "Лабораторія народження · окреме збереження",style=MaterialTheme.typography.bodySmall)
        val s=state
        if(s==null){
            Text("Обери початок історії",style=MaterialTheme.typography.titleLarge)
            Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Таємниче народження",style=MaterialTheme.typography.titleMedium)
                Text("Одна з восьми серій. Колір і характер — сюрприз. 8% шанс золотого забарвлення.")
                GenesisVessel(GenesisState(RobotSeries.CLASSIC,RobotSkin.PURPLE,GenesisTrait.CALM,true,energy=60),phase,0f,Modifier.fillMaxWidth().height(145.dp))
                Button(onClick={persist(GenesisRules.create(null));choose=false;onSound(PixubiSound.TAP)},modifier=Modifier.fillMaxWidth()){Text("Випадковий зародок")}
            }}
            Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Власний вибір",style=MaterialTheme.typography.titleMedium)
                Text("Обери серію. Колір та характер залишаться сюрпризом.")
                OutlinedButton(onClick={choose=!choose},modifier=Modifier.fillMaxWidth()){Text(if(choose)"Сховати серії" else "Обрати робота")}
                if(choose)RobotSeries.entries.forEach{series->
                    Card(onClick={persist(GenesisRules.create(series));choose=false;onSound(PixubiSound.TAP)},modifier=Modifier.fillMaxWidth()){
                        Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                            GenesisVessel(GenesisState(series,RobotSkin.CYAN,GenesisTrait.CALM,false,energy=60),phase,0f,Modifier.size(100.dp))
                            Column(Modifier.weight(1f)){
                                Text(series.title,style=MaterialTheme.typography.titleMedium)
                                Text(GenesisRules.vessel(series),style=MaterialTheme.typography.bodySmall)
                                Text(WorldCatalog.info(series).role,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }}
        }else{
            val reveal=s.born||!s.mystery
            Text(if(s.born)"MK-1 · Народження завершено" else if(reveal)GenesisRules.vessel(s.series) else "Невідомий цифровий зародок",style=MaterialTheme.typography.titleLarge)
            Card{
                if(s.born||opening&&hatch.value>.84f){
                    HabitatScene(s.series,phase,Modifier.fillMaxWidth().height(270.dp)){
                        RobotPreview(RobotView.FRONT,RobotAction.WAVE,RobotFace.HAPPY,RobotLoadout(s.skin.accent,RobotEquipment(),RobotTier.MK1,s.series),phase,false,Modifier.fillMaxSize())
                    }
                }else GenesisVessel(s,phase,if(opening)hatch.value else 0f,Modifier.fillMaxWidth().height(270.dp))
            }
            if(!s.born){
                Text("Енергія · ${s.energy}%")
                LinearProgressIndicator(progress={s.energy/100f},modifier=Modifier.fillMaxWidth())
                Text("Протоколи активації · ${Integer.bitCount(s.careMask)}/3")
                GenesisCare.entries.forEach{kind->
                    OutlinedButton(onClick={persist(s.care(kind));onSound(PixubiSound.TAP)},enabled=!opening&&(s.energy<100||(s.careMask and kind.bit)==0),modifier=Modifier.fillMaxWidth()){
                        Text(kind.title+" +${kind.amount}%"+if((s.careMask and kind.bit)!=0)" ✓" else "")
                    }
                }
                Button(onClick={opening=true},enabled=s.ready&&!opening,modifier=Modifier.fillMaxWidth()){Text(if(opening)"Народження…" else "Активувати народження")}
                Text(if(s.ready)"Ядро готове до народження!" else "Заряди ядро до 100% і виконай усі три протоколи активації.",style=MaterialTheme.typography.bodySmall)
            }else{
                Text(s.series.title+" · "+s.skin.title+if(s.rare)" · Рідкісний золотий" else "")
                Text("Вроджена риса: "+s.trait.title)
                OutlinedTextField(value=s.name,onValueChange={persist(s.rename(it))},label={Text("Ім’я робота")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Button(onClick={onAdopt(s)},enabled=s.name.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(if(world)"Увійти у світ PIXUBI" else "Перейти до Robot Lab")}
                Text(if(world)"Серія, колір, характер та ім’я будуть перенесені в основну гру без повторного розіграшу." else "Тестовий персонаж Robot Lab.",style=MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick={reset=true},enabled=!opening){Text(if(world)"Обрати інший зародок" else "Почати новий тест")}
        }
    }
    if(reset)AlertDialog(onDismissRequest={reset=false},title={Text("Скинути зародок?")},text={Text(if(world)"Поточне ядро і його прогрес активації буде скинуто. Активне збереження гри не зміниться." else "Буде видалено лише поточний тест GENESIS, не основне збереження.")},confirmButton={TextButton(onClick={persist(null);reset=false}){Text("Скинути")}},dismissButton={TextButton(onClick={reset=false}){Text("Скасувати")}})
}
