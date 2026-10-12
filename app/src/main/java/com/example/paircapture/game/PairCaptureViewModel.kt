package com.example.paircapture.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paircapture.data.SettingsRepository
import com.example.paircapture.data.StatisticsRepository
import com.example.paircapture.game.ai.CpuMemory
import com.example.paircapture.game.ai.PairCaptureCpuStrategy
import com.example.paircapture.game.logic.PairCaptureGameEngine
import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.Difficulty
import com.example.paircapture.game.model.GameOutcome
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.game.model.PairCaptureGameState
import com.example.paircapture.game.model.PairResult
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.RejectReason
import com.example.paircapture.game.model.SelectionOutcome
import com.example.paircapture.game.model.TOTAL_PAIRS
import com.example.paircapture.game.model.VisibleTile
import com.example.paircapture.game.model.toVisible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Durations for the game loop. Visual-only stages collapse when animations are off. */
data class GameTiming(
    val revealHoldMs: Long = 650,
    val feedbackMs: Long = 450,
    val cpuThinkMinMs: Long = 300,
    val cpuThinkMaxMs: Long = 700,
    val cpuBetweenPicksMs: Long = 450,
    val endDelayMs: Long = 900,
    val endDelayNoAnimationMs: Long = 350
)

enum class FeedbackKind { MATCH, MISMATCH }

data class TileFeedback(val kind: FeedbackKind, val tileIds: Set<Int>, val side: PlayerSide)

enum class GameEventType { REVEAL_FIRST, REVEAL_SECOND, MATCH, MISMATCH, CPU_TURN, PERFECT, VICTORY, DEFEAT, DRAW }

/** One-shot feedback cue (sound / haptics) and the side that caused it. */
data class GameEvent(val type: GameEventType, val side: PlayerSide)

/** Everything the game screen renders. Tiles are player-visible only; no hidden symbols. */
data class GameUiState(
    val tiles: List<VisibleTile>,
    val phase: GamePhase,
    val currentSide: PlayerSide,
    val selectedCount: Int,
    val playerPairs: Int,
    val cpuPairs: Int,
    val pairsRemaining: Int,
    val turnCount: Int,
    val playerStreak: Int,
    val difficulty: Difficulty,
    val feedback: TileFeedback? = null,
    val cpuThinking: Boolean = false,
    val busy: Boolean = false,
    val lastEvent: String? = null,
    val summary: MatchSummary? = null
) {
    val boardEnabled: Boolean get() = phase == GamePhase.PLAYER_TURN && !busy
    val canRestart: Boolean get() = phase == GamePhase.PLAYER_TURN && !busy
}

