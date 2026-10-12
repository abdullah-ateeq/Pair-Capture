package com.example.paircapture.ui.theme

import androidx.compose.ui.graphics.Color

// Light palette: warm white, soft grays, charcoal, muted teal player, muted clay CPU.
internal val WarmWhite = Color(0xFFF8F6F2)
internal val WarmSurface = Color(0xFFFCFBF8)
internal val LightContainerLowest = Color(0xFFFFFFFF)
internal val LightContainerLow = Color(0xFFF2F0EB)
internal val LightContainer = Color(0xFFECE9E3)
internal val LightContainerHigh = Color(0xFFE5E2DB)
internal val LightContainerHighest = Color(0xFFDDD9D1)
internal val Charcoal = Color(0xFF22252A)
internal val SoftCharcoal = Color(0xFF5B5F66)
internal val LightOutline = Color(0xFF8B8E94)
internal val LightOutlineVariant = Color(0xFFCFCCC5)

internal val TealLight = Color(0xFF2D6B70)
internal val TealContainerLight = Color(0xFFCBE5E4)
internal val OnTealContainerLight = Color(0xFF0E3437)
internal val SlateLight = Color(0xFF55606E)
internal val SlateContainerLight = Color(0xFFDCE2EA)
internal val ClayLight = Color(0xFFA0583A)
internal val ClayContainerLight = Color(0xFFF4D9CB)
internal val OnClayContainerLight = Color(0xFF41190A)
internal val ErrorLight = Color(0xFFB3261E)

// Dark palette: near-black, graphite, soft white, muted accents.
internal val NearBlack = Color(0xFF0F1012)
internal val DarkSurface = Color(0xFF141518)
internal val DarkContainerLowest = Color(0xFF0B0C0D)
internal val DarkContainerLow = Color(0xFF191A1D)
internal val DarkContainer = Color(0xFF1E2023)
internal val DarkContainerHigh = Color(0xFF26282C)
internal val DarkContainerHighest = Color(0xFF303237)
internal val SoftWhite = Color(0xFFE9E7E3)
internal val DimWhite = Color(0xFFB4B2AE)
internal val DarkOutline = Color(0xFF7D7F84)
internal val DarkOutlineVariant = Color(0xFF3B3D42)

internal val TealDark = Color(0xFF86C3C2)
internal val OnTealDark = Color(0xFF00363A)
internal val TealContainerDark = Color(0xFF1E4A4D)
internal val OnTealContainerDark = Color(0xFFC6E8E6)
internal val SlateDark = Color(0xFFB7C2D0)
internal val SlateContainerDark = Color(0xFF353D48)
internal val ClayDark = Color(0xFFE5A585)
internal val OnClayDark = Color(0xFF4A1E0B)
internal val ClayContainerDark = Color(0xFF5A3020)
internal val OnClayContainerDark = Color(0xFFF7DCCD)
internal val ErrorDark = Color(0xFFF2B8B5)

// Symbol inks: muted, distinct hues. Shape always differs too, so color is never the only cue.
internal val SymbolInksLight = listOf(
    Color(0xFF2D6B70), // circle – teal
    Color(0xFFA0583A), // triangle – clay
    Color(0xFF4F5B93), // square – indigo
    Color(0xFFA77B1E), // star – ochre
    Color(0xFF8A4A6E), // diamond – plum
    Color(0xFF4F7A45), // hexagon – moss
    Color(0xFF3F6C93), // moon – slate blue
    Color(0xFF9A6A12)  // bolt – amber
)

internal val SymbolInksDark = listOf(
    Color(0xFF86C3C2),
    Color(0xFFE5A585),
    Color(0xFFA9B3E6),
    Color(0xFFE4C374),
    Color(0xFFD9A3C2),
    Color(0xFFA6CF98),
    Color(0xFF9DC2E3),
    Color(0xFFF0C26B)
)
