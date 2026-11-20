package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import org.jellyfin.mobile.domain.model.EpisodeDomainModel
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * NextUpCard renders a prominent 16:9 hero card for the next series episode to watch.
 * Strictly adheres to 03_MEDIA_DETAILS.md Section 9.1.
 */
@Composable
fun NextUpCard(
    episode: EpisodeDomainModel,
    onResumeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "الحلقة التالية: الموسم ${episode.seasonNumber} الحلقة ${episode.episodeNumber}، ${episode.title}. انقر للتشغيل."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = JellyfinDimens.SpaceSmall)
        ) {
            Text(
                text = "الحلقة التالية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(
                    backgroundColor = JellyfinColors.SurfaceDark,
                    borderColor = JellyfinColors.BorderGlass,
                    cornerRadius = JellyfinDimens.RadiusDefault
                )
                .clickable(
                    role = Role.Button,
                    onClick = { onResumeClick(episode.id) }
                )
                .padding(JellyfinDimens.SpaceMedium)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = talkBackDesc
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 16:9 Episode Thumbnail with Play Overlay
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                        .background(JellyfinColors.Background)
                ) {
                    AsyncImage(
                        model = episode.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrim overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(JellyfinColors.PureBlack.copy(alpha = 0.35f))
                    )

                    // Play icon in center
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(JellyfinColors.PrimaryIndigo.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = JellyfinColors.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Progress bar if in progress
                    if (episode.progressPercentage > 0f) {
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

                // 2. Episode Information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "S${episode.seasonNumber.toString().padStart(2, '0')}E${episode.episodeNumber.toString().padStart(2, '0')} • ${episode.runtimeFormatted ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TechSky,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val overview = episode.overview.orEmpty()
                    if (overview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = JellyfinColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Resume / Play Button
                    Button(
                        onClick = { onResumeClick(episode.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JellyfinColors.PrimaryIndigo,
                            contentColor = JellyfinColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (episode.progressPercentage > 0f) "استئناف" else "تشغيل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
