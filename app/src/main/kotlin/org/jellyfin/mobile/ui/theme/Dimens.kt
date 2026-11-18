package org.jellyfin.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design system dimensional tokens to prevent magic numbers across Compose UI components.
 * Adheres strictly to docs/DESIGN_SYSTEM.md:
 * - Minimum interactive touch target >= 48.dp (WCAG 2.1 AA)
 * - Standard corner radius for glassmorphism = 16.dp
 * - Standard border width = 1.dp
 */
@Immutable
object JellyfinDimens {
    // Spacing scale
    val SpaceNone: Dp = 0.dp
    val SpaceExtraSmall: Dp = 4.dp
    val SpaceSmall: Dp = 8.dp
    val SpaceMedium: Dp = 12.dp
    val SpaceLarge: Dp = 16.dp
    val SpaceExtraLarge: Dp = 24.dp
    val Space2XL: Dp = 32.dp
    val Space3XL: Dp = 48.dp

    // Corner Radii
    val RadiusSmall: Dp = 8.dp
    val RadiusMedium: Dp = 12.dp
    val RadiusDefault: Dp = 16.dp
    val RadiusLarge: Dp = 24.dp
    val RadiusPill: Dp = 999.dp
    val RadiusFull: Dp = 999.dp

    // Borders
    val BorderThin: Dp = 1.dp
    val BorderMedium: Dp = 2.dp

    // Touch Targets (WCAG 2.1 AA Compliance)
    val MinTouchTarget: Dp = 48.dp

    // Elevation & Offsets
    val ElevationNone: Dp = 0.dp
    val ElevationLow: Dp = 4.dp
    val ElevationMedium: Dp = 8.dp
    val ElevationHigh: Dp = 16.dp
}
