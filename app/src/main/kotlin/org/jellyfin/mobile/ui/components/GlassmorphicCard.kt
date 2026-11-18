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
 * Modifier extension applying the cinematic polished glass (Glassmorphism) effect.
 * Strictly adheres to Section 4 of docs/DESIGN_SYSTEM.md.
 *
 * @param backgroundColor The translucent surface fill color (default 70% dark glass).
 * @param borderColor The subtle glass outline border color (default 20% cyan glass).
 * @param cornerRadius The radius of the rounded corners (default 16.dp).
 * @param borderWidth The stroke width of the glass border (default 1.dp).
 */
fun Modifier.glassmorphism(
    backgroundColor: Color = JellyfinColors.SurfaceGlass,
    borderColor: Color = JellyfinColors.BorderGlass,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    borderWidth: Dp = JellyfinDimens.BorderThin
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
}

/**
 * GlassmorphicCard is an accessible, 120Hz-ready glassmorphic container composable.
 * Supports interactive states, full TalkBack semantics, and enforces minimum
 * 48x48dp touch targets in compliance with WCAG 2.1 AA.
 *
 * @param modifier Additional external modifiers applied to the card container.
 * @param backgroundColor Translucent surface background color.
 * @param borderColor Border accent color for three-dimensional glass depth.
 * @param borderWidth Stroke thickness for the border.
 * @param cornerRadius Corner radius of the card.
 * @param onClick Optional click callback; when non-null, makes the card interactive with
 *                a touch target of at least 48dp and TalkBack button role semantics.
 * @param enabled Whether click interactions are enabled.
 * @param contentDescription Accessibility description for TalkBack screen reader.
 * @param content Composable child content rendered inside the glass container.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = JellyfinColors.SurfaceGlass,
    borderColor: Color = JellyfinColors.BorderGlass,
    borderWidth: Dp = JellyfinDimens.BorderThin,
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val cardModifier = if (onClick != null) {
        modifier
            .defaultMinSize(
                minWidth = JellyfinDimens.MinTouchTarget,
                minHeight = JellyfinDimens.MinTouchTarget
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                this.role = Role.Button
            }
    } else {
        modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                    }
                } else {
                    Modifier
                }
            )
    }

    CompositionLocalProvider(LocalContentColor provides JellyfinColors.TextPrimary) {
        Box(
            modifier = cardModifier,
            content = content
        )
    }
}
