package com.example.paircapture.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paircapture.AppContainer
import com.example.paircapture.game.PairCaptureViewModel
import com.example.paircapture.game.model.MatchSummary
import com.example.paircapture.ui.AppViewModel
import com.example.paircapture.ui.screens.AboutScreen
import com.example.paircapture.ui.screens.GameScreen
import com.example.paircapture.ui.screens.HomeScreen
import com.example.paircapture.ui.screens.ResultScreen
import com.example.paircapture.ui.screens.SettingsScreen
import com.example.paircapture.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val RESULT = "result/{player}/{cpu}/{turns}/{matches}/{run}"

    fun result(summary: MatchSummary) =
        "result/${summary.playerPairs}/${summary.cpuPairs}/${summary.turns}/${summary.matches}/${summary.longestPairStreak}"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val TopLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info)
)

@Composable
fun PairCaptureNavHost(
    container: AppContainer,
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val statistics by appViewModel.statistics.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(animations = settings.animations) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
            composable(Routes.HOME) {
                HomeScreen(
                    statistics = statistics,
                    settings = settings,
                    onPlay = { navController.navigate(Routes.GAME) { launchSingleTop = true } }
                )
            }
            composable(Routes.GAME) {
                val gameViewModel: PairCaptureViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PairCaptureViewModel(container.settingsRepository, container.statisticsRepository)
                        }
                    }
                )
                GameScreen(
                    viewModel = gameViewModel,
                    settings = settings,
                    onExit = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onFinished = { summary ->
                        navController.navigate(Routes.result(summary)) {
                            popUpTo(Routes.GAME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                Routes.RESULT,
                arguments = listOf("player", "cpu", "turns", "matches", "run").map {
                    navArgument(it) { type = NavType.IntType }
                }
            ) { entry ->
                val args = requireNotNull(entry.arguments)
                val summary = MatchSummary(
                    playerPairs = args.getInt("player"),
                    cpuPairs = args.getInt("cpu"),
                    turns = args.getInt("turns"),
                    matches = args.getInt("matches"),
                    longestPairStreak = args.getInt("run")
                )
                ResultScreen(
                    summary = summary,
                    statistics = statistics,
                    animations = settings.animations,
                    onPlayAgain = {
                        navController.navigate(Routes.GAME) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                    onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onUpdate = appViewModel::updateSettings,
                    onResetStatistics = appViewModel::resetStatistics,
                    onResetSettings = appViewModel::resetSettings
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen()
            }
        }
    }
}
