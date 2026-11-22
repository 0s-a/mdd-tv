package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.splash.model.SplashPoster
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Cinema-grade ambient artwork backdrop layer.
 * Cycles slowly through catalog artwork with smooth crossfades and a balanced vignette
 * that blends completely into OLED Pure Black (#000000) towards edges and center.
 * Eliminates distortion, aspect ratio stretching, and hard cutoff lines.
 */
@Composable
fun CinematicBackdropLayer(
    posters: List<SplashPoster>,
    modifier: Modifier = Modifier
) {
    if (posters.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(posters.size) {
        while (true) {
            delay(5000L)
            currentIndex = (currentIndex + 1) % posters.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinPrimitives.BlackOled)
    ) {
        // Subtle ambient artwork crossfade
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 1400)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 1400))
            },
            modifier = Modifier.fillMaxSize(),
            label = "cinematic_backdrop_crossfade"
        ) { index ->
            val poster = posters[index % posters.size]
            val backdropTarget = poster.backdropUrl.ifBlank { poster.imageUrl }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropTarget)
                    .memoryCacheKey(backdropTarget)
                    .diskCacheKey(backdropTarget)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.18f)
            )
        }

        // Radial Vignette: Darkens center for brand contrast and fades outer perimeter to pure black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.0f to Color.Black.copy(alpha = 0.88f),
                        0.40f to Color.Black.copy(alpha = 0.25f),
                        0.75f to Color.Black.copy(alpha = 0.70f),
                        1.0f to Color.Black
                    )
                )
        )

        // Vertical Scrim: Ensures top and bottom seamlessly dissolve into OLED Pure Black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black,
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.0f to Color.Black
                    )
                )
        )

        // Symmetrical Horizontal Scrim: Guarantees zero hard edges and balanced RTL presentation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black,
                        0.12f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1.0f to Color.Black
                    )
                )
        )
    }
}
