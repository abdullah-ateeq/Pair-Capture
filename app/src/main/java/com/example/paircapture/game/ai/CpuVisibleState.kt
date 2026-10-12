package com.example.paircapture.game.ai

import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.PairSymbol

/**
 * Everything the CPU is allowed to know when making a decision. There is deliberately no
 * field that could carry the symbol of a hidden tile.
 *
 * @property selectableCoordinates hidden, uncaptured cells (positions only)
 * @property capturedCoordinates cells already claimed by either side
 * @property faceUpTiles tiles currently revealed on the board, visible to everyone
 * @property memory what the CPU has previously revealed itself
 */
data class CpuVisibleState(
    val selectableCoordinates: Set<Coordinate>,
    val capturedCoordinates: Set<Coordinate>,
    val faceUpTiles: Map<Coordinate, PairSymbol>,
    val memory: CpuMemory,
    val isCpuTurn: Boolean
)
