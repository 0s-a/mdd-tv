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
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Modifier applying the Obsidian 2.2.0 matte smoked surface with clean hairline borders.
 * Replaces blurry neomorphic / glassmorphic glow with crisp cinematic edges.
 */
fun Modifier.obsidianCard(
    backgroundColor: Color = JellyfinPrimitives.Graphite900,
    borderColor: Color = JellyfinColors.BorderHairline,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    borderWidth: Dp = JellyfinDimens.BorderHairline
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
}

/**
 * ObsidianCard is the primary high-contrast container composable in Obsidian 2.2.0.
 * Designed for pure black OLED screens and 120Hz rendering.
 */
@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = JellyfinPrimitives.Graphite900,
    borderColor: Color = JellyfinColors.BorderHairline,
    borderWidth: Dp = JellyfinDimens.BorderHairline,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val cardModifier = modifier
        .then(
            if (onClick != null) {
                Modifier
                    .defaultMinSize(
                        minWidth = JellyfinDimens.MinTouchTarget,
                        minHeight = JellyfinDimens.MinTouchTarget
                    )
                    .clip(shape)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    )
                    .then(
                        if (contentDescription != null) {
                            Modifier.semantics {
                                this.contentDescription = contentDescription
                                this.role = Role.Button
                            }
                        } else Modifier
                    )
            } else {
                Modifier.clip(shape)
            }
        )
        .background(backgroundColor, shape)
        .border(borderWidth, borderColor, shape)

    CompositionLocalProvider(LocalContentColor provides JellyfinColors.TextPrimary) {
        Box(
            modifier = cardModifier,
            content = content
        )
    }
}
