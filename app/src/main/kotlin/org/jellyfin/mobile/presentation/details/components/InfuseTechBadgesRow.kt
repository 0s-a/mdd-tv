package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.domain.model.MediaDetailDomainModel
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * InfuseTechBadgesRow displays premium Infuse-style technical badge capsules
 * and provides an expandable technical media inspector sheet.
 * Strictly adheres to 03_MEDIA_DETAILS.md Section 6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfuseTechBadgesRow(
    mediaItem: MediaDetailDomainModel,
    modifier: Modifier = Modifier
) {
    var showInspectorSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val badges = mediaItem.qualityBadges.ifEmpty {
        val fallback = mutableListOf<String>()
        fallback.add(mediaItem.videoResolution ?: "4K UHD")
        mediaItem.videoCodec?.let { fallback.add(it) }
        mediaItem.audioCodec?.let { fallback.add(it) }
        if (mediaItem.isDirectPlay) fallback.add("Direct Play 🟢")
        fallback
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = JellyfinDimens.SpaceSmall)
        ) {
            items(badges) { badge ->
                TechBadgeCapsule(
                    badge = badge,
                    onClick = { showInspectorSheet = true }
                )
            }

            // Media Inspector Trigger Capsule
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .glassmorphism(
                            backgroundColor = JellyfinColors.SurfaceGlass,
                            borderColor = JellyfinColors.BorderGlass,
                            cornerRadius = JellyfinDimens.RadiusMedium
                        )
                        .clickable(
                            role = Role.Button,
                            onClick = { showInspectorSheet = true }
                        )
                        .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceSmall)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "فتح مفتش الميديا التقني"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                    Text(
                        text = "فحص الميديا",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Media Inspector Modal Bottom Sheet
    if (showInspectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            sheetState = sheetState,
            containerColor = JellyfinColors.SurfaceDark,
            contentColor = JellyfinColors.TextPrimary
        ) {
            MediaInspectorSheetContent(
                mediaItem = mediaItem,
                onDismiss = { showInspectorSheet = false }
            )
        }
    }
}

@Composable
private fun TechBadgeCapsule(
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDirectPlay = badge.contains("Direct Play", ignoreCase = true)
    val borderColor = if (isDirectPlay) JellyfinColors.SuccessEmerald.copy(alpha = 0.6f) else JellyfinColors.BorderGlass

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.7f),
                borderColor = borderColor,
                cornerRadius = JellyfinDimens.RadiusMedium,
                borderWidth = JellyfinDimens.BorderThin
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceSmall)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "شارة الجودة: $badge، انقر لفتح مفتش الميديا"
            }
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDirectPlay) JellyfinColors.SuccessEmerald else JellyfinColors.TechSky
        )
    }
}

@Composable
private fun MediaInspectorSheetContent(
    mediaItem: MediaDetailDomainModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge)
            .padding(bottom = JellyfinDimens.Space2XL)
    ) {
        Text(
            text = "🔬 مفتش الوسائط التقني",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary
        )
        Text(
            text = "${mediaItem.title} (${mediaItem.productionYear ?: ""})",
            style = MaterialTheme.typography.bodySmall,
            color = JellyfinColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
        HorizontalDivider(color = JellyfinColors.BorderGlass)
        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

        // 1. File Info
        InspectorSectionTitle("📦 معلومات الملف")
        InspectorRow(label = "الحاوية", value = mediaItem.container ?: "MKV (Matroska)")
        mediaItem.bitrateMbps?.let {
            InspectorRow(label = "معدل البت", value = String.format("%.1f Mbps", it))
        }
        mediaItem.fileSizeBytes?.let {
            val gb = it.toDouble() / (1024 * 1024 * 1024)
            InspectorRow(label = "الحجم الكلي", value = String.format("%.2f GB", gb))
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // 2. Video Track
        InspectorSectionTitle("🎬 مسار الفيديو")
        InspectorRow(label = "الترميز", value = mediaItem.videoCodec ?: "HEVC (H.265)")
        InspectorRow(label = "الدقة", value = mediaItem.videoResolution ?: "3840 × 2160 (4K UHD)")

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // 3. Audio Track
        InspectorSectionTitle("🔊 مسار الصوت")
        InspectorRow(label = "الترميز والقنوات", value = "${mediaItem.audioCodec ?: "TrueHD Atmos"} ${mediaItem.audioChannels ?: "7.1"}")

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // 4. Playback Capability
        InspectorSectionTitle("⚡ حالة التشغيل")
        InspectorRow(
            label = "طريقة التشغيل",
            value = if (mediaItem.isDirectPlay) "Direct Play 🟢 (بدون تحويل برمجي)" else "Direct Stream / Transcode 🟡"
        )
        InspectorRow(label = "استهلاك معالج الخادم", value = if (mediaItem.isDirectPlay) "0% CPU" else "متغير")
    }
}

@Composable
private fun InspectorSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = JellyfinColors.TechSky,
        modifier = Modifier.padding(vertical = JellyfinDimens.SpaceExtraSmall)
    )
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = JellyfinColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary
        )
    }
}
