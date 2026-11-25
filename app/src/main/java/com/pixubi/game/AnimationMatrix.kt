package com.pixubi.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Screen-recordable simultaneous preview. Uses the real shared game renderer. */
@Composable
internal fun AnimationMatrix(spec:DevSpec,seconds:Float,left:Boolean,modifier:Modifier=Modifier) {
    val actions=listOf(DevAction.IDLE,DevAction.BLINK,DevAction.WALK,DevAction.HOP,
        DevAction.HAPPY,DevAction.SAD,DevAction.SURPRISE,DevAction.TIRED,DevAction.EAT,
        DevAction.DRINK,DevAction.SLEEP,DevAction.HIBERNATE,DevAction.DIG,DevAction.SWIM,DevAction.FLY)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(4.dp)) {
        actions.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                row.forEach { action ->
                    val emotion=action.emotion?:PetEmotion.IDLE
                    val motion=action.motion?:PetMotion.IDLE
                    val clip=SpriteTimeline.clip(motion,emotion)
                    val supported=PixuAnimationPolicy.supports(spec.species,spec.stage,clip)
                    val scene=DevScene(action,seconds,left)
                    Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Color(0xFF263449))) {
                        Column(Modifier.fillMaxWidth().padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(action.title,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center,
                                color=Color.White,maxLines=2)
                            Box(Modifier.fillMaxWidth().height(88.dp),contentAlignment=Alignment.Center) {
                                if(supported) {
                                    val phase=scene.movementCycle
                                    val y=when(action) {
                                        DevAction.HOP -> PixuAnimationPolicy.hopOffset(spec.species,spec.stage,phase)*.4f
                                        DevAction.HAPPY,DevAction.SURPRISE -> MotionResolver.pose(motion,phase).yDp*.4f
                                        DevAction.FLY -> -8f+MotionResolver.pose(motion,phase).yDp*.4f
                                        else -> 0f
                                    }
                                    val x=if(action==DevAction.WALK)(if(left)-1f else 1f)*
                                        12f*kotlin.math.sin(kotlin.math.PI*phase).toFloat() else 0f
                                    FramePetSprite(spec.species,spec.stage,spec.sex,spec.color,emotion,motion,phase,
                                        false,if(action==DevAction.BLINK)NativeEyeMotion.blinkAt((seconds%1.2f)/1.2f)
                                        else NativeEyeMotion.blinkAt(scene.blinkCycle),
                                        Modifier.offset(x=x.dp,y=y.dp).size(78.dp),
                                        if(action==DevAction.WALK&&phase>.5f)!left else left)
                                } else Text("Не властиво виду",style=MaterialTheme.typography.labelSmall,
                                    textAlign=TextAlign.Center,color=Color(0xFFBFCFE9))
                            }
                        }
                    }
                }
            }
        }
    }
}
