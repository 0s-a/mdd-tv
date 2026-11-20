package org.jellyfin.mobile.presentation.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.SeasonInfo
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * SeasonTabs displays horizontal season selector capsules.
 * Strictly adheres to 03_MEDIA_DETAILS.md Section 9.2.
 */
@Composable
fun SeasonTabs(
    seasons: ImmutableList<SeasonInfo>,
    selectedSeasonNumber: Int,
    onSeasonSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (seasons.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = JellyfinDimens.SpaceSmall)
    ) {
        items(
            items = seasons,
            key = { it.seasonNumber }
        ) { season ->
            val isSelected = season.seasonNumber == selectedSeasonNumber
            SeasonTabItem(
                season = season,
                isSelected = isSelected,
                onClick = { onSeasonSelect(season.seasonNumber) }
            )
        }
    }
}

@Composable
private fun SeasonTabItem(
    season: SeasonInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.BorderGlass,
        label = "SeasonTabBorder"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) JellyfinColors.PrimaryIndigo.copy(alpha = 0.2f) else JellyfinColors.SurfaceGlass,
        label = "SeasonTabBg"
    )

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .glassmorphism(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                cornerRadius = JellyfinDimens.RadiusMedium,
                borderWidth = if (isSelected) JellyfinDimens.BorderMedium else JellyfinDimens.BorderThin
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceSmall)
            .semantics(mergeDescendants = true) {
                this.role = Role.Tab
                this.selected = isSelected
                this.contentDescription = "${season.seasonName}، ${season.episodeCount} حلقات"
            }
    ) {
        Text(
            text = season.seasonName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
        )
        Text(
            text = "${season.episodeCount} حلقات" + if (season.watchedCount > 0) " • ${season.watchedCount} تمت" else "",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) JellyfinColors.TechSky else JellyfinColors.TextMuted
        )
    }
}
