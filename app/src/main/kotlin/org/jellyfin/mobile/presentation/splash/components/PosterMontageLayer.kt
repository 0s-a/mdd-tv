package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.splash.model.SplashPoster
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

private val PosterCinematicThemes = listOf(
    Color(0xFF881337) to Color(0xFF1C1917),
    Color(0xFF701A75) to Color(0xFF0F172A),
    Color(0xFF0369A1) to Color(0xFF020617),
    Color(0xFFB45309) to Color(0xFF18181B),
    Color(0xFF3730A3) to Color(0xFF09090B),
    Color(0xFF334155) to Color(0xFF030712),
    Color(0xFF047857) to Color(0xFF14532D),
    Color(0xFF1E3A8A) to Color(0xFF082F49),
    Color(0xFF0D9488) to Color(0xFF134E4A),
    Color(0xFFD97706) to Color(0xFF451A03),
    Color(0xFF65A30D) to Color(0xFF27272A),
    Color(0xFF991B1B) to Color(0xFF1C1917),
    Color(0xFFC2410C) to Color(0xFF431407),
    Color(0xFF6D28D9) to Color(0xFF1E1B4B),
    Color(0xFF0284C7) to Color(0xFF064E3B),
    Color(0xFF475569) to Color(0xFF090D16),
    Color(0xFFEA580C) to Color(0xFF18181B),
    Color(0xFF78350F) to Color(0xFF0F172A)
)

@Composable
fun PosterMontageLayer(
    posters: List<SplashPoster>,
    onMontageComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (posters.isEmpty()) {
        LaunchedEffect(Unit) {
            onMontageComplete()
        }
        return
    }

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val totalCuts = minOf(6, posters.size)

    LaunchedEffect(Unit) {
        // Pre-warm the image pipeline for initial cards to avoid blank frames
        for (idx in 0 until totalCuts) {
            val req = ImageRequest.Builder(context)
                .data(posters[idx].imageUrl)
                .memoryCacheKey(posters[idx].imageUrl)
                .diskCacheKey(posters[idx].imageUrl)
                .build()
            context.imageLoader.enqueue(req)
        }

        // Filmic pacing with deliberate transitions avoiding rapid strobing
        val cutDurations = listOf(350L, 300L, 270L, 240L, 220L, 200L)
        for (i in 0 until totalCuts) {
            val duration = cutDurations.getOrElse(i) { 200L }
            currentIndex = i
            delay(duration)
        }
        delay(120L)
        onMontageComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinPrimitives.BlackOled)
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 180, easing = LinearEasing)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 180, easing = LinearEasing))
            },
            modifier = Modifier.fillMaxSize(),
            label = "poster_cut_transition"
        ) { index ->
            val poster = posters[index % posters.size]
            val (accentColor, darkBase) = PosterCinematicThemes[index % PosterCinematicThemes.size]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.40f),
                                darkBase.copy(alpha = 0.80f),
                                JellyfinPrimitives.BlackOled
                            ),
                            radius = 1200f
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(poster.imageUrl)
                        .crossfade(false)
                        .memoryCacheKey(poster.imageUrl)
                        .diskCacheKey(poster.imageUrl)
                        .build(),
                    contentDescription = poster.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic Vignette Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.65f),
                                0.35f to Color.Transparent,
                                0.70f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )

                // Poster Dynamic Info Overlay at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                JellyfinPrimitives.Graphite900.copy(alpha = 0.85f),
                                RoundedCornerShape(JellyfinDimens.RadiusMicro)
                            )
                            .border(
                                JellyfinDimens.BorderHairline,
                                JellyfinColors.AccentCyan.copy(alpha = 0.40f),
                                RoundedCornerShape(JellyfinDimens.RadiusMicro)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = poster.badge,
                            color = JellyfinColors.AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = poster.title,
                        color = JellyfinPrimitives.WhitePure,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
