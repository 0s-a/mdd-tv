package org.jellyfin.mobile.presentation.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * GenreChipsRow renders a horizontal list of interactive genre pills for TV and mobile.
 * Enables quick D-pad navigation and instant catalog filtering.
 */
@Composable
fun GenreChipsRow(
    genres: List<String>,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (genres.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxWidth()
    ) {
        items(genres) { genre ->
            GenreChip(
                genre = genre,
                onClick = { onGenreClick(genre) }
            )
        }
    }
}

/**
 * GenreChip is an interactive capsule component that scales and highlights
 * when focused via TV remote (D-pad) or touched on mobile.
 */
@Composable
fun GenreChip(
    genre: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "GenreChipScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) JellyfinColors.TextPrimary else JellyfinColors.SurfaceDark.copy(alpha = 0.7f),
        label = "GenreChipBackground"
    )

    val textColor by animateColorAsState(
        targetValue = if (isFocused) JellyfinColors.PureBlack else JellyfinColors.TextSecondary,
        label = "GenreChipTextColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) JellyfinColors.TextPrimary else JellyfinColors.BorderGlass,
        label = "GenreChipBorder"
    )

    val shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isFocused) 1.5.dp else JellyfinDimens.BorderThin,
                color = borderColor,
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceSmall)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "تصنيف $genre، انقر لاستعراض الأعمال"
            }
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
