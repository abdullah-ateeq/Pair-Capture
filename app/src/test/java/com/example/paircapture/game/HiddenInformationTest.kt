package com.example.paircapture.game

import com.example.paircapture.InMemoryPreferenceStore
import com.example.paircapture.boardOf
import com.example.paircapture.data.SettingsRepository
import com.example.paircapture.data.StatisticsRepository
import com.example.paircapture.game.logic.PairCaptureGameEngine
import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PlayerSide
import com.example.paircapture.game.model.SelectionOutcome
import com.example.paircapture.game.model.TileState
import com.example.paircapture.game.model.VisibleTile
import com.example.paircapture.game.model.accessibilityDescription
import com.example.paircapture.game.model.toVisible
import com.example.paircapture.id
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HiddenInformationTest {

    @Test
    fun `hidden tiles carry no symbol in the player-visible model`() {
        val visible = boardOf().map { it.toVisible() }
        assertTrue(visible.all { it.symbol == null })
    }

    @Test
    fun `hidden tile descriptions never name a symbol`() {
        boardOf().forEach { tile ->
            val description = tile.toVisible().accessibilityDescription()
            PairSymbol.entries.forEach { symbol ->
                assertFalse(description, description.contains(symbol.displayName, ignoreCase = true))
            }
            assertTrue(description.startsWith("Hidden tile"))
        }
    }

    @Test
    fun `accessibility descriptions for each state`() {
        val coordinate = Coordinate(1, 2)
        assertEquals(
            "Hidden tile at B3, row 2 column 3",
            VisibleTile(6, coordinate, TileState.HIDDEN, null).accessibilityDescription()
        )
        assertEquals(
            "Star tile at B3, row 2 column 3",
            VisibleTile(6, coordinate, TileState.REVEALED, PairSymbol.STAR).accessibilityDescription()
        )
        assertEquals(
            "Player captured tile at B3, row 2 column 3",
            VisibleTile(6, coordinate, TileState.CAPTURED_BY_PLAYER, null).accessibilityDescription()
        )
        assertEquals(
            "CPU captured tile at B3, row 2 column 3",
            VisibleTile(6, coordinate, TileState.CAPTURED_BY_CPU, null).accessibilityDescription()
        )
    }

    @Test
    fun `revealed tile exposes only its own symbol, and captured tiles none`() {
        var state = PairCaptureGameEngine.newGame(boardOf())
        state = (PairCaptureGameEngine.selectTile(state, id("A1"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        val visible = state.tiles.map { it.toVisible() }
        assertEquals(PairSymbol.CIRCLE, visible.first { it.id == id("A1") }.symbol)
        assertEquals(1, visible.count { it.symbol != null })

        state = (PairCaptureGameEngine.selectTile(state, id("B2"), PlayerSide.PLAYER) as SelectionOutcome.Accepted).state
        state = PairCaptureGameEngine.resolve(state)!!.first
        assertTrue(state.tiles.map { it.toVisible() }.all { it.symbol == null })
    }

    @Test
    fun `view model ui state never leaks hidden symbols`() {
        val vm = PairCaptureViewModel(
            SettingsRepository(InMemoryPreferenceStore()),
            StatisticsRepository(InMemoryPreferenceStore()),
            Random(3)
        )
        val tiles = vm.uiState.value.tiles
        assertEquals(16, tiles.size)
        tiles.forEach {
            assertEquals(TileState.HIDDEN, it.state)
            assertNull(it.symbol)
        }
    }
}
