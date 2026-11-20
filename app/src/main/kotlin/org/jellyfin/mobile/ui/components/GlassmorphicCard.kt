package org.jellyfin.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * Backward-compatible alias for existing screens.
 * Now applies the calm, matte smoked surface rather than flashy neon glass.
 */
fun Modifier.glassmorphism(
    backgroundColor: Color = JellyfinColors.SurfaceGlass,
    borderColor: Color = JellyfinColors.BorderGlass,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    borderWidth: Dp = JellyfinDimens.BorderHairline
): Modifier = obsidianCard(backgroundColor, borderColor, cornerRadius, borderWidth)

/**
 * Backward-compatible alias for GlassmorphicCard.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = JellyfinColors.SurfaceGlass,
    borderColor: Color = JellyfinColors.BorderGlass,
    borderWidth: Dp = JellyfinDimens.BorderHairline,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) = ObsidianCard(
    modifier = modifier,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    borderWidth = borderWidth,
    cornerRadius = cornerRadius,
    onClick = onClick,
    enabled = enabled,
    contentDescription = contentDescription,
    content = content
)
