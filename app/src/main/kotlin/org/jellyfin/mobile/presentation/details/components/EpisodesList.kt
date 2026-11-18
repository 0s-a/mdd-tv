package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.EpisodeDomainModel
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * EpisodesList renders the vertical series episodes items.
 * Strictly adheres to 03_MEDIA_DETAILS.md Section 9.3.
 */
@Composable
fun EpisodesList(
    episodes: ImmutableList<EpisodeDomainModel>,
    onEpisodeClick: (String) -> Unit,
    onToggleWatched: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (episodes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge)
    ) {
        episodes.forEachIndexed { index, episode ->
            EpisodeItemRow(
                episode = episode,
                onItemClick = { onEpisodeClick(episode.id) },
                onToggleWatched = { onToggleWatched(episode.id) },
                onDownloadClick = { onDownloadClick(episode.id) }
            )

            if (index < episodes.lastIndex) {
                HorizontalDivider(
                    color = JellyfinColors.SurfaceDark,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = JellyfinDimens.SpaceSmall)
                )
            }
        }
    }
}

@Composable
private fun EpisodeItemRow(
    episode: EpisodeDomainModel,
    onItemClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "الحلقة ${episode.episodeNumber}: ${episode.title}، ${episode.runtimeFormatted ?: ""}، ${if (episode.isWatched) "تمت المشاهدة" else "غير مشاهدة"}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .clickable(
                role = Role.Button,
                onClick = onItemClick
            )
            .padding(vertical = JellyfinDimens.SpaceExtraSmall)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            },
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
        verticalAlignment = Alignment.Top
    ) {
        // 1. 16:9 Episode Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                .background(JellyfinColors.SurfaceDark)
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JellyfinColors.PureBlack.copy(alpha = 0.25f))
            )

            // Watched or Play Icon
            if (episode.isWatched) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SuccessEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = JellyfinColors.PureBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.PrimaryIndigo.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress Bar
            if (episode.progressPercentage > 0f && !episode.isWatched) {
                LinearProgressIndicator(
                    progress = { episode.progressPercentage.coerceIn(0f, 1f) },
                    color = JellyfinColors.PrimaryIndigo,
                    trackColor = JellyfinColors.SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // 2. Details Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "E${episode.episodeNumber.toString().padStart(2, '0')} • ${episode.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!episode.runtimeFormatted.isNullOrBlank()) {
                Text(
                    text = episode.runtimeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TechSky
                )
            }

            if (episode.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Action Buttons (Toggle Watched + Download)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Watched Checkbox Icon Button (Min 48dp touch target)
            IconButton(
                onClick = onToggleWatched,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .semantics {
                        role = Role.Checkbox
                        contentDescription = if (episode.isWatched) "تحديد كغير مشاهد" else "تحديد كتمت المشاهدة"
                    }
            ) {
                Icon(
                    imageVector = if (episode.isWatched) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (episode.isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Download Icon Button (Min 48dp touch target)
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (episode.isDownloaded) "تم التنزيل" else "تنزيل الحلقة"
                    }
            ) {
                Icon(
                    imageVector = if (episode.isDownloaded) Icons.Rounded.FileDownloadDone else Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = if (episode.isDownloaded) JellyfinColors.TechSky else JellyfinColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
