package org.jellyfin.mobile.presentation.downloads.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.DownloadedMediaItem
import org.jellyfin.mobile.domain.model.DownloadsTab
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * DownloadedMediaTabs displays tabbed offline media lists (Movies, Series, Storage Management).
 * Strictly adheres to 06_DOWNLOADS_OFFLINE.md Section 2:
 * - Tabs: Movies, Series, Storage.
 * - Downloaded cards with instant offline play and delete buttons (>= 48dp touch targets).
 * - Arabic TalkBack accessibility semantics.
 */
@Composable
fun DownloadedMediaTabs(
    selectedTab: DownloadsTab,
    onTabSelected: (DownloadsTab) -> Unit,
    items: ImmutableList<DownloadedMediaItem>,
    onPlayOfflineClick: (String) -> Unit,
    onDeleteItemClick: (String) -> Unit,
    onDeleteAllWatchedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Horizontal Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            val tabs = listOf(DownloadsTab.MOVIES, DownloadsTab.SERIES, DownloadsTab.STORAGE)
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.BorderGlass,
                    label = "DownloadsTabBorder"
                )
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f) else JellyfinColors.SurfaceGlass,
                    label = "DownloadsTabBg"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .glassmorphism(
                            backgroundColor = backgroundColor,
                            borderColor = borderColor,
                            cornerRadius = JellyfinDimens.RadiusMedium,
                            borderWidth = if (isSelected) JellyfinDimens.BorderMedium else JellyfinDimens.BorderThin
                        )
                        .clickable(
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = JellyfinDimens.SpaceSmall, vertical = JellyfinDimens.SpaceSmall)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = "تبويب ${tab.title}"
                        }
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

        // 2. Tab Content
        if (selectedTab == DownloadsTab.STORAGE) {
            StorageManagementTabContent(
                itemsCount = items.size,
                onDeleteAllWatched = onDeleteAllWatchedClick,
                modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
            )
        } else {
            val filteredItems = when (selectedTab) {
                DownloadsTab.MOVIES -> items.filterNot { it.isSeries }
                DownloadsTab.SERIES -> items.filter { it.isSeries }
                else -> items
            }

            if (filteredItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(JellyfinDimens.Space2XL)
                ) {
                    Text(
                        text = "لا توجد ملفات محملة في هذا القسم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JellyfinColors.TextMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JellyfinDimens.SpaceLarge),
                    verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
                ) {
                    filteredItems.forEach { item ->
                        DownloadedMediaCard(
                            item = item,
                            onPlayOffline = { onPlayOfflineClick(item.id) },
                            onDelete = { onDeleteItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedMediaCard(
    item: DownloadedMediaItem,
    onPlayOffline: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "${item.title}، الحجم ${String.format("%.1f", item.fileSizeGb)} جيجابايت، جاهز للمشاهدة بدون إنترنت."

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
                // 2:3 Poster Frame
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        .background(JellyfinColors.Background)
                ) {
                    AsyncImage(
                        model = item.posterUrl,
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
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val metaString = buildString {
                        append(item.resolution)
                        if (!item.audioTrack.isNullOrBlank()) append(" • ${item.audioTrack}")
                        append(" • ${String.format("%.1f GB", item.fileSizeGb)}")
                    }

                    Text(
                        text = metaString,
                        style = MaterialTheme.typography.bodySmall,
                        color = JellyfinColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownloadDone,
                            contentDescription = null,
                            tint = JellyfinColors.SuccessEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "جاهز للمشاهدة بدون إنترنت",
                            style = MaterialTheme.typography.labelSmall,
                            color = JellyfinColors.SuccessEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
            HorizontalDivider(color = JellyfinColors.SurfaceDark)
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
            ) {
                Button(
                    onClick = onPlayOffline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JellyfinColors.PrimaryIndigo,
                        contentColor = JellyfinColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "تشغيل ${item.title} بدون اتصال بالإنترنت"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تشغيل أوفلاين", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "حذف ملف ${item.title} من ذاكرة الهاتف"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "حذف الملف",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageManagementTabContent(
    itemsCount: Int,
    onDeleteAllWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .padding(JellyfinDimens.SpaceLarge)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "إدارة المساحة والتخزين",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = "إجمالي الملفات المحملة حالياً: $itemsCount ملفات. يمكنك تحرير مساحة الهاتف بحذف الأعمال التي تمت مشاهدتها.",
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

            Button(
                onClick = onDeleteAllWatched,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.SurfaceGlass,
                    contentColor = JellyfinColors.TextPrimary
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = JellyfinColors.RatingGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حذف كافة الأعمال التي تمت مشاهدتها",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
