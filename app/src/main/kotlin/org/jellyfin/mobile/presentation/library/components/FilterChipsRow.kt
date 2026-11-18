package org.jellyfin.mobile.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import org.jellyfin.mobile.domain.model.LibraryFilterChip
import org.jellyfin.mobile.domain.model.SortOption
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * FilterChipsRow renders quick horizontal glassmorphic filter capsules.
 * Strictly adheres to 02_LIBRARY_BROWSER.md Section 2:
 * - Quick toggles: Unwatched, 4K UHD only, 8.0+ rating.
 * - Sort button with current sort label.
 * - 48dp touch targets and TalkBack semantics.
 */
@Composable
fun FilterChipsRow(
    activeFilters: ImmutableSet<LibraryFilterChip>,
    selectedSort: SortOption,
    onToggleFilter: (LibraryFilterChip) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = JellyfinDimens.SpaceSmall)
    ) {
        // 1. Sort Selector Chip
        item(key = "sort_chip") {
            SortChip(
                selectedSort = selectedSort,
                onClick = onSortClick
            )
        }

        // 2. Filter Toggle Chips
        items(
            items = LibraryFilterChip.entries.toTypedArray(),
            key = { it.name }
        ) { filter ->
            val isActive = activeFilters.contains(filter)
            FilterToggleChip(
                filter = filter,
                isActive = isActive,
                onClick = { onToggleFilter(filter) }
            )
        }
    }
}

@Composable
private fun SortChip(
    selectedSort: SortOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.PrimaryIndigo.copy(alpha = 0.6f),
                cornerRadius = JellyfinDimens.RadiusPill,
                borderWidth = JellyfinDimens.BorderThin
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = JellyfinDimens.SpaceMedium,
                vertical = JellyfinDimens.SpaceSmall
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "الترتيب حسب: ${selectedSort.title}. انقر للتغيير."
            }
    ) {
        Icon(
            imageVector = Icons.Rounded.Sort,
            contentDescription = null,
            tint = JellyfinColors.TechSky,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
        Text(
            text = selectedSort.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary
        )
    }
}

@Composable
private fun FilterToggleChip(
    filter: LibraryFilterChip,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) JellyfinColors.PrimaryIndigo else JellyfinColors.BorderGlass,
        animationSpec = tween(durationMillis = 200),
        label = "FilterChipBorder"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f) else JellyfinColors.SurfaceGlass,
        animationSpec = tween(durationMillis = 200),
        label = "FilterChipBackground"
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "FilterChipText"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .glassmorphism(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                cornerRadius = JellyfinDimens.RadiusPill,
                borderWidth = if (isActive) JellyfinDimens.BorderMedium else JellyfinDimens.BorderThin
            )
            .clickable(
                role = Role.Checkbox,
                onClick = onClick
            )
            .padding(
                horizontal = JellyfinDimens.SpaceMedium,
                vertical = JellyfinDimens.SpaceSmall
            )
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                selected = isActive
                contentDescription = "فلتر ${filter.title}، ${if (isActive) "مفعل" else "غير مفعل"}"
            }
    ) {
        if (isActive) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = JellyfinColors.PrimaryIndigo,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = filter.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
