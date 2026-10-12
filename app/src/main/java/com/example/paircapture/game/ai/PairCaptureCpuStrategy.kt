package com.example.paircapture.game.ai

import com.example.paircapture.game.model.Coordinate
import com.example.paircapture.game.model.Difficulty
import kotlin.random.Random

/**
 * Chooses CPU tiles using only a [CpuVisibleState]. It never sees the internal board.
 *
 * Easy guesses and only sometimes uses its short memory, Normal reliably uses what it
 * remembers, Hard always plays a remembered pair and never wastes a pick on a tile it
 * already knows cannot match.
 */
class PairCaptureCpuStrategy(
    private val difficulty: Difficulty,
    private val random: Random = Random.Default
) {

    /** How many CPU turns an entry survives in memory, or null for "forever". */
    val memoryLifetime: Int?
        get() = when (difficulty) {
            Difficulty.EASY -> EASY_MEMORY_TURNS
            Difficulty.NORMAL, Difficulty.HARD -> null
        }

    private val recallChance: Double
        get() = when (difficulty) {
            Difficulty.EASY -> 0.4
            Difficulty.NORMAL -> 0.85
            Difficulty.HARD -> 1.0
        }

    private fun recalls(): Boolean = random.nextDouble() < recallChance

    fun prepareMemory(memory: CpuMemory, cpuTurn: Int): CpuMemory =
        memoryLifetime?.let { memory.expire(cpuTurn, it) } ?: memory

    fun chooseFirst(view: CpuVisibleState): Coordinate {
        val selectable = view.selectableCoordinates
        require(selectable.size >= 2) { "CPU needs at least two hidden tiles" }

        view.memory.knownPair(selectable)?.let { (first, _) ->
            if (recalls()) return first
        }
        return pickExploring(selectable, view.memory)
    }

    fun chooseSecond(view: CpuVisibleState, first: Coordinate): Coordinate {
        val firstSymbol = requireNotNull(view.faceUpTiles[first]) { "First tile must be face up" }
        val candidates = view.selectableCoordinates - first
        require(candidates.isNotEmpty()) { "No second tile available" }

        view.memory.locationOf(firstSymbol, exclude = first, within = candidates)?.let { match ->
            if (recalls()) return match
        }
        return pickExploring(candidates, view.memory)
    }

    private fun pickExploring(candidates: Set<Coordinate>, memory: CpuMemory): Coordinate {
        val unknown = candidates.filterNot { it in memory.knownTiles }
        val pool = when {
            difficulty == Difficulty.EASY -> candidates.toList()
            unknown.isNotEmpty() -> unknown
            else -> candidates.toList()
        }
        return pool.sortedWith(compareBy({ it.row }, { it.column })).random(random)
    }

    companion object {
        const val EASY_MEMORY_TURNS = 1
    }
}
