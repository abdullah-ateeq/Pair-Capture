package com.example.paircapture.game.logic

import com.example.paircapture.game.ai.CpuMemory
import com.example.paircapture.game.ai.CpuVisibleState
import com.example.paircapture.game.model.BOARD_SIZE
import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.game.model.PairCaptureGameState
import com.example.paircapture.game.model.PairResult
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PairTile
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.RejectReason
import com.example.paircapture.game.model.SelectionOutcome
import com.example.paircapture.game.model.TOTAL_PAIRS
import com.example.paircapture.game.model.TileState
import kotlin.random.Random

/** Pure game rules. Holds no mutable state and has no UI dependencies. */
object PairCaptureGameEngine {

    fun generateBoard(random: Random = Random.Default): List<PairTile> {
        val symbols = PairSymbol.entries.flatMap { listOf(it, it) }.shuffled(random)
        return symbols.mapIndexed { index, symbol ->
            PairTile(
                id = index,
                coordinate = Coordinate(index / BOARD_SIZE, index % BOARD_SIZE),
                symbol = symbol,
                state = TileState.HIDDEN
            )
        }
    }

    fun newGame(random: Random = Random.Default): PairCaptureGameState =
        newGame(generateBoard(random))

    fun newGame(tiles: List<PairTile>): PairCaptureGameState {
        require(tiles.size == BOARD_SIZE * BOARD_SIZE) { "Board must contain ${BOARD_SIZE * BOARD_SIZE} tiles" }
        return PairCaptureGameState(
            tiles = tiles.map { it.copy(state = TileState.HIDDEN) },
            phase = GamePhase.PLAYER_TURN,
            currentSide = PlayerSide.PLAYER,
            selectedTileIds = emptyList(),
            playerPairs = 0,
            cpuPairs = 0,
            pairsRemaining = TOTAL_PAIRS,
            turnCount = 1,
            lastMatch = null,
            lastCapturedBy = null,
            playerStreak = 0,
            longestPlayerStreak = 0
        )
    }

    private fun turnPhaseFor(side: PlayerSide) =
        if (side == PlayerSide.PLAYER) GamePhase.PLAYER_TURN else GamePhase.CPU_TURN

    /** Reveals a tile for [side] if the move is legal. Two reveals move the game to RESOLVING. */
    fun selectTile(state: PairCaptureGameState, tileId: Int, side: PlayerSide): SelectionOutcome {
        if (state.currentSide != side || state.phase != turnPhaseFor(side)) {
            return SelectionOutcome.Rejected(RejectReason.NOT_YOUR_TURN)
        }
        val tile = state.tiles.firstOrNull { it.id == tileId }
            ?: return SelectionOutcome.Rejected(RejectReason.UNKNOWN_TILE)
        if (tileId in state.selectedTileIds) return SelectionOutcome.Rejected(RejectReason.SAME_TILE)
        if (state.selectedTileIds.size >= 2) return SelectionOutcome.Rejected(RejectReason.SELECTION_FULL)
        if (tile.state.isCaptured) return SelectionOutcome.Rejected(RejectReason.CAPTURED)
        if (tile.state == TileState.REVEALED) return SelectionOutcome.Rejected(RejectReason.ALREADY_REVEALED)

        val revealed = tile.copy(state = TileState.REVEALED)
        val selected = state.selectedTileIds + tileId
        val newState = state.copy(
            tiles = state.tiles.map { if (it.id == tileId) revealed else it },
            selectedTileIds = selected,
            phase = if (selected.size == 2) GamePhase.RESOLVING else state.phase
        )
        return SelectionOutcome.Accepted(newState, revealed)
    }

    /** Compares the two selected tiles without changing state. */
    fun evaluate(state: PairCaptureGameState): PairResult? {
        if (state.phase != GamePhase.RESOLVING || state.selectedTileIds.size != 2) return null
        val (firstId, secondId) = state.selectedTileIds
        val first = state.tiles.first { it.id == firstId }
        val second = state.tiles.first { it.id == secondId }
        return if (first.symbol == second.symbol) {
            PairResult.Match(firstId, secondId)
        } else {
            PairResult.Mismatch(firstId, secondId)
        }
    }

    /**
     * Applies the outcome of a two-tile reveal: a match captures the pair and keeps the turn,
     * a mismatch hides both tiles and passes the turn.
     */
    fun resolve(state: PairCaptureGameState): Pair<PairCaptureGameState, PairResult>? {
        val result = evaluate(state) ?: return null
        val side = state.currentSide
        val ids = setOf(result.first, result.second)

        val resolved = when (result) {
            is PairResult.Match -> {
                val capturedState =
                    if (side == PlayerSide.PLAYER) TileState.CAPTURED_BY_PLAYER else TileState.CAPTURED_BY_CPU
                val remaining = state.pairsRemaining - 1
                val streak = if (side == PlayerSide.PLAYER) state.playerStreak + 1 else state.playerStreak
                state.copy(
                    tiles = state.tiles.map { if (it.id in ids) it.copy(state = capturedState) else it },
                    selectedTileIds = emptyList(),
                    playerPairs = state.playerPairs + if (side == PlayerSide.PLAYER) 1 else 0,
                    cpuPairs = state.cpuPairs + if (side == PlayerSide.CPU) 1 else 0,
                    pairsRemaining = remaining,
                    phase = if (remaining == 0) GamePhase.GAME_OVER else turnPhaseFor(side),
                    turnCount = if (remaining == 0) state.turnCount else state.turnCount + 1,
                    lastMatch = true,
                    lastCapturedBy = side,
                    playerStreak = streak,
                    longestPlayerStreak = maxOf(state.longestPlayerStreak, streak)
                )
            }

            is PairResult.Mismatch -> {
                val next = side.opponent
                state.copy(
                    tiles = state.tiles.map { if (it.id in ids) it.copy(state = TileState.HIDDEN) else it },
                    selectedTileIds = emptyList(),
                    currentSide = next,
                    phase = turnPhaseFor(next),
                    turnCount = state.turnCount + 1,
                    lastMatch = false,
                    playerStreak = if (side == PlayerSide.PLAYER) 0 else state.playerStreak
                )
            }
        }
        return resolved to result
    }

    fun isGameOver(state: PairCaptureGameState): Boolean =
        state.pairsRemaining == 0 || state.phase == GamePhase.GAME_OVER

    fun summary(state: PairCaptureGameState): MatchSummary = MatchSummary(
        playerPairs = state.playerPairs,
        cpuPairs = state.cpuPairs,
        turns = state.turnCount,
        matches = state.matches,
        longestPairStreak = state.longestPlayerStreak
    )

    fun tileAt(state: PairCaptureGameState, coordinate: Coordinate): PairTile? =
        state.tiles.firstOrNull { it.coordinate == coordinate }

    /**
     * The only gateway from the internal board to the CPU. Hidden tiles are passed as bare
     * coordinates; symbols are included solely for tiles that are face up right now.
     */
    fun cpuView(state: PairCaptureGameState, memory: CpuMemory): CpuVisibleState {
        val captured = state.tiles.filter { it.state.isCaptured }.map { it.coordinate }.toSet()
        return CpuVisibleState(
            selectableCoordinates = state.tiles
                .filter { it.state == TileState.HIDDEN }
                .map { it.coordinate }
                .toSet(),
            capturedCoordinates = captured,
            faceUpTiles = state.tiles
                .filter { it.state == TileState.REVEALED }
                .associate { it.coordinate to it.symbol },
            memory = memory.forget(captured),
            isCpuTurn = state.currentSide == PlayerSide.CPU && state.phase == GamePhase.CPU_TURN
        )
    }
}
