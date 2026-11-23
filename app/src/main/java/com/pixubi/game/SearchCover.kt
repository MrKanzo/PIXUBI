package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Procedural scenery drawn *in front* of GrowthSprite: bushes, boulders,
 * pond, sand tunnels and clouds are actual cover, not a disappearing-Pix trick.
 * Only visible while an expedition is running; no files or network needed.
 */
@Composable
fun SearchCover(
    species: Species,
    frame: SearchFrame,
    modifier: Modifier = Modifier,
    abovePet: Boolean = true
) {
    // Back layer: the scenery always exists. Front layer: it gradually covers
    // the pet during hiding, then gradually reveals the pet on emergence.
    Canvas(modifier.fillMaxSize().graphicsLayer {
        alpha = if (abovePet) frame.concealment.coerceIn(0f, 1f) else 1f
    }) {
        val ground = size.height * .84f
        val left = size.width * .5f + frame.entryXDp.dp.toPx()
        val right = size.width * .5f + frame.exitXDp.dp.toPx()
        val moving = size.width * .5f + frame.coverXDp.dp.toPx()
        val firstIsActive = abs(frame.coverXDp - frame.entryXDp) <=
            abs(frame.coverXDp - frame.exitXDp)
        val rustle = frame.rustleDp.dp.toPx() * frame.concealment
        val breeze = (frame.pulse - .5f) * 2.dp.toPx()

        when (species) {
            Species.LISYK -> {
                drawBush(left, ground, if (firstIsActive) rustle else breeze)
                drawBush(right, ground, if (!firstIsActive) rustle else breeze)
                if (frame.concealment > .1f) {
                    val leafX = if (firstIsActive) left else right
                    for (i in 0..3) {
                        val sign = if (i % 2 == 0) 1f else -1f
                        drawCircle(
                            if (i % 2 == 0) Color(0xFFB3E78E) else Color(0xFFF7D97C),
                            (2.5f + i % 2).dp.toPx(),
                            Offset(leafX + sign * (26 + i * 9).dp.toPx(),
                                ground - (45 + i * 12).dp.toPx() +
                                    frame.pulse * 8.dp.toPx())
                        )
                    }
                }
            }
            Species.ISKRYK -> {
                drawRock(left, ground, if (firstIsActive) rustle else 0f)
                drawRock(right, ground, if (!firstIsActive) rustle else 0f)
                if (frame.concealment > .06f) {
                    val sx = moving + 13.dp.toPx()
                    val sy = ground - 75.dp.toPx()
                    val spark = Path().apply {
                        moveTo(sx, sy)
                        lineTo(sx - 8.dp.toPx(), sy + 18.dp.toPx())
                        lineTo(sx + 1.dp.toPx(), sy + 18.dp.toPx())
                        lineTo(sx - 11.dp.toPx(), sy + 35.dp.toPx())
                    }
                    drawPath(spark, Color(0xFFFFF0A2).copy(alpha = .55f + frame.pulse * .45f),
                        style = Stroke(width = 3.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = frame.pulse * .75f),
                        4.dp.toPx(), Offset(sx + 14.dp.toPx(), sy - 6.dp.toPx()))
                }
            }
            Species.KRAPLYK -> {
                // A wide pond with animated ripples and rising bubbles.
                drawOval(
                    Color(0xFF124B8A), Offset(size.width * .5f - 105.dp.toPx(),
                        ground - 35.dp.toPx()),
                    Size(210.dp.toPx(), 70.dp.toPx())
                )
                drawOval(
                    Color(0xFF4AC0DE), Offset(size.width * .5f - 98.dp.toPx(),
                        ground - 35.dp.toPx()),
                    Size(196.dp.toPx(), 52.dp.toPx())
                )
                for (i in 0..3) {
                    val xx = size.width * .5f + (i * 47f - 74f).dp.toPx()
                    drawLine(Color(0xFFBDF5FA).copy(alpha = .6f), Offset(xx, ground - 17.dp.toPx()),
                        Offset(xx + 20.dp.toPx(), ground - 17.dp.toPx()), 2.dp.toPx())
                }
                val ripple = (15f + 24f * frame.pulse).dp.toPx()
                drawOval(
                    Color.White.copy(alpha = .45f + frame.pulse * .42f),
                    Offset(moving - ripple, ground - 19.dp.toPx() - ripple * .17f),
                    Size(ripple * 2f, ripple * .62f),
                    style = Stroke(width = 2.dp.toPx())
                )
                if (frame.concealment > .05f) {
                    for (i in 0..4) {
                        val drift = (i * 11f - 20f).dp.toPx()
                        val rise = (i * 13f + 13f * frame.pulse).dp.toPx()
                        drawCircle(Color(0xFFE2FCFF).copy(alpha = .63f + .25f * frame.pulse),
                            (3f + i % 3).dp.toPx(),
                            Offset(moving + drift, ground - rise - 12.dp.toPx()),
                            style = Stroke(1.5.dp.toPx()))
                    }
                }
            }
            Species.PISHCHANYK -> {
                val sand = Color(0xFFE5B77B)
                // Entrance and exit remain in place: only the raised mound travels.
                for (hole in listOf(left, right)) {
                    drawOval(Color(0xFFAD734A).copy(alpha = .7f),
                        Offset(hole - 29.dp.toPx(), ground - 13.dp.toPx()),
                        Size(58.dp.toPx(), 21.dp.toPx()))
                    drawArc(Color(0xFFFFE3AE), startAngle = 8f, sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(hole - 31.dp.toPx(), ground - 14.dp.toPx()),
                        size = Size(62.dp.toPx(), 25.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx()))
                }
                if (frame.concealment > .025f) {
                    val hump = frame.concealment
                    val w = (27f + 28f * hump).dp.toPx()
                    drawOval(Color(0xFFB88150).copy(alpha = .55f),
                        Offset(moving - w, ground - 11.dp.toPx()),
                        Size(w * 2f, 27.dp.toPx()))
                    drawOval(sand, Offset(moving - w, ground - (12f + 24f * hump).dp.toPx()),
                        Size(w * 2f, (21f + 30f * hump).dp.toPx()))
                    drawOval(Color(0xFFF8D69C).copy(alpha = .7f),
                        Offset(moving - w * .68f, ground - (11f + 25f * hump).dp.toPx()),
                        Size(w * 1.37f, 10.dp.toPx()))
                    for (i in 0..5) {
                        val side = if (i % 2 == 0) 1f else -1f
                        drawCircle(
                            if (i % 2 == 0) Color(0xFFFCE3B6) else Color(0xFFC98B52),
                            (2f + i % 3).dp.toPx(),
                            Offset(moving + side * (w + (i * 6).dp.toPx()),
                                ground - (10 + i * 7).dp.toPx() -
                                    frame.pulse * 6.dp.toPx())
                        )
                    }
                } else {
                    drawCircle(Color(0xFFF8D69C), 3.dp.toPx(),
                        Offset(moving, ground - 11.dp.toPx()))
                }
            }
            Species.KHMARYK -> {
                val y = ground - 126.dp.toPx()
                drawCloud(left, y, if (firstIsActive) rustle else breeze)
                drawCloud(right, y - 8.dp.toPx(),
                    if (!firstIsActive) rustle else breeze)
                if (frame.concealment > .05f) {
                    for (i in 0..3) {
                        val xx = moving + (i * 17f - 25f).dp.toPx()
                        val yy = y + (35f + i * 9f - 16f * frame.pulse).dp.toPx()
                        drawCircle(Color.White.copy(alpha = .42f + frame.pulse * .4f),
                            (3f + i).dp.toPx(), Offset(xx, yy))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBush(cx: Float, ground: Float, rustle: Float) {
    val x = cx + rustle
    // Dark stem and clustered, shaded leaves give the creature a real hiding place.
    drawLine(Color(0xFF725039),
        Offset(x, ground - 4.dp.toPx()), Offset(x, ground - 54.dp.toPx()),
        9.dp.toPx())
    drawOval(Color(0xFF174B3C), Offset(x - 58.dp.toPx(), ground - 75.dp.toPx()),
        Size(116.dp.toPx(), 79.dp.toPx()))
    val crowns = listOf(
        Triple(-39f, -60f, 28f), Triple(-16f, -83f, 34f),
        Triple(18f, -79f, 32f), Triple(41f, -52f, 27f),
        Triple(-4f, -48f, 34f)
    )
    crowns.forEachIndexed { i, (dx, dy, rad) ->
        drawCircle(
            if (i % 2 == 0) Color(0xFF31865B) else Color(0xFF49A96A),
            rad.dp.toPx(), Offset(x + dx.dp.toPx(), ground + dy.dp.toPx())
        )
    }
    for (i in 0..4) {
        drawCircle(Color(0xFFA5DF81).copy(alpha = .55f), 4.dp.toPx(),
            Offset(x + (i * 19 - 39).dp.toPx(),
                ground - (65 + (i % 2) * 24).dp.toPx()))
    }
}

private fun DrawScope.drawRock(cx: Float, ground: Float, rustle: Float) {
    val x = cx + rustle * .30f
    val rock = Path().apply {
        moveTo(x - 54.dp.toPx(), ground + 3.dp.toPx())
        lineTo(x - 51.dp.toPx(), ground - 49.dp.toPx())
        lineTo(x - 24.dp.toPx(), ground - 79.dp.toPx())
        lineTo(x + 18.dp.toPx(), ground - 87.dp.toPx())
        lineTo(x + 54.dp.toPx(), ground - 48.dp.toPx())
        lineTo(x + 57.dp.toPx(), ground + 3.dp.toPx())
        close()
    }
    drawPath(rock, Color(0xFF354563))
    val bright = Path().apply {
        moveTo(x - 51.dp.toPx(), ground - 49.dp.toPx())
        lineTo(x - 24.dp.toPx(), ground - 79.dp.toPx())
        lineTo(x + 18.dp.toPx(), ground - 87.dp.toPx())
        lineTo(x + 4.dp.toPx(), ground - 33.dp.toPx())
        lineTo(x - 47.dp.toPx(), ground + 3.dp.toPx())
        close()
    }
    drawPath(bright, Color(0xFF8298B3))
    drawLine(Color(0xFFC8D6E7).copy(alpha = .5f),
        Offset(x - 20.dp.toPx(), ground - 67.dp.toPx()),
        Offset(x - 3.dp.toPx(), ground - 78.dp.toPx()), 3.dp.toPx())
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, rustle: Float) {
    val x = cx + rustle
    drawOval(Color(0xFF9BB6E7).copy(alpha = .75f),
        Offset(x - 65.dp.toPx(), cy - 8.dp.toPx()),
        Size(130.dp.toPx(), 62.dp.toPx()))
    drawCircle(Color(0xFFE8F6FF), 37.dp.toPx(), Offset(x - 27.dp.toPx(), cy))
    drawCircle(Color(0xFFF8FDFF), 44.dp.toPx(), Offset(x + 12.dp.toPx(), cy - 14.dp.toPx()))
    drawCircle(Color(0xFFD8EDFF), 34.dp.toPx(), Offset(x + 42.dp.toPx(), cy + 9.dp.toPx()))
    drawOval(Color(0xFFF4FAFF),
        Offset(x - 57.dp.toPx(), cy + 4.dp.toPx()),
        Size(112.dp.toPx(), 40.dp.toPx()))
}
