package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * SearchIdleView renders recent search queries and rich visual genre cards.
 * Strictly adheres to 05_SEARCH_AND_EXPLORE.md Section 2 (IDLE EXPLORE MODE).
 */
@Composable
fun SearchIdleView(
    recentQueries: ImmutableList<String>,
    genres: ImmutableList<GenreCategory>,
    onQueryClick: (String) -> Unit,
    onDeleteQuery: (String) -> Unit,
    onClearAllQueries: () -> Unit,
    onGenreClick: (GenreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 1. Recent Search Queries Row
        if (recentQueries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عمليات البحث الأخيرة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                TextButton(onClick = onClearAllQueries) {
                    Text(
                        text = "مسح الكل",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = recentQueries,
                    key = { it }
                ) { query ->
                    RecentQueryChip(
                        query = query,
                        onClick = { onQueryClick(query) },
                        onDelete = { onDeleteQuery(query) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
        }

        // 2. Explore by Genre Title
        Text(
            text = "استكشف حسب التصنيف",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            )
        )

        // 3. 2-Column Grid of Genre Cards
        val displayedGenres = if (genres.isNotEmpty()) genres else GenreCategory.getDefaultExploreGenres().toImmutableList()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            ),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = displayedGenres,
                key = { it.id }
            ) { genre ->
                GenreExploreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre) }
                )
            }
        }
    }
}

@Composable
private fun RecentQueryChip(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusPill
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(start = JellyfinDimens.SpaceMedium, end = JellyfinDimens.SpaceExtraSmall)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "بحث سابق: $query"
            }
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = JellyfinColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.labelMedium,
            color = JellyfinColors.TextPrimary
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "حذف $query من السجل"
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = JellyfinColors.TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun GenreExploreCard(
    genre: GenreCategory,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(genre.gradientStartHex),
            Color(genre.gradientEndHex)
        )
    )

    val iconVector = when (genre.iconName) {
        "RocketLaunch" -> Icons.Rounded.RocketLaunch
        "FlashOn" -> Icons.Rounded.FlashOn
        "TheaterComedy" -> Icons.Rounded.TheaterComedy
        "VisibilityOff" -> Icons.Rounded.VisibilityOff
        "Mood" -> Icons.Rounded.Mood
        "Explore" -> Icons.Rounded.Explore
        else -> Icons.Rounded.Movie
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .background(gradientBrush)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "تصنيف ${genre.name}"
            }
    ) {
        // Subtle Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JellyfinColors.PureBlack.copy(alpha = 0.25f))
        )

        // Watermark icon in background
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .padding(end = 4.dp, bottom = 4.dp)
        )

        // Title and Icon
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
