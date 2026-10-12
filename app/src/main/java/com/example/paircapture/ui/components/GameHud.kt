package com.example.paircapture.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.TOTAL_PAIRS
import com.example.paircapture.ui.theme.PairCaptureTheme

/** PLAYER  4   ·  PAIRS LEFT 1  ·   3  CPU */
@Composable
fun ScoreBoard(
    playerPairs: Int,
    cpuPairs: Int,
    pairsRemaining: Int,
    currentSide: PlayerSide,
    animations: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = PairCaptureTheme.gameColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreCell(
                label = "PLAYER",
                value = playerPairs,
                accent = colors.player,
                active = currentSide == PlayerSide.PLAYER,
                animations = animations,
                modifier = Modifier.weight(1f)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text("PAIRS LEFT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedNumber(pairsRemaining, animations, MaterialTheme.typography.titleLarge.color, small = true)
            }
            ScoreCell(
                label = "CPU",
                value = cpuPairs,
                accent = colors.cpu,
                active = currentSide == PlayerSide.CPU,
                animations = animations,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreCell(
    label: String,
    value: Int,
    accent: Color,
    active: Boolean,
    animations: Boolean,
    modifier: Modifier = Modifier
) {
    val border by animateColorAsState(
        if (active) accent.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(if (animations) 250 else 0),
        label = "activeBorder"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label $value pairs" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedNumber(value, animations, accent, small = false)
    }
}

@Composable
private fun AnimatedNumber(value: Int, animations: Boolean, color: Color, small: Boolean) {
    val style = if (small) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall
    if (!animations) {
        Text("$value", style = style, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
        return
    }
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn() + scaleIn(initialScale = 1.3f)) togetherWith
                (slideOutVertically { -it / 2 } + fadeOut())
        },
        label = "number"
    ) { number ->
        Text("$number", style = style, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

/** YOUR TURN / CPU TURN, with Thinking…, 1 / 2 and the turn counter. */
@Composable
fun TurnIndicator(
    currentSide: PlayerSide,
    cpuThinking: Boolean,
    selectedCount: Int,
    turnCount: Int,
    showTurnCounter: Boolean,
    gameOver: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = PairCaptureTheme.gameColors
    val accent = if (currentSide == PlayerSide.PLAYER) colors.player else colors.cpu
    val container = if (currentSide == PlayerSide.PLAYER) colors.playerContainer else colors.cpuContainer
    val onContainer = if (currentSide == PlayerSide.PLAYER) colors.onPlayerContainer else colors.onCpuContainer
    val title = when {
        gameOver -> "MATCH OVER"
        currentSide == PlayerSide.PLAYER -> "YOUR TURN"
        else -> "CPU TURN"
    }
    val detail = when {
        gameOver -> null
        cpuThinking -> "Thinking…"
        selectedCount == 1 -> "1 / 2 · SELECTED"
        selectedCount == 2 -> "2 / 2"
        else -> null
    }
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(50),
        color = container.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(title, style = MaterialTheme.typography.labelLarge, color = onContainer)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.75f))
            }
            if (showTurnCounter && !gameOver) {
                Text("TURN $turnCount", style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.6f))
            }
        }
    }
}

/** Eight slots that fill with ownership marks as pairs are captured. */
@Composable
fun CaptureTray(playerPairs: Int, cpuPairs: Int, modifier: Modifier = Modifier) {
    val colors = PairCaptureTheme.gameColors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Your captured $playerPairs, CPU captured $cpuPairs"
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (slot in 0 until TOTAL_PAIRS) {
                val owner = when {
                    slot < playerPairs -> PlayerSide.PLAYER
                    slot >= TOTAL_PAIRS - cpuPairs -> PlayerSide.CPU
                    else -> null
                }
                val color = when (owner) {
                    PlayerSide.PLAYER -> colors.player
                    PlayerSide.CPU -> colors.cpu
                    null -> MaterialTheme.colorScheme.outlineVariant
                }
                Canvas(Modifier.size(18.dp)) {
                    when (owner) {
                        PlayerSide.PLAYER -> drawCheckMark(color)
                        PlayerSide.CPU -> drawCpuMark(color)
                        null -> drawCircle(color, radius = size.minDimension * 0.18f)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "YOUR CAPTURED  $playerPairs",
                style = MaterialTheme.typography.labelSmall,
                color = colors.player
            )
            Text(
                "CPU CAPTURED  $cpuPairs",
                style = MaterialTheme.typography.labelSmall,
                color = colors.cpu
            )
        }
    }
}

/** Last event line plus the subtle pair-streak badge. */
@Composable
fun EventLine(lastEvent: String?, playerStreak: Int, animations: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (animations) {
            AnimatedContent(targetState = lastEvent.orEmpty(), label = "event") { text ->
                EventText(text)
            }
        } else {
            EventText(lastEvent.orEmpty())
        }
        AnimatedVisibility(visible = playerStreak >= 2) {
            Text(
                text = if (playerStreak >= 3) "PAIR STREAK ×$playerStreak" else "STREAK ×$playerStreak",
                style = MaterialTheme.typography.labelMedium,
                color = PairCaptureTheme.gameColors.player,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun EventText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
