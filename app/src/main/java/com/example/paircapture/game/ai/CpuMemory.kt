package com.example.paircapture.game.ai

import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.PairSymbol

/**
 * Everything the CPU has legitimately seen. Entries are only ever added for tiles the CPU
 * revealed itself; [seenOnCpuTurn] records when, so Easy can forget old information.
 */
data class CpuMemory(
    val knownTiles: Map<Coordinate, PairSymbol> = emptyMap(),
    val seenOnCpuTurn: Map<Coordinate, Int> = emptyMap()
) {
    fun remember(coordinate: Coordinate, symbol: PairSymbol, cpuTurn: Int): CpuMemory = copy(
        knownTiles = knownTiles + (coordinate to symbol),
        seenOnCpuTurn = seenOnCpuTurn + (coordinate to cpuTurn)
    )

    fun forget(coordinates: Collection<Coordinate>): CpuMemory {
        if (coordinates.none { it in knownTiles }) return this
        return copy(knownTiles = knownTiles - coordinates.toSet(), seenOnCpuTurn = seenOnCpuTurn - coordinates.toSet())
    }

    /** Drops entries seen more than [maxAge] CPU turns before [currentCpuTurn]. */
    fun expire(currentCpuTurn: Int, maxAge: Int): CpuMemory {
        val stale = seenOnCpuTurn.filterValues { currentCpuTurn - it > maxAge }.keys
        return forget(stale)
    }

    /** A pair of remembered coordinates sharing a symbol, restricted to [within]. */
    fun knownPair(within: Set<Coordinate>): Pair<Coordinate, Coordinate>? =
        knownTiles
            .filterKeys { it in within }
            .entries
            .groupBy({ it.value }, { it.key })
            .values
            .firstOrNull { it.size >= 2 }
            ?.let { it[0] to it[1] }

    /** A remembered coordinate (other than [exclude]) holding [symbol], restricted to [within]. */
    fun locationOf(symbol: PairSymbol, exclude: Coordinate, within: Set<Coordinate>): Coordinate? =
        knownTiles.entries.firstOrNull { (coordinate, known) ->
            known == symbol && coordinate != exclude && coordinate in within
        }?.key

    val size: Int get() = knownTiles.size
}
