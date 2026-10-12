package com.example.paircapture.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.paircapture.ui.components.drawCheckMark
import com.example.paircapture.ui.theme.PairCaptureTheme
import kotlinx.coroutines.delay

private const val SPLASH_MS = 1300

/**
 * Grid appears → two tiles flip to a decorative ring → the pair is claimed → title fades in.
 * The ring motif is decorative only and is not one of the game symbols.
 */
@Composable
fun SplashScreen(animations: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(if (animations) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animations) {
            progress.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing))
            delay(150)
        } else {
            delay(400)
        }
        onFinished()
    }

    val colors = PairCaptureTheme.gameColors
    val scheme = MaterialTheme.colorScheme
    val pairCells = setOf(5, 10)

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(168.dp)) {
                val p = progress.value
                val gap = size.minDimension * 0.05f
                val cell = (size.minDimension - gap * 3) / 4
                for (index in 0 until 16) {
                    val row = index / 4
                    val col = index % 4
                    // Stage 1 (0–0.35): tiles pop in, staggered by diagonal.
                    val appear = ((p - (row + col) * 0.03f) / 0.15f).coerceIn(0f, 1f)
                    if (appear <= 0f) continue
                    val topLeft = Offset(col * (cell + gap), row * (cell + gap))
                    val isPair = index in pairCells
                    // Stage 2 (0.38–0.6): pair flips. Stage 3 (0.62–0.8): pair claimed.
                    val flip = if (isPair) ((p - 0.38f) / 0.22f).coerceIn(0f, 1f) else 0f
                    val claimed = isPair && p >= 0.64f
                    val widthScale = if (isPair) kotlin.math.abs(1f - 2f * flip) else 1f
                    val faceUp = flip >= 0.5f
                    scale(appear * widthScale, appear, pivot = topLeft + Offset(cell / 2, cell / 2)) {
                        drawRoundRect(
                            color = when {
                                claimed -> colors.playerContainer
                                faceUp -> colors.tileFront
                                else -> colors.tileBack
                            },
                            topLeft = topLeft,
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(cell * 0.22f)
                        )
                        if (faceUp) {
                            inset(
                                left = topLeft.x,
                                top = topLeft.y,
                                right = size.width - topLeft.x - cell,
                                bottom = size.height - topLeft.y - cell
                            ) {
                                if (claimed) {
                                    drawCheckMark(colors.player)
                                } else {
                                    drawCircle(colors.player, radius = cell * 0.26f, style = Stroke(cell * 0.09f))
                                    drawCircle(colors.player, radius = cell * 0.08f)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            val titleAlpha = ((progress.value - 0.7f) / 0.3f).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = (1f - titleAlpha) * 24f
                }
            ) {
                Text("PAIR", style = MaterialTheme.typography.displayMedium, color = scheme.onBackground)
                Text("CAPTURE", style = MaterialTheme.typography.displayMedium, color = colors.player)
            }
        }
    }
}
