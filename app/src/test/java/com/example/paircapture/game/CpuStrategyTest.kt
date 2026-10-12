package com.example.paircapture.game

import com.example.paircapture.FIXED_LAYOUT
import com.example.paircapture.at
import com.example.paircapture.boardOf
import com.example.paircapture.game.ai.CpuMemory
import com.example.paircapture.game.ai.CpuVisibleState
import com.example.paircapture.game.ai.PairCaptureCpuStrategy
import com.example.paircapture.game.logic.PairCaptureGameEngine
import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.Difficulty
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.PairCaptureGameState
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.SelectionOutcome
import com.example.paircapture.game.model.TileState
import com.example.paircapture.id
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CpuStrategyTest {

    private val engine = PairCaptureGameEngine

    /** A fresh game where the player has just missed, so it is the CPU's turn. */
    private fun cpuTurnGame(symbols: List<PairSymbol> = FIXED_LAYOUT): PairCaptureGameState {
        var state = engine.newGame(boardOf(symbols))
        state = (engine.selectTile(state, id("A3"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = (engine.selectTile(state, id("A4"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        return engine.resolve(state)!!.first.also { check(it.phase == GamePhase.CPU_TURN) }
    }

    private fun revealForCpu(state: PairCaptureGameState, coordinate: Coordinate): PairCaptureGameState {
        val tile = engine.tileAt(state, coordinate)!!
        return (engine.selectTile(state, tile.id, PlayerSide.CPU) as SelectionOutcome.Accepted).state
    }

    /** Plays one full CPU turn exactly as the ViewModel does and returns the chosen pair. */
    private fun cpuPicks(
        state: PairCaptureGameState,
        memory: CpuMemory,
        strategy: PairCaptureCpuStrategy
    ): Pair<Coordinate, Coordinate> {
        val first = strategy.chooseFirst(engine.cpuView(state, memory))
        val afterFirst = revealForCpu(state, first)
        val second = strategy.chooseSecond(engine.cpuView(afterFirst, memory), first)
        return first to second
    }

    // 1. CPU cannot access hidden symbols.
    @Test
    fun `cpu view exposes no hidden symbols`() {
        val state = cpuTurnGame()
        val view = engine.cpuView(state, CpuMemory())
        assertTrue(view.faceUpTiles.isEmpty())
        assertTrue(view.memory.knownTiles.isEmpty())
        assertEquals(16, view.selectableCoordinates.size)

        val revealed = revealForCpu(state, at("B2"))
        val viewAfter = engine.cpuView(revealed, CpuMemory())
        assertEquals(mapOf(at("B2") to PairSymbol.CIRCLE), viewAfter.faceUpTiles)
        assertFalse(at("A1") in viewAfter.faceUpTiles)
    }

    @Test
    fun `cpu choices are independent of hidden symbols`() {
        // Same visible situation, different hidden layouts: the CPU must behave identically.
        val swapped = FIXED_LAYOUT.toMutableList().apply {
            this[id("A1")] = PairSymbol.STAR; this[id("B2")] = PairSymbol.STAR
            this[id("C3")] = PairSymbol.CIRCLE; this[id("D4")] = PairSymbol.CIRCLE
        }
        val boardA = cpuTurnGame(FIXED_LAYOUT)
        val boardB = cpuTurnGame(swapped)
        assertEquals(engine.cpuView(boardA, CpuMemory()), engine.cpuView(boardB, CpuMemory()))

        for (difficulty in Difficulty.entries) {
            repeat(200) { seed ->
                val firstA = PairCaptureCpuStrategy(difficulty, Random(seed)).chooseFirst(engine.cpuView(boardA, CpuMemory()))
                val firstB = PairCaptureCpuStrategy(difficulty, Random(seed)).chooseFirst(engine.cpuView(boardB, CpuMemory()))
                assertEquals(firstA, firstB)
            }
        }
    }

    @Test
    fun `cpu cannot intentionally select an unseen pair`() {
        // A1/B2 are circles and C3/D4 stars, but the CPU has never revealed them.
        val state = cpuTurnGame()
        var a1b2 = 0
        val trials = 2000
        repeat(trials) { seed ->
            val (first, second) = cpuPicks(state, CpuMemory(), PairCaptureCpuStrategy(Difficulty.HARD, Random(seed)))
            if (setOf(first, second) == setOf(at("A1"), at("B2"))) a1b2++
        }
        // A blind pick of one specific pair out of 120 is ~0.8%; anything near-certain is cheating.
        assertTrue("A1+B2 chosen $a1b2 / $trials times", a1b2 < trials * 0.03)
    }

    // 2. CPU avoids captured tiles.
    @Test
    fun `cpu never selects captured or face-up tiles`() {
        var state = engine.newGame(boardOf())
        state = (engine.selectTile(state, id("A1"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = (engine.selectTile(state, id("B2"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = engine.resolve(state)!!.first // player captured circles
        state = (engine.selectTile(state, id("C3"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = (engine.selectTile(state, id("A2"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = engine.resolve(state)!!.first // miss → CPU

        val memory = CpuMemory().remember(at("A1"), PairSymbol.CIRCLE, 1).remember(at("B2"), PairSymbol.CIRCLE, 1)
        repeat(300) { seed ->
            val (first, second) = cpuPicks(state, memory, PairCaptureCpuStrategy(Difficulty.HARD, Random(seed)))
            assertFalse(first in setOf(at("A1"), at("B2")))
            assertFalse(second in setOf(at("A1"), at("B2")))
        }
        assertTrue(engine.cpuView(state, memory).memory.knownTiles.isEmpty())
    }

    // 3. Never the same tile twice in one turn.
    @Test
    fun `cpu never selects the same tile twice in one turn`() {
        val state = cpuTurnGame()
        for (difficulty in Difficulty.entries) {
            repeat(300) { seed ->
                val memory = CpuMemory().remember(at("C1"), PairSymbol.BOLT, 1)
                val (first, second) = cpuPicks(state, memory, PairCaptureCpuStrategy(difficulty, Random(seed)))
                assertNotEquals(first, second)
            }
        }
    }

    // 4 & 5. Remember revealed symbols and complete a known pair.
    @Test
    fun `cpu completes a pair it has seen across turns`() {
        // CPU previously saw A1 = Circle; now reveals B2 = Circle and should pick A1.
        val memory = CpuMemory().remember(at("A1"), PairSymbol.CIRCLE, cpuTurn = 1)
        val state = revealForCpu(cpuTurnGame(), at("B2"))
        val view = engine.cpuView(state, memory.remember(at("B2"), PairSymbol.CIRCLE, 2))
        repeat(100) { seed ->
            assertEquals(at("A1"), PairCaptureCpuStrategy(Difficulty.HARD, Random(seed)).chooseSecond(view, at("B2")))
        }
    }

    @Test
    fun `cpu opens with a remembered pair`() {
        val memory = CpuMemory()
            .remember(at("A1"), PairSymbol.CIRCLE, 1)
            .remember(at("D4"), PairSymbol.STAR, 1)
            .remember(at("B2"), PairSymbol.CIRCLE, 2)
        val state = cpuTurnGame()
        repeat(100) { seed ->
            val (first, second) = cpuPicks(state, memory, PairCaptureCpuStrategy(Difficulty.HARD, Random(seed)))
            assertEquals(setOf(at("A1"), at("B2")), setOf(first, second))
        }
    }

    @Test
    fun `hard cpu does not waste second pick on a known non-match`() {
        val memory = CpuMemory().remember(at("A2"), PairSymbol.BOLT, 1)
        val state = revealForCpu(cpuTurnGame(), at("B2")) // circle; no circle in memory
        val view = engine.cpuView(state, memory)
        repeat(200) { seed ->
            assertNotEquals(at("A2"), PairCaptureCpuStrategy(Difficulty.HARD, Random(seed)).chooseSecond(view, at("B2")))
        }
    }

    // 6. Easy forgets.
    @Test
    fun `easy forgets old information, normal and hard keep it`() {
        val memory = CpuMemory()
            .remember(at("A1"), PairSymbol.CIRCLE, cpuTurn = 1)
            .remember(at("C3"), PairSymbol.STAR, cpuTurn = 3)

        val easy = PairCaptureCpuStrategy(Difficulty.EASY).prepareMemory(memory, cpuTurn = 4)
        assertEquals(setOf(at("C3")), easy.knownTiles.keys)
        assertTrue(PairCaptureCpuStrategy(Difficulty.EASY).prepareMemory(memory, cpuTurn = 6).knownTiles.isEmpty())

        // 7 & 8. Normal and Hard retain everything they legitimately saw.
        assertEquals(memory, PairCaptureCpuStrategy(Difficulty.NORMAL).prepareMemory(memory, cpuTurn = 50))
        assertEquals(memory, PairCaptureCpuStrategy(Difficulty.HARD).prepareMemory(memory, cpuTurn = 50))
    }

    @Test
    fun `difficulty changes how reliably memory is used`() {
        val memory = CpuMemory().remember(at("A1"), PairSymbol.CIRCLE, 1).remember(at("B2"), PairSymbol.CIRCLE, 1)
        val view = engine.cpuView(cpuTurnGame(), memory)
        fun recallRate(difficulty: Difficulty): Int = (0 until 1000).count { seed ->
            PairCaptureCpuStrategy(difficulty, Random(seed)).chooseFirst(view) in setOf(at("A1"), at("B2"))
        }
        val easy = recallRate(Difficulty.EASY)
        val normal = recallRate(Difficulty.NORMAL)
        val hard = recallRate(Difficulty.HARD)
        assertEquals(1000, hard)
        assertTrue("normal=$normal", normal in 780..940)
        assertTrue("easy=$easy normal=$normal", easy < normal)
    }

    @Test
    fun `memory only contains what was added and drops captured cells`() {
        val memory = CpuMemory()
            .remember(at("A1"), PairSymbol.CIRCLE, 1)
            .remember(at("B2"), PairSymbol.CIRCLE, 1)
            .remember(at("C1"), PairSymbol.BOLT, 1)
        assertEquals(3, memory.size)
        val pruned = memory.forget(listOf(at("A1"), at("B2")))
        assertEquals(mapOf(at("C1") to PairSymbol.BOLT), pruned.knownTiles)
        assertEquals(at("A1") to at("B2"), memory.knownPair(setOf(at("A1"), at("B2"), at("C1"))))
    }

    @Test
    fun `cpu visible state has no field that can carry the full board`() {
        val fieldTypes = CpuVisibleState::class.java.declaredFields.map { it.type.simpleName }
        assertFalse(fieldTypes.any { it.contains("PairTile") || it.contains("GameState") })
    }

    // 9 & 10. Turn handling for CPU (match keeps turn, miss passes) is driven by the engine.
    @Test
    fun `cpu keeps turn after match and passes after miss`() {
        var state = cpuTurnGame()
        state = revealForCpu(revealForCpu(state, at("C3")), at("D4"))
        state = engine.resolve(state)!!.first
        assertEquals(PlayerSide.CPU, state.currentSide)
        assertEquals(TileState.CAPTURED_BY_CPU, engine.tileAt(state, at("C3"))!!.state)

        state = revealForCpu(revealForCpu(state, at("A1")), at("A2"))
        state = engine.resolve(state)!!.first
        assertEquals(PlayerSide.PLAYER, state.currentSide)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
    }
}
