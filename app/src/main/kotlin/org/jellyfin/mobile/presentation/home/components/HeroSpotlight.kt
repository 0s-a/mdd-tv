package org.jellyfin.mobile.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * HeroSpotlight renders the enlarged, cinematic swipeable spotlight banner (440dp height).
 * Strictly adheres to docs/screens/01_HOME_DASHBOARD.md and Apple TV+ / Infuse immersion:
 * - Fluid HorizontalPager with auto-scroll and touch pause.
 * - Deep OLED True Black gradient overlay.
 * - Expanding pill indicator with spring animation.
 * - Glowing primary action button (min 48dp touch target).
 * - Full TalkBack screen reader accessibility.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroSpotlight(
    items: ImmutableList<MediaItemSummary>,
    currentIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onResumeClick: (String) -> Unit,
    onDetailsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, items.size - 1),
        pageCount = { items.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Notify parent on page changes to sync ambient glow
    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
    }

    // Smart Auto-Scroll every 6s (pauses during user swipe)
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(6000L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % items.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp) // Cinematic enlarged height for theater-grade immersion
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            val talkBackDesc = "العمل المميز: ${item.title}، تقييم ${item.communityRating ?: "غير متوفر"}، المدة ${item.durationText ?: ""}. انقر لعرض التفاصيل."

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. High-Resolution Backdrop Image
                AsyncImage(
                    model = item.backdropUrl ?: item.posterUrl,
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
                                colorStop(0.0f, JellyfinColors.PureBlack.copy(alpha = 0.55f)),
                                colorStop(0.28f, Color.Transparent),
                                colorStop(0.60f, JellyfinColors.Background.copy(alpha = 0.85f)),
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
                                    .padding(
                                        horizontal = JellyfinDimens.SpaceSmall,
                                        vertical = JellyfinDimens.SpaceExtraSmall
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = JellyfinColors.RatingGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.ROOT, "%.1f IMDb", item.communityRating),
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
                }
            }
        }

        // 4. Expanding Pill Indicator (Fluid Carousel Indicator)
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = JellyfinDimens.SpaceSmall),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PillWidth"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.TextSecondary.copy(alpha = 0.35f),
                        label = "PillColor"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(pillWidth)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "الانتقال للعمل المميز رقم ${index + 1}"
                            }
                    )
                }
            }
        }
    }
}

private fun colorStop(stop: Float, color: Color): Pair<Float, Color> = stop to color
