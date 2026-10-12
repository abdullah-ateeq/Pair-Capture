package com.example.paircapture.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paircapture.data.GameStatistics
import com.example.paircapture.game.model.GameOutcome
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.ui.components.StatItem
import com.example.paircapture.ui.theme.PairCaptureTheme

fun MatchSummary.headline(): String = when (outcome) {
    GameOutcome.WIN -> "YOU WIN"
    GameOutcome.LOSS -> "YOU LOSE"
    GameOutcome.DRAW -> "DRAW"
}

fun MatchSummary.message(): String = when {
    isPerfect -> "Perfect capture."
    outcome == GameOutcome.WIN -> "You captured more pairs."
    outcome == GameOutcome.LOSS -> "The CPU captured more pairs."
    else -> "Both sides captured the same number of pairs."
}

@Composable
fun ResultScreen(
    summary: MatchSummary,
    statistics: GameStatistics,
    animations: Boolean,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val colors = PairCaptureTheme.gameColors
    val accent = when (summary.outcome) {
        GameOutcome.WIN -> colors.player
        GameOutcome.LOSS -> colors.cpu
        GameOutcome.DRAW -> MaterialTheme.colorScheme.secondary
    }
    val entrance = remember { Animatable(if (animations) 0.85f else 1f) }
    LaunchedEffect(Unit) {
        if (animations) entrance.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f))
    }

    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = entrance.value
                    scaleY = entrance.value
                    alpha = ((entrance.value - 0.85f) / 0.15f).coerceIn(0f, 1f)
                }
            ) {
                if (summary.isPerfect) {
                    Surface(shape = RoundedCornerShape(50), color = colors.playerContainer) {
                        Text(
                            "PERFECT CAPTURE",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPlayerContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    summary.headline(),
                    style = MaterialTheme.typography.displayLarge,
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "You ${summary.playerPairs}, CPU ${summary.cpuPairs}"
                    }
                ) {
                    Text("${summary.playerPairs}", style = MaterialTheme.typography.displayMedium, color = colors.player)
                    Text(
                        "  —  ",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("${summary.cpuPairs}", style = MaterialTheme.typography.displayMedium, color = colors.cpu)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    summary.message(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(vertical = 18.dp, horizontal = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("YOUR PAIRS", "${summary.playerPairs}", Modifier.weight(1f))
                        StatItem("CPU PAIRS", "${summary.cpuPairs}", Modifier.weight(1f))
                        StatItem("MATCHES", "${summary.matches}", Modifier.weight(1f))
                    }
                    HorizontalDivider(
                        Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("TURNS", "${summary.turns}", Modifier.weight(1f))
                        StatItem("PAIR RUN", "${summary.longestPairStreak}", Modifier.weight(1f))
                        StatItem("BEST SCORE", "${statistics.bestScore}", Modifier.weight(1f))
                    }
                    HorizontalDivider(
                        Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("WIN STREAK", "${statistics.currentWinStreak}", Modifier.weight(1f))
                        StatItem("BEST STREAK", "${statistics.bestWinStreak}", Modifier.weight(1f))
                        StatItem("GAMES", "${statistics.gamesPlayed}", Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("PLAY AGAIN", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("HOME")
            }
        }
    }
}
