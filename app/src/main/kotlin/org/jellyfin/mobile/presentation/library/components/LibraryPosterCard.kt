package org.jellyfin.mobile.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * LibraryPosterCard renders a 2:3 poster item within the hierarchical catalog grid.
 * Adheres strictly to Obsidian 2.2.0 design tokens:
 * - 2:3 aspect ratio with graceful fallback and placeholder layer.
 * - Glowing cinema icon and title text when artwork is missing or loading.
 * - Tech badge overlay on top-start.
 * - Watched indicator on top-end.
 * - TalkBack semantics and 48dp minimum click target.
 */
@Composable
fun LibraryPosterCard(
    item: MediaItemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quality = item.qualityBadges.firstOrNull() ?: "HD"
    val watchedStateText = if (item.isWatched) "مشاهد" else "غير مشاهد"
    val talkBackDesc = "${item.title}، ${item.productionYear ?: ""}، جودة $quality، حالة المشاهدة: $watchedStateText."
    val shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = JellyfinDimens.MinTouchTarget, minHeight = JellyfinDimens.MinTouchTarget)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            }
    ) {
        // 1. 2:3 Poster Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JellyfinColors.SurfaceDark,
                            JellyfinColors.SurfaceGlass
                        )
                    )
                )
                .border(1.dp, JellyfinColors.BorderGlass, shape)
        ) {
            // Fallback / Placeholder Layer (visible while loading or when artwork is absent)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                JellyfinColors.PrimaryIndigo.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(JellyfinDimens.SpaceSmall),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, JellyfinColors.PrimaryIndigo.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val placeholderIcon = if (item.isSeries) {
                            Icons.Rounded.Tv
                        } else {
                            Icons.Rounded.Movie
                        }
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = JellyfinColors.TechSky,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.sp
                        ),
                        color = JellyfinColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Image Layer
            val imageModel = item.posterUrl?.takeIf { it.isNotBlank() } ?: item.backdropUrl?.takeIf { it.isNotBlank() }
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top-Start Tech Badge Overlay (e.g. 4K UHD, HDR)
            if (item.qualityBadges.isNotEmpty()) {
                Text(
                    text = item.qualityBadges.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TechSky,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .glassmorphism(
                            backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.85f),
                            borderColor = JellyfinColors.BorderGlass,
                            cornerRadius = JellyfinDimens.RadiusSmall
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Top-End Watched Status Indicator
            if (item.isWatched) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SuccessEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = JellyfinColors.PureBlack,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Title & Metadata
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Production Year
            if (item.productionYear != null) {
                Text(
                    text = item.productionYear.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TextSecondary
                )
            }

            // Rating
            if (item.communityRating != null && item.communityRating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = JellyfinColors.RatingGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", item.communityRating),
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.RatingGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
