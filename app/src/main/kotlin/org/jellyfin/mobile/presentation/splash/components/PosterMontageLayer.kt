package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.AnimatedContent
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

private val PosterCinematicThemes = listOf(
    Color(0xFF881337) to Color(0xFF1C1917), // Attack on Titan
    Color(0xFF701A75) to Color(0xFF0F172A), // Spider-Man
    Color(0xFF0369A1) to Color(0xFF020617), // Interstellar
    Color(0xFFB45309) to Color(0xFF18181B), // Demon Slayer
    Color(0xFF3730A3) to Color(0xFF09090B), // Avengers: Endgame
    Color(0xFF334155) to Color(0xFF030712), // The Dark Knight
    Color(0xFF047857) to Color(0xFF14532D), // Breaking Bad
    Color(0xFF1E3A8A) to Color(0xFF082F49), // Game of Thrones
    Color(0xFF0D9488) to Color(0xFF134E4A), // Spirited Away
    Color(0xFFD97706) to Color(0xFF451A03), // Dune
    Color(0xFF65A30D) to Color(0xFF27272A), // Fight Club
    Color(0xFF991B1B) to Color(0xFF1C1917), // Pulp Fiction
    Color(0xFFC2410C) to Color(0xFF431407), // Gladiator
    Color(0xFF6D28D9) to Color(0xFF1E1B4B), // Your Name
    Color(0xFF0284C7) to Color(0xFF064E3B), // Avatar
    Color(0xFF475569) to Color(0xFF090D16), // Inception
    Color(0xFFEA580C) to Color(0xFF18181B), // Oppenheimer
    Color(0xFF78350F) to Color(0xFF0F172A)  // The Shawshank Redemption
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
    val totalCuts = minOf(14, posters.size)

    LaunchedEffect(Unit) {
        // Pre-warm the image pipeline for initial cards
        for (idx in 0 until minOf(4, posters.size)) {
            val req = ImageRequest.Builder(context)
                .data(posters[idx].imageUrl)
                .build()
            context.imageLoader.enqueue(req)
        }

        for (i in 0 until totalCuts) {
            val stepDuration = maxOf(75L, 150L - (i * 6L))
            currentIndex = i
            delay(stepDuration)
        }
        delay(90L)
        onMontageComplete()
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 50)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 50))
        },
        modifier = modifier.fillMaxSize(),
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
                            accentColor.copy(alpha = 0.50f),
                            darkBase.copy(alpha = 0.85f),
                            Color(0xFF050811)
                        ),
                        radius = 1200f
                    )
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(poster.imageUrl)
                    .crossfade(true)
                    .memoryCacheKey(poster.imageUrl)
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
                            Color.Black.copy(alpha = 0.70f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFF38BDF8).copy(alpha = 0.40f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = poster.badge,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = poster.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
