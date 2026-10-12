package com.example.paircapture.game.model

/**
 * The player-visible view of a tile. [symbol] is only populated while the tile is face up,
 * so the UI layer physically cannot render or announce a hidden symbol.
 */
data class VisibleTile(
    val id: Int,
    val coordinate: Coordinate,
    val state: TileState,
    val symbol: PairSymbol?
)

fun PairTile.toVisible(): VisibleTile = VisibleTile(
    id = id,
    coordinate = coordinate,
    state = state,
    symbol = if (state == TileState.REVEALED) symbol else null
)

/** Accessibility description. Built only from player-visible information. */
fun VisibleTile.accessibilityDescription(): String {
    val position = "at ${coordinate.label}, row ${coordinate.row + 1} column ${coordinate.column + 1}"
    return when (state) {
        TileState.HIDDEN -> "Hidden tile $position"
        TileState.REVEALED -> "${symbol?.displayName ?: "Revealed"} tile $position"
        TileState.CAPTURED_BY_PLAYER -> "Player captured tile $position"
        TileState.CAPTURED_BY_CPU -> "CPU captured tile $position"
    }
}
