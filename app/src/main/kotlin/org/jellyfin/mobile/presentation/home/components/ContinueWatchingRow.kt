package org.jellyfin.mobile.presentation.home.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import org.jellyfin.mobile.domain.model.ContinueWatchingItem
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * ContinueWatchingRow renders the 16:9 resume cards.
 * Strictly adheres to 01_HOME_DASHBOARD.md Section 2:
 * - Automatically hides when empty.
 * - 16:9 Thumbnail cards with quick play button.
 * - Accurate progress indicator with remaining minutes.
 * - Accessible >= 48dp touch targets and Arabic TalkBack semantics.
 */
@Composable
fun ContinueWatchingRow(
    items: ImmutableList<ContinueWatchingItem>,
    onItemClick: (String) -> Unit,
    onQuickPlayClick: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "متابعة المشاهدة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ContinueWatchingCard(
                    item = item,
                    onCardClick = { onItemClick(item.id) },
                    onQuickPlayClick = { onQuickPlayClick(item.id, item.resumePositionMs) }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onCardClick: () -> Unit,
    onQuickPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "استئناف مشاهدة ${item.title} من الدقيقة ${item.remainingMinutes}"

    Column(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .clickable(
                role = Role.Button,
                onClick = onCardClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            }
    ) {
        // 1. 16:9 Thumbnail Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .background(JellyfinColors.SurfaceDark)
        ) {
            AsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay for play button visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JellyfinColors.PureBlack.copy(alpha = 0.3f))
            )

            // Quick Play Button (Centered, min 48dp touch target)
            IconButton(
                onClick = onQuickPlayClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = JellyfinColors.PrimaryIndigo.copy(alpha = 0.85f),
                    contentColor = JellyfinColors.TextPrimary
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(JellyfinDimens.MinTouchTarget)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = "تشغيل فوري لـ ${item.title}"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Progress bar anchored to bottom of thumbnail
            LinearProgressIndicator(
                progress = item.progressPercentage.coerceIn(0f, 1f),
                color = JellyfinColors.PrimaryIndigo,
                trackColor = JellyfinColors.SurfaceDark.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // 2. Title & Metadata
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
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
            val progressPercentText = "${(item.progressPercentage * 100).toInt()}%"
            val remainingText = if (item.remainingMinutes > 0) {
                "متبقي ${item.remainingMinutes} دقيقة"
            } else {
                item.subtitle ?: ""
            }

            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = progressPercentText,
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TechSky,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
