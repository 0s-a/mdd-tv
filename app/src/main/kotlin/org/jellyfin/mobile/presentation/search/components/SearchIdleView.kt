package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * SearchIdleView renders recent search queries, a dynamic spotlight recommendation from the server,
 * and dynamic server genres. Zero hardcoded data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchIdleView(
    recentQueries: ImmutableList<String>,
    availableGenres: ImmutableList<String>,
    spotlightItem: MediaItemSummary?,
    onQueryClick: (String) -> Unit,
    onDeleteQuery: (String) -> Unit,
    onClearAllQueries: () -> Unit,
    onGenreClick: (String) -> Unit,
    onQuickPlayClick: (String) -> Unit,
    onDetailsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Dynamic Server Spotlight (Random Recommendation)
        if (spotlightItem != null) {
            item(key = "dynamic_spotlight") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JellyfinDimens.SpaceLarge)
                        .padding(top = JellyfinDimens.SpaceSmall, bottom = JellyfinDimens.SpaceMedium)
                ) {
                    Text(
                        text = "مقترح فوري للمشاهدة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextSecondary,
                        modifier = Modifier.padding(bottom = JellyfinDimens.SpaceSmall)
                    )

                    DynamicSpotlightCard(
                        item = spotlightItem,
                        onPlayClick = { onQuickPlayClick(spotlightItem.id) },
                        onDetailsClick = { onDetailsClick(spotlightItem.id) }
                    )
                }
            }
        }

        // 2. Recent Search Queries Row
        if (recentQueries.isNotEmpty()) {
            item(key = "recent_queries_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                        items(items = recentQueries, key = { it }) { query ->
                            RecentQueryChip(
                                query = query,
                                onClick = { onQueryClick(query) },
                                onDelete = { onDeleteQuery(query) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                }
            }
        }

        // 3. Dynamic Server Genres Exploration
        if (availableGenres.isNotEmpty()) {
            item(key = "server_genres_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JellyfinDimens.SpaceLarge)
                ) {
                    Text(
                        text = "تصنيفات المكتبة المتاحة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        modifier = Modifier.padding(vertical = JellyfinDimens.SpaceSmall)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                    ) {
                        availableGenres.forEach { genre ->
                            DynamicGenreChip(
                                genre = genre,
                                onClick = { onGenreClick(genre) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicSpotlightCard(
    item: MediaItemSummary,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = JellyfinApiService.buildPrimaryImageUrl(
        itemId = item.id,
        maxWidth = 400,
        maxHeight = 600
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .clickable(onClick = onDetailsClick)
            .padding(JellyfinDimens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster (2:3 aspect ratio)
        Box(
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                .background(JellyfinColors.SurfaceElevated)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))

        // Metadata & Actions
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Year & Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                if (item.productionYear != null) {
                    Text(
                        text = item.productionYear.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = JellyfinColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (item.communityRating != null) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = JellyfinColors.AmberWarm,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", item.communityRating),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.AmberWarm
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
            ) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JellyfinColors.AccentCyan,
                        contentColor = JellyfinColors.PureBlack
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusPill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تشغيل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusPill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تفاصيل",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextPrimary
                    )
                }
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
            .defaultMinSize(minHeight = 40.dp)
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusPill
            )
            .clickable(role = Role.Button, onClick = onClick)
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
                .size(32.dp)
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
private fun DynamicGenreChip(
    genre: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
            .background(JellyfinColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = JellyfinColors.BorderGlass,
                shape = RoundedCornerShape(JellyfinDimens.RadiusPill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = 8.dp)
            .semantics {
                role = Role.Button
                contentDescription = "تصنيف $genre"
            }
    ) {
        Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = JellyfinColors.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = genre,
            style = MaterialTheme.typography.bodySmall,
            color = JellyfinColors.TextPrimary
        )
    }
}
