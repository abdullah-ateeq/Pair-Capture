package com.example.paircapture.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paircapture.data.AppSettings
import com.example.paircapture.data.GameStatistics
import com.example.paircapture.ui.components.BrandMark
import com.example.paircapture.ui.components.HowToPlaySheet
import com.example.paircapture.ui.components.StatItem
import com.example.paircapture.ui.theme.PairCaptureTheme
import java.util.Locale

@Composable
fun HomeScreen(
    statistics: GameStatistics,
    settings: AppSettings,
    onPlay: () -> Unit
) {
    var showHowTo by rememberSaveable { mutableStateOf(false) }
    val colors = PairCaptureTheme.gameColors

    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            BrandMark(size = 72.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                "PAIR\nCAPTURE",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Find the pair. Claim the point.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("PLAY", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "CPU · ${settings.difficulty.label.uppercase(Locale.ROOT)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.cpu
            )
            Spacer(Modifier.height(28.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(vertical = 18.dp, horizontal = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("GAMES", "${statistics.gamesPlayed}", Modifier.weight(1f))
                        StatItem("WINS", "${statistics.wins}", Modifier.weight(1f))
                        StatItem("WIN RATE", "${statistics.winRate}%", Modifier.weight(1f))
                    }
                    HorizontalDivider(
                        Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("BEST SCORE", "${statistics.bestScore}", Modifier.weight(1f))
                        StatItem("BEST STREAK", "${statistics.bestWinStreak}", Modifier.weight(1f))
                        StatItem(
                            "AVG PAIRS",
                            String.format(Locale.US, "%.1f", statistics.averagePairs),
                            Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { showHowTo = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("HOW TO PLAY")
            }
        }
    }

    if (showHowTo) {
        HowToPlaySheet(onDismiss = { showHowTo = false })
    }
}
