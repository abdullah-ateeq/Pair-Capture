package com.example.paircapture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.paircapture.ui.theme.PairCaptureTheme

/** Brand mark: a 2 × 2 tile cluster with one claimed pair. */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    val colors = PairCaptureTheme.gameColors
    Canvas(modifier.size(size)) {
        val gap = this.size.minDimension * 0.08f
        val cell = (this.size.minDimension - gap) / 2
        val radius = CornerRadius(cell * 0.22f)
        for (row in 0..1) for (col in 0..1) {
            val claimed = row == col
            drawRoundRect(
                color = if (claimed) colors.playerContainer else colors.tileBack,
                topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = radius
            )
        }
        val full = this.size.minDimension
        for (i in 0..1) {
            val start = i * (cell + gap)
            inset(left = start, top = start, right = full - start - cell, bottom = full - start - cell) {
                drawCheckMark(colors.player)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val HowToPlaySteps = listOf(
    "REVEAL" to "Tap two hidden tiles.",
    "MATCH" to "If the symbols match, you capture the pair.",
    "KEEP PLAYING" to "A successful match gives you another turn.",
    "MISS" to "If the tiles do not match, they hide again and the CPU plays.",
    "WIN" to "Capture more pairs than the CPU."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = PairCaptureTheme.gameColors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("HOW TO PLAY", style = MaterialTheme.typography.titleLarge)
            HowToPlaySteps.forEachIndexed { index, (title, body) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.player,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(title, style = MaterialTheme.typography.labelLarge)
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("GOT IT")
            }
        }
    }
}
