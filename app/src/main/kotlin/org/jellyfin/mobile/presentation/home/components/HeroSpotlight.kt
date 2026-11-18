package org.jellyfin.mobile.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * HeroSpotlight renders the cinematic 16:9 spotlight banner.
 * Strictly adheres to Section 2 & 3 of docs/screens/01_HOME_DASHBOARD.md:
 * - 16:9 Backdrop with OLED True Black gradient overlay.
 * - Prominent title, IMDb rating badge, duration, and technical badges.
 * - Glowing "استئناف المشاهدة" primary action button with min 48dp touch target.
 * - Glassmorphic "المزيد من التفاصيل" secondary action button.
 * - Carousel slide indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroSpotlight(
    item: MediaItemSummary,
    modifier: Modifier = Modifier,
    heroItemsCount: Int = 1,
    currentIndex: Int = 0,
    onSelectIndex: ((Int) -> Unit)? = null,
    onResumeClick: (String) -> Unit,
    onDetailsClick: (String) -> Unit
) {
    val talkBackDesc = "العمل المميز: ${item.title}، تقييم ${item.communityRating ?: "غير متوفر"}، المدة ${item.durationText ?: ""}. انقر لعرض التفاصيل."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDesc
            }
    ) {
        // 1. Backdrop Image
        AsyncImage(
            model = item.backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. OLED True Dark & Top Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStop(0.0f, JellyfinColors.PureBlack.copy(alpha = 0.5f)),
                        colorStop(0.35f, Color.Transparent),
                        colorStop(0.70f, JellyfinColors.Background.copy(alpha = 0.85f)),
                        colorStop(1.0f, JellyfinColors.Background)
                    )
                )
        )

        // 3. Content Details Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = JellyfinDimens.SpaceLarge,
                    vertical = JellyfinDimens.SpaceMedium
                ),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Badges Row (IMDb Rating + Metadata + Tech Badges)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = JellyfinDimens.SpaceSmall)
            ) {
                // IMDb Rating Chip
                if (item.communityRating != null && item.communityRating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .glassmorphism(
                                backgroundColor = JellyfinColors.SurfaceGlass,
                                borderColor = JellyfinColors.BorderGlass,
                                cornerRadius = JellyfinDimens.RadiusSmall
                            )
                            .padding(horizontal = JellyfinDimens.SpaceSmall, vertical = JellyfinDimens.SpaceExtraSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = JellyfinColors.RatingGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f IMDb", item.communityRating),
                            style = MaterialTheme.typography.labelSmall,
                            color = JellyfinColors.RatingGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Production Year
                if (item.productionYear != null) {
                    Text(
                        text = item.productionYear.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                // Duration Text
                if (!item.durationText.isNullOrBlank()) {
                    Text(
                        text = "• ${item.durationText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                // Tech Badges (4K UHD, Dolby Atmos, Direct Play)
                item.qualityBadges.forEach { badge ->
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TechSky,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .glassmorphism(
                                backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.6f),
                                borderColor = JellyfinColors.BorderGlass,
                                cornerRadius = JellyfinDimens.RadiusSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = JellyfinDimens.SpaceMedium)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button: استئناف المشاهدة / تشغيل
                Button(
                    onClick = { onResumeClick(item.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JellyfinColors.PrimaryIndigo,
                        contentColor = JellyfinColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                    modifier = Modifier
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .shadow(
                            elevation = JellyfinDimens.ElevationMedium,
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                            ambientColor = JellyfinColors.IndigoGlow,
                            spotColor = JellyfinColors.PrimaryIndigo
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "استئناف مشاهدة ${item.title}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                    Text(
                        text = "استئناف المشاهدة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Action Button: المزيد من التفاصيل
                OutlinedButton(
                    onClick = { onDetailsClick(item.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = JellyfinColors.SurfaceGlass,
                        contentColor = JellyfinColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                    modifier = Modifier
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .border(
                            width = JellyfinDimens.BorderThin,
                            color = JellyfinColors.BorderGlass,
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "المزيد من التفاصيل حول ${item.title}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                    Text(
                        text = "المزيد من التفاصيل",
                        style = MaterialTheme.typography.labelLarge,
                        color = JellyfinColors.TextPrimary
                    )
                }
            }

            // Carousel Indicators (if multiple hero items)
            if (heroItemsCount > 1) {
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(heroItemsCount) { index ->
                        val isSelected = index == currentIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) JellyfinColors.PrimaryIndigo
                                    else JellyfinColors.TextSecondary.copy(alpha = 0.4f)
                                )
                                .clickable(
                                    enabled = onSelectIndex != null,
                                    onClick = { onSelectIndex?.invoke(index) }
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun colorStop(stop: Float, color: Color): Pair<Float, Color> = stop to color