class PairCaptureViewModel(
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository,
    private val random: Random = Random.Default,
    private val timing: GameTiming = GameTiming()
) : ViewModel() {

    private lateinit var game: PairCaptureGameState
    private lateinit var strategy: PairCaptureCpuStrategy
    private var difficulty: Difficulty = Difficulty.NORMAL
    private var memory = CpuMemory()
    private var cpuTurnIndex = 0
    private var actionJob: Job? = null
    private var recorded = false
    private var resultConsumed = false

    private var feedback: TileFeedback? = null
    private var cpuThinking = false
    private var busy = false
    private var lastEvent: String? = null
    private var summary: MatchSummary? = null

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val animationsEnabled: Boolean get() = settingsRepository.settings.value.animations

    /** Exposes the CPU's legitimately gathered knowledge for tests. */
    internal val cpuMemory: CpuMemory get() = memory

    private fun buildInitialState(): GameUiState {
        resetMatch()
        return snapshot()
    }

    private fun resetMatch() {
        difficulty = settingsRepository.settings.value.difficulty
        strategy = PairCaptureCpuStrategy(difficulty, random)
        game = PairCaptureGameEngine.newGame(random)
        memory = CpuMemory()
        cpuTurnIndex = 0
        recorded = false
        resultConsumed = false
        feedback = null
        cpuThinking = false
        lastEvent = null
        summary = null
    }

    private fun snapshot() = GameUiState(
        tiles = game.tiles.map { it.toVisible() },
        phase = game.phase,
        currentSide = game.currentSide,
        selectedCount = game.selectedTileIds.size,
        playerPairs = game.playerPairs,
        cpuPairs = game.cpuPairs,
        pairsRemaining = game.pairsRemaining,
        turnCount = game.turnCount,
        playerStreak = game.playerStreak,
        difficulty = difficulty,
        feedback = feedback,
        cpuThinking = cpuThinking,
        busy = busy,
        lastEvent = lastEvent,
        summary = summary
    )

    private fun publish() {
        _uiState.value = snapshot()
    }

    fun selectTile(tileId: Int) {
        if (busy) return
        when (val outcome = PairCaptureGameEngine.selectTile(game, tileId, PlayerSide.PLAYER)) {
            is SelectionOutcome.Rejected -> {
                if (outcome.reason == RejectReason.SAME_TILE) _messages.tryEmit("Choose another tile.")
            }

            is SelectionOutcome.Accepted -> {
                game = outcome.state
                val second = game.selectedTileIds.size == 2
                emit(if (second) GameEventType.REVEAL_SECOND else GameEventType.REVEAL_FIRST, PlayerSide.PLAYER)
                if (second) {
                    busy = true
                    actionJob = viewModelScope.launch {
                        val self = coroutineContext[Job]
                        try {
                            resolveSelection()
                            continueGame()
                        } finally {
                            if (actionJob === self) {
                                busy = false
                                cpuThinking = false
                                publish()
                            }
                        }
                    }
                }
                publish()
            }
        }
    }

    fun restart() {
        actionJob?.cancel()
        actionJob = null
        busy = false
        resetMatch()
        publish()
    }

    /** Returns the final summary exactly once so the screen navigates a single time. */
    fun consumeResult(): MatchSummary? {
        val result = summary ?: return null
        if (resultConsumed) return null
        resultConsumed = true
        return result
    }

    private suspend fun resolveSelection() {
        delay(timing.revealHoldMs)
        val pending = PairCaptureGameEngine.evaluate(game) ?: return
        val side = game.currentSide
        if (animationsEnabled && timing.feedbackMs > 0) {
            val kind = if (pending is PairResult.Match) FeedbackKind.MATCH else FeedbackKind.MISMATCH
            feedback = TileFeedback(kind, setOf(pending.first, pending.second), side)
            publish()
            delay(timing.feedbackMs)
        }

        val (next, result) = PairCaptureGameEngine.resolve(game) ?: return
        game = next
        feedback = null
        when (result) {
            is PairResult.Match -> {
                memory = memory.forget(coordinatesOf(result.first, result.second))
                lastEvent = if (side == PlayerSide.PLAYER) {
                    if (game.playerStreak >= 3) "Pair streak ×${game.playerStreak}" else "Pair captured!"
                } else {
                    "CPU captured a pair."
                }
                emit(GameEventType.MATCH, side)
            }

            is PairResult.Mismatch -> {
                lastEvent = if (side == PlayerSide.PLAYER) "Not a match." else "CPU missed."
                emit(GameEventType.MISMATCH, side)
            }
        }
        publish()
    }

    private suspend fun continueGame() {
        while (game.phase == GamePhase.CPU_TURN) {
            playCpuTurn()
        }
        if (game.phase == GamePhase.GAME_OVER) finishGame()
    }

    private suspend fun playCpuTurn() {
        cpuTurnIndex++
        memory = strategy.prepareMemory(memory, cpuTurnIndex)
        cpuThinking = true
        emit(GameEventType.CPU_TURN, PlayerSide.CPU)
        publish()
        delay(random.nextLong(timing.cpuThinkMinMs, timing.cpuThinkMaxMs + 1))

        val first = strategy.chooseFirst(PairCaptureGameEngine.cpuView(game, memory))
        cpuThinking = false
        revealForCpu(first)
        emit(GameEventType.REVEAL_FIRST, PlayerSide.CPU)
        publish()
        delay(timing.cpuBetweenPicksMs)

        val second = strategy.chooseSecond(PairCaptureGameEngine.cpuView(game, memory), first)
        revealForCpu(second)
        emit(GameEventType.REVEAL_SECOND, PlayerSide.CPU)
        publish()

        resolveSelection()
    }

    private fun revealForCpu(coordinate: Coordinate) {
        val tile = checkNotNull(PairCaptureGameEngine.tileAt(game, coordinate))
        val outcome = PairCaptureGameEngine.selectTile(game, tile.id, PlayerSide.CPU)
        check(outcome is SelectionOutcome.Accepted) { "CPU chose an illegal tile: $outcome" }
        game = outcome.state
        // The tile is now face up, so the CPU has legitimately seen it.
        memory = memory.remember(coordinate, outcome.tile.symbol, cpuTurnIndex)
    }

    private suspend fun finishGame() {
        if (recorded) return
        recorded = true
        val result = PairCaptureGameEngine.summary(game)
        statisticsRepository.record(result)
        when {
            result.isPerfect -> {
                emit(GameEventType.PERFECT, PlayerSide.PLAYER)
                _messages.tryEmit("Perfect capture!")
            }
            result.outcome == GameOutcome.WIN -> emit(GameEventType.VICTORY, PlayerSide.PLAYER)
            result.outcome == GameOutcome.LOSS -> emit(GameEventType.DEFEAT, PlayerSide.PLAYER)
            else -> emit(GameEventType.DRAW, PlayerSide.PLAYER)
        }
        lastEvent = if (result.isPerfect) "Perfect capture." else "All $TOTAL_PAIRS pairs captured."
        publish()
        delay(if (animationsEnabled) timing.endDelayMs else timing.endDelayNoAnimationMs)
        summary = result
        publish()
    }

    private fun emit(type: GameEventType, side: PlayerSide) {
        _events.tryEmit(GameEvent(type, side))
    }

    private fun coordinatesOf(vararg ids: Int): List<Coordinate> =
        game.tiles.filter { it.id in ids }.map { it.coordinate }

    /** Test hook: start from a fixed board instead of a random one. */
    internal fun startWithBoard(state: PairCaptureGameState) {
        restart()
        game = state
        publish()
    }
}
