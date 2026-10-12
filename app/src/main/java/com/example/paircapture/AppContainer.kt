package com.example.paircapture

import android.content.Context
import com.example.paircapture.data.SettingsRepository
import com.example.paircapture.data.SharedPreferencesStore
import com.example.paircapture.data.StatisticsRepository

/** Process-wide dependencies. Created once by [PairCaptureApplication]. */
class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(SharedPreferencesStore(context, "pair_capture_settings"))
    val statisticsRepository = StatisticsRepository(SharedPreferencesStore(context, "pair_capture_stats"))
}
