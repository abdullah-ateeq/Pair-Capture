package com.example.paircapture.game

import com.example.paircapture.FIXED_LAYOUT
import com.example.paircapture.boardOf
import com.example.paircapture.game.logic.PairCaptureGameEngine
import com.example.paircapture.game.model.GameOutcome
import com.example.paircapture.game.model.GamePhase
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.game.model.PairCaptureGameState
import com.example.paircapture.game.model.PairResult
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.RejectReason
import com.example.paircapture.game.model.SelectionOutcome
import com.example.paircapture.game.model.TOTAL_PAIRS
import com.example.paircapture.game.model.TileState
import com.example.paircapture.id
import com.example.paircapture.pairsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PairCaptureGameEngineTest {

    private val engine = PairCaptureGameEngine

    private fun newFixedGame() = engine.newGame(boardOf())

    private fun select(state: PairCaptureGameState, tileId: Int, side: PlayerSide = state.currentSide) =
        (engine.selectTile(state, tileId, side) as SelectionOutcome.Accepted).state

    private fun play(state: PairCaptureGameState, a: Int, b: Int): PairCaptureGameState =
        engine.resolve(select(select(state, a), b))!!.first

    private fun tile(state: PairCaptureGameState, tileId: Int) = state.tiles.first { it.id == tileId }

    // --- Board creation -------------------------------------------------------------------

    @Test
    fun `board has 16 tiles, 8 pairs, each symbol exactly twice`() {
        val board = engine.generateBoard(Random(1))
        assertEquals(16, board.size)
        assertEquals(8, board.map { it.symbol }.distinct().size)
        PairSymbol.entries.forEach { symbol -> assertEquals(2, board.count { it.symbol == symbol }) }
        assertTrue(board.all { it.state == TileState.HIDDEN })
        assertEquals(16, board.map { it.coordinate }.distinct().size)
    }

    @Test
    fun `1000 random boards are all valid`() {
        val random = Random(42)
        repeat(1000) {
            val board = engine.generateBoard(random)
            assertEquals(16, board.size)
            assertEquals(8, board.map { it.symbol }.toSet().size)
            assertTrue(board.groupingBy { it.symbol }.eachCount().values.all { it == 2 })
            assertEquals((0 until 16).toList(), board.map { it.id })
        }
    }

    @Test
    fun `shuffle produces varied layouts`() {
        val random = Random(7)
        val layouts = (1..200).map { engine.generateBoard(random).map { it.symbol } }.toSet()
        assertTrue("Expected many distinct layouts, got ${layouts.size}", layouts.size > 190)
        val unshuffled = PairSymbol.entries.flatMap { listOf(it, it) }
        assertFalse(layouts.all { it == unshuffled })
    }

    @Test
    fun `new game starts with player turn and full counters`() {
        val state = newFixedGame()
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(PlayerSide.PLAYER, state.currentSide)
        assertEquals(TOTAL_PAIRS, state.pairsRemaining)
        assertEquals(0, state.playerPairs)
        assertEquals(0, state.cpuPairs)
        assertEquals(1, state.turnCount)
        assertTrue(state.selectedTileIds.isEmpty())
    }

    // --- Selection ------------------------------------------------------------------------

    @Test
    fun `first selection reveals tile and waits for second`() {
        val state = select(newFixedGame(), id("A1"))
        assertEquals(TileState.REVEALED, tile(state, id("A1")).state)
        assertEquals(listOf(id("A1")), state.selectedTileIds)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
    }

    @Test
    fun `second selection moves to resolving`() {
        val state = select(select(newFixedGame(), id("A1")), id("B2"))
        assertEquals(2, state.selectedTileIds.size)
        assertEquals(GamePhase.RESOLVING, state.phase)
    }

    @Test
    fun `same tile twice is rejected and stays revealed`() {
        val state = select(newFixedGame(), id("A1"))
        val outcome = engine.selectTile(state, id("A1"), PlayerSide.PLAYER)
        assertEquals(SelectionOutcome.Rejected(RejectReason.SAME_TILE), outcome)
        assertEquals(TileState.REVEALED, tile(state, id("A1")).state)
        assertEquals(1, state.selectedTileIds.size)
    }

    @Test
    fun `third selection is rejected while resolving`() {
        val state = select(select(newFixedGame(), id("A1")), id("A2"))
        val outcome = engine.selectTile(state, id("A3"), PlayerSide.PLAYER)
        assertTrue(outcome is SelectionOutcome.Rejected)
        assertEquals(TileState.HIDDEN, tile(state, id("A3")).state)
    }

    @Test
    fun `cpu cannot select on player turn and player cannot select on cpu turn`() {
        val state = newFixedGame()
        assertEquals(
            SelectionOutcome.Rejected(RejectReason.NOT_YOUR_TURN),
            engine.selectTile(state, 0, PlayerSide.CPU)
        )
        val cpuTurn = play(state, id("A1"), id("A2"))
        assertEquals(
            SelectionOutcome.Rejected(RejectReason.NOT_YOUR_TURN),
            engine.selectTile(cpuTurn, 0, PlayerSide.PLAYER)
        )
    }

    // --- Match / mismatch -----------------------------------------------------------------

    @Test
    fun `matching pair is captured, scores and keeps the turn`() {
        val resolving = select(select(newFixedGame(), id("A1")), id("B2"))
        assertEquals(PairResult.Match(id("A1"), id("B2")), engine.evaluate(resolving))

        val (state, result) = engine.resolve(resolving)!!
        assertTrue(result is PairResult.Match)
        assertEquals(1, state.playerPairs)
        assertEquals(0, state.cpuPairs)
        assertEquals(7, state.pairsRemaining)
        assertEquals(PlayerSide.PLAYER, state.currentSide)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(TileState.CAPTURED_BY_PLAYER, tile(state, id("A1")).state)
        assertEquals(TileState.CAPTURED_BY_PLAYER, tile(state, id("B2")).state)
        assertEquals(true, state.lastMatch)
        assertEquals(PlayerSide.PLAYER, state.lastCapturedBy)
        assertEquals(2, state.turnCount)
    }

    @Test
    fun `mismatch hides both tiles and passes the turn to cpu`() {
        val resolving = select(select(newFixedGame(), id("A1")), id("C3"))
        assertEquals(PairResult.Mismatch(id("A1"), id("C3")), engine.evaluate(resolving))

        val state = engine.resolve(resolving)!!.first
        assertEquals(TileState.HIDDEN, tile(state, id("A1")).state)
        assertEquals(TileState.HIDDEN, tile(state, id("C3")).state)
        assertEquals(PlayerSide.CPU, state.currentSide)
        assertEquals(GamePhase.CPU_TURN, state.phase)
        assertEquals(0, state.playerPairs)
        assertEquals(false, state.lastMatch)
        assertEquals(2, state.turnCount)
    }

    @Test
    fun `cpu match scores for cpu and keeps cpu turn, cpu miss returns turn`() {
        var state = play(newFixedGame(), id("A1"), id("C3")) // player miss → CPU
        state = play(state, id("C3"), id("D4")) // CPU captures stars
        assertEquals(1, state.cpuPairs)
        assertEquals(PlayerSide.CPU, state.currentSide)
        assertEquals(TileState.CAPTURED_BY_CPU, tile(state, id("C3")).state)

        state = play(state, id("A1"), id("A2")) // CPU miss
        assertEquals(PlayerSide.PLAYER, state.currentSide)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
    }

    @Test
    fun `captured tiles cannot be selected again`() {
        val state = play(newFixedGame(), id("A1"), id("B2"))
        val outcome = engine.selectTile(state, id("A1"), PlayerSide.PLAYER)
        assertEquals(SelectionOutcome.Rejected(RejectReason.CAPTURED), outcome)
        assertEquals(TileState.CAPTURED_BY_PLAYER, tile(state, id("A1")).state)
    }

    @Test
    fun `evaluate returns null unless two tiles are selected`() {
        assertNull(engine.evaluate(newFixedGame()))
        assertNull(engine.evaluate(select(newFixedGame(), 0)))
        assertNull(engine.resolve(newFixedGame()))
    }

    // --- Game end -------------------------------------------------------------------------

    @Test
    fun `capturing all pairs ends the game and final pair sets pairsRemaining to zero`() {
        var state = newFixedGame()
        val pairs = pairsOf(FIXED_LAYOUT)
        pairs.forEach { (a, b) -> state = play(state, a, b) }
        assertEquals(0, state.pairsRemaining)
        assertEquals(GamePhase.GAME_OVER, state.phase)
        assertTrue(engine.isGameOver(state))
        assertEquals(8, state.playerPairs)
        assertEquals(8, state.matches)
        // Every decision cycle counted; the final capture does not open a new turn.
        assertEquals(8, state.turnCount)
        assertTrue(engine.selectTile(state, 0, PlayerSide.PLAYER) is SelectionOutcome.Rejected)
    }

    @Test
    fun `perfect capture is detected`() {
        var state = newFixedGame()
        pairsOf(FIXED_LAYOUT).forEach { (a, b) -> state = play(state, a, b) }
        val summary = engine.summary(state)
        assertTrue(summary.isPerfect)
        assertEquals(GameOutcome.WIN, summary.outcome)
        assertEquals(8, summary.playerPairs)
        assertEquals(0, summary.cpuPairs)
    }

    @Test
    fun `win, loss and draw outcomes`() {
        assertEquals(GameOutcome.WIN, MatchSummary(5, 3, 10, 8, 2).outcome)
        assertEquals(GameOutcome.LOSS, MatchSummary(3, 5, 10, 8, 1).outcome)
        assertEquals(GameOutcome.DRAW, MatchSummary(4, 4, 10, 8, 1).outcome)
        assertFalse(MatchSummary(7, 1, 10, 8, 3).isPerfect)
    }

    @Test
    fun `split game produces a draw`() {
        var state = newFixedGame()
        val pairs = pairsOf(FIXED_LAYOUT)
        // Player takes 4 pairs, misses, CPU takes the remaining 4.
        pairs.take(4).forEach { (a, b) -> state = play(state, a, b) }
        val remaining = pairs.drop(4)
        state = play(state, remaining[0].first, remaining[1].first) // player miss
        remaining.forEach { (a, b) -> state = play(state, a, b) }
        assertEquals(4, state.playerPairs)
        assertEquals(4, state.cpuPairs)
        assertEquals(GameOutcome.DRAW, engine.summary(state).outcome)
    }

    // --- Streak ---------------------------------------------------------------------------

    @Test
    fun `player streak grows on matches and resets on a miss`() {
        val pairs = pairsOf(FIXED_LAYOUT)
        var state = newFixedGame()
        pairs.take(3).forEach { (a, b) -> state = play(state, a, b) }
        assertEquals(3, state.playerStreak)
        assertEquals(3, state.longestPlayerStreak)

        state = play(state, pairs[3].first, pairs[4].first) // miss
        assertEquals(0, state.playerStreak)
        assertEquals(3, state.longestPlayerStreak)
        assertEquals(PlayerSide.CPU, state.currentSide)
    }

    @Test
    fun `new game resets everything`() {
        val first = play(newFixedGame(), id("A1"), id("B2"))
        assertNotEquals(newFixedGame(), first)
        val fresh = engine.newGame(first.tiles)
        assertEquals(newFixedGame(), fresh)
    }
}
