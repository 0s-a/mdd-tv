package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.jellyfin.mobile.ui.components.adaptiveBlur
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * AdaptiveBackdropHeader provides a constrained 220dp cinematic header with dual scrims
 * and intelligent artwork fallback.
 *
 * Rules:
 * 1. Height is strictly bounded at 220dp to preserve above-the-fold visibility of primary actions.
 * 2. If a wide backdrop is missing, it gracefully renders the vertical poster with adaptive blur
 *    that performs smoothly across all Android versions (Android 7.0 to 15+).
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
