package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

enum class PixuEyeShape { OPEN, HAPPY, SAD, TIRED, WIDE, CLOSED }
enum class PixuMouthShape { SMILE, GRIN, FROWN, O, CHEW, SIP, YAWN, REST }

data class PixuExpression(val eyes: PixuEyeShape, val mouth: PixuMouthShape)

/** Testable emotion-to-face contract. Existing atlas stays unchanged. */
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

/** Facial features share one normalized coordinate system across juvenile and adult sprites. */
@Composable
fun PetFaceOverlay(
    species: Species,
    sex: Sex,
    coatIndex: Int,
    emotion: PetEmotion,
    blink: Boolean,
    chewOpen: Boolean,
    phase: Float = 0f,
    modifier: Modifier = Modifier
) {
    val expression = PixuFaces.expression(emotion, blink)
    val coats = listOf(
        Color(0xFF79D5BD), Color(0xFFFFAB77), Color(0xFFB9A5FF),
        Color(0xFFFF98C4), Color(0xFF7FD0FB), Color(0xFFFFD475)
    )
    val coat = coats[coatIndex.coerceIn(0, 5)]
    Canvas(modifier.fillMaxSize()) {
        val u = size.minDimension
        val eyeY = size.height * when (species) {
            Species.ISKRYK, Species.LISYK -> .445f
            Species.KRAPLYK -> .45f
            Species.KHMARYK -> .46f
            Species.PISHCHANYK -> .475f
        }
        val mouthY = eyeY + u * .115f
        val pupilShift = if (emotion == PetEmotion.IDLE || emotion == PetEmotion.SURPRISED)
            sin(phase.toDouble() * 2.0 * PI).toFloat() * u * .006f else 0f
        val eye1 = size.width * .43f
        val eye2 = size.width * .62f
        val mouthX = size.width * .525f
        val ink = Color(0xFF1A2A42)
        val pupil = Color(0xFF0D1C31)
        val shine = Color(0xFFF1FDFF)

        fun eye(x: Float) {
            // A soft-color eyelid conceals the old atlas pupils before the
            // expression is repainted, so blinking never creates four eyes.
            drawRoundRect(
                coat.copy(alpha = .97f),
                topLeft = Offset(x - u * .055f, eyeY - u * .052f),
                size = Size(u * .11f, u * .107f),
                cornerRadius = CornerRadius(u * .027f)
            )
            when (expression.eyes) {
                PixuEyeShape.CLOSED -> {
                    drawLine(ink, Offset(x - u * .043f, eyeY),
                        Offset(x + u * .043f, eyeY), strokeWidth = u * .017f)
                    drawLine(ink.copy(alpha = .50f),
                        Offset(x - u * .029f, eyeY + u * .010f),
                        Offset(x + u * .027f, eyeY + u * .010f),
                        strokeWidth = u * .006f)
                }
                PixuEyeShape.HAPPY -> {
                    drawArc(ink, 180f, 180f, false,
                        topLeft = Offset(x - u * .045f, eyeY - u * .027f),
                        size = Size(u * .09f, u * .073f),
                        style = Stroke(width = u * .017f))
                }
                PixuEyeShape.TIRED -> {
                    drawOval(ink, Offset(x - u * .032f, eyeY - u * .010f),
                        Size(u * .064f, u * .026f))
                    drawLine(coat, Offset(x - u * .05f, eyeY - u * .012f),
                        Offset(x + u * .05f, eyeY - u * .012f),
                        strokeWidth = u * .018f)
                }
                PixuEyeShape.SAD -> {
                    drawOval(ink, Offset(x - u * .030f, eyeY - u * .025f),
                        Size(u * .060f, u * .066f))
                    drawCircle(shine, u * .010f, Offset(x - u * .011f, eyeY - u * .011f))
                    val dir = if (x < size.width * .5f) -1f else 1f
                    drawLine(ink, Offset(x - u * .04f, eyeY - u * (.055f + dir * .008f)),
                        Offset(x + u * .04f, eyeY - u * (.055f - dir * .008f)),
                        strokeWidth = u * .012f)
                }
                PixuEyeShape.WIDE -> {
                    drawOval(shine, Offset(x - u * .037f, eyeY - u * .047f),
                        Size(u * .074f, u * .093f))
                    drawOval(ink, Offset(x - u * .025f, eyeY - u * .035f),
                        Size(u * .05f, u * .072f))
                    drawCircle(Color.White, u * .009f, Offset(x - u * .011f, eyeY - u * .014f))
                }
                PixuEyeShape.OPEN -> {
                    drawOval(ink, Offset(x - u * .032f, eyeY - u * .038f),
                        Size(u * .064f, u * .078f))
                    drawOval(pupil, Offset(x - u * .024f + pupilShift, eyeY - u * .026f),
                        Size(u * .048f, u * .054f))
                    drawCircle(shine, u * .012f,
                        Offset(x - u * .011f + pupilShift, eyeY - u * .016f))
                    drawCircle(shine.copy(alpha = .60f), u * .006f,
                        Offset(x + u * .012f + pupilShift, eyeY + u * .017f))
                }
            }
            if (sex == Sex.GIRL && expression.eyes !in
                listOf(PixuEyeShape.CLOSED, PixuEyeShape.TIRED)) {
                val dir = if (x < size.width * .5f) -1f else 1f
                drawLine(ink, Offset(x + dir * u * .032f, eyeY - u * .022f),
                    Offset(x + dir * u * .065f, eyeY - u * .049f),
                    strokeWidth = u * .010f)
            }
        }
        eye(eye1)
        eye(eye2)

        // Mouth cover is intentionally much smaller than the face, preserving
        // the original character's markings, cheeks, ears, hair and armor.
        drawRoundRect(coat.copy(alpha = .97f),
            topLeft = Offset(mouthX - u * .067f, mouthY - u * .032f),
            size = Size(u * .134f, u * .090f),
            cornerRadius = CornerRadius(u * .026f))
        val stroke = Stroke(width = u * .014f)
        when (expression.mouth) {
            PixuMouthShape.SMILE ->
                drawArc(ink, 0f, 165f, false,
                    topLeft = Offset(mouthX - u * .037f, mouthY - u * .015f),
                    size = Size(u * .074f, u * .048f), style = stroke)
            PixuMouthShape.GRIN -> {
                drawArc(ink, 0f, 180f, false,
                    topLeft = Offset(mouthX - u * .051f, mouthY - u * .025f),
                    size = Size(u * .102f, u * .085f), style = Stroke(width = u * .019f))
                drawOval(Color(0xFFFC91AB), Offset(mouthX - u * .018f, mouthY + u * .019f),
                    Size(u * .036f, u * .024f))
            }
            PixuMouthShape.FROWN ->
                drawArc(ink, 180f, 180f, false,
                    topLeft = Offset(mouthX - u * .044f, mouthY + u * .003f),
                    size = Size(u * .088f, u * .052f), style = stroke)
            PixuMouthShape.O -> drawOval(ink,
                Offset(mouthX - u * .022f, mouthY - u * .014f),
                Size(u * .044f, u * .061f))
            PixuMouthShape.CHEW -> {
                if (chewOpen) {
                    drawOval(ink, Offset(mouthX - u * .032f, mouthY - u * .010f),
                        Size(u * .064f, u * .050f))
                    drawOval(Color(0xFFFF9BAB), Offset(mouthX - u * .020f, mouthY + u * .016f),
                        Size(u * .040f, u * .014f))
                } else {
                    drawLine(ink, Offset(mouthX - u * .035f, mouthY + u * .020f),
                        Offset(mouthX + u * .035f, mouthY + u * .020f), u * .013f)
                }
            }
            PixuMouthShape.SIP -> {
                drawOval(ink, Offset(mouthX - u * .013f, mouthY + u * .009f),
                    Size(u * .026f, u * .038f))
                drawCircle(shine.copy(alpha = .75f), u * .010f,
                    Offset(mouthX + u * .042f, mouthY + u * .012f))
            }
            PixuMouthShape.YAWN ->
                drawOval(ink, Offset(mouthX - u * .029f, mouthY - u * .012f),
                    Size(u * .058f, u * .067f))
            PixuMouthShape.REST ->
                drawLine(ink, Offset(mouthX - u * .027f, mouthY + u * .013f),
                    Offset(mouthX + u * .027f, mouthY + u * .013f), u * .011f)
        }
        if (expression.eyes == PixuEyeShape.HAPPY) {
            drawCircle(Color(0xFFFF7EAF).copy(alpha = .66f),
                u * .019f, Offset(eye1 - u * .075f, eyeY + u * .10f))
            drawCircle(Color(0xFFFF7EAF).copy(alpha = .66f),
                u * .019f, Offset(eye2 + u * .075f, eyeY + u * .10f))
        }
    }
}
