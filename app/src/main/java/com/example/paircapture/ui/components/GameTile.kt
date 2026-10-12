package com.example.paircapture.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.example.paircapture.game.FeedbackKind
import com.example.paircapture.game.TileFeedback
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.TileState
import com.example.paircapture.game.model.VisibleTile
import com.example.paircapture.game.model.accessibilityDescription
import com.example.paircapture.ui.theme.PairCaptureTheme

private val TileShape = RoundedCornerShape(16.dp)
private const val FLIP_MS = 340

@Composable
fun GameTile(
    tile: VisibleTile,
    feedback: TileFeedback?,
    selectedBy: PlayerSide?,
    enabled: Boolean,
    animations: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PairCaptureTheme.gameColors
    val density = LocalDensity.current
    val faceUp = tile.state == TileState.REVEALED

    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(if (animations) FLIP_MS else 0, easing = FastOutSlowInEasing),
        label = "flip"
    )

    // Keep the last revealed symbol only so the face can finish flipping away; it is never
    // exposed through semantics, and is cleared once the tile is fully face down.
    val lastSymbol = remember { arrayOfNulls<PairSymbol>(1) }
    if (tile.symbol != null) lastSymbol[0] = tile.symbol
    if (tile.state != TileState.REVEALED && rotation == 0f) lastSymbol[0] = null
    val flippingSymbol = lastSymbol[0]

    val isMatch = feedback?.kind == FeedbackKind.MATCH && tile.id in feedback.tileIds
    val isMismatch = feedback?.kind == FeedbackKind.MISMATCH && tile.id in feedback.tileIds

    val shake = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(isMismatch, animations) {
        if (isMismatch && animations) {
            shake.animateTo(0f, keyframes {
                durationMillis = 320
                -10f at 50
                9f at 110
                -6f at 170
                4f at 230
                0f at 320
            })
        }
    }
    LaunchedEffect(isMatch, animations) {
        if (isMatch && animations) {
            pulse.animateTo(1.09f, tween(160))
            pulse.animateTo(1f, spring(dampingRatio = 0.45f))
        }
    }
    val fade by animateFloatAsState(
        targetValue = if (isMismatch) 0.72f else 1f,
        animationSpec = tween(if (animations) 220 else 0),
        label = "fade"
    )

    val captured = tile.state.isCaptured
    val clickable = enabled && tile.state == TileState.HIDDEN
    val description = tile.accessibilityDescription()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                translationX = shake.value * density.density
                scaleX = pulse.value
                scaleY = pulse.value
                alpha = fade
            }
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (clickable) onClick { onClick(); true } else disabled()
            }
            .clickable(enabled = clickable, onClick = onClick)
    ) {
        if (captured) {
            CapturedFace(
                owner = if (tile.state == TileState.CAPTURED_BY_PLAYER) PlayerSide.PLAYER else PlayerSide.CPU,
                animations = animations
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density.density
                    }
            ) {
                val showFront = rotation > 90f
                if (showFront) {
                    val highlight = when {
                        isMatch -> if (feedback.side == PlayerSide.PLAYER) colors.player else colors.cpu
                        selectedBy == PlayerSide.PLAYER -> colors.player
                        selectedBy == PlayerSide.CPU -> colors.cpu
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    TileFront(
                        symbol = flippingSymbol,
                        highlight = highlight,
                        symbolScale = if (isMatch) pulse.value else 1f,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                } else {
                    TileBack()
                }
            }
        }
    }
}

@Composable
private fun TileBack() {
    val colors = PairCaptureTheme.gameColors
    Box(
        Modifier
            .fillMaxSize()
            .shadow(2.dp, TileShape)
            .clip(TileShape)
            .background(colors.tileBack)
            .border(1.dp, colors.tileBackMark.copy(alpha = 0.12f), TileShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize(0.62f)) {
            drawCaptureMark(colors.tileBackMark.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun TileFront(
    symbol: PairSymbol?,
    highlight: androidx.compose.ui.graphics.Color,
    symbolScale: Float,
    modifier: Modifier = Modifier
) {
    val colors = PairCaptureTheme.gameColors
    Box(
        modifier
            .fillMaxSize()
            .shadow(3.dp, TileShape)
            .clip(TileShape)
            .background(colors.tileFront)
            .border(2.dp, highlight, TileShape),
        contentAlignment = Alignment.Center
    ) {
        if (symbol != null) {
            PairSymbolIcon(
                symbol = symbol,
                color = colors.ink(symbol),
                modifier = Modifier
                    .fillMaxSize(0.66f)
                    .graphicsLayer {
                        scaleX = symbolScale * symbolScale
                        scaleY = symbolScale * symbolScale
                    }
            )
        }
    }
}

@Composable
private fun CapturedFace(owner: PlayerSide, animations: Boolean) {
    val colors = PairCaptureTheme.gameColors
    val appear = remember { Animatable(if (animations) 0.6f else 1f) }
    LaunchedEffect(Unit) {
        if (animations) appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
    }
    val container = if (owner == PlayerSide.PLAYER) colors.playerContainer else colors.cpuContainer
    val ink = if (owner == PlayerSide.PLAYER) colors.player else colors.cpu
    val onContainer = if (owner == PlayerSide.PLAYER) colors.onPlayerContainer else colors.onCpuContainer
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                alpha = ((appear.value - 0.6f) / 0.4f).coerceIn(0f, 1f) * 0.6f + 0.4f
            }
            .clip(TileShape)
            .background(container.copy(alpha = 0.75f))
            .border(1.dp, ink.copy(alpha = 0.35f), TileShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(Modifier.size(22.dp)) {
                if (owner == PlayerSide.PLAYER) drawCheckMark(ink) else drawCpuMark(ink)
            }
            Text(
                text = if (owner == PlayerSide.PLAYER) "YOU" else "CPU",
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
