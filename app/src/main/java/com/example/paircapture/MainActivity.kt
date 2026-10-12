package com.example.paircapture

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.paircapture.navigation.PairCaptureNavHost
import com.example.paircapture.ui.AppViewModel
import com.example.paircapture.ui.theme.PairCaptureTheme
import com.example.paircapture.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PairCaptureApplication).container
        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AppViewModel(container.settingsRepository, container.statisticsRepository) }
                }
            )
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val dark = isAppInDarkTheme(settings.themeMode)
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            PairCaptureTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                PairCaptureNavHost(container = container, appViewModel = appViewModel)
            }
        }
    }
}
