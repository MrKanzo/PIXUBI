package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

enum class PixuEyeShape { OPEN, HAPPY, SAD, TIRED, WIDE, CLOSED }
enum class PixuMouthShape { SMILE, GRIN, FROWN, O, CHEW, SIP, YAWN, REST }

data class PixuExpression(val eyes: PixuEyeShape, val mouth: PixuMouthShape)

object PixuFaces {
    fun expression(emotion: PetEmotion, blink: Boolean = false): PixuExpression {
        val face = when (emotion) {
            PetEmotion.HAPPY -> PixuExpression(PixuEyeShape.HAPPY, PixuMouthShape.GRIN)
            PetEmotion.SAD -> PixuExpression(PixuEyeShape.SAD, PixuMouthShape.FROWN)
            PetEmotion.TIRED -> PixuExpression(PixuEyeShape.TIRED, PixuMouthShape.YAWN)
            PetEmotion.SURPRISED -> PixuExpression(PixuEyeShape.WIDE, PixuMouthShape.O)
            PetEmotion.EATING -> PixuExpression(PixuEyeShape.OPEN, PixuMouthShape.CHEW)
            PetEmotion.DRINKING -> PixuExpression(PixuEyeShape.OPEN, PixuMouthShape.SIP)
            PetEmotion.SLEEPING, PetEmotion.HIBERNATING ->
                PixuExpression(PixuEyeShape.CLOSED, PixuMouthShape.REST)
            PetEmotion.IDLE -> PixuExpression(PixuEyeShape.OPEN, PixuMouthShape.SMILE)
        }
        return if (blink) face.copy(eyes = PixuEyeShape.CLOSED) else face
    }
}

/**
 * Surgical facial accents, not a second face.
 *
 * The sprite atlases already have carefully illustrated eyes, noses and mouths.
 * In 0.13 we painted large flat-color blocks on top of them: that destroyed the
 * pixel-art shading and doubled the face. We DO NOT cover any existing pixels now.
 * Eyelids, tiny mouth movement and cheeks are transparent, drawn around the
 * original details, with coordinates adjusted for each evolution stage.
 */
