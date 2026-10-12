package com.example.paircapture.data

import com.example.paircapture.InMemoryPreferenceStore
import com.example.paircapture.game.model.Difficulty
import com.example.paircapture.game.model.MatchSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistenceTest {

    @Test
    fun `settings have documented defaults`() {
        val settings = SettingsRepository(InMemoryPreferenceStore()).settings.value
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(true, settings.dynamicColor)
        assertEquals(Difficulty.NORMAL, settings.difficulty)
        assertEquals(true, settings.sound)
        assertEquals(true, settings.haptics)
        assertEquals(true, settings.animations)
        assertEquals(true, settings.confirmRestart)
        assertEquals(true, settings.showTurnCounter)
    }

    @Test
    fun `settings persist across repository instances and reset to defaults`() {
        val store = InMemoryPreferenceStore()
        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            difficulty = Difficulty.HARD,
            sound = false,
            haptics = false,
            animations = false,
            confirmRestart = false,
            showTurnCounter = false
        )
        SettingsRepository(store).update { changed }
        assertEquals(changed, SettingsRepository(store).settings.value)

        val repo = SettingsRepository(store)
        repo.reset()
        assertEquals(AppSettings(), repo.settings.value)
        assertEquals(AppSettings(), SettingsRepository(store).settings.value)
    }

    @Test
    fun `corrupt enum values fall back to defaults`() {
        val store = InMemoryPreferenceStore().apply {
            values["theme_mode"] = "NEON"
            values["difficulty"] = "IMPOSSIBLE"
        }
        val settings = SettingsRepository(store).settings.value
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(Difficulty.NORMAL, settings.difficulty)
    }

    private fun summary(player: Int, cpu: Int) = MatchSummary(player, cpu, turns = 12, matches = 8, longestPairStreak = 2)

    @Test
    fun `statistics track outcomes, best score and win streak`() {
        var stats = GameStatistics()
        stats = stats.record(summary(5, 3)) // win
        stats = stats.record(summary(6, 2)) // win
        assertEquals(2, stats.currentWinStreak)
        stats = stats.record(summary(4, 4)) // draw: streak unchanged
        assertEquals(2, stats.currentWinStreak)
        stats = stats.record(summary(2, 6)) // loss: streak reset
        assertEquals(0, stats.currentWinStreak)
        stats = stats.record(summary(8, 0)) // win

        assertEquals(5, stats.gamesPlayed)
        assertEquals(3, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(1, stats.draws)
        assertEquals(8, stats.bestScore)
        assertEquals(1, stats.currentWinStreak)
        assertEquals(2, stats.bestWinStreak)
        assertEquals(25, stats.totalPairsCaptured)
        assertEquals(5.0f, stats.averagePairs, 0.001f)
        assertEquals(60, stats.winRate)
    }

    @Test
    fun `empty statistics avoid division by zero`() {
        assertEquals(0, GameStatistics().winRate)
        assertEquals(0f, GameStatistics().averagePairs, 0f)
    }

    @Test
    fun `statistics persist and reset`() {
        val store = InMemoryPreferenceStore()
        StatisticsRepository(store).record(summary(5, 3))
        val reloaded = StatisticsRepository(store)
        assertEquals(1, reloaded.statistics.value.gamesPlayed)
        assertEquals(5, reloaded.statistics.value.bestScore)
        reloaded.reset()
        assertEquals(GameStatistics(), StatisticsRepository(store).statistics.value)
    }
}
