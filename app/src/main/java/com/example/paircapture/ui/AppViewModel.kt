package com.example.paircapture.ui

import androidx.lifecycle.ViewModel
import com.example.paircapture.data.AppSettings
import com.example.paircapture.data.GameStatistics
import com.example.paircapture.data.SettingsRepository
import com.example.paircapture.data.StatisticsRepository
import kotlinx.coroutines.flow.StateFlow

/** Settings and statistics for the Home, Settings and Result screens. */
class AppViewModel(
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val statistics: StateFlow<GameStatistics> = statisticsRepository.statistics

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

    fun resetSettings() = settingsRepository.reset()

    fun resetStatistics() = statisticsRepository.reset()
}
