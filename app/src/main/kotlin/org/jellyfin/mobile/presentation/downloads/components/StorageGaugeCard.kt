package org.jellyfin.mobile.presentation.downloads.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.domain.model.StorageGaugeInfo
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * StorageGaugeCard renders a frosted glass internal storage breakdown gauge.
 * Strictly adheres to 06_DOWNLOADS_OFFLINE.md Section 2:
 * - Multi-segment storage bar: Jellyfin used (PrimaryIndigo), System/Apps (TechSky), Free space (Muted).
 * - Clear metrics breakdown in gigabytes.
 * - Arabic TalkBack semantics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageGaugeCard(
    storageInfo: StorageGaugeInfo,
    modifier: Modifier = Modifier
) {
    val totalGb = storageInfo.totalGb.coerceAtLeast(1.0)
    val appRatio = (storageInfo.appUsedGb / totalGb).toFloat().coerceIn(0f, 1f)
    val otherRatio = (storageInfo.otherUsedGb / totalGb).toFloat().coerceIn(0f, 1f)
    val freeRatio = (storageInfo.freeGb / totalGb).toFloat().coerceIn(0f, 1f)

    val usedTotalGb = storageInfo.appUsedGb + storageInfo.otherUsedGb
    val talkBackDesc = "مقياس التخزين: مساحة Jellyfin المستهلكة ${String.format("%.1f", storageInfo.appUsedGb)} جيجابايت، المساحة الفارغة ${String.format("%.1f", storageInfo.freeGb)} جيجابايت من إجمالي ${String.format("%.1f", storageInfo.totalGb)} جيجابايت"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceGlass,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDesc
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                    Text(
                        text = "وحدة التخزين الداخلية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary
                    )
                }

                Text(
                    text = "${String.format("%.1f", usedTotalGb)} GB من ${String.format("%.0f", totalGb)} GB",
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // 2. Multi-Segment Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(JellyfinColors.SurfaceDark)
            ) {
                if (appRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(appRatio.coerceAtLeast(0.01f))
                            .height(8.dp)
                            .background(JellyfinColors.PrimaryIndigo)
                    )
                }
                if (otherRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(otherRatio.coerceAtLeast(0.01f))
                            .height(8.dp)
                            .background(JellyfinColors.TechSky)
                    )
                }
                if (freeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(freeRatio.coerceAtLeast(0.01f))
                            .height(8.dp)
                            .background(JellyfinColors.BorderGlass.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // 3. Legend Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                StorageLegendItem(
                    color = JellyfinColors.PrimaryIndigo,
                    label = "Jellyfin",
                    value = String.format("%.1f GB", storageInfo.appUsedGb)
                )
                StorageLegendItem(
                    color = JellyfinColors.TechSky,
                    label = "النظام والتطبيقات",
                    value = String.format("%.1f GB", storageInfo.otherUsedGb)
                )
                StorageLegendItem(
                    color = JellyfinColors.TextMuted,
                    label = "مساحة فارغة",
                    value = String.format("%.1f GB", storageInfo.freeGb)
                )
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            color = JellyfinColors.TextSecondary
        )
    }
}
