package com.example.paircapture

import com.example.paircapture.data.PreferenceStore
import com.example.paircapture.game.model.BOARD_SIZE
import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.PairSymbol
import com.example.paircapture.game.model.PairSymbol.BOLT
import com.example.paircapture.game.model.PairSymbol.CIRCLE
import com.example.paircapture.game.model.PairSymbol.DIAMOND
import com.example.paircapture.game.model.PairSymbol.HEXAGON
import com.example.paircapture.game.model.PairSymbol.MOON
import com.example.paircapture.game.model.PairSymbol.SQUARE
import com.example.paircapture.game.model.PairSymbol.STAR
import com.example.paircapture.game.model.PairSymbol.TRIANGLE
import com.example.paircapture.game.model.PairTile
import com.example.paircapture.game.model.TileState

/**
 * Fixed layout used across tests (rows A–D, columns 1–4):
 *
 *     A: CIRCLE   BOLT     TRIANGLE SQUARE
 *     B: DIAMOND  CIRCLE   HEXAGON  MOON
 *     C: BOLT     TRIANGLE STAR     SQUARE
 *     D: DIAMOND  HEXAGON  MOON     STAR
 */
val FIXED_LAYOUT = listOf(
    CIRCLE, BOLT, TRIANGLE, SQUARE,
    DIAMOND, CIRCLE, HEXAGON, MOON,
    BOLT, TRIANGLE, STAR, SQUARE,
    DIAMOND, HEXAGON, MOON, STAR
)

fun boardOf(symbols: List<PairSymbol> = FIXED_LAYOUT): List<PairTile> = symbols.mapIndexed { index, symbol ->
    PairTile(index, Coordinate(index / BOARD_SIZE, index % BOARD_SIZE), symbol, TileState.HIDDEN)
}

/** "B2" → Coordinate(1, 1) */
fun at(label: String) = Coordinate(label[0] - 'A', label.substring(1).toInt() - 1)

/** "B2" → tile id 5 */
fun id(label: String) = at(label).let { it.row * BOARD_SIZE + it.column }

/** All matching pairs of a layout as tile-id pairs. */
fun pairsOf(symbols: List<PairSymbol>): List<Pair<Int, Int>> =
    symbols.indices.groupBy { symbols[it] }.values.map { it[0] to it[1] }

class InMemoryPreferenceStore : PreferenceStore {
    val values = mutableMapOf<String, Any>()

    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getString(key: String, default: String) = values[key] as? String ?: default

    override fun edit(block: PreferenceStore.Editor.() -> Unit) {
        object : PreferenceStore.Editor {
            override fun putInt(key: String, value: Int) { values[key] = value }
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
            override fun putString(key: String, value: String) { values[key] = value }
        }.block()
    }

    override fun remove(keys: Collection<String>) {
        keys.forEach { values.remove(it) }
    }
}
