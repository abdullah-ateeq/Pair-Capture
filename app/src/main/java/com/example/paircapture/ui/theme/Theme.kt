package com.example.paircapture.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.paircapture.data.ThemeMode
import com.example.paircapture.game.model.PairSymbol

private val LightColors = lightColorScheme(
    primary = TealLight,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = SlateLight,
    onSecondary = Color.White,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = Charcoal,
    tertiary = ClayLight,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainerLight,
    onTertiaryContainer = OnClayContainerLight,
    background = WarmWhite,
    onBackground = Charcoal,
    surface = WarmSurface,
    onSurface = Charcoal,
    surfaceVariant = LightContainerHigh,
    onSurfaceVariant = SoftCharcoal,
    surfaceContainerLowest = LightContainerLowest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
    surfaceBright = WarmSurface,
    surfaceDim = LightContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = OnTealDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = SlateDark,
    onSecondary = NearBlack,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SoftWhite,
    tertiary = ClayDark,
    onTertiary = OnClayDark,
    tertiaryContainer = ClayContainerDark,
    onTertiaryContainer = OnClayContainerDark,
    background = NearBlack,
    onBackground = SoftWhite,
    surface = DarkSurface,
    onSurface = SoftWhite,
    surfaceVariant = DarkContainerHigh,
    onSurfaceVariant = DimWhite,
    surfaceContainerLowest = DarkContainerLowest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    surfaceBright = DarkContainerHighest,
    surfaceDim = NearBlack,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark
)

/** Game-specific color roles, derived from the active scheme so they follow dynamic color. */
@Immutable
data class GameColors(
    val player: Color,
    val onPlayer: Color,
    val playerContainer: Color,
    val onPlayerContainer: Color,
    val cpu: Color,
    val onCpu: Color,
    val cpuContainer: Color,
    val onCpuContainer: Color,
    val board: Color,
    val tileBack: Color,
    val tileBackMark: Color,
    val tileFront: Color,
    val symbolInks: List<Color>
) {
    fun ink(symbol: PairSymbol): Color = symbolInks[symbol.ordinal % symbolInks.size]
}

private fun gameColorsFor(scheme: ColorScheme, dark: Boolean) = GameColors(
    player = scheme.primary,
    onPlayer = scheme.onPrimary,
    playerContainer = scheme.primaryContainer,
    onPlayerContainer = scheme.onPrimaryContainer,
    cpu = scheme.tertiary,
    onCpu = scheme.onTertiary,
    cpuContainer = scheme.tertiaryContainer,
    onCpuContainer = scheme.onTertiaryContainer,
    board = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLow,
    tileBack = if (dark) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh,
    tileBackMark = scheme.outline,
    tileFront = if (dark) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
    symbolInks = if (dark) SymbolInksDark else SymbolInksLight
)

val LocalGameColors = staticCompositionLocalOf { gameColorsFor(LightColors, dark = false) }

object PairCaptureTheme {
    val gameColors: GameColors
        @Composable get() = LocalGameColors.current
}

@Composable
fun isAppInDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PairCaptureTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = isAppInDarkTheme(themeMode)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalGameColors provides gameColorsFor(colorScheme, dark)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
