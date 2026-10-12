package com.example.paircapture.data

import com.example.paircapture.game.model.GameOutcome
import com.example.paircapture.game.model.MatchSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameStatistics(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val bestScore: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalPairsCaptured: Int = 0
) {
    /** Win percentage, 0–100. */
    val winRate: Int get() = if (gamesPlayed == 0) 0 else Math.round(wins * 100f / gamesPlayed)

    val averagePairs: Float get() = if (gamesPlayed == 0) 0f else totalPairsCaptured.toFloat() / gamesPlayed

    /** Pure statistics update for a finished match. Draws leave the win streak unchanged. */
    fun record(summary: MatchSummary): GameStatistics {
        val streak = when (summary.outcome) {
            GameOutcome.WIN -> currentWinStreak + 1
            GameOutcome.LOSS -> 0
            GameOutcome.DRAW -> currentWinStreak
        }
        return copy(
            gamesPlayed = gamesPlayed + 1,
            wins = wins + if (summary.outcome == GameOutcome.WIN) 1 else 0,
            losses = losses + if (summary.outcome == GameOutcome.LOSS) 1 else 0,
            draws = draws + if (summary.outcome == GameOutcome.DRAW) 1 else 0,
            bestScore = maxOf(bestScore, summary.playerPairs),
            currentWinStreak = streak,
            bestWinStreak = maxOf(bestWinStreak, streak),
            totalPairsCaptured = totalPairsCaptured + summary.playerPairs
        )
    }
}

class StatisticsRepository(private val store: PreferenceStore) {

    private val _statistics = MutableStateFlow(load())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()

    private fun load() = GameStatistics(
        gamesPlayed = store.getInt(KEY_GAMES, 0),
        wins = store.getInt(KEY_WINS, 0),
        losses = store.getInt(KEY_LOSSES, 0),
        draws = store.getInt(KEY_DRAWS, 0),
        bestScore = store.getInt(KEY_BEST_SCORE, 0),
        currentWinStreak = store.getInt(KEY_STREAK, 0),
        bestWinStreak = store.getInt(KEY_BEST_STREAK, 0),
        totalPairsCaptured = store.getInt(KEY_TOTAL_PAIRS, 0)
    )

    fun record(summary: MatchSummary): GameStatistics {
        val updated = _statistics.value.record(summary)
        save(updated)
        return updated
    }

    fun reset() {
        store.remove(ALL_KEYS)
        _statistics.value = GameStatistics()
    }

    private fun save(stats: GameStatistics) {
        store.edit {
            putInt(KEY_GAMES, stats.gamesPlayed)
            putInt(KEY_WINS, stats.wins)
            putInt(KEY_LOSSES, stats.losses)
            putInt(KEY_DRAWS, stats.draws)
            putInt(KEY_BEST_SCORE, stats.bestScore)
            putInt(KEY_STREAK, stats.currentWinStreak)
            putInt(KEY_BEST_STREAK, stats.bestWinStreak)
            putInt(KEY_TOTAL_PAIRS, stats.totalPairsCaptured)
        }
        _statistics.value = stats
    }

    companion object {
        private const val KEY_GAMES = "games_played"
        private const val KEY_WINS = "wins"
        private const val KEY_LOSSES = "losses"
        private const val KEY_DRAWS = "draws"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_STREAK = "current_win_streak"
        private const val KEY_BEST_STREAK = "best_win_streak"
        private const val KEY_TOTAL_PAIRS = "total_pairs_captured"
        private val ALL_KEYS = listOf(
            KEY_GAMES, KEY_WINS, KEY_LOSSES, KEY_DRAWS,
            KEY_BEST_SCORE, KEY_STREAK, KEY_BEST_STREAK, KEY_TOTAL_PAIRS
        )
    }
}
