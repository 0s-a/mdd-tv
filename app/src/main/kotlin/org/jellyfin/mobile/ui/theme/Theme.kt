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
 */
val LocalOledTrueBlack = staticCompositionLocalOf { false }

/**
 * Builds the Material 3 Dark ColorScheme configured with Cinematic Ambient Design Tokens.
 *
 * @param oledTrueBlack When true, root backgrounds and surfaces switch from Obsidian (#080B10)
 *                      to Pure Black (#000000) to shut off AMOLED screen pixels.
 */
fun jellyfinDarkColorScheme(oledTrueBlack: Boolean = false): ColorScheme = darkColorScheme(
    primary = JellyfinColors.PrimaryIndigo,
    onPrimary = JellyfinColors.TextPrimary,
    primaryContainer = JellyfinColors.IndigoGlow,
    onPrimaryContainer = JellyfinColors.TextPrimary,
    secondary = JellyfinColors.TechSky,
    onSecondary = JellyfinColors.PureBlack,
    secondaryContainer = JellyfinColors.SurfaceGlass,
    onSecondaryContainer = JellyfinColors.TechSky,
    tertiary = JellyfinColors.RatingGold,
    onTertiary = JellyfinColors.PureBlack,
    background = if (oledTrueBlack) JellyfinColors.PureBlack else JellyfinColors.Background,
    onBackground = JellyfinColors.TextPrimary,
    surface = if (oledTrueBlack) JellyfinColors.PureBlack else JellyfinColors.SurfaceDark,
    onSurface = JellyfinColors.TextPrimary,
    surfaceVariant = JellyfinColors.SurfaceDark,
    onSurfaceVariant = JellyfinColors.TextSecondary,
    outline = JellyfinColors.BorderGlass,
    outlineVariant = JellyfinColors.SurfaceGlass,
    error = JellyfinColors.Error,
    onError = JellyfinColors.OnError
)

/**
 * JellyfinTheme applies the Cinematic Ambient Design System (Infuse + Apple TV+ aesthetics)
 * built for 120Hz AMOLED displays.
 *
 * @param oledTrueBlack Set to true to switch to True Black OLED mode (#000000).
 *                      Defaults to false (Cinematic Obsidian #080B10).
 * @param content Composable tree to render under this theme.
 */
@Composable
fun JellyfinTheme(
    oledTrueBlack: Boolean = false,
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
 * Quick accessor for Jellyfin-specific design tokens from Compose tree.
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
