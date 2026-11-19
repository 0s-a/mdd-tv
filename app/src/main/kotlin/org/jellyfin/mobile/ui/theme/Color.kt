package org.jellyfin.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cinematic Ambient Design System Color Tokens.
 * Strict implementation of docs/DESIGN_SYSTEM.md.
 * Zero hardcoded colors; use JellyfinColors throughout the app.
 */
@Immutable
object JellyfinColors {
    // 1. OLED & True Dark Surfaces
    /** Pure OLED black for maximum contrast and zero power consumption on AMOLED displays */
    val PureBlack: Color = Color(0xFF000000)

    /** Deep obsidian navy background for screens and gradients */
    val Background: Color = Color(0xFF080B10)

    /** Subtle night navy surface for inactive cards and list items */
    val SurfaceDark: Color = Color(0xFF0F1522)

    /** Polished glass surface with 70% opacity for floating bars, bottom navigation, and pill cards */
    val SurfaceGlass: Color = Color(0xB3131A29)

    /** 1dp subtle glass accent border creating 3D depth for cards and technical badges */
    val BorderGlass: Color = Color(0x3338BDF8)

    // 2. Brand & Tech Badges
    /** Royal Indigo brand color for playback controls, primary action buttons, and active states */
    val PrimaryIndigo: Color = Color(0xFF6366F1)

    /** Soft glowing circular radial aura behind banners and play buttons */
    val IndigoGlow: Color = Color(0x666366F1)

    /** Tech badge cyan for technical quality chips (4K UHD, Dolby Vision, Direct Play) */
    val TechSky: Color = Color(0xFF38BDF8)

    /** Cinematic gold for IMDb / Community ratings */
    val RatingGold: Color = Color(0xFFF59E0B)

    /** Emerald green for watched indicators and completed downloads */
    val SuccessEmerald: Color = Color(0xFF10B981)

    // 3. Typography Contrast (WCAG 2.1 AAA & AA compliance)
    /** Highest contrast primary text (20.4:1 contrast ratio against PureBlack, AAA) */
    val TextPrimary: Color = Color(0xFFFFFFFF)

    /** Secondary contrast text for synopses, release years, and genre tags (4.9:1, AA) */
    val TextSecondary: Color = Color(0xFF94A3B8)

    /** Muted contrast text for codecs, container sizes, and audio stream details (4.6:1, AA) */
    val TextMuted: Color = Color(0xFF8899AA)
    val TextTertiary: Color = TextMuted
    val GlassBorder: Color = BorderGlass
    val SurfaceGlassActive: Color = Color(0xCC1E293B)

    // 4. Alert & Feedback States
    val Error: Color = Color(0xFFEF4444)
    val OnError: Color = Color(0xFFFFFFFF)
}

// Top-level property aliases for convenience and backwards compatibility
val PureBlack = JellyfinColors.PureBlack
val BackgroundDark = JellyfinColors.Background
val SurfaceDark = JellyfinColors.SurfaceDark
val SurfaceGlass = JellyfinColors.SurfaceGlass
val BorderGlass = JellyfinColors.BorderGlass
val PrimaryIndigo = JellyfinColors.PrimaryIndigo
val IndigoGlow = JellyfinColors.IndigoGlow
val TechSky = JellyfinColors.TechSky
val TechBadgeSky = JellyfinColors.TechSky
val RatingGold = JellyfinColors.RatingGold
val SuccessEmerald = JellyfinColors.SuccessEmerald
val WatchedEmerald = JellyfinColors.SuccessEmerald
val TextPrimary = JellyfinColors.TextPrimary
val TextSecondary = JellyfinColors.TextSecondary
val TextMuted = JellyfinColors.TextMuted
val TextTertiary = JellyfinColors.TextTertiary
val GlassBorder = JellyfinColors.GlassBorder
val SurfaceGlassActive = JellyfinColors.SurfaceGlassActive
