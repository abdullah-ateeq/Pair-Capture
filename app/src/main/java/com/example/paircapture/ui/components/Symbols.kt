package com.example.paircapture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.paircapture.game.model.PairSymbol
import kotlin.math.cos
import kotlin.math.sin

/**
 * Original geometric symbol set. Every symbol has a distinct silhouette so they read
 * clearly in grayscale; color is only a secondary cue.
 */
@Composable
fun PairSymbolIcon(symbol: PairSymbol, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawPairSymbol(symbol, color) }
}

fun DrawScope.drawPairSymbol(symbol: PairSymbol, color: Color) {
    val r = size.minDimension * 0.4f
    val c = center
    when (symbol) {
        PairSymbol.CIRCLE -> drawCircle(color, radius = r * 0.88f, center = c)

        PairSymbol.TRIANGLE -> drawPath(
            regularPolygon(Offset(c.x, c.y + r * 0.14f), r * 1.02f, sides = 3, startDegrees = -90f),
            color
        )

        PairSymbol.SQUARE -> {
            val side = r * 1.5f
            drawRoundRect(
                color,
                topLeft = Offset(c.x - side / 2, c.y - side / 2),
                size = Size(side, side),
                cornerRadius = CornerRadius(r * 0.16f)
            )
        }

        PairSymbol.STAR -> drawPath(star(Offset(c.x, c.y + r * 0.06f), r * 1.02f, r * 0.44f), color)

        PairSymbol.DIAMOND -> drawPath(
            Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.68f, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r * 0.68f, c.y)
                close()
            },
            color
        )

        PairSymbol.HEXAGON -> drawPath(regularPolygon(c, r * 0.95f, sides = 6, startDegrees = 0f), color)

        PairSymbol.MOON -> {
            val disc = Path().apply { addOval(Rect(c, r * 0.92f)) }
            val bite = Path().apply {
                addOval(Rect(Offset(c.x + r * 0.42f, c.y - r * 0.28f), r * 0.78f))
            }
            drawPath(Path.combine(PathOperation.Difference, disc, bite), color)
        }

        PairSymbol.BOLT -> {
            val left = c.x - r * 0.75f
            val top = c.y - r
            val w = r * 1.5f
            val h = r * 2f
            val bolt = Path().apply {
                moveTo(left + w * 0.62f, top)
                lineTo(left + w * 0.08f, top + h * 0.56f)
                lineTo(left + w * 0.44f, top + h * 0.56f)
                lineTo(left + w * 0.32f, top + h)
                lineTo(left + w * 0.92f, top + h * 0.40f)
                lineTo(left + w * 0.56f, top + h * 0.40f)
                lineTo(left + w * 0.74f, top)
                close()
            }
            drawPath(bolt, color)
        }
    }
}

/** The tile-back motif: two interlocking diamonds, one outlined, one solid. */
fun DrawScope.drawCaptureMark(color: Color, strokeFraction: Float = 0.06f) {
    val r = size.minDimension * 0.24f
    val offset = r * 0.42f
    val stroke = size.minDimension * strokeFraction
    drawPath(diamond(Offset(center.x - offset, center.y), r), color)
    drawPath(
        diamond(Offset(center.x + offset, center.y), r),
        color,
        style = Stroke(width = stroke, join = StrokeJoin.Round)
    )
}

/** Player ownership mark (check). */
fun DrawScope.drawCheckMark(color: Color) {
    val s = size.minDimension
    val path = Path().apply {
        moveTo(center.x - s * 0.3f, center.y + s * 0.02f)
        lineTo(center.x - s * 0.08f, center.y + s * 0.24f)
        lineTo(center.x + s * 0.32f, center.y - s * 0.22f)
    }
    drawPath(path, color, style = Stroke(width = s * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** CPU ownership mark (solid dot inside a ring). */
fun DrawScope.drawCpuMark(color: Color) {
    val s = size.minDimension
    drawCircle(color, radius = s * 0.36f, style = Stroke(width = s * 0.09f))
    drawCircle(color, radius = s * 0.17f)
}

private fun diamond(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y - r)
    lineTo(c.x + r, c.y)
    lineTo(c.x, c.y + r)
    lineTo(c.x - r, c.y)
    close()
}

private fun regularPolygon(c: Offset, radius: Float, sides: Int, startDegrees: Float) = Path().apply {
    for (i in 0 until sides) {
        val angle = Math.toRadians((startDegrees + i * 360f / sides).toDouble())
        val x = c.x + radius * cos(angle).toFloat()
        val y = c.y + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun star(c: Offset, outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians((-90.0 + i * 36.0))
        val x = c.x + radius * cos(angle).toFloat()
        val y = c.y + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