@Composable
fun PetFaceOverlay(
    species: Species,
    sex: Sex,
    coatIndex: Int,
    emotion: PetEmotion,
    blink: Boolean,
    chewOpen: Boolean,
    phase: Float = 0f,
    modifier: Modifier = Modifier,
    stage: GrowthStage = GrowthStage.ADULT
) {
    val expression = PixuFaces.expression(emotion, blink)
    Canvas(modifier.fillMaxSize()) {
        val u = size.minDimension
        // Young forms have a different portrait layout to the adult 160px atlas.
        val faceY = when (stage) {
            GrowthStage.BABY -> .48f
            GrowthStage.YOUNG -> .43f
            else -> when (species) {
                Species.PISHCHANYK -> .475f
                Species.KHMARYK -> .46f
                Species.KRAPLYK -> .45f
                else -> .445f
            }
        }
        val left = size.width * when (stage) {
            GrowthStage.YOUNG -> .46f
            GrowthStage.BABY -> .41f
            else -> .43f
        }
        val right = size.width * when (stage) {
            GrowthStage.YOUNG -> .61f
            GrowthStage.BABY -> .615f
            else -> .62f
        }
        val ey = size.height * faceY
        val mid = (left + right) / 2f
        val mouthY = ey + u * when (stage) {
            GrowthStage.BABY -> .105f
            GrowthStage.YOUNG -> .076f
            else -> .115f
        }
        val s = when (stage) {
            GrowthStage.BABY -> 1f
            GrowthStage.YOUNG -> .78f
            else -> .86f
        }
        val ink = Color(0xFF23314D)
        val soft = ink.copy(alpha = .66f)
        val line = u * .008f * s
        val wave = sin(phase.toDouble() * 2 * PI).toFloat()

        // On a neutral face we keep the ORIGINAL artwork completely unchanged.
        // Periodic blink is two delicate eyelid strokes, never opaque rectangles.
        if (expression.eyes == PixuEyeShape.CLOSED) {
            for (x in listOf(left, right)) {
                drawLine(soft, Offset(x - u * .035f * s, ey),
                    Offset(x + u * .035f * s, ey), strokeWidth = line * 1.4f)
            }
        } else when (expression.eyes) {
            PixuEyeShape.HAPPY -> {
                // Small upper eyelid smile; pupils remain visible underneath.
                for (x in listOf(left, right)) {
                    drawArc(soft, startAngle = 188f, sweepAngle = 164f,
                        useCenter = false,
                        topLeft = Offset(x - u * .038f * s, ey - u * .028f * s),
                        size = Size(u * .076f * s, u * .062f * s),
                        style = Stroke(line))
                }
                drawCircle(Color(0xFFF28FA8).copy(alpha = .40f),
                    u * .017f * s, Offset(left - u * .064f * s, ey + u * .069f * s))
                drawCircle(Color(0xFFF28FA8).copy(alpha = .40f),
                    u * .017f * s, Offset(right + u * .064f * s, ey + u * .069f * s))
            }
            PixuEyeShape.SAD -> {
                drawLine(soft,
                    Offset(left - u * .030f * s, ey - u * .064f * s),
                    Offset(left + u * .029f * s, ey - u * .049f * s), line)
                drawLine(soft,
                    Offset(right - u * .029f * s, ey - u * .049f * s),
                    Offset(right + u * .030f * s, ey - u * .064f * s), line)
                drawCircle(Color(0xFF9EE6FF).copy(alpha = .75f),
                    u * .012f * s, Offset(left - u * .032f * s, ey + u * .061f * s))
            }
            PixuEyeShape.TIRED -> {
                for (x in listOf(left, right)) {
                    drawLine(ink.copy(alpha = .50f),
                        Offset(x - u * .029f * s, ey - u * .004f * s),
                        Offset(x + u * .029f * s, ey + u * .009f * s),
                        line * .9f)
                }
            }
            PixuEyeShape.WIDE -> {
                for (x in listOf(left, right)) {
                    drawArc(soft, 198f, 146f, false,
                        Offset(x - u * .040f * s, ey - u * .068f * s),
                        Size(u * .080f * s, u * .043f * s), style = Stroke(line))
                }
            }
            PixuEyeShape.OPEN -> {
                // Tiny eye glimmer / looking around, never replacing the pupil.
                if (phase > .22f && phase < .28f) {
                    drawCircle(Color.White.copy(alpha = .65f), u * .005f * s,
                        Offset(left + wave * u * .006f * s, ey - u * .008f * s))
                }
            }
            PixuEyeShape.CLOSED -> Unit
        }

        // Expression accents over the *existing* mouth, never rectangular patches.
        // Idle/sleep leaves its original shape untouched.
        when (expression.mouth) {
            PixuMouthShape.GRIN -> {
                drawArc(soft.copy(alpha = .5f), 15f, 150f, false,
                    Offset(mid - u * .030f * s, mouthY - u * .007f * s),
                    Size(u * .06f * s, u * .035f * s), style = Stroke(line))
            }
            PixuMouthShape.FROWN -> {
                drawArc(soft, 192f, 155f, false,
                    Offset(mid - u * .021f * s, mouthY + u * .011f * s),
                    Size(u * .042f * s, u * .028f * s), style = Stroke(line))
            }
            PixuMouthShape.O -> {
                drawOval(ink.copy(alpha = .85f),
                    Offset(mid - u * .010f * s, mouthY),
                    Size(u * .020f * s, u * .024f * s))
            }
            PixuMouthShape.CHEW -> {
                if (chewOpen) {
                    drawOval(soft, Offset(mid - u * .014f * s, mouthY + u * .002f * s),
                        Size(u * .028f * s, u * .018f * s))
                }
            }
            PixuMouthShape.SIP -> {
                drawCircle(Color(0xFFBFF6FD).copy(alpha = .75f), u * .010f * s,
                    Offset(mid + u * .044f * s, mouthY - u * .005f * s))
            }
            PixuMouthShape.YAWN -> {
                drawOval(ink.copy(alpha = .72f),
                    Offset(mid - u * .013f * s, mouthY - u * .003f * s),
                    Size(u * .026f * s, u * .030f * s))
            }
            PixuMouthShape.REST, PixuMouthShape.SMILE -> Unit
        }

        // Gender features already exist in the original sprites / GrowthSprite;
        // don't redraw lashes or cheeks on top of them.
    }
}
