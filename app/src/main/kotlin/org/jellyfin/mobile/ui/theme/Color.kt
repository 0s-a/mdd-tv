package org.jellyfin.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Refined Cinema & Obsidian Design System (Tokens 2.2.0).
 * Implements docs/foundations/design_system.md:
 * - Anti-AI Manifesto: Zero neon aura, zero flashy cyan glass, 100% matte graphite and OLED true black.
 * - 3-Tier Token Architecture: Primitives -> States -> Semantic Tokens.
 */

// ──────────────────────────────────────────
// 1. Primitive Tokens (Raw Palette)
// ──────────────────────────────────────────
object JellyfinPrimitives {
    // Carbon & Obsidian
    val BlackOled = Color(0xFF000000)
    val Obsidian950 = Color(0xFF0B0C0E)
    val Graphite900 = Color(0xFF14171D)
    val Graphite800 = Color(0xFF1B1E26)
    val Slate700 = Color(0xFF272B35)

    // Neutral & Typography
    val Ivory50 = Color(0xFFEDEAE5)
    val WhitePure = Color(0xFFFFFFFF)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray700 = Color(0xFF374151)

    // Accent & Cinema Status
    val AmberWarm = Color(0xFFD4A373)
    val GoldCinema = Color(0xFFE5A93C)
    val CrimsonMuted = Color(0xFF9E4343)
    val EmeraldMuted = Color(0xFF2D8A66)
}

// ──────────────────────────────────────────
// 2. State Layer Tokens (Opacity & Interaction)
// ──────────────────────────────────────────
@Immutable
object JellyfinStates {
    const val DraggedOpacity: Float = 0.16f
    const val FocusOpacity: Float = 0.12f
    const val PressedOpacity: Float = 0.10f
    const val HoverOpacity: Float = 0.08f
    const val DisabledContainerOpacity: Float = 0.12f
    const val DisabledContentOpacity: Float = 0.38f
}

// ──────────────────────────────────────────
// 3. Semantic Color Tokens
// ──────────────────────────────────────────
@Immutable
data class JellyfinColorTokens(
    val backgroundRoot: Color,
    val onBackgroundRoot: Color,
    val surfaceDefault: Color,
    val onSurfaceDefault: Color,
    val surfaceElevated: Color,
    val onSurfaceElevated: Color,
    val surfaceOverlay: Color,
    val borderHairline: Color,
    val borderFocus: Color,
    val actionPrimary: Color,
    val onActionPrimary: Color,
    val accentHighlight: Color,
    val ratingBadge: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val statusSuccess: Color,
    val statusAlert: Color
)

fun createJellyfinColorScheme(isOledTrueBlack: Boolean = true): JellyfinColorTokens {
    return JellyfinColorTokens(
        backgroundRoot = if (isOledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Obsidian950,
        onBackgroundRoot = JellyfinPrimitives.Gray100,
        surfaceDefault = if (isOledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Graphite900,
        onSurfaceDefault = JellyfinPrimitives.Gray100,
        surfaceElevated = JellyfinPrimitives.Graphite800,
        onSurfaceElevated = JellyfinPrimitives.Gray100,
        surfaceOverlay = Color(0x990B0C0E),
        borderHairline = Color(0x1AFFFFFF), // Hairline 10% white for crisp contrast
        borderFocus = Color(0x80FFFFFF),    // 50% white focus border for 4.2:1 contrast
        actionPrimary = JellyfinPrimitives.Ivory50,
        onActionPrimary = JellyfinPrimitives.Obsidian950,
        accentHighlight = JellyfinPrimitives.AmberWarm,
        ratingBadge = JellyfinPrimitives.GoldCinema,
        textPrimary = JellyfinPrimitives.Gray100,
        textSecondary = JellyfinPrimitives.Gray400,
        textMuted = JellyfinPrimitives.Gray500,
        statusSuccess = JellyfinPrimitives.EmeraldMuted,
        statusAlert = JellyfinPrimitives.CrimsonMuted
    )
}

// ──────────────────────────────────────────
// 4. Backward-Compatible Semantic Object
// Maps legacy references directly to the new Obsidian & Matte Palette
// ──────────────────────────────────────────
@Immutable
object JellyfinColors {
    // OLED & Matte Obsidian Surfaces
    val PureBlack: Color = JellyfinPrimitives.BlackOled
    val Background: Color = JellyfinPrimitives.BlackOled
    val SurfaceDark: Color = JellyfinPrimitives.Graphite900
    val SurfaceElevated: Color = JellyfinPrimitives.Graphite800
    val SurfaceGlass: Color = JellyfinPrimitives.Graphite800
    val BorderGlass: Color = Color(0x1AFFFFFF)
    val BorderHairline: Color = Color(0x1AFFFFFF)
    val BorderFocus: Color = Color(0x80FFFFFF)

    // Brand, Accents & Badges
    val PrimaryIndigo: Color = JellyfinPrimitives.Ivory50
    val IndigoGlow: Color = Color(0x14FFFFFF)
    val TechSky: Color = JellyfinPrimitives.Gray400
    val RatingGold: Color = JellyfinPrimitives.GoldCinema
    val SuccessEmerald: Color = JellyfinPrimitives.EmeraldMuted
    val AccentAmber: Color = JellyfinPrimitives.AmberWarm
    val AmberWarm: Color = JellyfinPrimitives.AmberWarm
    val AccentCyan: Color = Color(0xFF38BDF8)

    // Typography Contrast (WCAG 2.1 & 2.2 Compliance)
    val TextPrimary: Color = JellyfinPrimitives.Gray100
    val TextSecondary: Color = JellyfinPrimitives.Gray400
    val TextMuted: Color = JellyfinPrimitives.Gray500
    val TextTertiary: Color = JellyfinPrimitives.Gray500
    val GlassBorder: Color = Color(0x1AFFFFFF)
    val SurfaceGlassActive: Color = JellyfinPrimitives.Graphite800

    // Alerts & Statuses
    val Error: Color = JellyfinPrimitives.CrimsonMuted
    val OnError: Color = JellyfinPrimitives.WhitePure
    val StatusAlert: Color = JellyfinPrimitives.CrimsonMuted
    val StatusSuccess: Color = JellyfinPrimitives.EmeraldMuted
}

// Top-level property aliases for convenience
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
val AccentCyan = JellyfinColors.AccentCyan
val AmberWarm = JellyfinColors.AmberWarm
