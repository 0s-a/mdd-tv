package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.jellyfin.mobile.R
import org.jellyfin.mobile.ui.components.adaptiveBlur
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * AdaptiveBackdropHeader provides a cinematic header with dual scrims
 * and intelligent artwork fallback.
 *
 * Rules:
 * 1. Height is configurable (defaults to 220dp, expandable to 50% screen height in specialized screens).
 * 2. If a wide backdrop is missing, it renders a subtle moody backdrop and places the pure monochrome
 *    app icon floating cleanly in the center (without borders, frames, or capsules).
 * 3. Incorporates a top status bar scrim (48dp) and bottom vignette to blend into pure OLED black.
 */
@Composable
fun AdaptiveBackdropHeader(
    backdropUrl: String?,
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 220.dp,
    backgroundColor: Color = JellyfinColors.PureBlack
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
            .background(backgroundColor)
    ) {
        val hasBackdrop = !backdropUrl.isNullOrBlank()
        val imageModel = if (hasBackdrop) backdropUrl else posterUrl

        if (!imageModel.isNullOrBlank()) {
            AsyncImage(
                model = imageModel,
                contentDescription = "غلاف $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!hasBackdrop) {
                            Modifier.adaptiveBlur(radius = 50.dp, fallbackTint = backgroundColor.copy(alpha = 0.85f))
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // Fallback placeholder when a dedicated backdrop is missing:
        // Elegant monochrome app icon centered without borders, frames, or capsules
        if (!hasBackdrop) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle ambient radial glow behind the icon
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    JellyfinColors.RatingGold.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                    contentDescription = "أيقونة التطبيق - $title",
                    colorFilter = ColorFilter.tint(JellyfinPrimitives.Ivory50.copy(alpha = 0.35f)),
                    modifier = Modifier.size(76.dp)
                )
            }
        }

        // 1. Top Status Bar Protection Scrim (48dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Bottom Vignette fading smoothly into OLED Black
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.85f),
                            backgroundColor
                        )
                    )
                )
        )
    }
}
