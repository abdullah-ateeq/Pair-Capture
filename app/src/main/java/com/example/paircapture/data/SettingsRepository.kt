package com.example.paircapture.data

import com.example.paircapture.game.model.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmRestart: Boolean = true,
    val showTurnCounter: Boolean = true
)

class SettingsRepository(private val store: PreferenceStore) {

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(store.getString(KEY_THEME, defaults.themeMode.name), defaults.themeMode),
            dynamicColor = store.getBoolean(KEY_DYNAMIC, defaults.dynamicColor),
            difficulty = enumOrDefault(store.getString(KEY_DIFFICULTY, defaults.difficulty.name), defaults.difficulty),
            sound = store.getBoolean(KEY_SOUND, defaults.sound),
            haptics = store.getBoolean(KEY_HAPTICS, defaults.haptics),
            animations = store.getBoolean(KEY_ANIMATIONS, defaults.animations),
            confirmRestart = store.getBoolean(KEY_CONFIRM_RESTART, defaults.confirmRestart),
            showTurnCounter = store.getBoolean(KEY_TURN_COUNTER, defaults.showTurnCounter)
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        store.edit {
            putString(KEY_THEME, updated.themeMode.name)
            putBoolean(KEY_DYNAMIC, updated.dynamicColor)
            putString(KEY_DIFFICULTY, updated.difficulty.name)
            putBoolean(KEY_SOUND, updated.sound)
            putBoolean(KEY_HAPTICS, updated.haptics)
            putBoolean(KEY_ANIMATIONS, updated.animations)
            putBoolean(KEY_CONFIRM_RESTART, updated.confirmRestart)
            putBoolean(KEY_TURN_COUNTER, updated.showTurnCounter)
        }
        _settings.value = updated
    }

    fun reset() {
        store.remove(ALL_KEYS)
        _settings.value = AppSettings()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_CONFIRM_RESTART = "confirm_restart"
        private const val KEY_TURN_COUNTER = "show_turn_counter"
        private val ALL_KEYS = listOf(
            KEY_THEME, KEY_DYNAMIC, KEY_DIFFICULTY, KEY_SOUND,
            KEY_HAPTICS, KEY_ANIMATIONS, KEY_CONFIRM_RESTART, KEY_TURN_COUNTER
        )
    }
}
