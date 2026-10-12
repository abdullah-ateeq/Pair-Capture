package com.example.paircapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paircapture.game.TileFeedback
import com.example.paircapture.game.model.BOARD_SIZE
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.TileState
import com.example.paircapture.game.model.VisibleTile
import com.example.paircapture.ui.theme.PairCaptureTheme

/** Fixed 4 × 4 grid. Captured tiles stay in place, so the layout never shifts. */
@Composable
fun GameBoard(
    tiles: List<VisibleTile>,
    feedback: TileFeedback?,
    currentSide: PlayerSide,
    enabled: Boolean,
    animations: Boolean,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(26.dp),
        color = PairCaptureTheme.gameColors.board,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until BOARD_SIZE) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (column in 0 until BOARD_SIZE) {
                        val tile = tiles.first { it.coordinate.row == row && it.coordinate.column == column }
                        GameTile(
                            tile = tile,
                            feedback = feedback,
                            selectedBy = if (tile.state == TileState.REVEALED) currentSide else null,
                            enabled = enabled,
                            animations = animations,
                            onClick = { onTileClick(tile.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
