package org.jellyfin.mobile.presentation.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * Creates an animated linear gradient shimmer brush for dark OLED screens.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -800f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            JellyfinColors.SurfaceDark.copy(alpha = 0.5f),
            JellyfinColors.SurfaceGlass.copy(alpha = 0.85f),
            JellyfinColors.SurfaceDark.copy(alpha = 0.5f)
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )
}

/**
 * HomeShimmerSkeleton provides a cinematic skeleton placeholder while Home dashboard data is loading.
 * Eliminates generic spinning progress indicators to provide perceived instantaneous load times.
 */
@Composable
fun HomeShimmerSkeleton(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinColors.PureBlack)
    ) {
        // 1. Hero Spotlight Skeleton (440dp matching new enlarged Hero)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(shimmerBrush)
        ) {
            // Hero details placeholder at bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(JellyfinDimens.SpaceLarge),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Badges placeholder
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        .background(JellyfinColors.SurfaceDark)
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                // Title placeholder
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        .background(JellyfinColors.SurfaceDark)
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                // Action buttons placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .background(JellyfinColors.SurfaceDark)
                    )
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .background(JellyfinColors.SurfaceDark)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

        // 2. Library Capsules Row Placeholder
        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusLarge))
                        .background(shimmerBrush)
                )
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

        // 3. Continue Watching Placeholder (16:9 Cards)
        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
        ) {
            items(2) {
                Column(modifier = Modifier.width(260.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

        // 4. Recently Added Posters Placeholder (2:3 Posters)
        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
        ) {
            items(3) {
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                        .background(shimmerBrush)
                )
            }
        }
    }
}
