package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.presentation.home.components.RecentlyAddedRow
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * SearchResultsView displays structured multi-domain search results.
 * Strictly adheres to 05_SEARCH_AND_EXPLORE.md Section 2 (ACTIVE RESULTS):
 * - Top Match Hero Card.
 * - Matching Movies & Series row (2:3 posters).
 * - Matching Cast & Crew row (circular avatars).
 */
@Composable
fun SearchResultsView(
    topMatch: MediaItemSummary?,
    mediaResults: ImmutableList<MediaItemSummary>,
    peopleResults: ImmutableList<CastMember>,
    onMediaClick: (String) -> Unit,
    onQuickPlayClick: (String) -> Unit,
    onPersonClick: (CastMember) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // Clearance for floating bottom bar
    ) {
        // 1. Top Match Hero Card
        if (topMatch != null) {
            item(key = "top_match_header") {
                Text(
                    text = "النتيجة الأبرز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = JellyfinDimens.SpaceLarge,
                        vertical = JellyfinDimens.SpaceSmall
                    )
                )
            }

            item(key = "top_match_card") {
                TopMatchHeroCard(
                    item = topMatch,
                    onCardClick = { onMediaClick(topMatch.id) },
                    onPlayClick = { onQuickPlayClick(topMatch.id) },
                    onDetailsClick = { onMediaClick(topMatch.id) },
                    modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            }
        }

        // 2. Movies & Series matching row
        if (mediaResults.isNotEmpty()) {
            item(key = "media_results_row") {
                RecentlyAddedRow(
                    items = mediaResults,
                    onItemClick = onMediaClick,
                    title = "أفلام ومسلسلات مطابقة"
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            }
        }

        // 3. Cast & People matching row
        if (peopleResults.isNotEmpty()) {
            item(key = "people_results_header") {
                Text(
                    text = "طاقم العمل والممثلون",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = JellyfinDimens.SpaceLarge,
                        vertical = JellyfinDimens.SpaceSmall
                    )
                )
            }

            item(key = "people_results_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
                    horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = peopleResults,
                        key = { it.id }
                    ) { person ->
                        PersonResultItem(
                            person = person,
                            onClick = { onPersonClick(person) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopMatchHeroCard(
    item: MediaItemSummary,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "النتيجة الأبرز: ${item.title}، تقييم ${item.communityRating ?: ""}."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .clickable(
                role = Role.Button,
                onClick = onCardClick
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
        ) {
            // 2:3 Poster Thumbnail
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                    .background(JellyfinColors.Background)
            ) {
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.communityRating != null && item.communityRating > 0f) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = JellyfinColors.RatingGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.communityRating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.RatingGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.productionYear != null) {
                            Text(
                                text = item.productionYear.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = JellyfinColors.TextSecondary
                            )
                        }

                        if (!item.durationText.isNullOrBlank()) {
                            Text(
                                text = " • ${item.durationText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = JellyfinColors.TextSecondary
                            )
                        }
                    }

                    if (item.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.genres.take(2).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = JellyfinColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                ) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JellyfinColors.PrimaryIndigo,
                            contentColor = JellyfinColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تشغيل", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onDetailsClick,
                        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = JellyfinColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "التفاصيل", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonResultItem(
    person: CastMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = person.role ?: "طاقم العمل"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${person.name}، $subtitle"
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(JellyfinColors.SurfaceDark)
        ) {
            if (!person.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = person.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = person.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = JellyfinColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
