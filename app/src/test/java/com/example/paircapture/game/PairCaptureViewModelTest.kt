package com.example.paircapture.game

import com.example.paircapture.FIXED_LAYOUT
import com.example.paircapture.InMemoryPreferenceStore
import com.example.paircapture.boardOf
import com.example.paircapture.data.SettingsRepository
import com.example.paircapture.data.StatisticsRepository
import com.example.paircapture.game.logic.PairCaptureGameEngine
import com.example.paircapture.game.model.Difficulty
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.TOTAL_PAIRS
import com.example.paircapture.game.model.TileState
import com.example.paircapture.id
import com.example.paircapture.pairsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PairCaptureViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val timing = GameTiming()
    private lateinit var settings: SettingsRepository
    private lateinit var statistics: StatisticsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = SettingsRepository(InMemoryPreferenceStore())
        statistics = StatisticsRepository(InMemoryPreferenceStore())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fixedViewModel(seed: Int = 1): PairCaptureViewModel =
        PairCaptureViewModel(settings, statistics, Random(seed), timing).apply {
            startWithBoard(PairCaptureGameEngine.newGame(boardOf()))
        }

    private fun PairCaptureViewModel.tile(tileId: Int) = uiState.value.tiles.first { it.id == tileId }

    @Test
    fun `player match captures pair and keeps the turn`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("B2"))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(1, state.playerPairs)
        assertEquals(PlayerSide.PLAYER, state.currentSide)
        assertEquals(TileState.CAPTURED_BY_PLAYER, vm.tile(id("A1")).state)
        assertTrue(state.boardEnabled)
    }

    @Test
    fun `player mismatch hides tiles and the cpu takes its turn`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("C3"))
        advanceTimeBy(timing.revealHoldMs + timing.feedbackMs + 1)
        runCurrent()
        assertEquals(PlayerSide.CPU, vm.uiState.value.currentSide)
        assertEquals(TileState.HIDDEN, vm.tile(id("A1")).state)
        assertEquals(TileState.HIDDEN, vm.tile(id("C3")).state)
        assertFalse(vm.uiState.value.boardEnabled)
        advanceUntilIdle()
        // CPU eventually hands the turn back (or the game ends).
        val end = vm.uiState.value
        assertTrue(end.phase == GamePhase.PLAYER_TURN || end.phase == GamePhase.GAME_OVER)
    }

    @Test
    fun `rapid taps only accept the first two selections`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        listOf("A1", "A2", "A3", "A4", "B1").forEach { vm.selectTile(id(it)) }
        val state = vm.uiState.value
        assertEquals(2, state.selectedCount)
        assertEquals(TileState.REVEALED, vm.tile(id("A1")).state)
        assertEquals(TileState.REVEALED, vm.tile(id("A2")).state)
        listOf("A3", "A4", "B1").forEach { assertEquals(TileState.HIDDEN, vm.tile(id(it)).state) }
        assertFalse(state.boardEnabled)
    }

    @Test
    fun `same tile twice shows a hint and keeps the tile revealed`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        val messages = mutableListOf<String>()
        backgroundScope.launch { vm.messages.collect { messages += it } }
        runCurrent()
        vm.selectTile(id("A1"))
        vm.selectTile(id("A1"))
        runCurrent()
        assertEquals(1, vm.uiState.value.selectedCount)
        assertEquals(TileState.REVEALED, vm.tile(id("A1")).state)
        assertEquals(listOf("Choose another tile."), messages)
    }

    @Test
    fun `taps during cpu turn are ignored`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("C3"))
        advanceTimeBy(timing.revealHoldMs + timing.feedbackMs + 1)
        runCurrent()
        assertEquals(PlayerSide.CPU, vm.uiState.value.currentSide)
        val before = vm.uiState.value.tiles
        vm.selectTile(id("D1"))
        assertEquals(before, vm.uiState.value.tiles)
    }

    @Test
    fun `animations off skips the feedback stage`() = runTest(dispatcher) {
        settings.update { it.copy(animations = false) }
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("B2"))
        advanceTimeBy(timing.revealHoldMs + 1)
        runCurrent()
        assertNull(vm.uiState.value.feedback)
        assertEquals(1, vm.uiState.value.playerPairs)
    }

    @Test
    fun `animations on shows match feedback before capture`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("B2"))
        advanceTimeBy(timing.revealHoldMs + 1)
        runCurrent()
        assertEquals(FeedbackKind.MATCH, vm.uiState.value.feedback?.kind)
        assertEquals(0, vm.uiState.value.playerPairs)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.playerPairs)
        assertNull(vm.uiState.value.feedback)
    }

    @Test
    fun `perfect game records statistics once and produces a result`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        pairsOf(FIXED_LAYOUT).forEach { (a, b) ->
            vm.selectTile(a)
            vm.selectTile(b)
            advanceUntilIdle()
        }
        val state = vm.uiState.value
        assertEquals(GamePhase.GAME_OVER, state.phase)
        assertEquals(0, state.pairsRemaining)
        val summary = requireNotNull(state.summary)
        assertTrue(summary.isPerfect)
        assertEquals(summary, vm.consumeResult())
        assertNull("Result must only be delivered once", vm.consumeResult())

        val stats = statistics.statistics.value
        assertEquals(1, stats.gamesPlayed)
        assertEquals(1, stats.wins)
        assertEquals(8, stats.bestScore)
        assertEquals(8, stats.totalPairsCaptured)
        assertFalse(state.boardEnabled)
    }

    @Test
    fun `full games against every difficulty finish consistently`() = runTest(dispatcher) {
        var games = 0
        for (difficulty in Difficulty.entries) {
            settings.update { it.copy(difficulty = difficulty) }
            repeat(5) { seed ->
                val vm = PairCaptureViewModel(settings, statistics, Random(seed), timing)
                val picker = Random(seed + 100)
                var guard = 0
                while (vm.uiState.value.summary == null && guard++ < 500) {
                    val state = vm.uiState.value
                    if (state.boardEnabled) {
                        val hidden = state.tiles.filter { it.state == TileState.HIDDEN }.map { it.id }
                        vm.selectTile(hidden.random(picker))
                    }
                    advanceUntilIdle()
                }
                val summary = requireNotNull(vm.uiState.value.summary)
                assertEquals(TOTAL_PAIRS, summary.playerPairs + summary.cpuPairs)
                assertEquals(TOTAL_PAIRS, summary.matches)
                assertTrue(vm.uiState.value.tiles.all { it.state.isCaptured })
                games++
            }
        }
        assertEquals(games, statistics.statistics.value.gamesPlayed)
    }

    @Test
    fun `cpu memory only contains tiles the cpu revealed`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        // Player reveals two tiles and misses; the CPU must not learn them.
        vm.selectTile(id("A1"))
        vm.selectTile(id("C3"))
        advanceTimeBy(timing.revealHoldMs + timing.feedbackMs + 1)
        runCurrent()
        assertTrue(vm.cpuMemory.knownTiles.isEmpty())
        advanceUntilIdle()
        val revealedByCpu = vm.cpuMemory.knownTiles.keys
        assertTrue(revealedByCpu.size <= 16)
        vm.cpuMemory.knownTiles.forEach { (coordinate, symbol) ->
            assertEquals(FIXED_LAYOUT[coordinate.row * 4 + coordinate.column], symbol)
        }
    }

    @Test
    fun `restart resets the match`() = runTest(dispatcher) {
        val vm = fixedViewModel()
        vm.selectTile(id("A1"))
        vm.selectTile(id("B2"))
        advanceUntilIdle()
        vm.restart()
        val state = vm.uiState.value
        assertEquals(0, state.playerPairs)
        assertEquals(0, state.cpuPairs)
        assertEquals(TOTAL_PAIRS, state.pairsRemaining)
        assertEquals(1, state.turnCount)
        assertEquals(0, state.playerStreak)
        assertTrue(state.tiles.all { it.state == TileState.HIDDEN })
        assertTrue(vm.cpuMemory.knownTiles.isEmpty())
        assertEquals(0, statistics.statistics.value.gamesPlayed)
    }
}
