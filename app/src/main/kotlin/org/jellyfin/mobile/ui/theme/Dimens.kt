package org.jellyfin.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Refined Cinema & Obsidian Dimensional Tokens (Tokens 2.2.0).
 * Implements docs/foundations/design_system.md Section 3:
 * - Anti-AI Manifesto: Complete ban on bloated 999dp pills.
 * - Strict 8pt/4pt mathematical scale.
 * - Minimum interactive touch target >= 48.dp (WCAG 2.1 AA).
 */
@Immutable
object JellyfinDimens {
    // 1. Spacing Scale (8pt Grid with 4pt half-steps)
    val SpaceNone: Dp = 0.dp
    val SpaceMicro: Dp = 2.dp
    val Space2XS: Dp = 4.dp
    val SpaceXS: Dp = 8.dp
    val SpaceSm: Dp = 12.dp
    val SpaceMd: Dp = 16.dp
    val SpaceLg: Dp = 24.dp
    val SpaceXL: Dp = 32.dp
    val Space2XL: Dp = 48.dp
    val Space3XL: Dp = 64.dp

    // Legacy Spacing Aliases (Maintains complete backward compatibility)
    val SpaceExtraSmall: Dp = 4.dp
    val SpaceSmall: Dp = 8.dp
    val SpaceMedium: Dp = 12.dp
    val SpaceLarge: Dp = 16.dp
    val SpaceExtraLarge: Dp = 24.dp

    // 2. Geometric Corner Radii (Rational architectural rounding, NO 999dp)
    val RadiusMicro: Dp = 4.dp       // Technical chips, indicators, badges
    val RadiusSmall: Dp = 8.dp       // Buttons, text fields, small cards
    val RadiusDefault: Dp = 10.dp    // Posters, media cards
    val RadiusMedium: Dp = 12.dp     // Floating bars, dialogue boxes
    val RadiusLarge: Dp = 16.dp      // Modal sheets, expansive containers
    val RadiusPill: Dp = 16.dp       // Rationalized pill (strictly 16dp instead of 999dp)
    val RadiusFull: Dp = 16.dp

    // 3. Borders & Hairlines
    val BorderHairline: Dp = 1.dp
    val BorderThin: Dp = 1.dp
    val BorderFocus: Dp = 2.dp
    val BorderMedium: Dp = 2.dp

    // 4. Touch Targets (WCAG 2.1 AA Compliance)
    val MinTouchTarget: Dp = 48.dp

    // 5. Elevation Offsets
    val ElevationNone: Dp = 0.dp
    val ElevationLow: Dp = 2.dp
    val ElevationMedium: Dp = 4.dp
    val ElevationHigh: Dp = 8.dp
}

// Z-Index hierarchy
object JellyfinZIndex {
    const val Base: Float = 0f
    const val Content: Float = 1f
    const val StickyHeader: Float = 10f
    const val PlayerControls: Float = 20f
    const val BottomSheet: Float = 30f
    const val ModalDialog: Float = 40f
    const val ToastNotification: Float = 50f
}
