package org.jellyfin.mobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal indicating whether OLED True Black mode is enabled.
 * Defaults to true for AMOLED/OLED displays.
 */
val LocalOledTrueBlack = staticCompositionLocalOf { true }

/**
 * Builds Material 3 ColorScheme with Refined Cinema Obsidian tokens.
 */
fun jellyfinDarkColorScheme(oledTrueBlack: Boolean = true): ColorScheme = darkColorScheme(
    primary = JellyfinPrimitives.Ivory50,
    onPrimary = JellyfinPrimitives.Obsidian950,
    primaryContainer = JellyfinPrimitives.Graphite800,
    onPrimaryContainer = JellyfinPrimitives.Gray100,
    secondary = JellyfinPrimitives.AmberWarm,
    onSecondary = JellyfinPrimitives.Obsidian950,
    secondaryContainer = JellyfinPrimitives.Graphite900,
    onSecondaryContainer = JellyfinPrimitives.Gray100,
    tertiary = JellyfinPrimitives.GoldCinema,
    onTertiary = JellyfinPrimitives.BlackOled,
    background = if (oledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Obsidian950,
    onBackground = JellyfinPrimitives.Gray100,
    surface = if (oledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Graphite900,
    onSurface = JellyfinPrimitives.Gray100,
    surfaceVariant = JellyfinPrimitives.Graphite800,
    onSurfaceVariant = JellyfinPrimitives.Gray400,
    outline = JellyfinColors.BorderHairline,
    outlineVariant = JellyfinPrimitives.Graphite800,
    error = JellyfinPrimitives.CrimsonMuted,
    onError = JellyfinPrimitives.WhitePure
)

/**
 * JellyfinTheme applies the Refined Cinema & Obsidian Design System (v2.2.0).
 */
@Composable
fun JellyfinTheme(
    oledTrueBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = jellyfinDarkColorScheme(oledTrueBlack = oledTrueBlack)

    CompositionLocalProvider(
        LocalOledTrueBlack provides oledTrueBlack
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JellyfinTypography,
            content = content
        )
    }
}

/**
 * Quick accessor for Jellyfin design tokens.
 */
object JellyfinTheme {
    val colors: JellyfinColors
        @Composable
        @ReadOnlyComposable
        get() = JellyfinColors

    val isOledTrueBlack: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalOledTrueBlack.current
}
