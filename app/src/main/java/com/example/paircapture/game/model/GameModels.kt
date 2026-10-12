package com.example.paircapture.game.model

const val BOARD_SIZE = 4
const val TOTAL_PAIRS = BOARD_SIZE * BOARD_SIZE / 2

enum class TileState {
    HIDDEN,
    REVEALED,
    CAPTURED_BY_PLAYER,
    CAPTURED_BY_CPU;

    val isCaptured: Boolean get() = this == CAPTURED_BY_PLAYER || this == CAPTURED_BY_CPU
}

/** Zero-based board position. Rows are labelled A–D, columns 1–4 (e.g. "B3"). */
data class Coordinate(
    val row: Int,
    val column: Int
) {
    val label: String get() = "${'A' + row}${column + 1}"
}

enum class PlayerSide {
    PLAYER,
    CPU;

    val opponent: PlayerSide get() = if (this == PLAYER) CPU else PLAYER
}

enum class GamePhase {
    PLAYER_TURN,
    CPU_TURN,
    RESOLVING,
    GAME_OVER
}

enum class PairSymbol(val displayName: String) {
    CIRCLE("Circle"),
    TRIANGLE("Triangle"),
    SQUARE("Square"),
    STAR("Star"),
    DIAMOND("Diamond"),
    HEXAGON("Hexagon"),
    MOON("Moon"),
    BOLT("Bolt")
}

enum class Difficulty(val label: String, val description: String) {
    EASY("Easy", "Forgets quickly. Mostly guesses."),
    NORMAL("Normal", "Remembers what it reveals."),
    HARD("Hard", "Sharp memory. Rarely misses a known pair.")
}

data class PairTile(
    val id: Int,
    val coordinate: Coordinate,
    val symbol: PairSymbol,
    val state: TileState
)

/**
 * Internal, authoritative board state. Contains every symbol, so it must never be handed
 * directly to the UI or to the CPU strategy.
 */
data class PairCaptureGameState(
    val tiles: List<PairTile>,
    val phase: GamePhase,
    val currentSide: PlayerSide,
    val selectedTileIds: List<Int>,
    val playerPairs: Int,
    val cpuPairs: Int,
    val pairsRemaining: Int,
    val turnCount: Int,
    val lastMatch: Boolean?,
    val lastCapturedBy: PlayerSide?,
    val playerStreak: Int,
    val longestPlayerStreak: Int
) {
    val matches: Int get() = playerPairs + cpuPairs
}

sealed class PairResult {
    abstract val first: Int
    abstract val second: Int

    data class Match(override val first: Int, override val second: Int) : PairResult()
    data class Mismatch(override val first: Int, override val second: Int) : PairResult()
}

enum class GameOutcome { WIN, LOSS, DRAW }

enum class RejectReason {
    NOT_YOUR_TURN,
    SAME_TILE,
    ALREADY_REVEALED,
    CAPTURED,
    SELECTION_FULL,
    UNKNOWN_TILE
}

sealed class SelectionOutcome {
    data class Accepted(val state: PairCaptureGameState, val tile: PairTile) : SelectionOutcome()
    data class Rejected(val reason: RejectReason) : SelectionOutcome()
}

/** Summary shown on the result screen. */
data class MatchSummary(
    val playerPairs: Int,
    val cpuPairs: Int,
    val turns: Int,
    val matches: Int,
    val longestPairStreak: Int
) {
    val outcome: GameOutcome
        get() = when {
            playerPairs > cpuPairs -> GameOutcome.WIN
            cpuPairs > playerPairs -> GameOutcome.LOSS
            else -> GameOutcome.DRAW
        }

    val isPerfect: Boolean get() = playerPairs == TOTAL_PAIRS
}
