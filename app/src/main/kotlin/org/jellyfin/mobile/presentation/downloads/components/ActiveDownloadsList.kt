package org.jellyfin.mobile.presentation.downloads.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.DownloadTaskProgress
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * ActiveDownloadsList renders ongoing background download tasks with transfer metrics.
 * Strictly adheres to 06_DOWNLOADS_OFFLINE.md Section 2:
 * - 16:9 Still, filename, total size, downloaded size, transfer speed, progress bar.
 * - Pause/Resume and Cancel buttons (>= 48dp touch target).
 * - Arabic TalkBack semantics.
 */
@Composable
fun ActiveDownloadsList(
    tasks: ImmutableList<DownloadTaskProgress>,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
        ) {
            Text(
                text = "قائمة التنزيل النشطة (${tasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
        }

        tasks.forEach { task ->
            ActiveDownloadTaskCard(
                task = task,
                onPause = { onPauseClick(task.mediaId) },
                onResume = { onResumeClick(task.mediaId) },
                onCancel = { onCancelClick(task.mediaId) },
                modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
            )
        }
    }
}

@Composable
private fun ActiveDownloadTaskCard(
    task: DownloadTaskProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentInt = (task.progressPercentage * 100).toInt()
    val talkBackDesc = "جارِ تنزيل ${task.title}، نسبة الإنجاز $percentInt بالمئة، السرعة ${String.format("%.1f", task.speedMbPerSec)} ميجابايت بالثانية"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDesc
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
            ) {
                // 16:9 Thumbnail
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        .background(JellyfinColors.Background)
                ) {
                    AsyncImage(
                        model = task.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${String.format("%.1f", task.downloadedGb)} GB من ${String.format("%.1f", task.totalGb)} GB ($percentInt%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = JellyfinColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val speedText = if (task.isPaused) {
                        "متوقف مؤقتاً"
                    } else {
                        "⚡ ${String.format("%.1f", task.speedMbPerSec)} MB/s · متبقي ${task.remainingSeconds} ثانية"
                    }

                    Text(
                        text = speedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isPaused) JellyfinColors.RatingGold else JellyfinColors.TechSky,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // Progress Bar
            LinearProgressIndicator(
                progress = task.progressPercentage,
                color = if (task.isPaused) JellyfinColors.RatingGold else JellyfinColors.PrimaryIndigo,
                trackColor = JellyfinColors.SurfaceDark.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
            ) {
                // Pause / Resume Button (min 48dp touch target)
                Button(
                    onClick = if (task.isPaused) onResume else onPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (task.isPaused) JellyfinColors.PrimaryIndigo else JellyfinColors.SurfaceGlass,
                        contentColor = JellyfinColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (task.isPaused) "استئناف تنزيل ${task.title}" else "إيقاف مؤقت لتنزيل ${task.title}"
                        }
                ) {
                    Icon(
                        imageVector = if (task.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.isPaused) "استئناف" else "إيقاف مؤقت",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Cancel Button (min 48dp touch target)
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "إلغاء تنزيل ${task.title}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }
        }
    }
}
